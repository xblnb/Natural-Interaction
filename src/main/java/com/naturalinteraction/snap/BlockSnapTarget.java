package com.naturalinteraction.snap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

public final class BlockSnapTarget implements SnapTarget {

    private final Minecraft mc;
    private final BlockHitResult hit;
    private final BlockPos pos;
    private final float screenX;
    private final float screenY;

    public BlockSnapTarget(Minecraft mc, BlockHitResult hit, float screenX, float screenY) {
        this.mc = mc;
        this.hit = hit;
        this.pos = hit.getBlockPos();
        this.screenX = screenX;
        this.screenY = screenY;
    }

    @Override public float getScreenX() { return screenX; }

    @Override public float getScreenY() { return screenY; }

    @Override public float getScreenWidth() { return 0.04f; }

    @Override public float getScreenHeight() { return 0.04f; }

    @Override public SnapTargetType getType() { return SnapTargetType.BLOCK; }

    @Override public float getPriority() { return 1.1f; }

    @Override
    public boolean isActive() {
        return mc.level != null && !mc.level.getBlockState(pos).isAir();
    }

    @Override
    public void activate() {
    }

    public BlockPos getPos() { return pos; }

    public BlockHitResult getHit() { return hit; }
}