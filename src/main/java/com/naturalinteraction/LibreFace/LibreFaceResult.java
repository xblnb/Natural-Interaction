package com.naturalinteraction.LibreFace;

import java.util.Locale;

public final class LibreFaceResult {

    public static final String[] AU_INTENSITY_LABELS = {
            "1", "2", "4", "5", "6", "9", "12", "15", "17", "20", "25", "26"
    };

    public static final String[] AU_PRESENCE_LABELS = {
            "1", "2", "4", "6", "7", "10", "12", "14", "15", "17", "23", "24"
    };

    public static final String[] EXPRESSION_LABELS = {
            "Neutral", "Happiness", "Sadness", "Surprise", "Fear", "Disgust", "Anger", "Contempt"
    };

    private static final float PRESENCE_THRESHOLD = 0.5f;

    private final long timestamp;
    private final long frameId;
    private final float[] rawAuIntensity;
    private final float[] auIntensity;
    private final float[] auPresence;
    private final float[] expression;
    private final long inferenceMs;

    public LibreFaceResult(long timestamp, long frameId,
                           float[] rawAuIntensity, float[] auIntensity,
                           float[] auPresence, float[] expression, long inferenceMs) {
        this.timestamp = timestamp;
        this.frameId = frameId;
        this.rawAuIntensity = rawAuIntensity;
        this.auIntensity = auIntensity;
        this.auPresence = auPresence;
        this.expression = expression;
        this.inferenceMs = inferenceMs;
    }

    public long getTimestamp() { return timestamp; }
    public long getFrameId() { return frameId; }
    public long getInferenceMs() { return inferenceMs; }

    public boolean hasAuIntensity() { return auIntensity != null; }
    public boolean hasAuPresence() { return auPresence != null; }
    public boolean hasExpression() { return expression != null; }

    public float[] getAuIntensity() { return auIntensity == null ? null : auIntensity.clone(); }
    public float[] getAuPresence() { return auPresence == null ? null : auPresence.clone(); }
    public float[] getExpression() { return expression == null ? null : expression.clone(); }
    public float[] getRawAuIntensity() { return rawAuIntensity == null ? null : rawAuIntensity.clone(); }

    public float getAuIntensity(String label) {
        return valueOf(auIntensity, AU_INTENSITY_LABELS, label);
    }

    public float getRawAuIntensity(String label) {
        return valueOf(rawAuIntensity, AU_INTENSITY_LABELS, label);
    }

    public float getAuPresence(String label) {
        return valueOf(auPresence, AU_PRESENCE_LABELS, label);
    }

    public boolean isAuPresent(String label) {
        return getAuPresence(label) >= PRESENCE_THRESHOLD;
    }

    public float getExpression(String label) {
        return valueOf(expression, EXPRESSION_LABELS, label);
    }

    public String getExpressionLabel() {
        if (expression == null || expression.length == 0) {
            return "n/a";
        }
        int best = 0;
        for (int i = 1; i < expression.length && i < EXPRESSION_LABELS.length; i++) {
            if (expression[i] > expression[best]) {
                best = i;
            }
        }
        return EXPRESSION_LABELS[best];
    }

    public float getExpressionScore() {
        if (expression == null || expression.length == 0) {
            return 0f;
        }
        float best = expression[0];
        for (float value : expression) {
            best = Math.max(best, value);
        }
        return best;
    }

    public String toDebugString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LibreFace{t=").append(timestamp)
                .append(", ").append(inferenceMs).append("ms");
        if (auIntensity != null) {
            builder.append("\n  AU intensity: ");
            for (int i = 0; i < auIntensity.length; i++) {
                builder.append(AU_INTENSITY_LABELS[i]).append('=')
                        .append(String.format(Locale.ROOT, "%.2f", auIntensity[i])).append(' ');
            }
        }
        if (auPresence != null) {
            builder.append("\n  AU presence : ");
            for (int i = 0; i < auPresence.length; i++) {
                builder.append(AU_PRESENCE_LABELS[i])
                        .append(auPresence[i] >= PRESENCE_THRESHOLD ? "+ " : "- ");
            }
        }
        if (expression != null) {
            builder.append("\n  expression  : ").append(getExpressionLabel())
                    .append(' ').append(String.format(Locale.ROOT, "%.2f", getExpressionScore()));
        }
        return builder.append('}').toString();
    }

    private static float valueOf(float[] values, String[] labels, String label) {
        if (values == null || label == null) {
            return 0f;
        }
        for (int i = 0; i < labels.length && i < values.length; i++) {
            if (labels[i].equals(label)) {
                return values[i];
            }
        }
        return 0f;
    }
}
