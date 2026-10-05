package com.natural_interaction.LibreFace;

import com.natural_interaction.LEA.LeaFaceAnalyzer;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public final class LibreFaceAnalyzerAdapter implements LeaFaceAnalyzer {

    private final LibreFaceEngine engine;
    private final LibreFaceConfig config;
    private final LibreFacePreprocessor preprocessor = new LibreFacePreprocessor();

    private volatile long lastSubmitAt;
    private volatile boolean usingAlignedCrop;

    public LibreFaceAnalyzerAdapter(LibreFaceEngine engine, LibreFaceConfig config) {
        this.engine = engine;
        this.config = config;
    }

    @Override
    public void submitFrame(BufferedImage frame, Rectangle2D faceRect, long timestamp) {
        submitFrame(frame, faceRect, null, null, timestamp);
    }

    @Override
    public void submitFrame(BufferedImage frame, Rectangle2D faceRect,
                            Point rightEye, Point leftEye, long timestamp) {
        if (!engine.isReady() || frame == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long interval = config.inferenceIntervalMs;
        if (interval > 0L && now - lastSubmitAt < interval) {
            return;
        }
        lastSubmitAt = now;

        BufferedImage crop = null;
        if (config.alignByEyes && rightEye != null && leftEye != null && faceRect != null) {
            BufferedImage aligned = preprocessor.alignFace(frame, rightEye, leftEye, config.alignSize);
            if (aligned != null) {
                crop = preprocessor.centerCrop(aligned, config.inputSize);
                usingAlignedCrop = true;
            }
        }
        if (crop == null) {
            if (faceRect == null) {
                return;
            }
            crop = preprocessor.cropFace(frame, faceRect, config.cropPadding,
                    config.cropOffsetY, config.inputSize);
            usingAlignedCrop = false;
        }
        if (crop == null) {
            return;
        }
        engine.submit(crop, timestamp);
    }

    public boolean isUsingAlignedCrop() {
        return usingAlignedCrop;
    }

    @Override
    public String getName() {
        return "LibreFace";
    }

    @Override
    public boolean isReady() {
        return engine.isReady();
    }

    @Override
    public String getLastError() {
        return engine.getLastError();
    }
}
