package com.naturalinteraction.LEA;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class LeaEyeOpennessEstimator {

    private static final int HISTOGRAM_BINS = 256;
    private static final int MAX_SAMPLES_PER_AXIS = 48;

    private final LeaConfig config;
    private final int[] histogram = new int[HISTOGRAM_BINS];

    private double baselineLeft = -1d;
    private double baselineRight = -1d;
    private float opennessLeft = 1f;
    private float opennessRight = 1f;
    private boolean leftValid;
    private boolean rightValid;

    public LeaEyeOpennessEstimator(LeaConfig config) {
        this.config = config;
    }

    public void update(BufferedImage image, Rectangle2D face, float eyeLineY) {
        leftValid = false;
        rightValid = false;
        if (image == null || face == null) {
            return;
        }

        double faceWidth = face.getWidth();
        double faceHeight = face.getHeight();
        if (faceWidth < config.minFaceWidth || faceHeight < config.minFaceWidth * 0.75d) {
            return;
        }

        double minY = face.getY() + faceHeight * 0.18d;
        double maxY = face.getY() + faceHeight * 0.62d;
        double lineY = eyeLineY;
        if (Double.isNaN(lineY) || lineY < minY || lineY > maxY) {
            lineY = face.getY() + faceHeight * 0.42d;
        }

        double windowWidth = faceWidth * config.opennessWindowWidth;
        double windowHeight = faceHeight * config.opennessWindowHeight;
        double leftCenterX = face.getX() + faceWidth * config.opennessLeftCenter;
        double rightCenterX = face.getX() + faceWidth * config.opennessRightCenter;

        double leftRange = windowRange(image, leftCenterX - windowWidth / 2d, lineY - windowHeight / 2d,
                windowWidth, windowHeight);
        double rightRange = windowRange(image, rightCenterX - windowWidth / 2d, lineY - windowHeight / 2d,
                windowWidth, windowHeight);

        if (leftRange >= 0d) {
            baselineLeft = updateBaseline(baselineLeft, leftRange);
            opennessLeft = normalize(leftRange, baselineLeft);
            leftValid = true;
        }
        if (rightRange >= 0d) {
            baselineRight = updateBaseline(baselineRight, rightRange);
            opennessRight = normalize(rightRange, baselineRight);
            rightValid = true;
        }
    }

    public void clear() {
        leftValid = false;
        rightValid = false;
    }

    public void resetBaselines() {
        baselineLeft = -1d;
        baselineRight = -1d;
        opennessLeft = 1f;
        opennessRight = 1f;
        clear();
    }

    public float getOpennessLeft() {
        return opennessLeft;
    }

    public float getOpennessRight() {
        return opennessRight;
    }

    public boolean isLeftValid() {
        return leftValid;
    }

    public boolean isRightValid() {
        return rightValid;
    }

    public double getBaselineLeft() {
        return baselineLeft;
    }

    public double getBaselineRight() {
        return baselineRight;
    }

    private double updateBaseline(double baseline, double range) {
        if (baseline < 0d) {
            return range;
        }
        if (range >= baseline * config.blinkOpenThreshold) {
            return baseline + config.opennessBaselineAlpha * (range - baseline);
        }
        return baseline;
    }

    private float normalize(double range, double baseline) {
        if (baseline < config.opennessMinBaseline) {
            return 1f;
        }
        return (float) Math.max(0d, Math.min(1.5d, range / baseline));
    }

    private double windowRange(BufferedImage image, double x, double y, double width, double height) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        if (imageWidth < 4 || imageHeight < 4) {
            return -1d;
        }

        int x0 = clamp((int) Math.floor(x), 0, imageWidth - 1);
        int y0 = clamp((int) Math.floor(y), 0, imageHeight - 1);
        int x1 = clamp((int) Math.ceil(x + width), x0 + 1, imageWidth);
        int y1 = clamp((int) Math.ceil(y + height), y0 + 1, imageHeight);

        int stepX = Math.max(1, (x1 - x0) / MAX_SAMPLES_PER_AXIS);
        int stepY = Math.max(1, (y1 - y0) / MAX_SAMPLES_PER_AXIS);

        Arrays.fill(histogram, 0);
        int total = 0;
        for (int py = y0; py < y1; py += stepY) {
            for (int px = x0; px < x1; px += stepX) {
                int rgb = image.getRGB(px, py);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                int luminance = (red * 77 + green * 151 + blue * 28) >> 8;
                histogram[luminance]++;
                total++;
            }
        }

        if (total < 8) {
            return -1d;
        }

        int lowTarget = Math.max(1, (int) Math.round(total * 0.05d));
        int highTarget = Math.max(1, (int) Math.round(total * 0.95d));
        int low = percentile(lowTarget);
        int high = percentile(highTarget);
        return high - low;
    }

    private int percentile(int target) {
        int cumulative = 0;
        for (int i = 0; i < HISTOGRAM_BINS; i++) {
            cumulative += histogram[i];
            if (cumulative >= target) {
                return i;
            }
        }
        return HISTOGRAM_BINS - 1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
