package com.gtladd.gtladditions.client.render.machine;

import org.gtlcore.gtlcore.client.ClientUtil;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tterrag.registrate.util.entry.BlockEntry;
import org.joml.*;

import java.util.function.Consumer;

public class ForgeOfAntichristRenderer extends PartWorkableCasingMachineRenderer {

    private static final ResourceLocation STAR_LAYER_0 = GTLAdditions.id("obj/star_layer_0");
    private static final ResourceLocation STAR_LAYER_1 = GTLAdditions.id("obj/star_layer_1");
    private static final ResourceLocation STAR_LAYER_2 = GTLAdditions.id("obj/star_layer_2");
    private static final ResourceLocation HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex.png");

    public ForgeOfAntichristRenderer(ResourceLocation baseCasing, ResourceLocation workableModel, BlockEntry<Block> partEntry, ResourceLocation partCasing) {
        super(baseCasing, workableModel, partEntry, partCasing);
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (blockEntity instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof ForgeOfTheAntichrist machine && machine.isActive()) {

            float tick = machine.getOffsetTimer() + partialTicks;

            double x = 0.5, y = 0.5, z = 0.5;
            switch (machine.getFrontFacing()) {
                case NORTH -> z = 122.5;
                case SOUTH -> z = -121.5;
                case WEST -> x = 122.5;
                case EAST -> x = -121.5;
            }

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            float baseRadius = 0.12F * machine.getRadiusMultiplier();
            float middleRadius = 0.16F * machine.getRadiusMultiplier();
            float outerRadius = 0.2F * machine.getRadiusMultiplier();

            long seed = blockEntity.getBlockPos().asLong();

            renderMultiLayerStar(tick, poseStack, buffer, baseRadius, middleRadius, outerRadius, seed, machine.getRGBFromTime());

            poseStack.popPose();
        }
    }

    private static void renderStarLayer(PoseStack poseStack, MultiBufferSource buffer,
                                        ResourceLocation modelLocation, float size,
                                        Vector3f rotationAxis, float angle, int argb32) {
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(
                rotationAxis.x, rotationAxis.y, rotationAxis.z, angle));

        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());

        ClientUtil.modelRenderer().renderModel(
                poseStack.last(),
                consumer,
                null,
                ClientUtil.getBakedModel(modelLocation),
                FastColor.ARGB32.red(argb32) / 255F, FastColor.ARGB32.green(argb32) / 255F, FastColor.ARGB32.blue(argb32) / 255F,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                RenderType.translucent());
        poseStack.popPose();
    }

    private static void renderHaloLayer(PoseStack poseStack, MultiBufferSource buffer, float size,
                                        Vector3f rotationAxis, float angle) {
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(
                rotationAxis.x, rotationAxis.y, rotationAxis.z, angle));

        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(HALO_TEX));

        ClientUtil.modelRenderer().renderModel(
                poseStack.last(),
                consumer,
                null,
                ClientUtil.getBakedModel(ForgeOfAntichristRenderer.STAR_LAYER_2),
                1.0F, 1.0F, 1.0F,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                RenderType.eyes(HALO_TEX));
        poseStack.popPose();
    }

    private static void renderMultiLayerStar(float tick, PoseStack poseStack, MultiBufferSource buffer,
                                             float baseRadius, float middleRadius, float outerRadius,
                                             long randomSeed, int argb32) {
        RandomSource random = RandomSource.create(randomSeed);

        Vector3f rotationAxis0 = new Vector3f(
                random.nextFloat() - 0.5F,
                random.nextFloat() - 0.5F,
                random.nextFloat() - 0.5F).normalize();
        float rotationSpeed0 = 0.5F + random.nextFloat() * 1.5F;
        float rotationOffset0 = random.nextFloat() * 360F;

        Vector3f rotationAxis1 = new Vector3f(
                random.nextFloat() - 0.5F,
                random.nextFloat() - 0.5F,
                random.nextFloat() - 0.5F).normalize();
        float rotationSpeed1 = 0.3F + random.nextFloat() * 1.2F;
        float rotationOffset1 = random.nextFloat() * 360F;

        Vector3f rotationAxis2 = new Vector3f(
                random.nextFloat() - 0.5F,
                random.nextFloat() - 0.5F,
                random.nextFloat() - 0.5F).normalize();
        float rotationSpeed2 = 0.4F + random.nextFloat();
        float rotationOffset2 = random.nextFloat() * 360F;

        renderStarLayer(
                poseStack, buffer, STAR_LAYER_2, outerRadius,
                rotationAxis2, (rotationOffset2 + tick * rotationSpeed2) % 360F,
                argb32);

        renderStarLayer(
                poseStack, buffer, STAR_LAYER_1, middleRadius,
                rotationAxis1, (rotationOffset1 + tick * rotationSpeed1) % 360F,
                argb32);

        renderStarLayer(
                poseStack, buffer, STAR_LAYER_0, baseRadius,
                rotationAxis0, (rotationOffset0 + tick * rotationSpeed0) % 360F,
                argb32);

        renderHaloLayer(
                poseStack, buffer, outerRadius * 1.02F,
                rotationAxis0, (rotationOffset0 + tick * rotationSpeed0) % 360F);
    }

    @Override
    public void onAdditionalModel(Consumer<ResourceLocation> registry) {
        super.onAdditionalModel(registry);
        registry.accept(STAR_LAYER_0);
        registry.accept(STAR_LAYER_1);
        registry.accept(STAR_LAYER_2);
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
        return 256;
    }
}
