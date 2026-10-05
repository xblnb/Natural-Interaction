package com.natural_interaction;

import com.natural_interaction.LEA.LeaInput;
import com.natural_interaction.snap.SnapController;
import com.natural_interaction.snap.SnapOutlineRenderer;
import com.natural_interaction.TAL.TouchDispatcher;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = natural_interaction.MODID, value = Dist.CLIENT)
public final class AccessibilityClient {

    private static final Logger LOG = LoggerFactory.getLogger("NaturalInteraction");

    private static TouchDispatcher dispatcher;
    private static MinecraftInputHandler inputHandler;
    private static SnapController snapController;
    private static SnapOutlineRenderer snapRenderer;
    private static boolean started;
    private static int tickCount;

    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!started) {
            startOnce(mc);
        }
        if (!started) return;

        LeaInput.get().tick();
        dispatcher.tick();

        if (snapController != null) {
            float gx = dispatcher.getCursor().getX();
            float gy = dispatcher.getCursor().getY();
            snapController.update(gx, gy);
        }

        tickCount++;
        if (tickCount == 100) {
            LOG.info("[NaturalInteraction] status: {}", LeaInput.get().getStatusLine());
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (snapRenderer != null) {
            snapRenderer.onRenderLevel(event);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (event.getKey() == GLFW.GLFW_KEY_K) {
            requestCalibration();
            LOG.info("[NaturalInteraction] calibration requested");
        }
    }

    private static void startOnce(Minecraft mc) {
        dispatcher = new TouchDispatcher();
        inputHandler = new MinecraftInputHandler(mc);
        dispatcher.register(inputHandler);

        snapController = new SnapController(mc);
        snapRenderer = new SnapOutlineRenderer(snapController);

        boolean ok = LeaInput.get().start(dispatcher);
        started = ok;

        if (!ok) {
            LOG.error("[NaturalInteraction] LEA start failed: {}", LeaInput.get().getLastError());
            LOG.error("[NaturalInteraction] status: {}", LeaInput.get().getStatusLine());
        } else {
            LOG.info("[NaturalInteraction] LEA started");
        }
    }

    public static SnapController getSnapController() { return snapController; }

    public static MinecraftInputHandler getInputHandler() { return inputHandler; }

    public static void requestCalibration() {
        if (started) LeaInput.get().requestCalibration();
    }
}