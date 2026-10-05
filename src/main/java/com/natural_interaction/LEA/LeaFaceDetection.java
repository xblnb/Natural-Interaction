package com.natural_interaction.LEA;

import java.awt.Point;
import java.awt.geom.Rectangle2D;

public final class LeaFaceDetection {

    private final Rectangle2D faceBox;
    private final Point rightEye;
    private final Point leftEye;
    private final float score;
    private final long timestamp;

    public LeaFaceDetection(Rectangle2D faceBox, Point rightEye, Point leftEye,
                            float score, long timestamp) {
        this.faceBox = faceBox;
        this.rightEye = rightEye;
        this.leftEye = leftEye;
        this.score = score;
        this.timestamp = timestamp;
    }

    public Rectangle2D getFaceBox() { return faceBox; }
    public Point getRightEye() { return rightEye; }
    public Point getLeftEye() { return leftEye; }
    public float getScore() { return score; }
    public long getTimestamp() { return timestamp; }

    public boolean hasEyes() {
        return rightEye != null && leftEye != null;
    }

    public double getInterOcularDistance() {
        if (!hasEyes()) {
            return 0d;
        }
        return rightEye.distance(leftEye);
    }

    public Point getEyeMidpoint() {
        if (!hasEyes()) {
            return null;
        }
        return new Point((rightEye.x + leftEye.x) / 2, (rightEye.y + leftEye.y) / 2);
    }

    @Override
    public String toString() {
        return String.format("LeaFaceDetection{box=%.0f,%.0f %.0fx%.0f score=%.2f right=%s left=%s}",
                faceBox.getX(), faceBox.getY(), faceBox.getWidth(), faceBox.getHeight(), score,
                rightEye, leftEye);
    }
}
