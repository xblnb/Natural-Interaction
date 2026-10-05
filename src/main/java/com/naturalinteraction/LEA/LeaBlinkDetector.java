package com.naturalinteraction.LEA;

public final class LeaBlinkDetector {

    private final LeaConfig config;

    private boolean leftClosed;
    private boolean rightClosed;
    private long leftClosedAt;
    private long rightClosedAt;
    private boolean bothWereClosed;
    private long bothClosedAt = -1L;
    private long leftSoloAt;
    private long rightSoloAt;
    private LeaEyeState leftState = LeaEyeState.UNKNOWN;
    private LeaEyeState rightState = LeaEyeState.UNKNOWN;
    private long lastBilateralAt = -1L;
    private boolean pendingBlink;
    private long pendingEnd;
    private long pendingDuration;

    public LeaBlinkDetector(LeaConfig config) {
        this.config = config;
    }

    public LeaBlinkEvent update(boolean enabled, float opennessLeft, float opennessRight,
                                boolean leftValid, boolean rightValid, long now) {
        if (!enabled) {
            invalidate();
            return null;
        }

        LeaBlinkEvent deferred = flushPending(now);
        if (deferred != null) {
            return deferred;
        }
        if (!leftValid && !rightValid) {
            invalidate();
            return null;
        }

        LeaBlinkEvent winkEvent = updateEye(true, opennessLeft, leftValid, rightValid, now);
        LeaBlinkEvent rightWink = updateEye(false, opennessRight, rightValid, leftValid, now);
        if (winkEvent == null) {
            winkEvent = rightWink;
        }

        boolean bothClosed = leftClosed && rightClosed;
        if (bothClosed) {
            if (!bothWereClosed) {
                bothWereClosed = true;
                bothClosedAt = now;
            }
            leftSoloAt = 0L;
            rightSoloAt = 0L;
            return null;
        }

        if (bothWereClosed) {
            bothWereClosed = false;
            long duration = now - bothClosedAt;
            bothClosedAt = -1L;
            leftSoloAt = 0L;
            rightSoloAt = 0L;
            if (!leftClosed && !rightClosed) {
                LeaBlinkEvent bilateral = completeBilateral(duration, now);
                if (bilateral != null) {
                    return bilateral;
                }
            }
        }

        return winkEvent;
    }

    private LeaBlinkEvent updateEye(boolean left, float openness, boolean selfValid,
                                    boolean otherValid, long now) {
        if (!selfValid) {
            if (left) {
                leftClosed = false;
                leftSoloAt = 0L;
                leftState = LeaEyeState.UNKNOWN;
            } else {
                rightClosed = false;
                rightSoloAt = 0L;
                rightState = LeaEyeState.UNKNOWN;
            }
            return null;
        }

        boolean closed = left ? leftClosed : rightClosed;
        boolean otherClosed = left ? rightClosed : leftClosed;
        long soloAt = left ? leftSoloAt : rightSoloAt;

        if (!closed && openness < config.blinkThreshold) {
            closed = true;
            if (left) {
                leftClosed = true;
                leftClosedAt = now;
                leftState = LeaEyeState.CLOSING;
                if (config.enableWink && otherValid && !otherClosed) {
                    leftSoloAt = now;
                }
            } else {
                rightClosed = true;
                rightClosedAt = now;
                rightState = LeaEyeState.CLOSING;
                if (config.enableWink && otherValid && !otherClosed) {
                    rightSoloAt = now;
                }
            }
            return null;
        }

        if (closed && openness > config.blinkOpenThreshold) {
            long closedAt = left ? leftClosedAt : rightClosedAt;
            long duration = now - closedAt;
            if (left) {
                leftClosed = false;
                leftState = LeaEyeState.OPEN;
            } else {
                rightClosed = false;
                rightState = LeaEyeState.OPEN;
            }
            if (soloAt > 0L) {
                if (left) {
                    leftSoloAt = 0L;
                } else {
                    rightSoloAt = 0L;
                }
                if (config.enableWink && otherValid && !otherClosed
                        && duration >= config.winkMinDurationMs
                        && duration <= config.blinkMaxDurationMs) {
                    LeaBlinkEvent.Type type = left ? LeaBlinkEvent.Type.WINK_LEFT : LeaBlinkEvent.Type.WINK_RIGHT;
                    return new LeaBlinkEvent(type, now, duration, left, !left);
                }
            }
            return null;
        }

        if (left) {
            leftState = leftClosed ? LeaEyeState.CLOSED : LeaEyeState.OPEN;
        } else {
            rightState = rightClosed ? LeaEyeState.CLOSED : LeaEyeState.OPEN;
        }
        return null;
    }

    private LeaBlinkEvent completeBilateral(long duration, long now) {
        if (duration < config.blinkMinDurationMs || duration > config.blinkMaxDurationMs) {
            return null;
        }
        if (lastBilateralAt > 0L && now - lastBilateralAt < config.blinkRefractoryMs) {
            return null;
        }
        lastBilateralAt = now;

        if (config.enableDoubleBlink) {
            if (pendingBlink && now - pendingEnd <= config.doubleBlinkMaxGapMs) {
                pendingBlink = false;
                return new LeaBlinkEvent(LeaBlinkEvent.Type.DOUBLE_BLINK, now,
                        pendingDuration + duration, true, true);
            }
            pendingBlink = true;
            pendingEnd = now;
            pendingDuration = duration;
            return null;
        }

        return new LeaBlinkEvent(LeaBlinkEvent.Type.BLINK, now, duration, true, true);
    }

    private LeaBlinkEvent flushPending(long now) {
        if (!pendingBlink) {
            return null;
        }
        if (now - pendingEnd <= config.doubleBlinkMaxGapMs) {
            return null;
        }
        pendingBlink = false;
        return new LeaBlinkEvent(LeaBlinkEvent.Type.BLINK, pendingEnd, pendingDuration, true, true);
    }

    public void invalidate() {
        leftClosed = false;
        rightClosed = false;
        bothWereClosed = false;
        bothClosedAt = -1L;
        leftSoloAt = 0L;
        rightSoloAt = 0L;
        leftState = LeaEyeState.UNKNOWN;
        rightState = LeaEyeState.UNKNOWN;
    }

    public void reset() {
        invalidate();
        pendingBlink = false;
        pendingEnd = 0L;
        pendingDuration = 0L;
        lastBilateralAt = -1L;
        leftClosedAt = 0L;
        rightClosedAt = 0L;
    }

    public boolean isLeftClosed() {
        return leftClosed;
    }

    public boolean isRightClosed() {
        return rightClosed;
    }

    public boolean isBlinkActive() {
        return leftClosed && rightClosed;
    }

    public LeaEyeState getLeftState() {
        return leftState;
    }

    public LeaEyeState getRightState() {
        return rightState;
    }
}
