package com.naturalinteraction.LEA;

public final class LeaGestureEvent {

    private final LeaGesture gesture;
    private final float magnitude;
    private final boolean far;
    private final long timestamp;

    public LeaGestureEvent(LeaGesture gesture, float magnitude, boolean far, long timestamp) {
        this.gesture = gesture;
        this.magnitude = magnitude;
        this.far = far;
        this.timestamp = timestamp;
    }

    public LeaGesture getGesture() { return gesture; }
    public float getMagnitude() { return magnitude; }
    public boolean isFar() { return far; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "LeaGestureEvent{" + gesture + ", magnitude=" + magnitude
                + ", far=" + far + "}";
    }
}
