package com.naturalinteraction.LEA;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import de.darkblue.lea.model.EyePosition;
import de.darkblue.lea.model.FaceAndEyePosition;
import de.darkblue.lea.model.LEAImplementation;

public final class LeaEyeTracker {

    private static final String XSTREAM_CLASS = "com.thoughtworks.xstream.XStream";
    private static final String NEUTRAL_MODE_BOX = "BOX";
    private static final String NEUTRAL_MODE_INTEROCULAR = "INTEROCULAR";
    private static final int OUTLIER_FRAMES_BEFORE_ACCEPT = 3;
    private static final long CALIBRATION_TIMEOUT_MS = 20000L;

    private final LeaConfig config;
    private final List<LeaTrackerListener> listeners = new CopyOnWriteArrayList<>();
    private final LeaCaptureDevice captureDevice;
    private final LeaEyeOpennessEstimator opennessEstimator;
    private final LeaBlinkDetector blinkDetector;

    private volatile LEAImplementation implementation;
    private volatile boolean running;
    private volatile Thread worker;
    private volatile String lastError = "";
    private volatile String lastNotifiedError = "";
    private volatile String status = "idle";
    private volatile LeaGazeSample latestSample;
    private volatile long frameCount;
    private volatile LeaFaceAnalyzer faceAnalyzer;
    private final Rectangle2D.Double analyzerRect = new Rectangle2D.Double();
    private final LeaFaceDetector onnxDetector;
    private final LeaEyeLocator eyeLocator;
    private final Rectangle2D.Double smoothedFace = new Rectangle2D.Double();
    private boolean hasSmoothedFace;
    private float lastInterOcular;
    private float smoothedLandmarkX = Float.NaN;
    private float smoothedLandmarkY = Float.NaN;
    private float lastLandmarkX;
    private float lastLandmarkY;
    private boolean hasLandmark;
    private Point analyzerRightEye;
    private Point analyzerLeftEye;

    private volatile boolean calibrationRequested;
    private volatile boolean calibrating;
    private volatile int calibrationCollected;
    private volatile String calibrationMessage = "";
    private long calibrationStartedAt;

    private BufferedImage detectionBuffer;
    private float filterEyeX = Float.NaN;
    private float filterEyeY = Float.NaN;
    private int outlierFrames;
    private int faceMissingFrames;
    private int eyeMissingFrames;
    private boolean faceLostNotified;
    private boolean hasFace;
    private boolean hasRelative;
    private float lastFaceX;
    private float lastFaceY;
    private float lastFaceWidth;
    private float lastFaceHeight;
    private float lastEyeX = -1f;
    private float lastEyeY = -1f;
    private float lastRelativeX = 0.5f;
    private float lastRelativeY = 0.5f;
    private float lastOffsetX;
    private float lastOffsetY;
    private double calibrationSumX;
    private double calibrationSumY;
    private double calibrationSumX2;
    private double calibrationSumY2;
    private LeaGesture activeGesture = LeaGesture.NONE;
    private long gestureSince;
    private boolean gestureReported;
    private float neutralRelX;
    private float neutralRelY;
    private volatile boolean hasNeutral;
    private boolean autoCalibrationPending = true;

    public LeaEyeTracker(LeaConfig config) {
        this.config = config;
        this.captureDevice = new LeaCaptureDevice(config);
        this.opennessEstimator = new LeaEyeOpennessEstimator(config);
        this.blinkDetector = new LeaBlinkDetector(config);
        this.hasNeutral = config.hasNeutral;
        this.neutralRelX = config.neutralRelX;
        this.neutralRelY = config.neutralRelY;
        this.eyeLocator = new LeaEyeLocator(config);
        this.onnxDetector = createOnnxDetector(config);
        String mode = config.gazeNormalizeByInterOcular ? NEUTRAL_MODE_INTEROCULAR : NEUTRAL_MODE_BOX;
        if (hasNeutral && !mode.equals(config.neutralMode)) {
            hasNeutral = false;
            config.clearNeutral();
        }
        config.neutralMode = mode;
    }

    private static LeaFaceDetector createOnnxDetector(LeaConfig config) {
        if (config.faceSource == LeaFaceSource.LEA || !LeaFaceDetector.isAvailable()) {
            return null;
        }
        try {
            return new LeaFaceDetector(config);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isXStreamAvailable() {
        try {
            Class.forName(XSTREAM_CLASS, false, LeaEyeTracker.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean start() {
        if (running) {
            return true;
        }
        lastError = "";
        lastNotifiedError = "";
        status = "starting";

        if (!isXStreamAvailable()) {
            lastError = "xstream is missing, LEA cannot read faceData.xml";
            status = "xstream missing";
            notifyError(lastError);
            return false;
        }
        if (!captureDevice.open()) {
            lastError = "capture: " + captureDevice.getLastError();
            status = "capture failed";
            notifyError(lastError);
            return false;
        }
        if (!ensureImplementation()) {
            captureDevice.close();
            status = "detector failed";
            return false;
        }

        if (onnxDetector != null && !onnxDetector.open()) {
            lastError = onnxDetector.getLastError();
            notifyError(lastError);
        }

        running = true;
        autoCalibrationPending = !hasNeutral;
        Thread thread = new Thread(this::pump, "LEA-EyeTracker");
        thread.setDaemon(true);
        worker = thread;
        thread.start();
        status = "running (" + captureDevice.getSourceName() + ", face="
                + (useOnnxFace() ? "onnx" : "lea") + ")";
        return true;
    }

    private boolean ensureImplementation() {
        if (implementation != null) {
            return true;
        }
        BufferedImage probe = new BufferedImage(48, 48, BufferedImage.TYPE_INT_RGB);
        Throwable primaryFailure = null;
        try {
            LEAImplementation created = LeaFaceDataLoader.createImplementation();
            created.getEyePosition(probe);
            implementation = created;
            return true;
        } catch (Throwable t) {
            primaryFailure = t;
        }
        try {
            LEAImplementation created = new LEAImplementation();
            created.getEyePosition(probe);
            implementation = created;
            return true;
        } catch (Throwable t) {
            lastError = "LEA face data unavailable: " + t + " / own loader: " + primaryFailure;
            notifyError(lastError);
            return false;
        }
    }

    public void stop() {
        running = false;
        Thread thread = worker;
        worker = null;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(1500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        implementation = null;
        captureDevice.close();
        if (onnxDetector != null) {
            onnxDetector.close();
        }
        hasSmoothedFace = false;
        lastInterOcular = 0f;
        hasLandmark = false;
        smoothedLandmarkX = Float.NaN;
        smoothedLandmarkY = Float.NaN;
        blinkDetector.reset();
        opennessEstimator.resetBaselines();
        resetTrackingState();
        if (calibrating) {
            calibrating = false;
            calibrationRequested = false;
            calibrationMessage = "aborted";
        }
        status = "stopped";
    }

    private void pump() {
        long interval = Math.max(1L, 1000L / Math.max(1, config.targetFps));
        while (running) {
            long started = System.currentTimeMillis();
            try {
                processFrame(started);
            } catch (Throwable t) {
                lastError = String.valueOf(t);
                notifyError(lastError);
            }
            long elapsed = System.currentTimeMillis() - started;
            long sleep = interval - elapsed;
            if (sleep > 0L) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processFrame(long now) {
        LEAImplementation detector = implementation;
        if (detector == null) {
            return;
        }

        if (calibrating && calibrationStartedAt > 0L && now - calibrationStartedAt > CALIBRATION_TIMEOUT_MS) {
            finishCalibration(false, "calibration timed out");
            return;
        }

        frameCount++;

        BufferedImage raw = captureDevice.getImage();
        if (raw == null) {
            handleFaceMissing(now);
            return;
        }

        BufferedImage image = prepareFrame(raw);
        double scale = image.getWidth() <= 0 ? 1d : raw.getWidth() / (double) image.getWidth();

        Rectangle2D face = null;
        EyePosition eyePosition = null;
        float interOcular = 0f;

        if (useOnnxFace()) {
            LeaFaceDetection detection = onnxDetector.detect(raw, now);
            if (detection != null) {
                Point right = detection.getRightEye();
                Point left = detection.getLeftEye();
                if (config.eyeRefineEnabled) {
                    right = eyeLocator.locate(raw, right, detection.getFaceBox());
                    left = eyeLocator.locate(raw, left, detection.getFaceBox());
                }
                Rectangle2D smoothed = smoothFaceBox(toDetectionSpace(detection.getFaceBox(), scale));
                if (eyeLocator.isPlausiblePair(right, left, detection.getFaceBox())) {
                    Point midpoint = new Point((right.x + left.x) / 2, (right.y + left.y) / 2);
                    Point landmarkMidpoint = new Point(
                            (detection.getRightEye().x + detection.getLeftEye().x) / 2,
                            (detection.getRightEye().y + detection.getLeftEye().y) / 2);
                    interOcular = (float) (right.distance(left) / scale);
                    Point midpointInFrame = toDetectionSpace(midpoint, scale);
                    Point landmarkInFrame = toDetectionSpace(landmarkMidpoint, scale);
                    trackLandmark(landmarkInFrame);
                    eyePosition = new EyePosition(midpointInFrame.x, midpointInFrame.y, smoothed);
                    analyzerRightEye = right;
                    analyzerLeftEye = left;
                } else {
                    analyzerRightEye = null;
                    analyzerLeftEye = null;
                }
                face = smoothed;
            }
        }

        if (face == null) {
            FaceAndEyePosition position;
            try {
                position = detector.getEyePosition(image);
            } catch (Throwable t) {
                handleFaceMissing(now);
                return;
            }
            face = position == null ? null : position.getFacePosition();
            eyePosition = position == null ? null : position.getEyePosition();
            interOcular = 0f;
            analyzerRightEye = null;
            analyzerLeftEye = null;
        }

        if (face == null || face.getWidth() < config.minFaceWidth) {
            handleFaceMissing(now);
            return;
        }

        faceMissingFrames = 0;
        faceLostNotified = false;
        hasFace = true;
        lastFaceX = (float) face.getX();
        lastFaceY = (float) face.getY();
        lastFaceWidth = (float) face.getWidth();
        lastFaceHeight = (float) face.getHeight();
        lastInterOcular = interOcular;

        submitToFaceAnalyzer(raw, image, face, now);

        boolean eyeFound = trackEye(eyePosition, (float) face.getWidth());

        if (eyeFound) {
            eyeMissingFrames = 0;
            lastEyeX = filterEyeX;
            lastEyeY = filterEyeY;
        } else {
            eyeMissingFrames++;
        }

        float relativeX;
        float relativeY;
        if (eyeFound) {
            if (interOcular > 0f && config.gazeNormalizeByInterOcular && hasLandmark) {
                float ratio = clampRatio(interOcular / lastFaceWidth);
                relativeX = 0.5f + ((filterEyeX - lastLandmarkX) / interOcular) * ratio;
                relativeY = 0.5f + ((filterEyeY - lastLandmarkY) / interOcular) * ratio;
            } else {
                relativeX = (filterEyeX - lastFaceX) / lastFaceWidth;
                relativeY = (filterEyeY - lastFaceY) / lastFaceHeight;
            }
            lastRelativeX = relativeX;
            lastRelativeY = relativeY;
            hasRelative = true;
        } else if (hasRelative) {
            relativeX = lastRelativeX;
            relativeY = lastRelativeY;
        } else {
            relativeX = 0.5f;
            relativeY = 0.5f;
        }

        float offsetX = hasNeutral ? relativeX - neutralRelX : 0f;
        float offsetY = hasNeutral ? relativeY - neutralRelY : 0f;
        lastOffsetX = offsetX;
        lastOffsetY = offsetY;

        float eyeLineY = eyeMissingFrames < config.eyeLostFrames && lastEyeY >= 0f ? lastEyeY : Float.NaN;
        opennessEstimator.update(image, face, eyeLineY);
        LeaBlinkEvent blinkEvent = blinkDetector.update(config.enableBlinkDetection,
                opennessEstimator.getOpennessLeft(), opennessEstimator.getOpennessRight(),
                opennessEstimator.isLeftValid(), opennessEstimator.isRightValid(), now);
        if (blinkEvent != null) {
            notifyBlink(blinkEvent);
        }

        if (calibrating) {
            collectCalibration(relativeX, relativeY, eyeFound);
        } else if (autoCalibrationPending && !hasNeutral && config.autoCalibrateWhenUncalibrated) {
            autoCalibrationPending = false;
            requestCalibration();
        }

        updateGesture(offsetX, offsetY, now);

        publishSample(new LeaGazeSample(now, true, eyeFound, hasNeutral,
                lastFaceX, lastFaceY, lastFaceWidth, lastFaceHeight,
                lastEyeX, lastEyeY, relativeX, relativeY, offsetX, offsetY,
                opennessEstimator.getOpennessLeft(), opennessEstimator.getOpennessRight(),
                blinkDetector.getLeftState(), blinkDetector.getRightState()));
    }

    private void submitToFaceAnalyzer(BufferedImage raw, BufferedImage detectionFrame,
                                      Rectangle2D face, long now) {
        LeaFaceAnalyzer analyzer = faceAnalyzer;
        if (analyzer == null) {
            return;
        }
        double scale = detectionFrame.getWidth() <= 0
                ? 1d
                : raw.getWidth() / (double) detectionFrame.getWidth();
        analyzerRect.setRect(face.getX() * scale, face.getY() * scale,
                face.getWidth() * scale, face.getHeight() * scale);
        try {
            analyzer.submitFrame(raw, analyzerRect, analyzerRightEye, analyzerLeftEye, now);
        } catch (Throwable ignored) {
        }
    }

    private boolean useOnnxFace() {
        return config.faceSource != LeaFaceSource.LEA
                && onnxDetector != null
                && onnxDetector.isReady();
    }

    private static Rectangle2D toDetectionSpace(Rectangle2D rectangle, double scale) {
        if (scale <= 0d || scale == 1d) {
            return new Rectangle2D.Double(rectangle.getX(), rectangle.getY(),
                    rectangle.getWidth(), rectangle.getHeight());
        }
        return new Rectangle2D.Double(rectangle.getX() / scale, rectangle.getY() / scale,
                rectangle.getWidth() / scale, rectangle.getHeight() / scale);
    }

    private static Point toDetectionSpace(Point point, double scale) {
        if (scale <= 0d || scale == 1d) {
            return new Point(point.x, point.y);
        }
        return new Point((int) Math.round(point.x / scale), (int) Math.round(point.y / scale));
    }

    private Rectangle2D smoothFaceBox(Rectangle2D box) {
        float alpha = config.faceBoxSmoothing;
        if (!hasSmoothedFace) {
            smoothedFace.setRect(box);
            hasSmoothedFace = true;
            return smoothedFace;
        }
        double currentWidth = smoothedFace.getWidth();
        double currentHeight = smoothedFace.getHeight();
        double widthDifference = Math.abs(currentWidth - box.getWidth()) / Math.max(1d, currentWidth);
        double heightDifference = Math.abs(currentHeight - box.getHeight()) / Math.max(1d, currentHeight);
        double effective = widthDifference > 0.4d || heightDifference > 0.4d ? 1d : alpha;
        smoothedFace.setRect(
                smoothedFace.getX() + (box.getX() - smoothedFace.getX()) * effective,
                smoothedFace.getY() + (box.getY() - smoothedFace.getY()) * effective,
                currentWidth + (box.getWidth() - currentWidth) * effective,
                currentHeight + (box.getHeight() - currentHeight) * effective);
        return smoothedFace;
    }

    private static float clampRatio(float ratio) {
        return Math.max(0.2f, Math.min(1f, ratio));
    }

    private void trackLandmark(Point landmark) {
        if (landmark == null) {
            return;
        }
        float alpha = config.sampleSmoothing;
        if (!hasLandmark || Float.isNaN(smoothedLandmarkX)) {
            smoothedLandmarkX = landmark.x;
            smoothedLandmarkY = landmark.y;
        } else {
            smoothedLandmarkX += alpha * (landmark.x - smoothedLandmarkX);
            smoothedLandmarkY += alpha * (landmark.y - smoothedLandmarkY);
        }
        lastLandmarkX = smoothedLandmarkX;
        lastLandmarkY = smoothedLandmarkY;
        hasLandmark = true;
    }

    private boolean trackEye(EyePosition eye, float faceWidth) {        if (eye == null) {
            return false;
        }
        float rawX = eye.getX();
        float rawY = eye.getY();
        if (Float.isNaN(filterEyeX)) {
            filterEyeX = rawX;
            filterEyeY = rawY;
            outlierFrames = 0;
            return true;
        }
        float distance = (float) (Math.hypot(rawX - filterEyeX, rawY - filterEyeY)
                / Math.max(1d, faceWidth));
        if (distance <= config.maxEyeJumpRatio) {
            filterEyeX += config.sampleSmoothing * (rawX - filterEyeX);
            filterEyeY += config.sampleSmoothing * (rawY - filterEyeY);
            outlierFrames = 0;
            return true;
        }
        outlierFrames++;
        if (outlierFrames >= OUTLIER_FRAMES_BEFORE_ACCEPT) {
            filterEyeX = rawX;
            filterEyeY = rawY;
            outlierFrames = 0;
            return true;
        }
        return false;
    }

    private void handleFaceMissing(long now) {
        faceMissingFrames++;
        eyeMissingFrames++;
        opennessEstimator.clear();
        blinkDetector.invalidate();
        resetGesture();

        if (faceMissingFrames >= config.faceLostFrames && !faceLostNotified) {
            faceLostNotified = true;
            notifyFaceLost(now);
        }

        publishSample(new LeaGazeSample(now, false, false, hasNeutral,
                lastFaceX, lastFaceY, lastFaceWidth, lastFaceHeight,
                lastEyeX, lastEyeY, lastRelativeX, lastRelativeY,
                lastOffsetX, lastOffsetY, 0f, 0f,
                LeaEyeState.UNKNOWN, LeaEyeState.UNKNOWN));
    }

    private BufferedImage prepareFrame(BufferedImage raw) {
        int targetWidth = Math.max(80, config.detectionWidth);
        if (raw.getWidth() <= targetWidth) {
            return raw;
        }
        int targetHeight = Math.max(1,
                (int) Math.round(raw.getHeight() * (targetWidth / (double) raw.getWidth())));
        if (detectionBuffer == null
                || detectionBuffer.getWidth() != targetWidth
                || detectionBuffer.getHeight() != targetHeight) {
            detectionBuffer = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D graphics = detectionBuffer.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        graphics.drawImage(raw, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return detectionBuffer;
    }

    private void collectCalibration(float relativeX, float relativeY, boolean eyeFound) {
        if (!eyeFound) {
            return;
        }
        calibrationSumX += relativeX;
        calibrationSumY += relativeY;
        calibrationSumX2 += (double) relativeX * relativeX;
        calibrationSumY2 += (double) relativeY * relativeY;
        calibrationCollected++;

        if (calibrationCollected < config.calibrationSamples) {
            return;
        }

        double meanX = calibrationSumX / calibrationCollected;
        double meanY = calibrationSumY / calibrationCollected;
        double varianceX = Math.max(0d, calibrationSumX2 / calibrationCollected - meanX * meanX);
        double varianceY = Math.max(0d, calibrationSumY2 / calibrationCollected - meanY * meanY);
        double spread = Math.max(Math.sqrt(varianceX), Math.sqrt(varianceY));

        if (spread <= config.calibrationMaxSpread) {
            neutralRelX = (float) meanX;
            neutralRelY = (float) meanY;
            hasNeutral = true;
            config.setNeutral(neutralRelX, neutralRelY);
            config.save();
            finishCalibration(true, "calibrated");
        } else {
            finishCalibration(false, "unstable gaze, spread "
                    + String.format(Locale.ROOT, "%.4f", spread));
        }
    }

    private void finishCalibration(boolean success, String message) {
        resetCalibrationAccumulator();
        calibrationRequested = false;
        calibrating = false;
        calibrationMessage = message;
        notifyCalibration(success, message);
    }

    private void resetCalibrationAccumulator() {
        calibrationCollected = 0;
        calibrationSumX = 0d;
        calibrationSumY = 0d;
        calibrationSumX2 = 0d;
        calibrationSumY2 = 0d;
        calibrationStartedAt = 0L;
    }

    private void updateGesture(float offsetX, float offsetY, long now) {
        if (!config.gesturesEnabled) {
            resetGesture();
            return;
        }
        int directionX = Math.abs(offsetX) >= config.gestureDeadzone ? (offsetX > 0f ? 1 : -1) : 0;
        int directionY = Math.abs(offsetY) >= config.gestureDeadzone ? (offsetY > 0f ? 1 : -1) : 0;
        LeaGesture gesture = LeaGesture.fromDirections(directionX, directionY);

        if (gesture != activeGesture) {
            activeGesture = gesture;
            gestureSince = now;
            gestureReported = false;
            return;
        }
        if (gesture == LeaGesture.NONE || gestureReported) {
            return;
        }
        if (now - gestureSince < config.gestureHoldMs) {
            return;
        }
        gestureReported = true;
        float magnitude = Math.max(Math.abs(offsetX), Math.abs(offsetY));
        notifyGesture(new LeaGestureEvent(gesture, magnitude,
                magnitude >= config.gestureFarThreshold, now));
    }

    private void resetGesture() {
        activeGesture = LeaGesture.NONE;
        gestureSince = 0L;
        gestureReported = false;
    }

    private void resetTrackingState() {
        filterEyeX = Float.NaN;
        filterEyeY = Float.NaN;
        outlierFrames = 0;
        faceMissingFrames = 0;
        eyeMissingFrames = 0;
        faceLostNotified = false;
        hasFace = false;
        hasRelative = false;
        lastEyeX = -1f;
        lastEyeY = -1f;
        lastOffsetX = 0f;
        lastOffsetY = 0f;
        latestSample = null;
        resetGesture();
    }

    private void publishSample(LeaGazeSample sample) {
        latestSample = sample;
        for (LeaTrackerListener listener : listeners) {
            try {
                listener.onGaze(sample);
            } catch (Throwable ignored) {
            }
        }
    }

    private void notifyFaceLost(long timestamp) {
        for (LeaTrackerListener listener : listeners) {
            try {
                listener.onFaceLost(timestamp);
            } catch (Throwable ignored) {
            }
        }
    }

    private void notifyBlink(LeaBlinkEvent event) {
        for (LeaTrackerListener listener : listeners) {
            try {
                listener.onBlink(event);
            } catch (Throwable ignored) {
            }
        }
    }

    private void notifyGesture(LeaGestureEvent event) {
        for (LeaTrackerListener listener : listeners) {
            try {
                listener.onGesture(event);
            } catch (Throwable ignored) {
            }
        }
    }

    private void notifyCalibration(boolean success, String message) {
        for (LeaTrackerListener listener : listeners) {
            try {
                listener.onCalibration(success, message);
            } catch (Throwable ignored) {
            }
        }
    }

    private void notifyError(String message) {
        if (message == null || message.equals(lastNotifiedError)) {
            return;
        }
        lastNotifiedError = message;
        for (LeaTrackerListener listener : listeners) {
            try {
                listener.onError(message);
            } catch (Throwable ignored) {
            }
        }
    }

    public void addListener(LeaTrackerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(LeaTrackerListener listener) {
        listeners.remove(listener);
    }

    public boolean requestCalibration() {
        if (!running || calibrating) {
            return false;
        }
        resetCalibrationAccumulator();
        calibrationStartedAt = System.currentTimeMillis();
        calibrationCollected = 0;
        calibrating = true;
        calibrationRequested = true;
        calibrationMessage = "collecting";
        return true;
    }

    public void clearCalibration() {
        hasNeutral = false;
        neutralRelX = 0f;
        neutralRelY = 0f;
        config.clearNeutral();
        config.save();
        resetCalibrationAccumulator();
        calibrating = false;
        calibrationRequested = false;
        calibrationMessage = "cleared";
        resetTrackingState();
    }

    public void setNeutral(float relX, float relY) {
        neutralRelX = relX;
        neutralRelY = relY;
        hasNeutral = true;
        config.setNeutral(relX, relY);
        config.save();
    }

    public boolean isRunning() {
        return running;
    }

    public String getLastError() {
        return lastError;
    }

    public String getStatus() {
        return status;
    }

    public LeaGazeSample getLatestSample() {
        return latestSample;
    }

    public long getFrameCount() {
        return frameCount;
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    public boolean isCalibrated() {
        return hasNeutral;
    }

    public int getCalibrationCollected() {
        return calibrationCollected;
    }

    public float getCalibrationProgress() {
        if (!calibrating) {
            return hasNeutral ? 1f : 0f;
        }
        return Math.min(1f, (float) calibrationCollected / (float) config.calibrationSamples);
    }

    public String getCalibrationMessage() {
        return calibrationMessage;
    }

    public float getNeutralRelX() {
        return neutralRelX;
    }

    private boolean hasSeenEye() {
        return lastEyeY >= 0f && eyeMissingFrames < config.eyeLostFrames;
    }

    public float getNeutralRelY() {
        return neutralRelY;
    }

    public LeaConfig getConfig() {
        return config;
    }

    public void setFaceAnalyzer(LeaFaceAnalyzer analyzer) {
        this.faceAnalyzer = analyzer;
    }

    public LeaFaceAnalyzer getFaceAnalyzer() {
        return faceAnalyzer;
    }

    public LeaCaptureDevice getCaptureDevice() {
        return captureDevice;
    }

    public String getStatusLine() {
        StringBuilder builder = new StringBuilder();
        builder.append("LEA ").append(status);
        builder.append(" | face: ").append(hasFace ? "ok" : "none");
        builder.append(" | eye: ").append(hasSeenEye() ? "ok" : "none");
        builder.append(" | ").append(hasNeutral ? "calibrated" : "not calibrated");
        if (calibrating) {
            builder.append(" | calibrating ")
                    .append(Math.round(getCalibrationProgress() * 100f)).append('%');
        }
        if (!lastError.isEmpty()) {
            builder.append(" | ").append(lastError);
        }
        return builder.toString();
    }
}
