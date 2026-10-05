package com.naturalinteraction.snap;

import java.util.ArrayList;
import java.util.List;

public final class CompositeSnapCollector {

    private final List<SnapCollector> collectors = new ArrayList<>();

    public void add(SnapCollector collector) {
        collectors.add(collector);
    }

    public void remove(SnapCollector collector) {
        collectors.remove(collector);
    }

    public List<SnapTarget> collectAll() {
        List<SnapTarget> out = new ArrayList<>();
        for (SnapCollector c : collectors) {
            if (c.isApplicable()) c.collect(out);
        }
        return out;
    }
}