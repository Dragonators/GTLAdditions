package com.gtladd.gtladditions.client.render.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.utils.CommonUtils;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.function.Consumer;

public class HeartOfTheUniverseRenderer extends WorkableCasingMachineRenderer {

    private static final ResourceLocation SPACE_MODEL = GTLAdditions.id("obj/heart_of_universe");
    private static final ResourceLocation HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex.png");

    public HeartOfTheUniverseRenderer() {
        super(GTCEu.id("block/casings/hpca/high_power_casing"), GTCEu.id("block/multiblock/cosmos_simulation"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        if (blockEntity instanceof IMachineBlockEntity machineBlockEntity &&
                machineBlockEntity.getMetaMachine() instanceof WorkableElectricMultiblockMachine machine && machine.isActive()) {

            float tick = machine.getOffsetTimer() + partialTicks;

            double x = 0.5, y = 36.5, z = 0.5;
            switch (machine.getFrontFacing()) {
                case NORTH -> z = 39.5;
                case SOUTH -> z = -38.5;
                case WEST -> x = 39.5;
                case EAST -> x = -38.5;
            }

            long seed = blockEntity.getBlockPos().asLong();

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            renderStar(tick, poseStack, buffer, seed);

            poseStack.popPose();
        }
    }

    private static void renderStar(float tick, PoseStack poseStack, MultiBufferSource buffer, long randomSeed) {
        var rotation = CommonUtils.createRandomRotation(RandomSource.create(randomSeed), 0.5F, 2.0F);

        CommonUtils.renderStarLayer(poseStack, buffer, SPACE_MODEL, 0.45F,
                rotation.axis(), rotation.getAngle(tick),
                FastColor.ARGB32.color(255, 255, 255, 255),
                RenderType.solid());

        CommonUtils.renderHaloLayer(poseStack, buffer, 0.45F * 1.02F,
                rotation.axis(), rotation.getAngle(tick),
                HALO_TEX, SPACE_MODEL);
    }

    @Override
    public void onAdditionalModel(Consumer<ResourceLocation> registry) {
        super.onAdditionalModel(registry);
        registry.accept(SPACE_MODEL);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasTESR(BlockEntity blockEntity) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGlobalRenderer(BlockEntity blockEntity) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getViewDistance() {
        return 512;
    }
}
