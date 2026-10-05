package com.natural_interaction.snap;

import java.util.List;

public final class SnapEngine {

    private final SnapConfig config;

    private SnapTarget current;
    private float snappedX;
    private float snappedY;

    public SnapEngine(SnapConfig config) {
        this.config = config;
    }

    public SnapTarget update(float cursorX, float cursorY, List<SnapTarget> candidates) {
        if (!config.enabled || candidates == null || candidates.isEmpty()) {
            reset();
            return null;
        }

        SnapTarget best = null;
        float bestScore = 0f;

        for (SnapTarget target : candidates) {
            if (!target.isActive()) continue;

            float dx = target.getScreenX() - cursorX;
            float dy = target.getScreenY() - cursorY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            float radius = (target == current) ? config.releaseRadius : config.acquireRadius;
            if (dist > radius) continue;

            float size = (target.getScreenWidth() + target.getScreenHeight()) * 0.5f;
            float distanceFactor = 1f - dist / radius;
            float score = target.getPriority() * (1f + size * config.sizeWeight) * distanceFactor;
            if (target == current) score *= config.stickiness;

            if (score > bestScore) {
                bestScore = score;
                best = target;
            }
        }

        current = best;
        if (best != null) {
            snappedX = best.getScreenX();
            snappedY = best.getScreenY();
        }
        return best;
    }

    public void reset() {
        current = null;
    }

    public SnapTarget getCurrent() { return current; }

    public boolean hasSnap() { return current != null; }

    public float getSnappedX(float fallback) { return current != null ? snappedX : fallback; }

    public float getSnappedY(float fallback) { return current != null ? snappedY : fallback; }
}