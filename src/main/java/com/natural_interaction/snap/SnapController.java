package com.natural_interaction.snap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class SnapController {

    private static final float GAZE_EPSILON = 0.005f;

    private final Minecraft mc;
    private final SnapConfig config;
    private final SnapEngine engine;
    private final CompositeSnapCollector collector;
    private final WorldSnapCollector worldCollector;

    private Screen lastScreen;
    private float lastGazeX = Float.NaN;
    private float lastGazeY = Float.NaN;

    public SnapController(Minecraft mc) {
        this(mc, new SnapConfig());
    }

    public SnapController(Minecraft mc, SnapConfig config) {
        this.mc = mc;
        this.config = config;
        this.engine = new SnapEngine(config);
        this.collector = new CompositeSnapCollector();

        if (config.uiSnapEnabled) {
            collector.add(new GUiSnapCollector(mc));
        }
        if (config.inventorySnapEnabled) {
            collector.add(new InventorySnapCollector(mc));
        }
        if (config.worldSnapEnabled) {
            this.worldCollector = new WorldSnapCollector(mc);
            collector.add(worldCollector);
        } else {
            this.worldCollector = null;
        }
    }

    public void update(float cursorX, float cursorY) {
        boolean screenChanged = mc.screen != lastScreen;
        if (screenChanged) {
            engine.reset();
            lastScreen = mc.screen;
            lastGazeX = Float.NaN;
            lastGazeY = Float.NaN;
        }

        boolean gazeMoved = Float.isNaN(lastGazeX)
                || Math.abs(cursorX - lastGazeX) > GAZE_EPSILON
                || Math.abs(cursorY - lastGazeY) > GAZE_EPSILON;

        boolean currentLost = engine.hasSnap() && !engine.getCurrent().isActive();

        if (!gazeMoved && !currentLost && !screenChanged) {
            return;
        }

        lastGazeX = cursorX;
        lastGazeY = cursorY;

        if (worldCollector != null) {
            worldCollector.setGaze(cursorX, cursorY);
        }

        List<SnapTarget> candidates = collector.collectAll();
        engine.update(cursorX, cursorY, candidates);
    }

    public float getSnappedX(float fallback) { return engine.getSnappedX(fallback); }

    public float getSnappedY(float fallback) { return engine.getSnappedY(fallback); }

    public boolean hasSnap() { return engine.hasSnap(); }

    public SnapTarget getCurrent() { return engine.getCurrent(); }

    public SnapConfig getConfig() { return config; }

    public void reset() {
        engine.reset();
        lastGazeX = Float.NaN;
        lastGazeY = Float.NaN;
    }
}