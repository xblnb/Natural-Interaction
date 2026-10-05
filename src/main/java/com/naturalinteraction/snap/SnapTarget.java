package com.naturalinteraction.snap;

public interface SnapTarget {

    float getScreenX();

    float getScreenY();

    float getScreenWidth();

    float getScreenHeight();

    SnapTargetType getType();

    float getPriority();

    boolean isActive();

    void activate();
}