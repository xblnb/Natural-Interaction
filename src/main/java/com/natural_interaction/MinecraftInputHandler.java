package com.natural_interaction;

import com.natural_interaction.TAL.TouchAction;
import com.natural_interaction.TAL.TouchEvent;
import com.natural_interaction.TAL.TouchListener;
import com.natural_interaction.TAL.TouchPointerIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class MinecraftInputHandler implements TouchListener {

    private static final float VIEW_DEADZONE = 0.10f;
    private static final float VIEW_MAX_SPEED = 8.0f;

    private final Minecraft mc;

    private boolean primaryDown;
    private boolean secondaryDown;

    public MinecraftInputHandler(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public boolean onTouch(TouchEvent event) {
        if (mc.player == null || mc.level == null) {
            return false;
        }

        int id = event.getPointerId();
        if (id == TouchPointerIds.GAZE) {
            handleGaze(event);
            return true;
        }
        if (id == TouchPointerIds.PRIMARY) {
            handlePrimary(event);
            return true;
        }
        if (id == TouchPointerIds.SECONDARY) {
            handleSecondary(event);
            return true;
        }
        return false;
    }

    private void handleGaze(TouchEvent event) {
        if (mc.screen == null) {
            handleWorldGaze(event);
        } else {
            handleGuiGaze(event);
        }
    }

    private void handleWorldGaze(TouchEvent event) {
        switch (event.getAction()) {
            case MOVE -> updateView(event.getX(), event.getY());
            case DOWN -> performAttack();
            case UP -> { }
            case CANCEL -> { }
        }
    }

    private void handleGuiGaze(TouchEvent event) {
        Screen screen = mc.screen;
        if (screen == null) return;

        switch (event.getAction()) {
            case MOVE -> sendGuiMove(event.getX(), event.getY());
            case DOWN -> sendGuiClick(event.getX(), event.getY(), 0);
            case UP -> sendGuiRelease(event.getX(), event.getY(), 0);
            case CANCEL -> { }
        }
    }

    private void updateView(float x, float y) {
        float dx = x - 0.5f;
        float dy = y - 0.5f;

        float yawDelta = axisDelta(dx, -1f);
        float pitchDelta = axisDelta(dy, 1f);

        if (yawDelta != 0f || pitchDelta != 0f) {
            mc.player.turn(yawDelta, pitchDelta);
        }
    }

    private float axisDelta(float offset, float sign) {
        float abs = Math.abs(offset);
        if (abs <= VIEW_DEADZONE) return 0f;
        float factor = (abs - VIEW_DEADZONE) / (0.5f - VIEW_DEADZONE);
        factor = Math.min(1f, factor);
        return sign * Math.signum(offset) * factor * VIEW_MAX_SPEED;
    }

    private void performAttack() {
        if (mc.player == null || mc.gameMode == null) return;

        mc.player.swing(InteractionHand.MAIN_HAND);

        HitResult hit = mc.hitResult;
        if (hit == null) return;

        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) hit;
            Entity entity = ehr.getEntity();
            mc.gameMode.attack(mc.player, entity);
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            mc.gameMode.startDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
        }
    }

    private void handlePrimary(TouchEvent event) {
        switch (event.getAction()) {
            case DOWN -> {
                primaryDown = true;
                performAttack();
            }
            case UP, CANCEL -> {
                primaryDown = false;
                if (mc.gameMode != null) {
                    mc.gameMode.stopDestroyBlock();
                }
            }
        }
    }

    private void handleSecondary(TouchEvent event) {
        switch (event.getAction()) {
            case DOWN -> {
                secondaryDown = true;
                if (mc.options != null) {
                    mc.options.keyUse.setDown(true);
                }
            }
            case UP, CANCEL -> {
                secondaryDown = false;
                if (mc.options != null) {
                    mc.options.keyUse.setDown(false);
                }
            }
        }
    }

    private void sendGuiMove(float nx, float ny) {
        Screen screen = mc.screen;
        if (screen == null) return;
        double x = nx * mc.getWindow().getGuiScaledWidth();
        double y = ny * mc.getWindow().getGuiScaledHeight();
        screen.mouseMoved(x, y);
    }

    private void sendGuiClick(float nx, float ny, int button) {
        Screen screen = mc.screen;
        if (screen == null) return;
        double x = nx * mc.getWindow().getGuiScaledWidth();
        double y = ny * mc.getWindow().getGuiScaledHeight();
        screen.mouseClicked(x, y, button);
    }

    private void sendGuiRelease(float nx, float ny, int button) {
        Screen screen = mc.screen;
        if (screen == null) return;
        double x = nx * mc.getWindow().getGuiScaledWidth();
        double y = ny * mc.getWindow().getGuiScaledHeight();
        screen.mouseReleased(x, y, button);
    }

    public void releaseAll() {
        if (primaryDown) {
            primaryDown = false;
            if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        }
        if (secondaryDown) {
            secondaryDown = false;
            if (mc.options != null) mc.options.keyUse.setDown(false);
        }
    }
}