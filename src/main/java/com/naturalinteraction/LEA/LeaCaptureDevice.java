package com.naturalinteraction.LEA;

import com.naturalinteraction.LEA.capture.LeaFrameSource;
import com.naturalinteraction.LEA.capture.LeaFrameSources;

import java.awt.image.BufferedImage;

import de.darkblue.lea.ifaces.ICaptureDevice;

public final class LeaCaptureDevice implements ICaptureDevice {

    private final LeaConfig config;
    private volatile LeaFrameSource source;
    private volatile String sourceName = "none";
    private volatile String lastError = "";

    public LeaCaptureDevice(LeaConfig config) {
        this.config = config;
    }

    public boolean open() {
        close();
        LeaFrameSource opened = LeaFrameSources.open(config);
        if (opened == null) {
            lastError = LeaFrameSources.getLastError();
            sourceName = "none";
            return false;
        }
        source = opened;
        sourceName = opened.getName();
        lastError = "";
        return true;
    }

    @Override
    public int getFrameRate() {
        LeaFrameSource current = source;
        int fps = current == null ? 0 : current.getFrameRate();
        if (fps <= 0) {
            fps = config.targetFps;
        }
        return Math.max(1, Math.min(config.maxFrameRate, fps));
    }

    @Override
    public BufferedImage getImage() {
        LeaFrameSource current = source;
        if (current == null) {
            return null;
        }
        BufferedImage image = current.grab();
        if (image == null) {
            String message = current.getLastError();
            if (message != null && !message.isEmpty()) {
                lastError = message;
            }
        }
        return image;
    }

    @Override
    public void close() {
        LeaFrameSource current = source;
        source = null;
        if (current != null) {
            try {
                current.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean isOpen() {
        LeaFrameSource current = source;
        return current != null && current.isOpen();
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getLastError() {
        return lastError;
    }

    public LeaFrameSource getSource() {
        return source;
    }
}
