package com.natural_interaction.LEA.capture;

import com.natural_interaction.LEA.LeaCaptureBackend;
import com.natural_interaction.LEA.LeaConfig;

public final class LeaFrameSources {

    private static final LeaCaptureBackend[] AUTO_ORDER = {
            LeaCaptureBackend.SARXOS,
            LeaCaptureBackend.OPENCV,
            LeaCaptureBackend.JMF,
            LeaCaptureBackend.REPLAY
    };

    private static volatile String lastError = "";

    private LeaFrameSources() {
    }

    public static LeaFrameSource open(LeaConfig config) {
        StringBuilder errors = new StringBuilder();
        for (LeaCaptureBackend backend : order(config.captureBackend)) {
            LeaFrameSource source = null;
            try {
                source = create(backend, config);
                if (source != null && source.isOpen()) {
                    lastError = "";
                    return source;
                }
            } catch (Throwable t) {
                errors.append(backend.name()).append(": ").append(LeaReflect.describe(t)).append("; ");
                source = null;
            }
            if (source != null) {
                String message = source.getLastError();
                errors.append(backend.name()).append(": ")
                        .append(message == null || message.isEmpty() ? "not available" : message)
                        .append("; ");
                try {
                    source.close();
                } catch (Throwable ignored) {
                }
            }
        }
        String collected = errors.toString().trim();
        lastError = collected.isEmpty() ? "no capture backend available" : collected;
        return null;
    }

    public static String getLastError() {
        return lastError;
    }

    private static LeaCaptureBackend[] order(LeaCaptureBackend requested) {
        if (requested != null && requested != LeaCaptureBackend.AUTO) {
            return new LeaCaptureBackend[]{requested};
        }
        return AUTO_ORDER;
    }

    private static LeaFrameSource create(LeaCaptureBackend backend, LeaConfig config) {
        switch (backend) {
            case SARXOS:
                return new LeaSarxosFrameSource(config);
            case OPENCV:
                return new LeaOpenCvFrameSource(config);
            case JMF:
                return new LeaJmfFrameSource(config);
            case REPLAY:
                return new LeaReplayFrameSource(config);
            default:
                return null;
        }
    }
}
