package com.natural_interaction.LEA.capture;

import com.natural_interaction.LEA.LeaConfig;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public final class LeaSarxosFrameSource implements LeaFrameSource {

    private static final String WEBCAM_CLASS = "com.github.sarxos.webcam.Webcam";

    private final LeaConfig config;
    private Object webcam;
    private boolean open;
    private int frameRate;
    private volatile String lastError = "";

    public static boolean isAvailable() {
        return LeaReflect.find(WEBCAM_CLASS) != null;
    }

    public LeaSarxosFrameSource(LeaConfig config) {
        this.config = config;
        this.frameRate = config.targetFps;
        try {
            Class<?> type = LeaReflect.find(WEBCAM_CLASS);
            if (type == null) {
                lastError = "webcam-capture is not on the classpath";
                return;
            }
            Object device = LeaReflect.callStatic(type, "getDefault", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
            if (device == null) {
                lastError = "no default webcam found";
                return;
            }
            LeaReflect.callQuietly(device, "setViewSize", new Class<?>[]{Dimension.class},
                    new Object[]{new Dimension(config.cameraWidth, config.cameraHeight)});
            Object result = LeaReflect.call(device, "open", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
            if (result instanceof Boolean && !((Boolean) result).booleanValue()) {
                lastError = "webcam.open() returned false";
                return;
            }
            this.webcam = device;
            this.open = true;
            this.frameRate = readFrameRate(device);
        } catch (Throwable t) {
            lastError = LeaReflect.describe(t);
            this.webcam = null;
            this.open = false;
        }
    }

    private int readFrameRate(Object device) {
        Object value = LeaReflect.callQuietly(device, "getFPS", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
        if (value instanceof Number) {
            int fps = (int) Math.round(((Number) value).doubleValue());
            if (fps > 0) {
                return fps;
            }
        }
        return config.targetFps;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public int getFrameRate() {
        return frameRate;
    }

    @Override
    public BufferedImage grab() {
        if (!open || webcam == null) {
            return null;
        }
        try {
            Object image = LeaReflect.call(webcam, "getImage", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
            if (image instanceof BufferedImage) {
                return (BufferedImage) image;
            }
            lastError = "webcam returned no image";
            return null;
        } catch (Throwable t) {
            lastError = LeaReflect.describe(t);
            return null;
        }
    }

    @Override
    public String getName() {
        return "sarxos-webcam-capture";
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void close() {
        open = false;
        Object device = webcam;
        webcam = null;
        if (device != null) {
            LeaReflect.callQuietly(device, "close", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
        }
    }
}
