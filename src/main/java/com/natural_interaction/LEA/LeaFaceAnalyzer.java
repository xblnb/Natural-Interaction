package com.natural_interaction.LEA;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public interface LeaFaceAnalyzer {

    void submitFrame(BufferedImage frame, Rectangle2D faceRect, long timestamp);

    default void submitFrame(BufferedImage frame, Rectangle2D faceRect,
                             Point rightEye, Point leftEye, long timestamp) {
        submitFrame(frame, faceRect, timestamp);
    }

    String getName();

    boolean isReady();

    String getLastError();
}
