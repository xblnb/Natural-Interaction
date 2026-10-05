package com.natural_interaction.LEA;

public final class LeaGazeSample {

    private final long timestamp;
    private final boolean faceFound;
    private final boolean eyeFound;
    private final boolean calibrated;
    private final float faceX;
    private final float faceY;
    private final float faceWidth;
    private final float faceHeight;
    private final float eyeX;
    private final float eyeY;
    private final float relativeX;
    private final float relativeY;
    private final float offsetX;
    private final float offsetY;
    private final float opennessLeft;
    private final float opennessRight;
    private final LeaEyeState leftEyeState;
    private final LeaEyeState rightEyeState;

    public LeaGazeSample(long timestamp, boolean faceFound, boolean eyeFound, boolean calibrated,
                         float faceX, float faceY, float faceWidth, float faceHeight,
                         float eyeX, float eyeY, float relativeX, float relativeY,
                         float offsetX, float offsetY,
                         float opennessLeft, float opennessRight,
                         LeaEyeState leftEyeState, LeaEyeState rightEyeState) {
        this.timestamp = timestamp;
        this.faceFound = faceFound;
        this.eyeFound = eyeFound;
        this.calibrated = calibrated;
        this.faceX = faceX;
        this.faceY = faceY;
        this.faceWidth = faceWidth;
        this.faceHeight = faceHeight;
        this.eyeX = eyeX;
        this.eyeY = eyeY;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.opennessLeft = opennessLeft;
        this.opennessRight = opennessRight;
        this.leftEyeState = leftEyeState;
        this.rightEyeState = rightEyeState;
    }

    public long getTimestamp() { return timestamp; }
    public boolean isFaceFound() { return faceFound; }
    public boolean isEyeFound() { return eyeFound; }
    public boolean isCalibrated() { return calibrated; }
    public float getFaceX() { return faceX; }
    public float getFaceY() { return faceY; }
    public float getFaceWidth() { return faceWidth; }
    public float getFaceHeight() { return faceHeight; }
    public float getEyeX() { return eyeX; }
    public float getEyeY() { return eyeY; }
    public float getRelativeX() { return relativeX; }
    public float getRelativeY() { return relativeY; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOpennessLeft() { return opennessLeft; }
    public float getOpennessRight() { return opennessRight; }
    public LeaEyeState getLeftEyeState() { return leftEyeState; }
    public LeaEyeState getRightEyeState() { return rightEyeState; }

    public boolean isUsable() {
        return faceFound && eyeFound;
    }

    public float getOffsetMagnitude() {
        return (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY);
    }

    @Override
    public String toString() {
        return "LeaGazeSample{face=" + faceFound + ", eye=" + eyeFound
                + ", rel=(" + relativeX + "," + relativeY + ")"
                + ", offset=(" + offsetX + "," + offsetY + ")}";
    }
}
