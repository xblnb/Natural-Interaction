package com.natural_interaction.snap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.natural_interaction.mixin.accessor.LevelRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class SnapOutlineRenderer {

    private static final float R = 0.20f;
    private static final float G = 1.00f;
    private static final float B = 0.60f;
    private static final float A = 1.00f;

    private final SnapController controller;

    public SnapOutlineRenderer(SnapController controller) {
        this.controller = controller;
    }

    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!controller.hasSnap()) return;

        SnapTarget target = controller.getCurrent();
        if (target == null || !target.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        boolean rendered = false;

        if (target instanceof BlockSnapTarget block) {
            rendered = renderBlock(mc, pose, cam, lines, block);
        } else if (target instanceof EntitySnapTarget entity) {
            rendered = renderEntity(pose, cam, lines, entity);
        }

        if (rendered) {
            buffers.endBatch(RenderType.lines());
        }
    }

    private boolean renderBlock(Minecraft mc, PoseStack pose, Vec3 cam,
                                VertexConsumer lines, BlockSnapTarget target) {
        BlockPos pos = target.getPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return false;

        VoxelShape shape = state.getShape(mc.level, pos);
        if (shape.isEmpty()) shape = Shapes.block();

        pose.pushPose();
        pose.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        LevelRendererAccessor.callRenderShape(pose, lines, shape, 0, 0, 0, R, G, B, A);
        pose.popPose();
        return true;
    }

    private boolean renderEntity(PoseStack pose, Vec3 cam,
                                 VertexConsumer lines, EntitySnapTarget target) {
        Entity entity = target.getEntity();
        if (!entity.isAlive()) return false;

        AABB box = entity.getBoundingBox().move(-cam.x, -cam.y, -cam.z);
        LevelRenderer.renderLineBox(pose, lines, box, R, G, B, A);
        return true;
    }
}