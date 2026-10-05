package com.natural_interaction.LEA;

public final class LeaBlinkEvent {

    public enum Type {
        BLINK,
        DOUBLE_BLINK,
        WINK_LEFT,
        WINK_RIGHT
    }

    private final Type type;
    private final long timestamp;
    private final long durationMs;
    private final boolean leftEye;
    private final boolean rightEye;

    public LeaBlinkEvent(Type type, long timestamp, long durationMs,
                         boolean leftEye, boolean rightEye) {
        this.type = type;
        this.timestamp = timestamp;
        this.durationMs = durationMs;
        this.leftEye = leftEye;
        this.rightEye = rightEye;
    }

    public Type getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public long getDurationMs() { return durationMs; }
    public boolean isLeftEye() { return leftEye; }
    public boolean isRightEye() { return rightEye; }
    public boolean isBothEyes() { return leftEye && rightEye; }

    public boolean isBilateralBlink() {
        return type == Type.BLINK || type == Type.DOUBLE_BLINK;
    }

    public boolean isWink() {
        return type == Type.WINK_LEFT || type == Type.WINK_RIGHT;
    }

    @Override
    public String toString() {
        return "LeaBlinkEvent{" + type + ", duration=" + durationMs
                + ", left=" + leftEye + ", right=" + rightEye + "}";
    }
}
