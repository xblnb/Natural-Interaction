package com.naturalinteraction.LEA;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class LeaConfig {

    public static final Path DEFAULT_PATH = Paths.get("config", "financialmarket-lea.properties");

    private Path path = DEFAULT_PATH;

    public LeaCaptureBackend captureBackend = LeaCaptureBackend.AUTO;
    public int captureDeviceIndex = 0;
    public int cameraWidth = 640;
    public int cameraHeight = 480;
    public int targetFps = 20;
    public int maxFrameRate = 60;
    public int detectionWidth = 320;
    public String replayDirectory = "lea-frames";
    public boolean replayLoop = true;

    public LeaTrackingMode trackingMode = LeaTrackingMode.ABSOLUTE;
    public LeaClickMode clickMode = LeaClickMode.DWELL;
    public float gazeGainX = 6f;
    public float gazeGainY = 6f;
    public float gazeDeadzone = 0.012f;
    public float gazeSmoothing = 0.45f;
    public float sampleSmoothing = 0.5f;
    public float maxEyeJumpRatio = 0.35f;
    public int eyeLostFrames = 6;
    public int faceLostFrames = 5;
    public float minFaceWidth = 40f;

    public LeaFaceSource faceSource = LeaFaceSource.AUTO;
    public String faceModelPath = "face-models/face_detection_yunet_2023mar.onnx";
    public float faceScoreThreshold = 0.7f;
    public float faceNmsThreshold = 0.3f;
    public float faceMinSize = 60f;
    public float faceBoxSmoothing = 0.5f;
    public boolean eyeRefineEnabled = true;
    public float eyeSearchRadius = 0.08f;
    public float eyeDarkestFraction = 0.05f;
    public int eyeDarknessTolerance = 12;
    public boolean gazeNormalizeByInterOcular = true;
    public String neutralMode = "BOX";

    public float stepSize = 0.05f;
    public float stepDeadzone = 0.02f;
    public long stepInitialDelayMs = 450L;
    public long stepRepeatMs = 320L;

    public long dwellTimeMs = 900L;
    public float dwellMovementThreshold = 0.015f;
    public float dwellReleaseThreshold = 0.06f;
    public long dwellRefractoryMs = 400L;
    public long dwellMaxHoldMs = 8000L;
    public boolean dwellDragEnabled = true;
    public long dwellHoldDelayMs = 350L;
    public long clickHoldMs = 60L;

    public boolean enableBlinkDetection = true;
    public float blinkThreshold = 0.55f;
    public float blinkOpenThreshold = 0.75f;
    public long blinkMinDurationMs = 60L;
    public long blinkMaxDurationMs = 700L;
    public long blinkRefractoryMs = 350L;
    public boolean enableWink = false;
    public long winkMinDurationMs = 120L;
    public boolean enableDoubleBlink = false;
    public long doubleBlinkMaxGapMs = 400L;
    public float opennessBaselineAlpha = 0.05f;
    public float opennessMinBaseline = 6f;
    public float opennessWindowWidth = 0.24f;
    public float opennessWindowHeight = 0.18f;
    public float opennessLeftCenter = 0.30f;
    public float opennessRightCenter = 0.70f;
    public LeaTouchAction winkLeftAction = LeaTouchAction.SECONDARY_CLICK;
    public LeaTouchAction winkRightAction = LeaTouchAction.CANCEL;
    public LeaTouchAction doubleBlinkAction = LeaTouchAction.TOGGLE_DRAG;

    public boolean gesturesEnabled = true;
    public long gestureHoldMs = 700L;
    public float gestureDeadzone = 0.035f;
    public float gestureFarThreshold = 0.075f;

    public int calibrationSamples = 30;
    public float calibrationMaxSpread = 0.02f;
    public long calibrationCooldownMs = 5000L;
    public boolean autoCalibrateWhenUncalibrated = true;
    public boolean hasNeutral = false;
    public float neutralRelX = 0f;
    public float neutralRelY = 0f;

    public boolean debugLogging = false;

    private LeaConfig() {
    }

    public static LeaConfig createDefault() {
        LeaConfig config = new LeaConfig();
        config.sanitize();
        return config;
    }

    public static LeaConfig load() {
        return load(DEFAULT_PATH);
    }

    public static LeaConfig load(Path file) {
        LeaConfig config = new LeaConfig();
        if (file != null) {
            config.path = file;
        }
        config.reload();
        return config;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path file) {
        if (file != null) {
            this.path = file;
        }
    }

    public void reload() {
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException ignored) {
            }
        }
        apply(properties);
        sanitize();
    }

    public void save() {
        Properties properties = new Properties();
        collect(properties);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ignored) {
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "LEA accessibility settings");
        } catch (IOException ignored) {
        }
    }

    public void setNeutral(float relX, float relY) {
        this.neutralRelX = relX;
        this.neutralRelY = relY;
        this.hasNeutral = true;
    }

    public Path resolveFaceModelPath() {
        Path configured = Paths.get(faceModelPath);
        if (configured.isAbsolute()) {
            return configured;
        }
        Path candidate = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 4 && candidate != null; depth++) {
            Path resolved = candidate.resolve(configured);
            if (Files.isRegularFile(resolved)) {
                return resolved;
            }
            candidate = candidate.getParent();
        }
        return Paths.get("").toAbsolutePath().resolve(configured);
    }

    public void clearNeutral() {
        this.hasNeutral = false;
        this.neutralRelX = 0f;
        this.neutralRelY = 0f;
    }

    public void sanitize() {
        captureDeviceIndex = clampInt(captureDeviceIndex, 0, 15);
        cameraWidth = clampInt(cameraWidth, 80, 3840);
        cameraHeight = clampInt(cameraHeight, 60, 2160);
        targetFps = clampInt(targetFps, 1, 60);
        maxFrameRate = clampInt(maxFrameRate, 1, 120);
        detectionWidth = clampInt(detectionWidth, 80, 1280);
        if (replayDirectory == null || replayDirectory.isEmpty()) {
            replayDirectory = "lea-frames";
        }

        gazeGainX = clampFloat(gazeGainX, 0.5f, 40f);
        gazeGainY = clampFloat(gazeGainY, 0.5f, 40f);
        gazeDeadzone = clampFloat(gazeDeadzone, 0f, 0.2f);
        gazeSmoothing = clampFloat(gazeSmoothing, 0.05f, 1f);
        sampleSmoothing = clampFloat(sampleSmoothing, 0.05f, 1f);
        maxEyeJumpRatio = clampFloat(maxEyeJumpRatio, 0.05f, 2f);
        eyeLostFrames = clampInt(eyeLostFrames, 1, 600);
        faceLostFrames = clampInt(faceLostFrames, 1, 600);
        minFaceWidth = clampFloat(minFaceWidth, 16f, 400f);

        if (faceModelPath == null || faceModelPath.isEmpty()) {
            faceModelPath = "face-models/face_detection_yunet_2023mar.onnx";
        }
        faceScoreThreshold = clampFloat(faceScoreThreshold, 0.1f, 0.99f);
        faceNmsThreshold = clampFloat(faceNmsThreshold, 0.05f, 0.95f);
        faceMinSize = clampFloat(faceMinSize, 16f, 1000f);
        faceBoxSmoothing = clampFloat(faceBoxSmoothing, 0.05f, 1f);
        eyeSearchRadius = clampFloat(eyeSearchRadius, 0.01f, 0.4f);
        eyeDarkestFraction = clampFloat(eyeDarkestFraction, 0.005f, 0.3f);
        eyeDarknessTolerance = Math.max(1, Math.min(80, eyeDarknessTolerance));
        if (neutralMode == null || neutralMode.isEmpty()) {
            neutralMode = "BOX";
        }

        stepSize = clampFloat(stepSize, 0.005f, 0.5f);
        stepDeadzone = clampFloat(stepDeadzone, 0.001f, 0.3f);
        stepInitialDelayMs = clampLong(stepInitialDelayMs, 0L, 10000L);
        stepRepeatMs = clampLong(stepRepeatMs, 40L, 5000L);

        dwellTimeMs = clampLong(dwellTimeMs, 120L, 10000L);
        dwellMovementThreshold = clampFloat(dwellMovementThreshold, 0.001f, 0.3f);
        dwellReleaseThreshold = Math.max(dwellMovementThreshold * 1.5f,
                clampFloat(dwellReleaseThreshold, 0.002f, 0.5f));
        dwellRefractoryMs = clampLong(dwellRefractoryMs, 0L, 10000L);
        dwellMaxHoldMs = clampLong(dwellMaxHoldMs, 300L, 120000L);
        clickHoldMs = clampLong(clickHoldMs, 10L, 2000L);
        dwellHoldDelayMs = clampLong(dwellHoldDelayMs, 0L, 5000L);

        blinkThreshold = clampFloat(blinkThreshold, 0.05f, 0.95f);
        blinkOpenThreshold = Math.max(blinkThreshold + 0.05f,
                clampFloat(blinkOpenThreshold, 0.1f, 1.5f));
        blinkMinDurationMs = clampLong(blinkMinDurationMs, 10L, 2000L);
        blinkMaxDurationMs = Math.max(blinkMinDurationMs + 10L,
                clampLong(blinkMaxDurationMs, 20L, 5000L));
        blinkRefractoryMs = clampLong(blinkRefractoryMs, 0L, 5000L);
        winkMinDurationMs = clampLong(winkMinDurationMs, 20L, 2000L);
        doubleBlinkMaxGapMs = clampLong(doubleBlinkMaxGapMs, 80L, 2000L);
        opennessBaselineAlpha = clampFloat(opennessBaselineAlpha, 0.005f, 0.5f);
        opennessMinBaseline = clampFloat(opennessMinBaseline, 1f, 60f);
        opennessWindowWidth = clampFloat(opennessWindowWidth, 0.05f, 0.6f);
        opennessWindowHeight = clampFloat(opennessWindowHeight, 0.05f, 0.6f);
        opennessLeftCenter = clampFloat(opennessLeftCenter, 0.05f, 0.5f);
        opennessRightCenter = clampFloat(opennessRightCenter, 0.5f, 0.95f);

        gestureHoldMs = clampLong(gestureHoldMs, 80L, 5000L);
        gestureDeadzone = clampFloat(gestureDeadzone, 0.002f, 0.3f);
        gestureFarThreshold = Math.max(gestureDeadzone * 1.2f,
                clampFloat(gestureFarThreshold, 0.003f, 0.5f));

        calibrationSamples = clampInt(calibrationSamples, 5, 600);
        calibrationMaxSpread = clampFloat(calibrationMaxSpread, 0.001f, 0.2f);
        calibrationCooldownMs = clampLong(calibrationCooldownMs, 0L, 600000L);
        neutralRelX = clampFloat(neutralRelX, -1f, 2f);
        neutralRelY = clampFloat(neutralRelY, -1f, 2f);
    }

    private void apply(Properties p) {
        captureBackend = enumOf(p, "captureBackend", LeaCaptureBackend.class, captureBackend);
        captureDeviceIndex = intOf(p, "captureDeviceIndex", captureDeviceIndex);
        cameraWidth = intOf(p, "cameraWidth", cameraWidth);
        cameraHeight = intOf(p, "cameraHeight", cameraHeight);
        targetFps = intOf(p, "targetFps", targetFps);
        maxFrameRate = intOf(p, "maxFrameRate", maxFrameRate);
        detectionWidth = intOf(p, "detectionWidth", detectionWidth);
        replayDirectory = stringOf(p, "replayDirectory", replayDirectory);
        replayLoop = boolOf(p, "replayLoop", replayLoop);

        trackingMode = enumOf(p, "trackingMode", LeaTrackingMode.class, trackingMode);
        clickMode = enumOf(p, "clickMode", LeaClickMode.class, clickMode);
        gazeGainX = floatOf(p, "gazeGainX", gazeGainX);
        gazeGainY = floatOf(p, "gazeGainY", gazeGainY);
        gazeDeadzone = floatOf(p, "gazeDeadzone", gazeDeadzone);
        gazeSmoothing = floatOf(p, "gazeSmoothing", gazeSmoothing);
        sampleSmoothing = floatOf(p, "sampleSmoothing", sampleSmoothing);
        maxEyeJumpRatio = floatOf(p, "maxEyeJumpRatio", maxEyeJumpRatio);
        eyeLostFrames = intOf(p, "eyeLostFrames", eyeLostFrames);
        faceLostFrames = intOf(p, "faceLostFrames", faceLostFrames);
        minFaceWidth = floatOf(p, "minFaceWidth", minFaceWidth);

        faceSource = enumOf(p, "faceSource", LeaFaceSource.class, faceSource);
        faceModelPath = stringOf(p, "faceModelPath", faceModelPath);
        faceScoreThreshold = floatOf(p, "faceScoreThreshold", faceScoreThreshold);
        faceNmsThreshold = floatOf(p, "faceNmsThreshold", faceNmsThreshold);
        faceMinSize = floatOf(p, "faceMinSize", faceMinSize);
        faceBoxSmoothing = floatOf(p, "faceBoxSmoothing", faceBoxSmoothing);
        eyeRefineEnabled = boolOf(p, "eyeRefineEnabled", eyeRefineEnabled);
        eyeSearchRadius = floatOf(p, "eyeSearchRadius", eyeSearchRadius);
        eyeDarkestFraction = floatOf(p, "eyeDarkestFraction", eyeDarkestFraction);
        eyeDarknessTolerance = intOf(p, "eyeDarknessTolerance", eyeDarknessTolerance);
        gazeNormalizeByInterOcular = boolOf(p, "gazeNormalizeByInterOcular", gazeNormalizeByInterOcular);
        neutralMode = stringOf(p, "neutralMode", neutralMode);

        stepSize = floatOf(p, "stepSize", stepSize);
        stepDeadzone = floatOf(p, "stepDeadzone", stepDeadzone);
        stepInitialDelayMs = longOf(p, "stepInitialDelayMs", stepInitialDelayMs);
        stepRepeatMs = longOf(p, "stepRepeatMs", stepRepeatMs);

        dwellTimeMs = longOf(p, "dwellTimeMs", dwellTimeMs);
        dwellMovementThreshold = floatOf(p, "dwellMovementThreshold", dwellMovementThreshold);
        dwellReleaseThreshold = floatOf(p, "dwellReleaseThreshold", dwellReleaseThreshold);
        dwellRefractoryMs = longOf(p, "dwellRefractoryMs", dwellRefractoryMs);
        dwellMaxHoldMs = longOf(p, "dwellMaxHoldMs", dwellMaxHoldMs);
        dwellDragEnabled = boolOf(p, "dwellDragEnabled", dwellDragEnabled);
        dwellHoldDelayMs = longOf(p, "dwellHoldDelayMs", dwellHoldDelayMs);
        clickHoldMs = longOf(p, "clickHoldMs", clickHoldMs);

        enableBlinkDetection = boolOf(p, "enableBlinkDetection", enableBlinkDetection);
        blinkThreshold = floatOf(p, "blinkThreshold", blinkThreshold);
        blinkOpenThreshold = floatOf(p, "blinkOpenThreshold", blinkOpenThreshold);
        blinkMinDurationMs = longOf(p, "blinkMinDurationMs", blinkMinDurationMs);
        blinkMaxDurationMs = longOf(p, "blinkMaxDurationMs", blinkMaxDurationMs);
        blinkRefractoryMs = longOf(p, "blinkRefractoryMs", blinkRefractoryMs);
        enableWink = boolOf(p, "enableWink", enableWink);
        winkMinDurationMs = longOf(p, "winkMinDurationMs", winkMinDurationMs);
        enableDoubleBlink = boolOf(p, "enableDoubleBlink", enableDoubleBlink);
        doubleBlinkMaxGapMs = longOf(p, "doubleBlinkMaxGapMs", doubleBlinkMaxGapMs);
        opennessBaselineAlpha = floatOf(p, "opennessBaselineAlpha", opennessBaselineAlpha);
        opennessMinBaseline = floatOf(p, "opennessMinBaseline", opennessMinBaseline);
        opennessWindowWidth = floatOf(p, "opennessWindowWidth", opennessWindowWidth);
        opennessWindowHeight = floatOf(p, "opennessWindowHeight", opennessWindowHeight);
        opennessLeftCenter = floatOf(p, "opennessLeftCenter", opennessLeftCenter);
        opennessRightCenter = floatOf(p, "opennessRightCenter", opennessRightCenter);
        winkLeftAction = enumOf(p, "winkLeftAction", LeaTouchAction.class, winkLeftAction);
        winkRightAction = enumOf(p, "winkRightAction", LeaTouchAction.class, winkRightAction);
        doubleBlinkAction = enumOf(p, "doubleBlinkAction", LeaTouchAction.class, doubleBlinkAction);

        gesturesEnabled = boolOf(p, "gesturesEnabled", gesturesEnabled);
        gestureHoldMs = longOf(p, "gestureHoldMs", gestureHoldMs);
        gestureDeadzone = floatOf(p, "gestureDeadzone", gestureDeadzone);
        gestureFarThreshold = floatOf(p, "gestureFarThreshold", gestureFarThreshold);

        calibrationSamples = intOf(p, "calibrationSamples", calibrationSamples);
        calibrationMaxSpread = floatOf(p, "calibrationMaxSpread", calibrationMaxSpread);
        calibrationCooldownMs = longOf(p, "calibrationCooldownMs", calibrationCooldownMs);
        autoCalibrateWhenUncalibrated = boolOf(p, "autoCalibrateWhenUncalibrated", autoCalibrateWhenUncalibrated);
        hasNeutral = boolOf(p, "hasNeutral", hasNeutral);
        neutralRelX = floatOf(p, "neutralRelX", neutralRelX);
        neutralRelY = floatOf(p, "neutralRelY", neutralRelY);

        debugLogging = boolOf(p, "debugLogging", debugLogging);
    }

    private void collect(Properties p) {
        p.setProperty("captureBackend", captureBackend.name());
        p.setProperty("captureDeviceIndex", Integer.toString(captureDeviceIndex));
        p.setProperty("cameraWidth", Integer.toString(cameraWidth));
        p.setProperty("cameraHeight", Integer.toString(cameraHeight));
        p.setProperty("targetFps", Integer.toString(targetFps));
        p.setProperty("maxFrameRate", Integer.toString(maxFrameRate));
        p.setProperty("detectionWidth", Integer.toString(detectionWidth));
        p.setProperty("replayDirectory", replayDirectory);
        p.setProperty("replayLoop", Boolean.toString(replayLoop));

        p.setProperty("trackingMode", trackingMode.name());
        p.setProperty("clickMode", clickMode.name());
        p.setProperty("gazeGainX", Float.toString(gazeGainX));
        p.setProperty("gazeGainY", Float.toString(gazeGainY));
        p.setProperty("gazeDeadzone", Float.toString(gazeDeadzone));
        p.setProperty("gazeSmoothing", Float.toString(gazeSmoothing));
        p.setProperty("sampleSmoothing", Float.toString(sampleSmoothing));
        p.setProperty("maxEyeJumpRatio", Float.toString(maxEyeJumpRatio));
        p.setProperty("eyeLostFrames", Integer.toString(eyeLostFrames));
        p.setProperty("faceLostFrames", Integer.toString(faceLostFrames));
        p.setProperty("minFaceWidth", Float.toString(minFaceWidth));

        p.setProperty("faceSource", faceSource.name());
        p.setProperty("faceModelPath", faceModelPath);
        p.setProperty("faceScoreThreshold", Float.toString(faceScoreThreshold));
        p.setProperty("faceNmsThreshold", Float.toString(faceNmsThreshold));
        p.setProperty("faceMinSize", Float.toString(faceMinSize));
        p.setProperty("faceBoxSmoothing", Float.toString(faceBoxSmoothing));
        p.setProperty("eyeRefineEnabled", Boolean.toString(eyeRefineEnabled));
        p.setProperty("eyeSearchRadius", Float.toString(eyeSearchRadius));
        p.setProperty("eyeDarkestFraction", Float.toString(eyeDarkestFraction));
        p.setProperty("eyeDarknessTolerance", Integer.toString(eyeDarknessTolerance));
        p.setProperty("gazeNormalizeByInterOcular", Boolean.toString(gazeNormalizeByInterOcular));
        p.setProperty("neutralMode", neutralMode);

        p.setProperty("stepSize", Float.toString(stepSize));
        p.setProperty("stepDeadzone", Float.toString(stepDeadzone));
        p.setProperty("stepInitialDelayMs", Long.toString(stepInitialDelayMs));
        p.setProperty("stepRepeatMs", Long.toString(stepRepeatMs));

        p.setProperty("dwellTimeMs", Long.toString(dwellTimeMs));
        p.setProperty("dwellMovementThreshold", Float.toString(dwellMovementThreshold));
        p.setProperty("dwellReleaseThreshold", Float.toString(dwellReleaseThreshold));
        p.setProperty("dwellRefractoryMs", Long.toString(dwellRefractoryMs));
        p.setProperty("dwellMaxHoldMs", Long.toString(dwellMaxHoldMs));
        p.setProperty("dwellDragEnabled", Boolean.toString(dwellDragEnabled));
        p.setProperty("dwellHoldDelayMs", Long.toString(dwellHoldDelayMs));
        p.setProperty("clickHoldMs", Long.toString(clickHoldMs));

        p.setProperty("enableBlinkDetection", Boolean.toString(enableBlinkDetection));
        p.setProperty("blinkThreshold", Float.toString(blinkThreshold));
        p.setProperty("blinkOpenThreshold", Float.toString(blinkOpenThreshold));
        p.setProperty("blinkMinDurationMs", Long.toString(blinkMinDurationMs));
        p.setProperty("blinkMaxDurationMs", Long.toString(blinkMaxDurationMs));
        p.setProperty("blinkRefractoryMs", Long.toString(blinkRefractoryMs));
        p.setProperty("enableWink", Boolean.toString(enableWink));
        p.setProperty("winkMinDurationMs", Long.toString(winkMinDurationMs));
        p.setProperty("enableDoubleBlink", Boolean.toString(enableDoubleBlink));
        p.setProperty("doubleBlinkMaxGapMs", Long.toString(doubleBlinkMaxGapMs));
        p.setProperty("opennessBaselineAlpha", Float.toString(opennessBaselineAlpha));
        p.setProperty("opennessMinBaseline", Float.toString(opennessMinBaseline));
        p.setProperty("opennessWindowWidth", Float.toString(opennessWindowWidth));
        p.setProperty("opennessWindowHeight", Float.toString(opennessWindowHeight));
        p.setProperty("opennessLeftCenter", Float.toString(opennessLeftCenter));
        p.setProperty("opennessRightCenter", Float.toString(opennessRightCenter));
        p.setProperty("winkLeftAction", winkLeftAction.name());
        p.setProperty("winkRightAction", winkRightAction.name());
        p.setProperty("doubleBlinkAction", doubleBlinkAction.name());

        p.setProperty("gesturesEnabled", Boolean.toString(gesturesEnabled));
        p.setProperty("gestureHoldMs", Long.toString(gestureHoldMs));
        p.setProperty("gestureDeadzone", Float.toString(gestureDeadzone));
        p.setProperty("gestureFarThreshold", Float.toString(gestureFarThreshold));

        p.setProperty("calibrationSamples", Integer.toString(calibrationSamples));
        p.setProperty("calibrationMaxSpread", Float.toString(calibrationMaxSpread));
        p.setProperty("calibrationCooldownMs", Long.toString(calibrationCooldownMs));
        p.setProperty("autoCalibrateWhenUncalibrated", Boolean.toString(autoCalibrateWhenUncalibrated));
        p.setProperty("hasNeutral", Boolean.toString(hasNeutral));
        p.setProperty("neutralRelX", Float.toString(neutralRelX));
        p.setProperty("neutralRelY", Float.toString(neutralRelY));

        p.setProperty("debugLogging", Boolean.toString(debugLogging));
    }

    private static int intOf(Properties p, String key, int fallback) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long longOf(Properties p, String key, long fallback) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float floatOf(Properties p, String key, float fallback) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean boolOf(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static String stringOf(Properties p, String key, String fallback) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        return raw.trim();
    }

    private static <E extends Enum<E>> E enumOf(Properties p, String key, Class<E> type, E fallback) {
        String raw = p.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        if (Float.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
