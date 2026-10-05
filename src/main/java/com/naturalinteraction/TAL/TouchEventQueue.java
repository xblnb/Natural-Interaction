package com.naturalinteraction.TAL;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class TouchEventQueue {

    private final ConcurrentLinkedQueue<TouchEvent> queue = new ConcurrentLinkedQueue<>();

    public void offer(TouchEvent event) {
        queue.offer(event);
    }

    public TouchEvent poll() {
        return queue.poll();
    }

    public void clear() {
        queue.clear();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
