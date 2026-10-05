package com.naturalinteraction.LEA.capture;

import com.naturalinteraction.LEA.LeaConfig;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.lang.reflect.Method;

public final class LeaOpenCvFrameSource implements LeaFrameSource {

    private static final String CAPTURE_CLASS = "org.opencv.videoio.VideoCapture";
    private static final String VIDEOIO_CLASS = "org.opencv.videoio.Videoio";
    private static final String MAT_CLASS = "org.opencv.core.Mat";

    private final LeaConfig config;
    private Object capture;
    private Object buffer;
    private Method readMethod;
    private Method rowsMethod;
    private Method colsMethod;
    private Method elementSizeMethod;
    private Method getMethod;
    private boolean open;
    private int frameRate;
    private volatile String lastError = "";

    public static boolean isAvailable() {
        return LeaReflect.find(CAPTURE_CLASS) != null && LeaReflect.find(MAT_CLASS) != null;
    }

    public LeaOpenCvFrameSource(LeaConfig config) {
        this.config = config;
        this.frameRate = config.targetFps;
        try {
            Class<?> captureType = LeaReflect.find(CAPTURE_CLASS);
            Class<?> matType = LeaReflect.find(MAT_CLASS);
            if (captureType == null || matType == null) {
                lastError = "opencv is not on the classpath";
                return;
            }
            Object device = LeaReflect.construct(captureType, new Class<?>[]{int.class},
                    new Object[]{config.captureDeviceIndex});
            Object opened = LeaReflect.call(device, "isOpened", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
            if (!(opened instanceof Boolean) || !((Boolean) opened).booleanValue()) {
                lastError = "camera index " + config.captureDeviceIndex + " could not be opened";
                LeaReflect.callQuietly(device, "release", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
                return;
            }

            Class<?> videoioType = LeaReflect.find(VIDEOIO_CLASS);
            applyProperty(device, videoioType, "CAP_PROP_FRAME_WIDTH", config.cameraWidth);
            applyProperty(device, videoioType, "CAP_PROP_FRAME_HEIGHT", config.cameraHeight);

            this.capture = device;
            this.buffer = LeaReflect.construct(matType, LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
            this.readMethod = captureType.getMethod("read", matType);
            this.rowsMethod = matType.getMethod("rows");
            this.colsMethod = matType.getMethod("cols");
            this.elementSizeMethod = matType.getMethod("elemSize");
            this.getMethod = matType.getMethod("get", int.class, int.class, byte[].class);
            this.open = true;
            this.frameRate = readFrameRate(device, videoioType);
        } catch (Throwable t) {
            lastError = LeaReflect.describe(t);
            this.capture = null;
            this.buffer = null;
            this.open = false;
        }
    }

    private void applyProperty(Object device, Class<?> videoioType, String name, double value) {
        if (videoioType == null) {
            return;
        }
        Object constant = LeaReflect.field(videoioType, name);
        if (!(constant instanceof Integer)) {
            return;
        }
        LeaReflect.callQuietly(device, "set", new Class<?>[]{int.class, double.class},
                new Object[]{constant, value});
    }

    private int readFrameRate(Object device, Class<?> videoioType) {
        if (videoioType == null) {
            return config.targetFps;
        }
        Object constant = LeaReflect.field(videoioType, "CAP_PROP_FPS");
        if (!(constant instanceof Integer)) {
            return config.targetFps;
        }
        Object value = LeaReflect.callQuietly(device, "get", new Class<?>[]{int.class},
                new Object[]{constant});
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
        if (!open || capture == null || buffer == null) {
            return null;
        }
        try {
            Object result = readMethod.invoke(capture, buffer);
            if (!(result instanceof Boolean) || !((Boolean) result).booleanValue()) {
                lastError = "frame read failed";
                return null;
            }
            int rows = ((Number) rowsMethod.invoke(buffer)).intValue();
            int cols = ((Number) colsMethod.invoke(buffer)).intValue();
            long elementSize = ((Number) elementSizeMethod.invoke(buffer)).longValue();
            if (rows <= 0 || cols <= 0) {
                lastError = "empty frame";
                return null;
            }
            int type;
            if (elementSize == 1L) {
                type = BufferedImage.TYPE_BYTE_GRAY;
            } else if (elementSize == 3L) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            } else {
                lastError = "unsupported channel count: " + elementSize;
                return null;
            }
            byte[] data = new byte[rows * cols * (int) elementSize];
            getMethod.invoke(buffer, 0, 0, data);
            BufferedImage image = new BufferedImage(cols, rows, type);
            byte[] target = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(data, 0, target, 0, Math.min(data.length, target.length));
            return image;
        } catch (Throwable t) {
            lastError = LeaReflect.describe(t);
            return null;
        }
    }

    @Override
    public String getName() {
        return "opencv-videoio";
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void close() {
        open = false;
        Object device = capture;
        capture = null;
        buffer = null;
        if (device != null) {
            LeaReflect.callQuietly(device, "release", LeaReflect.NO_TYPES, LeaReflect.NO_ARGS);
        }
    }
}
