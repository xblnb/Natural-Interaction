package com.natural_interaction.TAL;

public final class TouchPointer {

    private final int id;
    private boolean active;
    private float x;
    private float y;
    private float pressure;
    private long downTime;

    public TouchPointer(int id) {
        this.id = id;
    }

    public void down(float x, float y, float pressure, long timestamp) {
        this.active = true;
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.downTime = timestamp;
    }

    public void move(float x, float y, float pressure) {
        this.x = x;
        this.y = y;
        this.pressure = pressure;
    }

    public void up() {
        this.active = false;
        this.pressure = 0f;
    }

    public int getId() { return id; }
    public boolean isActive() { return active; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getPressure() { return pressure; }
    public long getDownTime() { return downTime; }

    public long getHoldDuration(long now) {
        return active ? now - downTime : 0L;
    }
}
