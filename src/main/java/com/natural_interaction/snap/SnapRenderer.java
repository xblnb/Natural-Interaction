package com.natural_interaction.snap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class SnapRenderer {

    private static final int PADDING = 2;
    private static final int BAR_HEIGHT = 2;

    private final Minecraft mc;
    private final SnapConfig config;

    public SnapRenderer(Minecraft mc, SnapConfig config) {
        this.mc = mc;
        this.config = config;
    }

    public void renderGui(GuiGraphics graphics, SnapTarget target, float dwellProgress) {
        if (!config.renderHighlight || target == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int w = Math.max(2, Math.round(target.getScreenWidth() * sw));
        int h = Math.max(2, Math.round(target.getScreenHeight() * sh));
        int cx = Math.round(target.getScreenX() * sw);
        int cy = Math.round(target.getScreenY() * sh);
        int x = cx - w / 2;
        int y = cy - h / 2;

        int color = config.highlightColor;

        graphics.renderOutline(
                x - PADDING,
                y - PADDING,
                w + PADDING * 2,
                h + PADDING * 2,
                color);

        if (dwellProgress > 0f) {
            int barY = y + h + PADDING + 2;
            int totalW = w + PADDING * 2;
            int filledW = Math.round(totalW * Math.max(0f, Math.min(1f, dwellProgress)));
            int barX = x - PADDING;
            graphics.fill(barX, barY, barX + totalW, barY + BAR_HEIGHT, 0x80000000);
            graphics.fill(barX, barY, barX + filledW, barY + BAR_HEIGHT, color);
        }
    }
}