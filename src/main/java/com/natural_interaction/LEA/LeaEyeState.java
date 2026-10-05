package com.natural_interaction.LEA;

public enum LeaEyeState {
    UNKNOWN,
    OPEN,
    CLOSING,
    CLOSED,
    OPENING;

    public boolean isClosed() {
        return this == CLOSED || this == CLOSING;
    }

    public boolean isOpen() {
        return this == OPEN || this == OPENING;
    }
}
