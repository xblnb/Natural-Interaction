package com.naturalinteraction.TAL;

import java.util.ArrayList;
import java.util.List;

public final class TouchSourceRegistry {

    private final TouchDispatcher dispatcher;
    private final List<TouchSource> sources = new ArrayList<>();

    public TouchSourceRegistry(TouchDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void register(TouchSource source) {
        sources.add(source);
        source.attach(dispatcher);
    }

    public void unregister(TouchSource source) {
        sources.remove(source);
        source.detach();
    }

    public void dispose() {
        for (TouchSource source : sources) {
            source.detach();
        }
        sources.clear();
    }

    public List<TouchSource> getSources() {
        return new ArrayList<>(sources);
    }
}
