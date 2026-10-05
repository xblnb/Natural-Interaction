package com.natural_interaction.LibreFace;

import com.natural_interaction.LEA.LeaFacialGesture;
import com.natural_interaction.LEA.LeaTouchAction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class LibreFaceConfig {

    public static final Path DEFAULT_PATH = Paths.get("config", "financialmarket-libreface.properties");

    private Path path = DEFAULT_PATH;

    public boolean enabled = true;
    public String modelDirectory = "libreface-models";
    public int inputSize = 224;
    public float cropPadding = 0.25f;
    public float cropOffsetY = 0f;
    public boolean alignByEyes = true;
    public int alignSize = 256;
    public long inferenceIntervalMs = 150L;
    public float auIntensityScale = 5f;
    public boolean runAuIntensity = true;
    public boolean runAuPresence = true;
    public boolean runFacialExpression = true;
    public float auThreshold = 2f;
    public float mouthOpenThreshold = 2.5f;
    public float expressionThreshold = 0.55f;
    public long gestureHoldMs = 300L;
    public long gestureRefractoryMs = 800L;
    public String enabledGestures = "MOUTH_OPEN,SMILE,SURPRISE";
    public String gestureActions = "MOUTH_OPEN=PRIMARY_CLICK,SMILE=SECONDARY_CLICK,SURPRISE=CANCEL";
    public String gestureThresholds = "";
    public boolean debugLogging = false;

    private LibreFaceConfig() {
    }

    public static LibreFaceConfig createDefault() {
        LibreFaceConfig config = new LibreFaceConfig();
        config.sanitize();
        return config;
    }

    public static LibreFaceConfig load() {
        return load(DEFAULT_PATH);
    }

    public static LibreFaceConfig load(Path file) {
        LibreFaceConfig config = new LibreFaceConfig();
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
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("modelDirectory", modelDirectory);
        properties.setProperty("inputSize", Integer.toString(inputSize));
        properties.setProperty("cropPadding", Float.toString(cropPadding));
        properties.setProperty("cropOffsetY", Float.toString(cropOffsetY));
        properties.setProperty("alignByEyes", Boolean.toString(alignByEyes));
        properties.setProperty("alignSize", Integer.toString(alignSize));
        properties.setProperty("inferenceIntervalMs", Long.toString(inferenceIntervalMs));
        properties.setProperty("auIntensityScale", Float.toString(auIntensityScale));
        properties.setProperty("runAuIntensity", Boolean.toString(runAuIntensity));
        properties.setProperty("runAuPresence", Boolean.toString(runAuPresence));
        properties.setProperty("runFacialExpression", Boolean.toString(runFacialExpression));
        properties.setProperty("auThreshold", Float.toString(auThreshold));
        properties.setProperty("mouthOpenThreshold", Float.toString(mouthOpenThreshold));
        properties.setProperty("expressionThreshold", Float.toString(expressionThreshold));
        properties.setProperty("gestureHoldMs", Long.toString(gestureHoldMs));
        properties.setProperty("gestureRefractoryMs", Long.toString(gestureRefractoryMs));
        properties.setProperty("enabledGestures", enabledGestures);
        properties.setProperty("gestureActions", gestureActions);
        properties.setProperty("gestureThresholds", gestureThresholds);
        properties.setProperty("debugLogging", Boolean.toString(debugLogging));
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ignored) {
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "LibreFace ONNX settings");
        } catch (IOException ignored) {
        }
    }

    private void apply(Properties p) {
        enabled = boolOf(p, "enabled", enabled);
        modelDirectory = stringOf(p, "modelDirectory", modelDirectory);
        inputSize = intOf(p, "inputSize", inputSize);
        cropPadding = floatOf(p, "cropPadding", cropPadding);
        cropOffsetY = floatOf(p, "cropOffsetY", cropOffsetY);
        alignByEyes = boolOf(p, "alignByEyes", alignByEyes);
        alignSize = intOf(p, "alignSize", alignSize);
        inferenceIntervalMs = longOf(p, "inferenceIntervalMs", inferenceIntervalMs);
        auIntensityScale = floatOf(p, "auIntensityScale", auIntensityScale);
        runAuIntensity = boolOf(p, "runAuIntensity", runAuIntensity);
        runAuPresence = boolOf(p, "runAuPresence", runAuPresence);
        runFacialExpression = boolOf(p, "runFacialExpression", runFacialExpression);
        auThreshold = floatOf(p, "auThreshold", auThreshold);
        mouthOpenThreshold = floatOf(p, "mouthOpenThreshold", mouthOpenThreshold);
        expressionThreshold = floatOf(p, "expressionThreshold", expressionThreshold);
        gestureHoldMs = longOf(p, "gestureHoldMs", gestureHoldMs);
        gestureRefractoryMs = longOf(p, "gestureRefractoryMs", gestureRefractoryMs);
        enabledGestures = stringOf(p, "enabledGestures", enabledGestures);
        gestureActions = stringOf(p, "gestureActions", gestureActions);
        gestureThresholds = stringOf(p, "gestureThresholds", gestureThresholds);
        debugLogging = boolOf(p, "debugLogging", debugLogging);
    }

    public void sanitize() {
        if (modelDirectory == null || modelDirectory.isEmpty()) {
            modelDirectory = "libreface-models";
        }
        inputSize = Math.max(112, Math.min(512, inputSize));
        cropPadding = clampFloat(cropPadding, 0f, 1.5f);
        cropOffsetY = clampFloat(cropOffsetY, -0.5f, 0.5f);
        alignSize = Math.max(128, Math.min(512, alignSize));
        inferenceIntervalMs = Math.max(50L, Math.min(5000L, inferenceIntervalMs));
        auIntensityScale = clampFloat(auIntensityScale, 0.2f, 10f);
        auThreshold = clampFloat(auThreshold, 0f, 5f);
        mouthOpenThreshold = clampFloat(mouthOpenThreshold, 0f, 5f);
        expressionThreshold = clampFloat(expressionThreshold, 0.05f, 1f);
        gestureHoldMs = Math.max(0L, Math.min(5000L, gestureHoldMs));
        gestureRefractoryMs = Math.max(0L, Math.min(10000L, gestureRefractoryMs));
        if (enabledGestures == null) {
            enabledGestures = "";
        }
        if (gestureActions == null) {
            gestureActions = "";
        }
        if (gestureThresholds == null) {
            gestureThresholds = "";
        }
    }

    public Set<LeaFacialGesture> getEnabledGestureSet() {
        Set<LeaFacialGesture> gestures = EnumSet.noneOf(LeaFacialGesture.class);
        for (String token : split(enabledGestures)) {
            LeaFacialGesture gesture = gestureOf(token);
            if (gesture != null) {
                gestures.add(gesture);
            }
        }
        return gestures;
    }

    public LeaTouchAction getGestureAction(LeaFacialGesture gesture) {
        if (gesture == null) {
            return LeaTouchAction.NONE;
        }
        Map<String, String> entries = parseAssignments(gestureActions);
        String value = entries.get(gesture.name());
        if (value == null) {
            return LeaTouchAction.NONE;
        }
        try {
            return LeaTouchAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LeaTouchAction.NONE;
        }
    }

    public float getGestureThreshold(LeaFacialGesture gesture) {
        if (gesture == null) {
            return 0f;
        }
        Map<String, String> entries = parseAssignments(gestureThresholds);
        String override = entries.get(gesture.name());
        if (override != null) {
            try {
                return Float.parseFloat(override.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (!gesture.isActionUnitBased()) {
            return expressionThreshold;
        }
        switch (gesture) {
            case MOUTH_OPEN:
            case LIPS_PART:
            case JAW_DROP:
                return mouthOpenThreshold;
            default:
                return Math.max(gesture.getDefaultThreshold(), auThreshold);
        }
    }

    public Path resolveModelDirectory() {
        Path configured = Paths.get(modelDirectory);
        if (configured.isAbsolute()) {
            return configured;
        }
        Path candidate = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 4 && candidate != null; depth++) {
            Path resolved = candidate.resolve(configured);
            if (Files.isDirectory(resolved)) {
                return resolved;
            }
            candidate = candidate.getParent();
        }
        return Paths.get("").toAbsolutePath().resolve(configured);
    }

    public List<String> findMissingModels() {
        List<String> missing = new ArrayList<>();
        Path directory = resolveModelDirectory();
        for (LibreFaceModel model : LibreFaceModel.values()) {
            if (!isRequired(model)) {
                continue;
            }
            Path file = directory.resolve(model.getFileName());
            if (!Files.isRegularFile(file) || !isPlausibleSize(file)) {
                missing.add(file.toString());
            }
        }
        return missing;
    }

    public boolean isRequired(LibreFaceModel model) {
        switch (model) {
            case AU_ENCODER:
                return runAuIntensity || runAuPresence;
            case AU_INTENSITY:
                return runAuIntensity;
            case AU_PRESENCE:
                return runAuPresence;
            case FACIAL_EXPRESSION:
                return runFacialExpression;
            default:
                return false;
        }
    }

    private static boolean isPlausibleSize(Path file) {
        try {
            return Files.size(file) > 100_000L;
        } catch (IOException e) {
            return false;
        }
    }

    private static List<String> split(String value) {
        List<String> tokens = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return tokens;
        }
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private static Map<String, String> parseAssignments(String value) {
        Map<String, String> entries = new HashMap<>();
        if (value == null || value.isEmpty()) {
            return entries;
        }
        for (String token : value.split(",")) {
            int separator = token.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = token.substring(0, separator).trim().toUpperCase(Locale.ROOT);
            String entryValue = token.substring(separator + 1).trim();
            if (!key.isEmpty() && !entryValue.isEmpty()) {
                entries.put(key, entryValue);
            }
        }
        return entries;
    }

    private static LeaFacialGesture gestureOf(String token) {
        try {
            return LeaFacialGesture.valueOf(token.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    private static float clampFloat(float value, float min, float max) {
        if (Float.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
