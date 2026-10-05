package com.naturalinteraction.snap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class EntitySnapTarget implements SnapTarget {

    private final Minecraft mc;
    private final Entity entity;
    private final float screenX;
    private final float screenY;

    public EntitySnapTarget(Minecraft mc, Entity entity, float screenX, float screenY) {
        this.mc = mc;
        this.entity = entity;
        this.screenX = screenX;
        this.screenY = screenY;
    }

    @Override public float getScreenX() { return screenX; }

    @Override public float getScreenY() { return screenY; }

    @Override public float getScreenWidth() { return 0.06f; }

    @Override public float getScreenHeight() { return 0.06f; }

    @Override public SnapTargetType getType() { return SnapTargetType.ENTITY; }

    @Override public float getPriority() { return 1.15f; }

    @Override public boolean isActive() { return entity.isAlive(); }

    @Override
    public void activate() {
        if (mc.gameMode != null && mc.player != null && entity.isAlive()) {
            mc.gameMode.attack(mc.player, entity);
        }
    }

    public Entity getEntity() { return entity; }
}