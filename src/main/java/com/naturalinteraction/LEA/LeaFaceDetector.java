package com.naturalinteraction.LEA;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LeaFaceDetector implements AutoCloseable {

    private static final int VALUES_PER_FACE = 15;
    private static final double MIN_ASPECT = 0.75d;
    private static final double MAX_ASPECT = 2.6d;

    private final LeaConfig config;

    private FaceDetectorYN detector;
    private Mat buffer;
    private Mat result;
    private BufferedImage scratch;
    private int bufferWidth;
    private int bufferHeight;
    private volatile boolean ready;
    private volatile String lastError = "";
    private volatile long lastDetectMs;

    public LeaFaceDetector(LeaConfig config) {
        this.config = config;
    }

    public static boolean isAvailable() {
        try {
            Class.forName("org.opencv.objdetect.FaceDetectorYN", false,
                    LeaFaceDetector.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean open() {
        if (ready) {
            return true;
        }
        try {
            loadNative();
            Path model = config.resolveFaceModelPath();
            if (!Files.isRegularFile(model)) {
                lastError = "face model not found: " + model.toAbsolutePath();
                return false;
            }
            detector = FaceDetectorYN.create(model.toString(), "",
                    new Size(320d, 320d), config.faceScoreThreshold,
                    config.faceNmsThreshold, 5000);
            result = new Mat();
            ready = true;
            lastError = "";
            return true;
        } catch (Throwable t) {
            ready = false;
            lastError = "face detector init failed: " + t;
            return false;
        }
    }

    private static void loadNative() {
        nu.pattern.OpenCV.loadLocally();
    }

    public LeaFaceDetection detect(BufferedImage image, long timestamp) {
        if (!ready || image == null) {
            return null;
        }
        try {
            Mat frame = toMat(image);
            Size current = detector.getInputSize();
            if (current.width != image.getWidth() || current.height != image.getHeight()) {
                detector.setInputSize(new Size(image.getWidth(), image.getHeight()));
            }
            long started = System.currentTimeMillis();
            detector.detect(frame, result);
            lastDetectMs = System.currentTimeMillis() - started;

            int rows = result.rows();
            if (rows <= 0) {
                return null;
            }
            float[] values = new float[VALUES_PER_FACE];
            float[] best = null;
            for (int row = 0; row < rows; row++) {
                result.get(row, 0, values);
                if (best == null || values[2] * values[3] > best[2] * best[3]) {
                    best = values.clone();
                }
            }
            if (best == null) {
                return null;
            }
            double width = best[2];
            double height = best[3];
            if (width < config.faceMinSize || height < config.faceMinSize) {
                return null;
            }
            double aspect = height / Math.max(1d, width);
            if (aspect < MIN_ASPECT || aspect > MAX_ASPECT) {
                return null;
            }
            if (best[14] < config.faceScoreThreshold) {
                return null;
            }
            Rectangle2D box = new Rectangle2D.Double(best[0], best[1], width, height);
            Point rightEye = new Point(Math.round(best[4]), Math.round(best[5]));
            Point leftEye = new Point(Math.round(best[6]), Math.round(best[7]));
            return new LeaFaceDetection(box, rightEye, leftEye, best[14], timestamp);
        } catch (Throwable t) {
            lastError = "face detect failed: " + t;
            return null;
        }
    }

    private Mat toMat(BufferedImage image) {
        BufferedImage source = image;
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            if (scratch == null || scratch.getWidth() != image.getWidth()
                    || scratch.getHeight() != image.getHeight()) {
                scratch = new BufferedImage(image.getWidth(), image.getHeight(),
                        BufferedImage.TYPE_3BYTE_BGR);
            }
            Graphics2D graphics = scratch.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            source = scratch;
        }
        byte[] data = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
        if (buffer == null || bufferWidth != image.getWidth() || bufferHeight != image.getHeight()) {
            if (buffer != null) {
                buffer.release();
            }
            buffer = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
            bufferWidth = image.getWidth();
            bufferHeight = image.getHeight();
        }
        buffer.put(0, 0, data);
        return buffer;
    }

    public boolean isReady() {
        return ready;
    }

    public String getLastError() {
        return lastError;
    }

    public long getLastDetectMs() {
        return lastDetectMs;
    }

    @Override
    public void close() {
        ready = false;
        if (result != null) {
            result.release();
            result = null;
        }
        if (buffer != null) {
            buffer.release();
            buffer = null;
        }
        bufferWidth = 0;
        bufferHeight = 0;
        scratch = null;
        detector = null;
    }
}
