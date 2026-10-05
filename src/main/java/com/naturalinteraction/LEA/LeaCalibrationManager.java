package com.naturalinteraction.LEA;

public final class LeaCalibrationManager {

    private final LeaEyeTracker tracker;
    private final LeaConfig config;

    private long lastCalibrationAt;

    public LeaCalibrationManager(LeaEyeTracker tracker, LeaConfig config) {
        this.tracker = tracker;
        this.config = config;
    }

    public boolean requestCalibration() {
        long now = System.currentTimeMillis();
        if (!tracker.isRunning()) {
            return false;
        }
        if (tracker.isCalibrating()) {
            return false;
        }
        if (lastCalibrationAt > 0L && now - lastCalibrationAt < config.calibrationCooldownMs) {
            return false;
        }
        if (!tracker.requestCalibration()) {
            return false;
        }
        lastCalibrationAt = now;
        return true;
    }

    public void markCompleted() {
        lastCalibrationAt = System.currentTimeMillis();
    }

    public boolean isCalibrated() {
        return config.hasNeutral;
    }

    public float getProgress() {
        return tracker.getCalibrationProgress();
    }

    public int getCollectedSamples() {
        return tracker.getCalibrationCollected();
    }

    public String getLastMessage() {
        return tracker.getCalibrationMessage();
    }

    public long getRemainingCooldownMs() {
        if (lastCalibrationAt <= 0L) {
            return 0L;
        }
        long remaining = config.calibrationCooldownMs - (System.currentTimeMillis() - lastCalibrationAt);
        return Math.max(0L, remaining);
    }

    public void reset() {
        tracker.clearCalibration();
        lastCalibrationAt = 0L;
    }
}
