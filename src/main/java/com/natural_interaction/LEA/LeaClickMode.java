package com.natural_interaction.LEA;

public enum LeaClickMode {
    DWELL,
    BLINK,
    DWELL_OR_BLINK,
    DISABLED;

    public boolean usesDwell() {
        return this == DWELL || this == DWELL_OR_BLINK;
    }

    public boolean usesBlink() {
        return this == BLINK || this == DWELL_OR_BLINK;
    }
}
