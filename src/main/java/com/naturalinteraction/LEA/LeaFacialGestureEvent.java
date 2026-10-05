package com.naturalinteraction.LEA;

public final class LeaFacialGestureEvent {

    private final LeaFacialGesture gesture;
    private final float intensity;
    private final float threshold;
    private final long timestamp;

    public LeaFacialGestureEvent(LeaFacialGesture gesture, float intensity,
                                 float threshold, long timestamp) {
        this.gesture = gesture;
        this.intensity = intensity;
        this.threshold = threshold;
        this.timestamp = timestamp;
    }

    public LeaFacialGesture getGesture() { return gesture; }
    public float getIntensity() { return intensity; }
    public float getThreshold() { return threshold; }
    public long getTimestamp() { return timestamp; }

    public float getNormalizedIntensity() {
        float maximum = gesture.getScaleMaximum();
        return maximum <= 0f ? 0f : Math.max(0f, Math.min(1f, intensity / maximum));
    }

    @Override
    public String toString() {
        return "LeaFacialGestureEvent{" + gesture + ", intensity=" + intensity
                + ", threshold=" + threshold + "}";
    }
}
