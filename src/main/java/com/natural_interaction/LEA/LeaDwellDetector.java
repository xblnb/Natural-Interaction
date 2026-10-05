package com.natural_interaction.LEA;

public final class LeaDwellDetector {

    private enum Phase {
        IDLE,
        DWELLING,
        LATCHED
    }

    private final long dwellTimeMs;
    private final float movementThreshold;
    private final float releaseThreshold;
    private final long refractoryMs;

    private Phase phase = Phase.IDLE;
    private float anchorX;
    private float anchorY;
    private long anchorTime;
    private long triggerTime;
    private long lastTriggerTime;

    public LeaDwellDetector(LeaConfig config) {
        this.dwellTimeMs = config.dwellTimeMs;
        this.movementThreshold = config.dwellMovementThreshold;
        this.releaseThreshold = config.dwellReleaseThreshold;
        this.refractoryMs = config.dwellRefractoryMs;
    }

    public LeaDwellDetector(long dwellTimeMs, float movementThreshold, float releaseThreshold, long refractoryMs) {
        this.dwellTimeMs = dwellTimeMs;
        this.movementThreshold = movementThreshold;
        this.releaseThreshold = releaseThreshold;
        this.refractoryMs = refractoryMs;
    }

    public boolean update(float x, float y, long now) {
        if (phase == Phase.LATCHED) {
            if (distance(x, y) > releaseThreshold) {
                phase = Phase.IDLE;
            } else {
                return false;
            }
        }

        if (phase == Phase.IDLE) {
            startDwell(x, y, now);
            return false;
        }

        if (distance(x, y) > movementThreshold) {
            startDwell(x, y, now);
            return false;
        }

        if (now - anchorTime < dwellTimeMs) {
            return false;
        }
        if (lastTriggerTime > 0L && now - lastTriggerTime < refractoryMs) {
            return false;
        }

        lastTriggerTime = now;
        triggerTime = now;
        phase = Phase.LATCHED;
        return true;
    }

    private void startDwell(float x, float y, long now) {
        anchorX = x;
        anchorY = y;
        anchorTime = now;
        phase = Phase.DWELLING;
    }

    private float distance(float x, float y) {
        float dx = x - anchorX;
        float dy = y - anchorY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void reset() {
        phase = Phase.IDLE;
        triggerTime = 0L;
    }

    public void resetAll() {
        reset();
        lastTriggerTime = 0L;
        anchorX = 0f;
        anchorY = 0f;
        anchorTime = 0L;
    }

    public boolean isDwelling() {
        return phase == Phase.DWELLING || phase == Phase.LATCHED;
    }

    public boolean isLatched() {
        return phase == Phase.LATCHED;
    }

    public boolean isTriggered() {
        return phase == Phase.LATCHED;
    }

    public float getProgress() {
        if (phase == Phase.IDLE) {
            return 0f;
        }
        if (phase == Phase.LATCHED) {
            return 1f;
        }
        long elapsed = System.currentTimeMillis() - anchorTime;
        return Math.max(0f, Math.min(1f, (float) elapsed / (float) dwellTimeMs));
    }

    public long getHeldMs(long now) {
        if (phase != Phase.LATCHED) {
            return 0L;
        }
        return now - triggerTime;
    }

    public float getAnchorX() {
        return anchorX;
    }

    public float getAnchorY() {
        return anchorY;
    }

    public long getDwellTimeMs() {
        return dwellTimeMs;
    }
}
