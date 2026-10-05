package com.naturalinteraction.LEA.capture;

import com.naturalinteraction.LEA.LeaConfig;

import java.awt.image.BufferedImage;

public final class LeaJmfFrameSource implements LeaFrameSource {

    private static final String DEVICE_CLASS = "de.darkblue.lea.capturedevices.JMFCaptureDevice";
    private static final String JMF_CLASS = "javax.media.CaptureDeviceManager";

    private final LeaConfig config;
    private Object device;
    private boolean open;
    private int frameRate;
    private volatile String lastError = "";

    public static boolean isAvailable() {
        return LeaReflect.find(JMF_CLASS) != null && LeaReflect.find(DEVICE_CLASS) != null;
    }

    public LeaJmfFrameSource(LeaConfig config) {
        this.config = config;
        this.frameRate = config.targetFps;
        try {
            Class<?> type = LeaReflect.find(DEVICE_CLASS);
            if (type == null) {
                lastError = "LEA JMF capture device not found";
                return;
            }
            Object created = LeaReflect.construct(type, LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
            if (created == null) {
                lastError = "JMF capture device could not be created";
                return;
            }
            this.device = created;
            this.open = true;
            Object value = LeaReflect.callQuietly(created, "getFrameRate", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
            if (value instanceof Number) {
                int fps = ((Number) value).intValue();
                if (fps > 0) {
                    this.frameRate = fps;
                }
            }
        } catch (Throwable t) {
            lastError = LeaReflect.describe(t);
            this.device = null;
            this.open = false;
        }
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
        if (!open || device == null) {
            return null;
        }
        try {
            Object image = LeaReflect.call(device, "getImage", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
            if (image instanceof BufferedImage) {
                return (BufferedImage) image;
            }
            return null;
        } catch (Throwable t) {
            lastError = LeaReflect.describe(t);
            return null;
        }
    }

    @Override
    public String getName() {
        return "lea-jmf";
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void close() {
        open = false;
        Object current = device;
        device = null;
        if (current != null) {
            LeaReflect.callQuietly(current, "close", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
        }
    }
}
