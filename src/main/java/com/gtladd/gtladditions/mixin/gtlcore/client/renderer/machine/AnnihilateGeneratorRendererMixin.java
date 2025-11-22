package com.gtladd.gtladditions.mixin.gtlcore.client.renderer.machine;

import org.gtlcore.gtlcore.client.renderer.machine.AnnihilateGeneratorRenderer;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gtladd.gtladditions.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AnnihilateGeneratorRenderer.class, remap = false)
public abstract class AnnihilateGeneratorRendererMixin {

    @Shadow(remap = false)
    private static void renderStar(float tick, PoseStack poseStack, MultiBufferSource buffer) {
        throw new AssertionError();
    }

    /**
     * @author Dragons
     * @reason Smooth render
     */
    @Overwrite(remap = false)
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        if (blockEntity instanceof IMachineBlockEntity machineBlockEntity &&
                machineBlockEntity.getMetaMachine() instanceof WorkableElectricMultiblockMachine machine && machine.isActive()) {
            float tick = RenderUtils.INSTANCE.getSmoothTick(machine, partialTicks);
            double x = 0.5, y = 36.5, z = 0.5;
            switch (machine.getFrontFacing()) {
                case NORTH -> z = 39.5;
                case SOUTH -> z = -38.5;
                case WEST -> x = 39.5;
                case EAST -> x = -38.5;
            }
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            renderStar(tick, poseStack, buffer);
            poseStack.popPose();
        }
    }
}
