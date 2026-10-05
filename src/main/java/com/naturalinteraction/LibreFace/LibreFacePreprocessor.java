package com.naturalinteraction.LibreFace;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public final class LibreFacePreprocessor {

    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};
    private static final double RIGHT_EYE_X = 0.35d;
    private static final double LEFT_EYE_X = 0.65d;
    private static final double EYE_Y = 0.40d;

    public static double getTemplateRightEyeX() {
        return RIGHT_EYE_X;
    }

    public static double getTemplateLeftEyeX() {
        return LEFT_EYE_X;
    }

    public static double getTemplateEyeY() {
        return EYE_Y;
    }

    public Rectangle2D computeCropRect(Rectangle2D faceRect, float padding, float offsetY) {
        if (faceRect == null) {
            return null;
        }
        double side = Math.max(faceRect.getWidth(), faceRect.getHeight());
        if (side < 2d) {
            side = 2d;
        }
        side *= 1d + Math.max(0f, padding);

        double centerX = faceRect.getX() + faceRect.getWidth() / 2d;
        double centerY = faceRect.getY() + faceRect.getHeight() / 2d
                + faceRect.getHeight() * offsetY;
        return new Rectangle2D.Double(centerX - side / 2d, centerY - side / 2d, side, side);
    }

    public BufferedImage alignFace(BufferedImage source, Point rightEye, Point leftEye, int outputSize) {
        if (source == null || rightEye == null || leftEye == null) {
            return null;
        }
        double interOcular = rightEye.distance(leftEye);
        if (interOcular < 4d) {
            return null;
        }
        int size = Math.max(64, outputSize);
        double targetInterOcular = (LEFT_EYE_X - RIGHT_EYE_X) * size;
        double scale = targetInterOcular / interOcular;
        double theta = -Math.atan2(leftEye.y - rightEye.y, leftEye.x - rightEye.x);
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double m00 = scale * cos;
        double m01 = -scale * sin;
        double m10 = scale * sin;
        double m11 = scale * cos;
        double sourceMidX = (rightEye.x + leftEye.x) / 2d;
        double sourceMidY = (rightEye.y + leftEye.y) / 2d;
        double targetMidX = (RIGHT_EYE_X + LEFT_EYE_X) / 2d * size;
        double targetMidY = EYE_Y * size;

        AffineTransform transform = new AffineTransform(m00, m10, m01, m11,
                targetMidX - (m00 * sourceMidX + m01 * sourceMidY),
                targetMidY - (m10 * sourceMidX + m11 * sourceMidY));
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, transform, null);
        graphics.dispose();
        return output;
    }

    public BufferedImage centerCrop(BufferedImage source, int outputSize) {
        if (source == null) {
            return null;
        }
        int size = Math.max(32, outputSize);
        if (source.getWidth() == size && source.getHeight() == size
                && source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        int side = Math.min(source.getWidth(), source.getHeight());
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int offsetX = (source.getWidth() - side) / 2;
        int offsetY = (source.getHeight() - side) / 2;
        graphics.drawImage(source, 0, 0, size, size,
                offsetX, offsetY, offsetX + side, offsetY + side, null);
        graphics.dispose();
        return output;
    }

    public BufferedImage cropFace(BufferedImage source, Rectangle2D faceRect,
                                  float padding, float offsetY, int outputSize) {
        if (source == null || faceRect == null) {
            return null;
        }
        int size = Math.max(32, outputSize);
        Rectangle2D crop = computeCropRect(faceRect, padding, offsetY);
        int cropX = (int) Math.round(crop.getX());
        int cropY = (int) Math.round(crop.getY());
        int cropWidth = Math.max(1, (int) Math.round(crop.getWidth()));
        int cropHeight = Math.max(1, (int) Math.round(crop.getHeight()));

        BufferedImage canvas = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_RGB);
        int sourceX0 = Math.max(cropX, 0);
        int sourceY0 = Math.max(cropY, 0);
        int sourceX1 = Math.min(cropX + cropWidth, source.getWidth());
        int sourceY1 = Math.min(cropY + cropHeight, source.getHeight());
        if (sourceX1 > sourceX0 && sourceY1 > sourceY0) {
            Graphics2D canvasGraphics = canvas.createGraphics();
            canvasGraphics.drawImage(source,
                    sourceX0 - cropX, sourceY0 - cropY,
                    sourceX1 - cropX, sourceY1 - cropY,
                    sourceX0, sourceY0, sourceX1, sourceY1, null);
            canvasGraphics.dispose();
        }

        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(canvas, 0, 0, size, size, 0, 0, cropWidth, cropHeight, null);
        graphics.dispose();
        return output;
    }

    public float[] toChw(BufferedImage image) {
        return toChw(image, null);
    }

    public float[] toChw(BufferedImage image, float[] target) {
        if (image == null) {
            return null;
        }
        int size = Math.min(image.getWidth(), image.getHeight());
        int plane = size * size;
        float[] data = target != null && target.length >= 3 * plane ? target : new float[3 * plane];
        int[] row = new int[size];
        for (int y = 0; y < size; y++) {
            image.getRGB(0, y, size, 1, row, 0, size);
            int rowOffset = y * size;
            for (int x = 0; x < size; x++) {
                int rgb = row[x];
                float red = ((rgb >> 16) & 0xFF) / 255f;
                float green = ((rgb >> 8) & 0xFF) / 255f;
                float blue = (rgb & 0xFF) / 255f;
                data[rowOffset + x] = (red - MEAN[0]) / STD[0];
                data[plane + rowOffset + x] = (green - MEAN[1]) / STD[1];
                data[2 * plane + rowOffset + x] = (blue - MEAN[2]) / STD[2];
            }
        }
        return data;
    }

    public static int tensorLength(int size) {
        return 3 * size * size;
    }

    public static float[] mean() {
        return MEAN.clone();
    }

    public static float[] standardDeviation() {
        return STD.clone();
    }
}
