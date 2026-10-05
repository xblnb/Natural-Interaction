package com.naturalinteraction.TAL;

public final class TouchEvent {

    private final int pointerId;
    private final TouchAction action;
    private final float x;
    private final float y;
    private final float pressure;
    private final long timestamp;

    public TouchEvent(int pointerId, TouchAction action, float x, float y,
                      float pressure, long timestamp) {
        this.pointerId = pointerId;
        this.action = action;
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.timestamp = timestamp;
    }

    public int getPointerId() { return pointerId; }
    public TouchAction getAction() { return action; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getPressure() { return pressure; }
    public long getTimestamp() { return timestamp; }

    public boolean isDown()   { return action == TouchAction.DOWN; }
    public boolean isMove()   { return action == TouchAction.MOVE; }
    public boolean isUp()     { return action == TouchAction.UP; }
    public boolean isCancel() { return action == TouchAction.CANCEL; }

    @Override
    public String toString() {
        return "TouchEvent{" + pointerId + ", " + action
                + ", (" + x + "," + y + "), p=" + pressure + "}";
    }
}