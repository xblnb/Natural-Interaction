package com.naturalinteraction.LEA;

public interface LeaTrackerListener {

    default void onGaze(LeaGazeSample sample) {
    }

    default void onFaceLost(long timestamp) {
    }

    default void onBlink(LeaBlinkEvent event) {
    }

    default void onGesture(LeaGestureEvent event) {
    }

    default void onCalibration(boolean success, String message) {
    }

    default void onError(String message) {
    }
}
