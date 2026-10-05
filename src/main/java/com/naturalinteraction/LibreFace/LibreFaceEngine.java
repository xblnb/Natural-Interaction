package com.naturalinteraction.LibreFace;

import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class LibreFaceEngine implements AutoCloseable {

    private static final String ENGINE_NAME = "OnnxRuntime";
    private static final int QUEUE_CAPACITY = 1;
    private static final long WORKER_POLL_MS = 100L;

    private final LibreFaceConfig config;
    private final LibreFacePreprocessor preprocessor = new LibreFacePreprocessor();
    private final List<LibreFaceListener> listeners = new CopyOnWriteArrayList<>();
    private final LinkedBlockingQueue<PendingRequest> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final Object inferenceLock = new Object();

    private ZooModel<NDList, NDList> encoderModel;
    private ZooModel<NDList, NDList> intensityModel;
    private ZooModel<NDList, NDList> presenceModel;
    private ZooModel<NDList, NDList> expressionModel;
    private Predictor<NDList, NDList> encoderPredictor;
    private Predictor<NDList, NDList> intensityPredictor;
    private Predictor<NDList, NDList> presencePredictor;
    private Predictor<NDList, NDList> expressionPredictor;

    private volatile boolean started;
    private volatile boolean ready;
    private volatile boolean closed;
    private volatile boolean workerRunning;
    private volatile Thread worker;
    private volatile String lastError = "";
    private volatile String status = "idle";
    private volatile LibreFaceResult latestResult;
    private volatile long submittedFrames;
    private volatile long analyzedFrames;
    private volatile long droppedFrames;
    private volatile long lastInferenceMs;
    private volatile double averageInferenceMs;

    private static final class PendingRequest {
        private final BufferedImage crop;
        private final long timestamp;

        private PendingRequest(BufferedImage crop, long timestamp) {
            this.crop = crop;
            this.timestamp = timestamp;
        }
    }

    public LibreFaceEngine(LibreFaceConfig config) {
        this.config = config;
    }

    public boolean start() {
        if (started) {
            return ready;
        }
        synchronized (inferenceLock) {
            if (closed) {
                return false;
            }
            return startLocked();
        }
    }

    private boolean startLocked() {
        started = true;
        lastError = "";
        status = "starting";

        List<String> missing = config.findMissingModels();
        if (!missing.isEmpty()) {
            lastError = "LibreFace models missing: " + String.join(", ", missing);
            status = "models missing";
            started = false;
            notifyError(lastError);
            return false;
        }

        Path directory = config.resolveModelDirectory();
        long loadStarted = System.nanoTime();
        try {
            if (config.runAuIntensity || config.runAuPresence) {
                encoderModel = load(directory.resolve(LibreFaceModel.AU_ENCODER.getFileName()));
                encoderPredictor = encoderModel.newPredictor();
            }
            if (config.runAuIntensity) {
                intensityModel = load(directory.resolve(LibreFaceModel.AU_INTENSITY.getFileName()));
                intensityPredictor = intensityModel.newPredictor();
            }
            if (config.runAuPresence) {
                presenceModel = load(directory.resolve(LibreFaceModel.AU_PRESENCE.getFileName()));
                presencePredictor = presenceModel.newPredictor();
            }
            if (config.runFacialExpression) {
                expressionModel = load(directory.resolve(LibreFaceModel.FACIAL_EXPRESSION.getFileName()));
                expressionPredictor = expressionModel.newPredictor();
            }
        } catch (Throwable t) {
            lastError = "LibreFace model load failed: " + t;
            status = "load failed";
            notifyError(lastError);
            closeModelsLocked();
            started = false;
            return false;
        }

        ready = true;
        workerRunning = true;
        Thread thread = new Thread(this::workerLoop, "LibreFace-Inference");
        thread.setDaemon(true);
        worker = thread;
        thread.start();
        long loadMs = (System.nanoTime() - loadStarted) / 1_000_000L;
        status = "ready (" + directory.getFileName() + ", " + describeLoadedModels()
                + ", load " + loadMs + " ms)";
        return true;
    }

    private ZooModel<NDList, NDList> load(Path modelPath) throws Exception {
        Criteria<NDList, NDList> criteria = Criteria.builder()
                .setTypes(NDList.class, NDList.class)
                .optModelPath(modelPath)
                .optEngine(ENGINE_NAME)
                .build();
        return criteria.loadModel();
    }

    private String describeLoadedModels() {
        List<String> names = new ArrayList<>();
        if (intensityPredictor != null) {
            names.add("AU_intensity");
        }
        if (presencePredictor != null) {
            names.add("AU_presence");
        }
        if (expressionPredictor != null) {
            names.add("FE");
        }
        return names.isEmpty() ? "no heads" : String.join("+", names);
    }

    private void workerLoop() {
        while (workerRunning) {
            PendingRequest request;
            try {
                request = queue.poll(WORKER_POLL_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (request == null) {
                continue;
            }
            try {
                LibreFaceResult result = analyzeCrop(request.crop, request.timestamp);
                if (result != null) {
                    latestResult = result;
                    analyzedFrames++;
                    for (LibreFaceListener listener : listeners) {
                        try {
                            listener.onResult(result);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable t) {
                lastError = "LibreFace inference failed: " + t;
                notifyError(lastError);
            }
        }
    }

    public void submit(BufferedImage faceCrop, long timestamp) {
        if (!ready || faceCrop == null) {
            return;
        }
        submittedFrames++;
        PendingRequest request = new PendingRequest(faceCrop, timestamp);
        if (queue.offer(request)) {
            return;
        }
        if (queue.poll() != null) {
            droppedFrames++;
        }
        queue.offer(request);
    }

    public LibreFaceResult analyze(BufferedImage frame, Rectangle2D faceRect, long timestamp) {
        BufferedImage crop = preprocessor.cropFace(frame, faceRect, config.cropPadding,
                config.cropOffsetY, config.inputSize);
        return analyzeCrop(crop, timestamp);
    }

    public LibreFaceResult analyzeCrop(BufferedImage crop, long timestamp) {
        if (!ready || crop == null) {
            return null;
        }
        float[] tensor = preprocessor.toChw(crop);
        long started = System.nanoTime();
        synchronized (inferenceLock) {
            try (NDManager manager = NDManager.newBaseManager(ENGINE_NAME)) {
                float[] feature = null;
                if (encoderPredictor != null) {
                    NDArray image = manager.create(tensor,
                            new Shape(1, 3, config.inputSize, config.inputSize));
                    NDList encoderOutput = encoderPredictor.predict(new NDList(image));
                    feature = selectOutput(encoderOutput, LibreFaceModel.FEATURE_SIZE);
                }

                float[] rawIntensity = null;
                float[] presence = null;
                float[] expression = null;
                if (intensityPredictor != null && feature != null) {
                    rawIntensity = runFeatureHead(intensityPredictor, feature, manager,
                            LibreFaceModel.AU_COUNT);
                }
                if (presencePredictor != null && feature != null) {
                    presence = runFeatureHead(presencePredictor, feature, manager,
                            LibreFaceModel.AU_COUNT);
                }
                if (expressionPredictor != null) {
                    expression = runImageHead(expressionPredictor, tensor, manager,
                            LibreFaceModel.EXPRESSION_COUNT);
                }

                long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
                lastInferenceMs = elapsedMs;
                averageInferenceMs = averageInferenceMs == 0d
                        ? elapsedMs
                        : averageInferenceMs * 0.8d + elapsedMs * 0.2d;
                return new LibreFaceResult(timestamp, submittedFrames,
                        rawIntensity, scaleIntensity(rawIntensity),
                        presence == null ? null : presence.clone(),
                        softmax(expression), elapsedMs);
            } catch (Throwable t) {
                lastError = "LibreFace inference failed: " + t;
                notifyError(lastError);
                return null;
            }
        }
    }

    private float[] runFeatureHead(Predictor<NDList, NDList> predictor, float[] feature,
                                   NDManager manager, int expectedSize) throws TranslateException {
        NDArray featureArray = manager.create(feature, new Shape(1, feature.length));
        NDList output = predictor.predict(new NDList(featureArray));
        return selectOutput(output, expectedSize);
    }

    private float[] runImageHead(Predictor<NDList, NDList> predictor, float[] tensor,
                                 NDManager manager, int expectedSize) throws TranslateException {
        NDArray image = manager.create(tensor,
                new Shape(1, 3, config.inputSize, config.inputSize));
        NDList output = predictor.predict(new NDList(image));
        return selectOutput(output, expectedSize);
    }

    private static float[] selectOutput(NDList outputs, int expectedSize) {
        float[] smallest = null;
        for (NDArray array : outputs) {
            float[] values = array.toFloatArray();
            if (values.length == expectedSize) {
                return values;
            }
            if (smallest == null || values.length < smallest.length) {
                smallest = values;
            }
        }
        if (smallest == null) {
            throw new IllegalStateException("model returned no outputs");
        }
        return smallest;
    }

    private float[] scaleIntensity(float[] raw) {
        if (raw == null) {
            return null;
        }
        float[] scaled = new float[raw.length];
        for (int i = 0; i < raw.length; i++) {
            scaled[i] = Math.max(0f, Math.min(5f, raw[i] * config.auIntensityScale));
        }
        return scaled;
    }

    private static float[] softmax(float[] logits) {
        if (logits == null || logits.length == 0) {
            return null;
        }
        float maximum = logits[0];
        for (float value : logits) {
            maximum = Math.max(maximum, value);
        }
        float[] output = new float[logits.length];
        float sum = 0f;
        for (int i = 0; i < logits.length; i++) {
            output[i] = (float) Math.exp(logits[i] - maximum);
            sum += output[i];
        }
        if (sum <= 0f) {
            return output;
        }
        for (int i = 0; i < output.length; i++) {
            output[i] /= sum;
        }
        return output;
    }

    public void stop() {
        workerRunning = false;
        ready = false;
        Thread thread = worker;
        worker = null;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        queue.clear();
        status = "stopped";
    }

    @Override
    public void close() {
        stop();
        synchronized (inferenceLock) {
            closed = true;
            closeModelsLocked();
        }
        started = false;
    }

    private void closeModelsLocked() {
        closeQuietly(encoderPredictor);
        closeQuietly(intensityPredictor);
        closeQuietly(presencePredictor);
        closeQuietly(expressionPredictor);
        closeQuietly(encoderModel);
        closeQuietly(intensityModel);
        closeQuietly(presenceModel);
        closeQuietly(expressionModel);
        encoderPredictor = null;
        intensityPredictor = null;
        presencePredictor = null;
        expressionPredictor = null;
        encoderModel = null;
        intensityModel = null;
        presenceModel = null;
        expressionModel = null;
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable ignored) {
        }
    }

    public void addListener(LibreFaceListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(LibreFaceListener listener) {
        listeners.remove(listener);
    }

    private void notifyError(String message) {
        for (LibreFaceListener listener : listeners) {
            try {
                listener.onError(message);
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean isReady() {
        return ready;
    }

    public LibreFaceResult getLatestResult() {
        return latestResult;
    }

    public String getLastError() {
        return lastError;
    }

    public String getStatus() {
        return status;
    }

    public long getLastInferenceMs() {
        return lastInferenceMs;
    }

    public double getAverageInferenceMs() {
        return averageInferenceMs;
    }

    public long getSubmittedFrames() {
        return submittedFrames;
    }

    public long getAnalyzedFrames() {
        return analyzedFrames;
    }

    public long getDroppedFrames() {
        return droppedFrames;
    }

    public String getStatusLine() {
        StringBuilder builder = new StringBuilder("LibreFace ").append(status);
        if (ready) {
            builder.append(" | frames ").append(analyzedFrames).append('/').append(submittedFrames);
            if (droppedFrames > 0) {
                builder.append(" (dropped ").append(droppedFrames).append(')');
            }
            builder.append(" | ").append(String.format(Locale.ROOT, "%.0f ms/frame", averageInferenceMs));
        }
        if (!lastError.isEmpty()) {
            builder.append(" | ").append(lastError);
        }
        return builder.toString();
    }
}
