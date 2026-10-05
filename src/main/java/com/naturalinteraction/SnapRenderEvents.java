package com.naturalinteraction;

import com.naturalinteraction.snap.SnapOutlineRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = naturalinteraction.MODID, value = Dist.CLIENT)
public final class SnapRenderEvents {

    private static SnapOutlineRenderer renderer;

    public static void init(SnapOutlineRenderer r) {
        renderer = r;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (renderer == null) return;
        renderer.onRenderLevel(event);
    }
}