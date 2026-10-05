package com.naturalinteraction.LEA;

import com.naturalinteraction.TAL.TouchDispatcher;
import com.naturalinteraction.TAL.TouchSourceRegistry;

import com.naturalinteraction.LibreFace.LibreFaceAnalyzerAdapter;
import com.naturalinteraction.LibreFace.LibreFaceConfig;
import com.naturalinteraction.LibreFace.LibreFaceEngine;
import com.naturalinteraction.LibreFace.LibreFaceGestureDetector;
import com.naturalinteraction.LibreFace.LibreFaceListener;
import com.naturalinteraction.LibreFace.LibreFaceResult;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class LeaInput {

    private static volatile LeaInput instance;

    private final LeaConfig config;
    private final LibreFaceConfig libreFaceConfig;
    private final LeaEyeTracker tracker;
    private final LeaCalibrationManager calibration;
    private final LeaTouchSource touchSource;
    private final LibreFaceEngine libreFaceEngine;
    private final LibreFaceGestureDetector gestureDetector;
    private final LibreFaceAnalyzerAdapter faceAdapter;
    private final ConcurrentLinkedQueue<LeaFacialGestureEvent> facialGestures = new ConcurrentLinkedQueue<>();
    private final LibreFaceListener engineListener;

    private TouchSourceRegistry registry;
    private TouchDispatcher dispatcher;
    private volatile boolean started;

    private LeaInput(LeaConfig config, LibreFaceConfig libreFaceConfig) {
        this.config = config;
        this.libreFaceConfig = libreFaceConfig == null ? LibreFaceConfig.load() : libreFaceConfig;
        this.tracker = new LeaEyeTracker(config);
        this.calibration = new LeaCalibrationManager(tracker, config);
        this.touchSource = new LeaTouchSource(config, tracker, calibration);
        this.libreFaceEngine = new LibreFaceEngine(this.libreFaceConfig);
        this.gestureDetector = new LibreFaceGestureDetector(this.libreFaceConfig);
        this.faceAdapter = new LibreFaceAnalyzerAdapter(libreFaceEngine, this.libreFaceConfig);
        this.engineListener = result -> {
            for (LeaFacialGestureEvent event : gestureDetector.update(result,
                    System.currentTimeMillis())) {
                facialGestures.offer(event);
            }
        };
    }

    public static LeaInput get() {
        LeaInput current = instance;
        if (current == null) {
            synchronized (LeaInput.class) {
                current = instance;
                if (current == null) {
                    current = new LeaInput(LeaConfig.load(), LibreFaceConfig.load());
                    instance = current;
                }
            }
        }
        return current;
    }

    public static LeaInput create(LeaConfig config) {
        return create(config, LibreFaceConfig.load());
    }

    public static LeaInput create(LeaConfig config, LibreFaceConfig libreFaceConfig) {
        LeaConfig resolved = config == null ? LeaConfig.load() : config;
        synchronized (LeaInput.class) {
            if (instance != null && instance.started) {
                instance.stop();
            }
            instance = new LeaInput(resolved, libreFaceConfig);
            return instance;
        }
    }

    public static LeaInput getIfPresent() {
        return instance;
    }

    public boolean start(TouchDispatcher target) {
        if (target == null) {
            return false;
        }
        if (started) {
            return tracker.isRunning();
        }
        dispatcher = target;
        registry = new TouchSourceRegistry(target);
        registry.register(touchSource);
        started = true;
        startLibreFace();
        return tracker.isRunning();
    }

    private void startLibreFace() {
        if (!libreFaceConfig.enabled) {
            return;
        }
        libreFaceEngine.addListener(engineListener);
        Thread loader = new Thread(() -> {
            if (libreFaceEngine.start()) {
                tracker.setFaceAnalyzer(faceAdapter);
            }
        }, "LibreFace-Init");
        loader.setDaemon(true);
        loader.start();
    }

    public void stop() {
        tracker.setFaceAnalyzer(null);
        libreFaceEngine.removeListener(engineListener);
        libreFaceEngine.close();
        gestureDetector.reset();
        facialGestures.clear();
        if (registry != null) {
            registry.dispose();
            registry = null;
        } else {
            touchSource.detach();
        }
        started = false;
        dispatcher = null;
    }

    public void tick() {
        if (!started) {
            return;
        }
        touchSource.tick();
        drainFacialGestures();
        TouchDispatcher target = dispatcher;
        if (target != null) {
            target.tick();
        }
    }

    private void drainFacialGestures() {
        LeaFacialGestureEvent event;
        while ((event = facialGestures.poll()) != null) {
            touchSource.dispatchFacialGesture(event, libreFaceConfig.getGestureAction(event.getGesture()));
        }
    }

    public boolean restart() {
        return touchSource.restart();
    }

    public boolean requestCalibration() {
        return touchSource.requestCalibration();
    }

    public void clearCalibration() {
        calibration.reset();
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isRunning() {
        return tracker.isRunning();
    }

    public boolean isCalibrated() {
        return tracker.isCalibrated();
    }

    public boolean isCalibrating() {
        return tracker.isCalibrating();
    }

    public float getCalibrationProgress() {
        return tracker.getCalibrationProgress();
    }

    public boolean isLibreFaceReady() {
        return libreFaceEngine.isReady();
    }

    public LibreFaceResult getLatestFaceResult() {
        return libreFaceEngine.getLatestResult();
    }

    public String getLastError() {
        String touchError = touchSource.getLastError();
        if (touchError != null && !touchError.isEmpty()) {
            return touchError;
        }
        return libreFaceEngine.getLastError();
    }

    public String getStatusLine() {
        StringBuilder builder = new StringBuilder(tracker.getStatusLine());
        if (libreFaceConfig.enabled) {
            builder.append(" || ").append(libreFaceEngine.getStatusLine());
        }
        return builder.toString();
    }

    public void addInputListener(LeaInputListener listener) {
        touchSource.addInputListener(listener);
    }

    public void removeInputListener(LeaInputListener listener) {
        touchSource.removeInputListener(listener);
    }

    public LeaTouchSource getTouchSource() {
        return touchSource;
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

    public LibreFaceConfig getLibreFaceConfig() {
        return libreFaceConfig;
    }

    public LibreFaceEngine getLibreFaceEngine() {
        return libreFaceEngine;
    }

    public LibreFaceGestureDetector getGestureDetector() {
        return gestureDetector;
    }

    public void saveConfig() {
        config.save();
        libreFaceConfig.save();
    }
}
