package com.natural_interaction.snap;

import java.util.List;

public interface SnapCollector {

    boolean isApplicable();

    void collect(List<SnapTarget> out);
}