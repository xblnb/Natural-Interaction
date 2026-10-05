package com.naturalinteraction.snap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ScreenProjector {

    private static final double NEAR_CLIP = 0.01;

    private ScreenProjector() {}

    public static Vec3 screenToWorldDirection(Minecraft mc, float nx, float ny) {
        var camera = mc.gameRenderer.getMainCamera();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();

        double halfHeightTan = halfFovTan(mc);
        double halfWidthTan = halfHeightTan * aspect(mc);

        double ndcX = nx * 2.0 - 1.0;
        double ndcY = 1.0 - ny * 2.0;

        double sx = -ndcX * halfWidthTan;
        double sy = ndcY * halfHeightTan;

        Vec3 dir = new Vec3(
                look.x() + left.x() * sx + up.x() * sy,
                look.y() + left.y() * sx + up.y() * sy,
                look.z() + left.z() * sx + up.z() * sy
        );
        return dir.normalize();
    }

    public static float[] projectToScreen(Minecraft mc, Vec3 world) {
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();

        Vector3f lf = camera.getLookVector();
        Vector3f uf = camera.getUpVector();
        Vector3f lfLeft = camera.getLeftVector();

        Vec3 forward = new Vec3(lf.x(), lf.y(), lf.z());
        Vec3 up = new Vec3(uf.x(), uf.y(), uf.z());
        Vec3 left = new Vec3(lfLeft.x(), lfLeft.y(), lfLeft.z());

        Vec3 rel = world.subtract(eye);

        double camZ = rel.dot(forward);
        if (camZ <= NEAR_CLIP) return null;

        double camX = rel.dot(left);
        double camY = rel.dot(up);

        double halfHeightTan = halfFovTan(mc);
        double halfWidthTan = halfHeightTan * aspect(mc);

        double ndcX = -camX / (camZ * halfWidthTan);
        double ndcY = camY / (camZ * halfHeightTan);

        float sx = (float) ((ndcX + 1.0) * 0.5);
        float sy = (float) ((1.0 - ndcY) * 0.5);

        if (sx < -0.5f || sx > 1.5f || sy < -0.5f || sy > 1.5f) return null;

        return new float[]{sx, sy};
    }

    private static double halfFovTan(Minecraft mc) {
        double fovDeg = mc.options.fov().get();
        return Math.tan(Math.toRadians(fovDeg) * 0.5);
    }

    private static double aspect(Minecraft mc) {
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        return (double) w / Math.max(1, h);
    }
}