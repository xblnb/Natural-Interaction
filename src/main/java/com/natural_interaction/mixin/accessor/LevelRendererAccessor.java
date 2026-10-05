package com.natural_interaction.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Invoker("renderShape")
    static void callRenderShape(
            PoseStack pose,
            VertexConsumer lines,
            VoxelShape shape,
            double x, double y, double z,
            float r, float g, float b, float a
    ) {
        throw new AssertionError();
    }
}