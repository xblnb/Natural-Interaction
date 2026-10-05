package com.natural_interaction.snap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public final class InventorySnapCollector implements SnapCollector {

    private final Minecraft mc;

    public InventorySnapCollector(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public boolean isApplicable() {
        return mc.screen instanceof AbstractContainerScreen<?>;
    }

    @Override
    public void collect(List<SnapTarget> out) {
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            out.add(new SlotTarget(slot, guiLeft, guiTop, sw, sh));
        }
    }

    private static final class SlotTarget implements SnapTarget {

        private final Slot slot;
        private final int absX;
        private final int absY;
        private final int sw;
        private final int sh;

        SlotTarget(Slot slot, int guiLeft, int guiTop, int sw, int sh) {
            this.slot = slot;
            this.absX = guiLeft + slot.x;
            this.absY = guiTop + slot.y;
            this.sw = sw;
            this.sh = sh;
        }

        @Override
        public float getScreenX() { return (absX + 8f) / sw; }

        @Override
        public float getScreenY() { return (absY + 8f) / sh; }

        @Override
        public float getScreenWidth() { return 16f / sw; }

        @Override
        public float getScreenHeight() { return 16f / sh; }

        @Override
        public SnapTargetType getType() { return SnapTargetType.UI_SLOT; }

        @Override
        public float getPriority() { return slot.hasItem() ? 1.1f : 0.9f; }

        @Override
        public boolean isActive() { return slot.isActive(); }

        @Override
        public void activate() {
        }
    }
}