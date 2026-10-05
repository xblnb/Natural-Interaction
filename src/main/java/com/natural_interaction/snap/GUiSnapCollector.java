package com.natural_interaction.snap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class GUiSnapCollector implements SnapCollector {

    private final Minecraft mc;

    public GUiSnapCollector(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public boolean isApplicable() {
        return mc.screen != null;
    }

    @Override
    public void collect(List<SnapTarget> out) {
        Screen screen = mc.screen;
        if (screen == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        for (var element : screen.children()) {
            if (!(element instanceof AbstractWidget widget)) continue;
            if (!widget.visible || !widget.active) continue;
            out.add(new WidgetTarget(widget, sw, sh));
        }
    }

    private static final class WidgetTarget implements SnapTarget {

        private final AbstractWidget widget;
        private final int sw;
        private final int sh;

        WidgetTarget(AbstractWidget widget, int sw, int sh) {
            this.widget = widget;
            this.sw = sw;
            this.sh = sh;
        }

        @Override
        public float getScreenX() {
            return (widget.getX() + widget.getWidth() * 0.5f) / sw;
        }

        @Override
        public float getScreenY() {
            return (widget.getY() + widget.getHeight() * 0.5f) / sh;
        }

        @Override
        public float getScreenWidth() { return (float) widget.getWidth() / sw; }

        @Override
        public float getScreenHeight() { return (float) widget.getHeight() / sh; }

        @Override
        public SnapTargetType getType() { return SnapTargetType.UI_BUTTON; }

        @Override
        public float getPriority() { return widget.isFocused() ? 1.3f : 1.0f; }

        @Override
        public boolean isActive() { return widget.active && widget.visible; }

        @Override
        public void activate() {

        }

        /*@Override
        public void activate() {
            widget.onPress();
        }*/
    }
}