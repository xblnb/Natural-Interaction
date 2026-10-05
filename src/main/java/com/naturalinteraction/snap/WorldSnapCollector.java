package com.naturalinteraction.snap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WorldSnapCollector implements SnapCollector {

    private static final int GRID = 3;
    private static final float GRID_STEP = 0.035f;

    private final Minecraft mc;

    private float gazeX = 0.5f;
    private float gazeY = 0.5f;

    public WorldSnapCollector(Minecraft mc) {
        this.mc = mc;
    }

    public void setGaze(float x, float y) {
        this.gazeX = x;
        this.gazeY = y;
    }

    @Override
    public boolean isApplicable() {
        return mc.screen == null && mc.level != null && mc.player != null;
    }

    @Override
    public void collect(List<SnapTarget> out) {
        if (!isApplicable()) return;

        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        double blockReach = mc.player.blockInteractionRange();
        double entityReach = mc.player.entityInteractionRange();

        Set<Long> seenBlocks = new HashSet<>();
        Set<Entity> seenEntities = new HashSet<>();

        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                float ox = (i - GRID / 2) * GRID_STEP;
                float oy = (j - GRID / 2) * GRID_STEP;

                float sx = gazeX + ox;
                float sy = gazeY + oy;
                if (sx < 0f || sx > 1f || sy < 0f || sy > 1f) continue;

                Vec3 dir = ScreenProjector.screenToWorldDirection(mc, sx, sy);

                scanBlock(eye, eye.add(dir.scale(blockReach)), seenBlocks, out);
                scanEntity(eye, eye.add(dir.scale(entityReach)), seenEntities, out);
            }
        }
    }

    private void scanBlock(Vec3 eye, Vec3 end, Set<Long> seen, List<SnapTarget> out) {
        BlockHitResult hit = mc.level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        if (hit.getType() != HitResult.Type.BLOCK) return;

        long key = hit.getBlockPos().asLong();
        if (!seen.add(key)) return;

        Vec3 center = Vec3.atCenterOf(hit.getBlockPos());
        float[] screen = ScreenProjector.projectToScreen(mc, center);
        if (screen == null) return;

        out.add(new BlockSnapTarget(mc, hit, screen[0], screen[1]));
    }

    private void scanEntity(Vec3 eye, Vec3 end, Set<Entity> seen, List<SnapTarget> out) {
        AABB box = new AABB(eye, end).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.level, mc.player, eye, end, box,
                e -> !e.isSpectator() && e.isPickable(),
                0.3f
        );
        if (hit == null) return;

        Entity entity = hit.getEntity();
        if (!seen.add(entity)) return;

        Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        float[] screen = ScreenProjector.projectToScreen(mc, center);
        if (screen == null) return;

        out.add(new EntitySnapTarget(mc, entity, screen[0], screen[1]));
    }
}