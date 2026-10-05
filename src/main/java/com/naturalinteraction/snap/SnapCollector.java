package com.naturalinteraction.snap;

import java.util.List;

public interface SnapCollector {

    boolean isApplicable();

    void collect(List<SnapTarget> out);
}