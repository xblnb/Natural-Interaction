package com.natural_interaction.LEA;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public final class LeaEyeLocator {

    private static final double EYE_VERTICAL_LIMIT = 0.72d;
    private static final double CONTRAST_LIMIT = 0.85d;
    private static final double MAX_OFFSET_RATIO = 0.9d;

    private final LeaConfig config;

    public LeaEyeLocator(LeaConfig config) {
        this.config = config;
    }

    public Point locate(BufferedImage image, Point landmark, Rectangle2D faceBox) {
        if (landmark == null || image == null || faceBox == null) {
            return landmark;
        }
        Point darkest = darkestCentroid(image, landmark, faceBox);
        if (darkest == null) {
            return landmark;
        }
        return isPlausible(darkest, faceBox) ? darkest : landmark;
    }

    public boolean isPlausiblePair(Point right, Point left, Rectangle2D faceBox) {
        if (right == null || left == null || faceBox == null) {
            return false;
        }
        if (!isPlausible(right, faceBox) || !isPlausible(left, faceBox)) {
            return false;
        }
        double distance = right.distance(left);
        if (distance < faceBox.getWidth() * 0.15d || distance > faceBox.getWidth() * 0.95d) {
            return false;
        }
        return Math.abs(right.y - left.y) <= faceBox.getWidth() * 0.2d;
    }

    public boolean isPlausible(Point point, Rectangle2D faceBox) {
        if (point == null || faceBox == null) {
            return false;
        }
        if (point.x < faceBox.getX() || point.x > faceBox.getX() + faceBox.getWidth()) {
            return false;
        }
        if (point.y < faceBox.getY() || point.y > faceBox.getY() + faceBox.getHeight()) {
            return false;
        }
        return point.y <= faceBox.getY() + faceBox.getHeight() * EYE_VERTICAL_LIMIT;
    }

    private Point darkestCentroid(BufferedImage image, Point landmark, Rectangle2D faceBox) {
        double radius = Math.max(2d, faceBox.getWidth() * config.eyeSearchRadius);
        int x0 = clamp((int) Math.round(landmark.x - radius), 0, image.getWidth() - 1);
        int x1 = clamp((int) Math.round(landmark.x + radius), x0 + 1, image.getWidth());
        int y0 = clamp((int) Math.round(landmark.y - radius), 0, image.getHeight() - 1);
        int y1 = clamp((int) Math.round(landmark.y + radius), y0 + 1, image.getHeight());
        int total = (x1 - x0) * (y1 - y0);
        if (total < 8) {
            return null;
        }

        long brightnessSum = 0L;
        int darkest = Integer.MAX_VALUE;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int brightness = brightnessAt(image, x, y);
                brightnessSum += brightness;
                if (brightness < darkest) {
                    darkest = brightness;
                }
            }
        }

        double average = brightnessSum / (double) total;
        if (average <= 0d || darkest > average * CONTRAST_LIMIT) {
            return null;
        }

        int threshold = darkest + config.eyeDarknessTolerance;
        long sumX = 0L;
        long sumY = 0L;
        int count = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (brightnessAt(image, x, y) <= threshold) {
                    sumX += x;
                    sumY += y;
                    count++;
                }
            }
        }
        if (count == 0 || count > total / 4) {
            return null;
        }
        Point centroid = new Point((int) Math.round(sumX / (double) count),
                (int) Math.round(sumY / (double) count));
        return centroid.distance(landmark) > radius * MAX_OFFSET_RATIO ? null : centroid;
    }

    private static int brightnessAt(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        return Math.min((rgb >> 16) & 0xFF, Math.min((rgb >> 8) & 0xFF, rgb & 0xFF));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
