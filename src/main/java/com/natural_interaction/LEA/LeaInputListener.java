package com.natural_interaction.LEA;

public interface LeaInputListener {

    default void onGesture(LeaGestureEvent event) {
    }

    default void onBlink(LeaBlinkEvent event) {
    }

    default void onFacialGesture(LeaFacialGestureEvent event) {
    }

    default void onCalibration(boolean success, String message) {
    }

    default void onFaceLost() {
    }

    default void onTouchFeedback(String action, float x, float y) {
    }
}
