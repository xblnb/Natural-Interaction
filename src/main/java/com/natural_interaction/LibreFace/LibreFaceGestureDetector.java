package com.natural_interaction.LibreFace;

import com.natural_interaction.LEA.LeaFacialGesture;
import com.natural_interaction.LEA.LeaFacialGestureEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LibreFaceGestureDetector {

    private static final class State {
        private long activeSince;
        private boolean reported;
        private float peak;
        private long lastFiredAt;

        private void clear() {
            activeSince = 0L;
            reported = false;
            peak = 0f;
        }
    }

    private final LibreFaceConfig config;
    private final Map<LeaFacialGesture, State> states = new EnumMap<>(LeaFacialGesture.class);

    public LibreFaceGestureDetector(LibreFaceConfig config) {
        this.config = config;
        for (LeaFacialGesture gesture : LeaFacialGesture.values()) {
            states.put(gesture, new State());
        }
    }

    public List<LeaFacialGestureEvent> update(LibreFaceResult result, long now) {
        List<LeaFacialGestureEvent> events = new ArrayList<>();
        if (result == null) {
            return events;
        }
        Set<LeaFacialGesture> enabled = config.getEnabledGestureSet();
        if (enabled.isEmpty()) {
            return events;
        }

        for (LeaFacialGesture gesture : LeaFacialGesture.values()) {
            State state = states.get(gesture);
            float intensity = evaluate(gesture, result);
            float threshold = config.getGestureThreshold(gesture);
            if (intensity < threshold) {
                state.clear();
                continue;
            }
            if (!enabled.contains(gesture)) {
                continue;
            }
            state.peak = Math.max(state.peak, intensity);
            if (state.activeSince == 0L) {
                state.activeSince = now;
                state.reported = false;
            }
            if (state.reported) {
                continue;
            }
            if (now - state.activeSince < config.gestureHoldMs) {
                continue;
            }
            if (state.lastFiredAt > 0L && now - state.lastFiredAt < config.gestureRefractoryMs) {
                continue;
            }
            state.reported = true;
            state.lastFiredAt = now;
            events.add(new LeaFacialGestureEvent(gesture, state.peak, threshold, now));
        }
        return events;
    }

    public void reset() {
        for (State state : states.values()) {
            state.clear();
            state.lastFiredAt = 0L;
        }
    }

    public static float evaluate(LeaFacialGesture gesture, LibreFaceResult result) {
        switch (gesture) {
            case MOUTH_OPEN:
                return Math.max(actionUnit(result, "25"), actionUnit(result, "26"));
            case LIPS_PART:
                return actionUnit(result, "25");
            case JAW_DROP:
                return actionUnit(result, "26");
            case SMILE:
                return actionUnit(result, "12");
            case CHEEK_RAISE:
                return actionUnit(result, "6");
            case BROW_RAISE:
                return Math.min(actionUnit(result, "1"), actionUnit(result, "2"));
            case BROW_LOWER:
                return actionUnit(result, "4");
            case NOSE_WRINKLE:
                return actionUnit(result, "9");
            case LID_TIGHTEN:
                return actionUnit(result, "7");
            case LIP_TIGHTEN:
                return actionUnit(result, "24");
            case CHIN_RAISE:
                return actionUnit(result, "17");
            case LIP_DEPRESSOR:
                return actionUnit(result, "15");
            case HAPPINESS:
                return result.getExpression("Happiness");
            case SURPRISE:
                return result.getExpression("Surprise");
            case SADNESS:
                return result.getExpression("Sadness");
            case ANGER:
                return result.getExpression("Anger");
            case DISGUST:
                return result.getExpression("Disgust");
            case FEAR:
                return result.getExpression("Fear");
            case CONTEMPT:
                return result.getExpression("Contempt");
            default:
                return 0f;
        }
    }

    private static float actionUnit(LibreFaceResult result, String label) {
        if (result.hasAuIntensity() && contains(LibreFaceResult.AU_INTENSITY_LABELS, label)) {
            return result.getAuIntensity(label);
        }
        if (result.hasAuPresence() && contains(LibreFaceResult.AU_PRESENCE_LABELS, label)) {
            return result.getAuPresence(label) * 5f;
        }
        return 0f;
    }

    private static boolean contains(String[] labels, String label) {
        for (String value : labels) {
            if (value.equals(label)) {
                return true;
            }
        }
        return false;
    }
}
