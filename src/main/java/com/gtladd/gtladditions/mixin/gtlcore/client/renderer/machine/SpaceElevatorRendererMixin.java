package com.gtladd.gtladditions.mixin.gtlcore.client.renderer.machine;

import org.gtlcore.gtlcore.client.renderer.RenderBufferHelper;
import org.gtlcore.gtlcore.client.renderer.machine.SpaceElevatorRenderer;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gtladd.gtladditions.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpaceElevatorRenderer.class)
public abstract class SpaceElevatorRendererMixin {

    @Shadow(remap = false)
    private void renderClimber(PoseStack poseStack, MultiBufferSource buffer) {
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
                machineBlockEntity.getMetaMachine() instanceof SpaceElevatorMachine machine && machine.isFormed()) {
            float tick = RenderUtils.INSTANCE.getSmoothTick(machine, partialTicks);
            double x = 0.5, y = 1, z = 0.5;
            switch (machine.getFrontFacing()) {
                case NORTH -> z = 3.5;
                case SOUTH -> z = -2.5;
                case WEST -> x = 3.5;
                case EAST -> x = -2.5;
            }
            poseStack.pushPose();
            RenderBufferHelper.renderCylinder(poseStack, buffer.getBuffer(GTRenderTypes.getLightRing()), (float) x, (float) (y - 2), (float) z, 0.3F, 360, 10, 0, 0, 0, 255);
            poseStack.translate(x, y + 180 + (140 * Math.sin(tick / 160)), z);
            renderClimber(poseStack, buffer);
            poseStack.popPose();
        }
    }
}
