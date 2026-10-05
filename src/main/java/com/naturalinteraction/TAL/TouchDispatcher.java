package com.naturalinteraction.TAL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TouchDispatcher {

    private final List<TouchListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Integer, TouchPointer> pointers = new HashMap<>();
    private final TouchEventQueue eventQueue = new TouchEventQueue();
    private final VirtualCursor cursor = new VirtualCursor();

    public void register(TouchListener listener) {
        listeners.add(listener);
    }

    public void unregister(TouchListener listener) {
        listeners.remove(listener);
    }

    public VirtualCursor getCursor() { return cursor; }

    public void onPointerMotionRelative(int pointerId, float dx, float dy) {
        TouchPointer pointer = pointers.get(pointerId);
        if (pointer != null && pointer.isActive()) {
            cursor.moveRelative(dx, dy);
            pointer.move(cursor.getX(), cursor.getY(), 1f);
            enqueueMove(pointer);
        } else {
            cursor.moveRelative(dx, dy);
        }
    }

    public void onPointerMotionAbsolute(int pointerId, float x, float y) {
        TouchPointer pointer = pointers.get(pointerId);
        if (pointer != null && pointer.isActive()) {
            cursor.moveAbsolute(x, y);
            pointer.move(cursor.getX(), cursor.getY(), 1f);
            enqueueMove(pointer);
        } else {
            cursor.moveAbsolute(x, y);
        }
    }

    public void onPointerDown(int pointerId, float x, float y, float pressure) {
        long now = System.currentTimeMillis();
        TouchPointer pointer = pointers.computeIfAbsent(pointerId, TouchPointer::new);
        pointer.down(x, y, pressure, now);
        enqueue(new TouchEvent(pointerId, TouchAction.DOWN, x, y, pressure, now));
    }

    public void onPointerDownAtCursor(int pointerId, float pressure) {
        onPointerDown(pointerId, cursor.getX(), cursor.getY(), pressure);
    }

    public void onPointerMove(int pointerId, float x, float y, float pressure) {
        TouchPointer pointer = pointers.get(pointerId);
        if (pointer == null || !pointer.isActive()) return;
        pointer.move(x, y, pressure);
        enqueue(new TouchEvent(pointerId, TouchAction.MOVE, x, y, pressure,
                System.currentTimeMillis()));
    }

    public void onPointerMoveAtCursor(int pointerId, float pressure) {
        onPointerMove(pointerId, cursor.getX(), cursor.getY(), pressure);
    }

    public void onPointerUp(int pointerId) {
        TouchPointer pointer = pointers.get(pointerId);
        if (pointer == null || !pointer.isActive()) return;
        float x = pointer.getX();
        float y = pointer.getY();
        pointer.up();
        enqueue(new TouchEvent(pointerId, TouchAction.UP, x, y, 0f,
                System.currentTimeMillis()));
    }

    public void onPointerCancel(int pointerId) {
        TouchPointer pointer = pointers.get(pointerId);
        if (pointer == null || !pointer.isActive()) return;
        float x = pointer.getX();
        float y = pointer.getY();
        pointer.up();
        enqueue(new TouchEvent(pointerId, TouchAction.CANCEL, x, y, 0f,
                System.currentTimeMillis()));
    }

    public void cancelAll() {
        for (TouchPointer pointer : pointers.values()) {
            if (pointer.isActive()) {
                onPointerCancel(pointer.getId());
            }
        }
    }

    public void tick() {
        TouchEvent event;
        while ((event = eventQueue.poll()) != null) {
            for (TouchListener listener : listeners) {
                if (listener.onTouch(event)) break;
            }
        }
    }

    public TouchPointer getPointer(int id) {
        return pointers.get(id);
    }

    public boolean isPointerActive(int id) {
        TouchPointer p = pointers.get(id);
        return p != null && p.isActive();
    }

    public void clearPointers() {
        pointers.clear();
    }

    private void enqueue(TouchEvent event) {
        eventQueue.offer(event);
    }

    private void enqueueMove(TouchPointer pointer) {
        enqueue(new TouchEvent(pointer.getId(), TouchAction.MOVE,
                pointer.getX(), pointer.getY(), pointer.getPressure(),
                System.currentTimeMillis()));
    }
}