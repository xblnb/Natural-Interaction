package com.naturalinteraction.LEA;

public enum LeaFacialGesture {
    MOUTH_OPEN(true, 2.5f),
    LIPS_PART(true, 2.5f),
    JAW_DROP(true, 2.5f),
    SMILE(true, 2.0f),
    CHEEK_RAISE(true, 2.0f),
    BROW_RAISE(true, 2.0f),
    BROW_LOWER(true, 2.0f),
    NOSE_WRINKLE(true, 2.0f),
    LID_TIGHTEN(true, 2.0f),
    LIP_TIGHTEN(true, 2.0f),
    CHIN_RAISE(true, 2.0f),
    LIP_DEPRESSOR(true, 2.0f),
    HAPPINESS(false, 0.55f),
    SURPRISE(false, 0.55f),
    SADNESS(false, 0.55f),
    ANGER(false, 0.55f),
    DISGUST(false, 0.55f),
    FEAR(false, 0.55f),
    CONTEMPT(false, 0.55f);

    private final boolean actionUnitBased;
    private final float defaultThreshold;

    LeaFacialGesture(boolean actionUnitBased, float defaultThreshold) {
        this.actionUnitBased = actionUnitBased;
        this.defaultThreshold = defaultThreshold;
    }

    public boolean isActionUnitBased() {
        return actionUnitBased;
    }

    public float getDefaultThreshold() {
        return defaultThreshold;
    }

    public float getScaleMaximum() {
        return actionUnitBased ? 5f : 1f;
    }
}
