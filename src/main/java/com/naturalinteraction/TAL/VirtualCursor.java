package com.naturalinteraction.TAL;

public final class VirtualCursor {

    private static final float DEFAULT_X = 0.5f;
    private static final float DEFAULT_Y = 0.5f;

    private float x = DEFAULT_X;
    private float y = DEFAULT_Y;
    private float smoothedX = DEFAULT_X;
    private float smoothedY = DEFAULT_Y;

    private float sensitivityX = 1.8f;
    private float sensitivityY = 1.8f;
    private float smoothing = 0.35f;

    public void moveRelative(float dx, float dy) {
        x += dx * sensitivityX;
        y += dy * sensitivityY;
        clamp();
        smooth();
    }

    public void moveAbsolute(float nx, float ny) {
        x = nx;
        y = ny;
        clamp();
        smooth();
    }

    public void setPosition(float nx, float ny) {
        x = nx;
        y = ny;
        smoothedX = nx;
        smoothedY = ny;
        clamp();
    }

    public void reset() {
        x = DEFAULT_X;
        y = DEFAULT_Y;
        smoothedX = DEFAULT_X;
        smoothedY = DEFAULT_Y;
    }

    private void clamp() {
        x = Math.max(0f, Math.min(1f, x));
        y = Math.max(0f, Math.min(1f, y));
    }

    private void smooth() {
        smoothedX = smoothing * x + (1f - smoothing) * smoothedX;
        smoothedY = smoothing * y + (1f - smoothing) * smoothedY;
    }

    public float getX() { return smoothedX; }
    public float getY() { return smoothedY; }
    public float getRawX() { return x; }
    public float getRawY() { return y; }

    public float getSensitivityX() { return sensitivityX; }
    public float getSensitivityY() { return sensitivityY; }
    public float getSmoothing() { return smoothing; }

    public void setSensitivity(float sx, float sy) {
        this.sensitivityX = sx;
        this.sensitivityY = sy;
    }

    public void setSmoothing(float s) {
        this.smoothing = Math.max(0f, Math.min(1f, s));
    }
}