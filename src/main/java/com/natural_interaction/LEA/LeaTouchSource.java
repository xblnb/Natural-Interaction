package com.natural_interaction.LEA;

import com.natural_interaction.TAL.TouchDispatcher;
import com.natural_interaction.TAL.TouchPointerIds;
import com.natural_interaction.TAL.TouchSource;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LeaTouchSource implements TouchSource, LeaTrackerListener {

    private enum PressState {
        IDLE,
        CLICKING,
        HOLDING
    }

    private final LeaConfig config;
    private final LeaEyeTracker tracker;
    private final LeaCalibrationManager calibration;
    private final LeaDwellDetector dwell;
    private final List<LeaInputListener> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<LeaBlinkEvent> blinkEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<LeaGestureEvent> gestureEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String[]> calibrationEvents = new ConcurrentLinkedQueue<>();

    private volatile TouchDispatcher dispatcher;
    private volatile boolean attached;
    private volatile boolean faceLost;
    private volatile String lastError = "";

    private PressState pressState = PressState.IDLE;
    private int pressPointer = TouchPointerIds.GAZE;
    private long pressStartedAt;
    private boolean dragLocked;
    private long stepStartedAt;
    private long lastStepAt;
    private boolean stepActive;

    public LeaTouchSource() {
        this(LeaConfig.load());
    }

    public LeaTouchSource(LeaConfig config) {
        this(config, new LeaEyeTracker(config));
    }

    public LeaTouchSource(LeaConfig config, LeaEyeTracker tracker) {
        this.config = config;
        this.tracker = tracker;
        this.calibration = new LeaCalibrationManager(tracker, config);
        this.dwell = new LeaDwellDetector(config);
    }

    public LeaTouchSource(LeaConfig config, LeaEyeTracker tracker, LeaCalibrationManager calibration) {
        this.config = config;
        this.tracker = tracker;
        this.calibration = calibration;
        this.dwell = new LeaDwellDetector(config);
    }

    @Override
    public void attach(TouchDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        if (config.trackingMode == LeaTrackingMode.STEP) {
            dispatcher.getCursor().setSensitivity(1f, 1f);
        } else {
            dispatcher.getCursor().setSensitivity(config.gazeGainX, config.gazeGainY);
        }
        dispatcher.getCursor().setSmoothing(config.gazeSmoothing);
        dispatcher.getCursor().setPosition(0.5f, 0.5f);

        tracker.addListener(this);
        attached = true;
        faceLost = false;
        pressState = PressState.IDLE;
        dragLocked = false;
        dwell.resetAll();

        if (!tracker.start()) {
            lastError = tracker.getLastError();
        }
    }

    @Override
    public void detach() {
        attached = false;
        TouchDispatcher target = dispatcher;
        release(target, true);
        tracker.removeListener(this);
        tracker.stop();
        dispatcher = null;
        dwell.resetAll();
        faceLost = false;
        blinkEvents.clear();
        gestureEvents.clear();
        calibrationEvents.clear();
    }

    @Override
    public String getName() {
        return "LEA";
    }

    public void tick() {
        TouchDispatcher target = dispatcher;
        if (target == null) {
            return;
        }

        long now = System.currentTimeMillis();

        drainCalibration();
        drainBlinkEvents(target);
        drainGestureEvents();

        if (!tracker.isRunning()) {
            String message = tracker.getLastError();
            if (message != null && !message.isEmpty()) {
                lastError = message;
            }
            release(target, true);
            dwell.reset();
            return;
        }

        LeaGazeSample sample = tracker.getLatestSample();
        if (sample == null) {
            return;
        }

        if (faceLost) {
            release(target, true);
            dwell.reset();
            return;
        }

        float offsetX = sample.getOffsetX();
        float offsetY = sample.getOffsetY();

        if (config.trackingMode == LeaTrackingMode.ABSOLUTE) {
            moveCursorAbsolute(target, offsetX, offsetY);
        } else if (config.trackingMode == LeaTrackingMode.STEP) {
            moveCursorStep(target, offsetX, offsetY, now);
        } else {
            release(target, true);
            dwell.reset();
            return;
        }

        if (config.clickMode.usesDwell()) {
            boolean triggered = dwell.update(offsetX, offsetY, now);
            if (triggered) {
                beginPress(target, TouchPointerIds.GAZE, false);
            } else if (canStartDrag(now)) {
                beginPress(target, TouchPointerIds.GAZE, true);
            }
        } else {
            dwell.reset();
        }

        updatePress(target, now);
    }

    private boolean canStartDrag(long now) {
        if (!config.dwellDragEnabled || config.dwellHoldDelayMs <= 0L) {
            return false;
        }
        if (pressState != PressState.IDLE || !dwell.isLatched()) {
            return false;
        }
        return dwell.getHeldMs(now) >= config.clickHoldMs + config.dwellHoldDelayMs;
    }

    private void moveCursorAbsolute(TouchDispatcher target, float offsetX, float offsetY) {
        float deadzone = config.gazeDeadzone;
        float dx = Math.abs(offsetX) <= deadzone ? 0f : offsetX - Math.signum(offsetX) * deadzone;
        float dy = Math.abs(offsetY) <= deadzone ? 0f : offsetY - Math.signum(offsetY) * deadzone;
        float x = clamp01(0.5f + dx * config.gazeGainX);
        float y = clamp01(0.5f + dy * config.gazeGainY);
        target.onPointerMotionAbsolute(TouchPointerIds.GAZE, x, y);
    }

    private void moveCursorStep(TouchDispatcher target, float offsetX, float offsetY, long now) {
        float deadzone = config.stepDeadzone;
        int directionX = Math.abs(offsetX) >= deadzone ? (offsetX > 0f ? 1 : -1) : 0;
        int directionY = Math.abs(offsetY) >= deadzone ? (offsetY > 0f ? 1 : -1) : 0;

        if (directionX == 0 && directionY == 0) {
            stepStartedAt = 0L;
            lastStepAt = 0L;
            stepActive = false;
            return;
        }
        if (stepStartedAt == 0L) {
            stepStartedAt = now;
            lastStepAt = 0L;
            stepActive = false;
            return;
        }

        boolean ready;
        if (!stepActive) {
            ready = now - stepStartedAt >= config.stepInitialDelayMs;
        } else {
            ready = now - lastStepAt >= config.stepRepeatMs;
        }
        if (!ready) {
            return;
        }

        stepActive = true;
        lastStepAt = now;
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        float scale = config.stepSize / length;
        target.onPointerMotionRelative(TouchPointerIds.GAZE, directionX * scale, directionY * scale);
    }

    private void updatePress(TouchDispatcher target, long now) {
        if (pressState == PressState.IDLE) {
            return;
        }
        long held = now - pressStartedAt;
        if (pressState == PressState.CLICKING) {
            if (held >= config.clickHoldMs) {
                release(target, false);
            }
            return;
        }
        boolean expired = held >= config.dwellMaxHoldMs;
        boolean stillHolding = dragLocked || (config.dwellDragEnabled && dwell.isTriggered());
        if (expired || !stillHolding) {
            release(target, false);
        }
    }

    private void beginPress(TouchDispatcher target, int pointerId, boolean hold) {
        if (pressState != PressState.IDLE || target == null) {
            return;
        }
        if (hold) {
            pressState = PressState.HOLDING;
        } else {
            pressState = PressState.CLICKING;
        }
        pressPointer = pointerId;
        pressStartedAt = System.currentTimeMillis();

        float x = target.getCursor().getX();
        float y = target.getCursor().getY();
        target.onPointerDown(pointerId, x, y, 1f);
        notifyTouch("down", x, y);
    }

    private void beginDragLock(TouchDispatcher target) {
        if (pressState == PressState.IDLE) {
            pressState = PressState.HOLDING;
            pressPointer = TouchPointerIds.GAZE;
            pressStartedAt = System.currentTimeMillis();
            dragLocked = true;
            float x = target.getCursor().getX();
            float y = target.getCursor().getY();
            target.onPointerDown(TouchPointerIds.GAZE, x, y, 1f);
            notifyTouch("drag-down", x, y);
            return;
        }
        if (dragLocked) {
            release(target, false);
        }
    }

    private void release(TouchDispatcher target, boolean cancel) {
        if (pressState == PressState.IDLE) {
            return;
        }
        int pointerId = pressPointer;
        pressState = PressState.IDLE;
        dragLocked = false;
        pressPointer = TouchPointerIds.GAZE;
        if (target == null) {
            return;
        }
        if (cancel) {
            target.onPointerCancel(pointerId);
            notifyTouch("cancel", target.getCursor().getX(), target.getCursor().getY());
        } else {
            target.onPointerUp(pointerId);
            notifyTouch("up", target.getCursor().getX(), target.getCursor().getY());
        }
    }

    private void handleAction(TouchDispatcher target, LeaTouchAction action) {
        if (target == null || action == null) {
            return;
        }
        switch (action) {
            case PRIMARY_CLICK:
                if (pressState == PressState.IDLE) {
                    pressState = PressState.CLICKING;
                    pressPointer = TouchPointerIds.GAZE;
                    pressStartedAt = System.currentTimeMillis();
                    float x = target.getCursor().getX();
                    float y = target.getCursor().getY();
                    target.onPointerDown(TouchPointerIds.GAZE, x, y, 1f);
                    notifyTouch("blink-down", x, y);
                }
                break;
            case SECONDARY_CLICK:
                if (pressState == PressState.IDLE) {
                    pressState = PressState.CLICKING;
                    pressPointer = TouchPointerIds.SECONDARY;
                    pressStartedAt = System.currentTimeMillis();
                    float x = target.getCursor().getX();
                    float y = target.getCursor().getY();
                    target.onPointerDown(TouchPointerIds.SECONDARY, x, y, 1f);
                    notifyTouch("secondary-down", x, y);
                }
                break;
            case CANCEL:
                target.cancelAll();
                pressState = PressState.IDLE;
                dragLocked = false;
                dwell.resetAll();
                notifyTouch("cancel-all", target.getCursor().getX(), target.getCursor().getY());
                break;
            case TOGGLE_DRAG:
                beginDragLock(target);
                break;
            default:
                break;
        }
    }

    private void drainBlinkEvents(TouchDispatcher target) {
        LeaBlinkEvent event;
        while ((event = blinkEvents.poll()) != null) {
            for (LeaInputListener listener : listeners) {
                try {
                    listener.onBlink(event);
                } catch (Throwable ignored) {
                }
            }
            if (faceLost) {
                continue;
            }
            switch (event.getType()) {
                case BLINK:
                    if (config.clickMode.usesBlink()) {
                        if (dragLocked) {
                            release(target, false);
                        } else {
                            beginPress(target, TouchPointerIds.GAZE, false);
                        }
                    }
                    break;
                case DOUBLE_BLINK:
                    handleAction(target, config.doubleBlinkAction);
                    break;
                case WINK_LEFT:
                    handleAction(target, config.winkLeftAction);
                    break;
                case WINK_RIGHT:
                    handleAction(target, config.winkRightAction);
                    break;
                default:
                    break;
            }
        }
    }

    private void drainGestureEvents() {
        LeaGestureEvent event;
        while ((event = gestureEvents.poll()) != null) {
            for (LeaInputListener listener : listeners) {
                try {
                    listener.onGesture(event);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void drainCalibration() {
        String[] event;
        while ((event = calibrationEvents.poll()) != null) {
            if (event.length < 2) {
                continue;
            }
            boolean success = Boolean.parseBoolean(event[0]);
            if (success) {
                calibration.markCompleted();
            }
            for (LeaInputListener listener : listeners) {
                try {
                    listener.onCalibration(success, event[1]);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public void onGaze(LeaGazeSample sample) {
        if (sample.isFaceFound()) {
            faceLost = false;
        }
    }

    @Override
    public void onFaceLost(long timestamp) {
        faceLost = true;
        for (LeaInputListener listener : listeners) {
            try {
                listener.onFaceLost();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void onBlink(LeaBlinkEvent event) {
        blinkEvents.offer(event);
    }

    @Override
    public void onGesture(LeaGestureEvent event) {
        gestureEvents.offer(event);
    }

    @Override
    public void onCalibration(boolean success, String message) {
        calibrationEvents.offer(new String[]{Boolean.toString(success),
                message == null ? "" : message});
    }

    @Override
    public void onError(String message) {
        lastError = message;
    }

    public void addInputListener(LeaInputListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeInputListener(LeaInputListener listener) {
        listeners.remove(listener);
    }

    private void notifyTouch(String action, float x, float y) {
        for (LeaInputListener listener : listeners) {
            try {
                listener.onTouchFeedback(action, x, y);
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean requestCalibration() {
        return calibration.requestCalibration();
    }

    public boolean performAction(LeaTouchAction action) {
        TouchDispatcher target = dispatcher;
        if (target == null || action == null || action == LeaTouchAction.NONE) {
            return false;
        }
        if (faceLost && action != LeaTouchAction.CANCEL) {
            return false;
        }
        handleAction(target, action);
        return true;
    }

    public void dispatchFacialGesture(LeaFacialGestureEvent event, LeaTouchAction action) {
        if (event == null) {
            return;
        }
        TouchDispatcher target = dispatcher;
        if (target != null && action != null && action != LeaTouchAction.NONE
                && !(faceLost && action != LeaTouchAction.CANCEL)) {
            handleAction(target, action);
        }
        for (LeaInputListener listener : listeners) {
            try {
                listener.onFacialGesture(event);
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean restart() {
        tracker.stop();
        blinkEvents.clear();
        gestureEvents.clear();
        dwell.resetAll();
        faceLost = false;
        boolean started = tracker.start();
        lastError = started ? "" : tracker.getLastError();
        return started;
    }

    public boolean isAttached() {
        return attached;
    }

    public boolean isRunning() {
        return tracker.isRunning();
    }

    public boolean isCalibrated() {
        return tracker.isCalibrated();
    }

    public boolean isPressed() {
        return pressState != PressState.IDLE;
    }

    public boolean isFaceLost() {
        return faceLost;
    }

    public float getDwellProgress() {
        return dwell.getProgress();
    }

    public float getDwellAnchorX() {
        return dwell.getAnchorX();
    }

    public float getDwellAnchorY() {
        return dwell.getAnchorY();
    }

    public String getLastError() {
        return lastError.isEmpty() ? tracker.getLastError() : lastError;
    }

    public LeaEyeTracker getTracker() {
        return tracker;
    }

    public LeaCalibrationManager getCalibrationManager() {
        return calibration;
    }

    public LeaConfig getConfig() {
        return config;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
