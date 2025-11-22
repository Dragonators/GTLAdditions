package com.gtladd.gtladditions.mixin.gtlcore.client.renderer.machine;

import org.gtlcore.gtlcore.client.renderer.machine.EyeOfHarmonyRenderer;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gtladd.gtladditions.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EyeOfHarmonyRenderer.class)
public abstract class EyeOfHarmonyRendererMixin {

    @Shadow(remap = false)
    private static void renderStar(float tick, PoseStack poseStack, MultiBufferSource buffer) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private void renderOrbitObjects(float tick, PoseStack poseStack, MultiBufferSource buffer, double x, double y, double z) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private void renderOuterSpaceShell(PoseStack poseStack, MultiBufferSource buffer) {
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
                machineBlockEntity.getMetaMachine() instanceof HarmonyMachine machine && machine.isActive()) {
            float tick = RenderUtils.INSTANCE.getSmoothTick(machine, partialTicks);
            double x = 0.5, y = 0.5, z = 0.5;
            switch (machine.getFrontFacing()) {
                case NORTH -> z = 16.5;
                case SOUTH -> z = -15.5;
                case WEST -> x = 16.5;
                case EAST -> x = -15.5;
            }
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            renderStar(tick, poseStack, buffer);
            renderOrbitObjects(tick, poseStack, buffer, x, y, z);
            renderOuterSpaceShell(poseStack, buffer);
            poseStack.popPose();
        }
    }
}
