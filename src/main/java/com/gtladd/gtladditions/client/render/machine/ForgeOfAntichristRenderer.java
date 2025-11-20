package com.gtladd.gtladditions.client.render.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist;
import com.gtladd.gtladditions.common.record.RotationParams;
import com.gtladd.gtladditions.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tterrag.registrate.util.entry.BlockEntry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ForgeOfAntichristRenderer extends PartWorkableCasingMachineRenderer {

    private static final ResourceLocation STAR_LAYER_0 = GTLAdditions.id("obj/star_layer_0");
    private static final ResourceLocation STAR_LAYER_2 = GTLAdditions.id("obj/star_layer_2");
    private static final ResourceLocation HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex2.png");

    @OnlyIn(Dist.CLIENT)
    private static final ConcurrentHashMap<Long, RenderCache> CACHE_MAP = new ConcurrentHashMap<>();

    public ForgeOfAntichristRenderer(ResourceLocation baseCasing, ResourceLocation workableModel, BlockEntry<Block> partEntry, ResourceLocation partCasing) {
        super(baseCasing, workableModel, partEntry, partCasing);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (blockEntity instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof ForgeOfTheAntichrist machine && machine.isActive()) {

            final float tick = machine.getOffsetTimer() + partialTicks;
            final long seed = blockEntity.getBlockPos().asLong();
            final Vec3 starPos = RenderUtils.getRotatedRenderPosition(Direction.EAST, machine.getFrontFacing(), -121.5, 0.0, 0.0);
            final double x = starPos.x;
            final double y = starPos.y;
            final double z = starPos.z;

            final int argb32 = machine.getRGBFromTime();
            final float baseRadius = 0.175F * machine.getRadiusMultiplier();
            final float middleRadius = baseRadius + Math.min(0.0055F, baseRadius * 0.02F);
            final float outerRadius = middleRadius * 1.02F;

            RenderUtils.drawBeaconToStar(poseStack, buffer, x, y, z, argb32, tick, blockEntity, outerRadius);

            renderMultiLayerStar(tick, poseStack, buffer, baseRadius, middleRadius, outerRadius, getOrCreateCache(seed), argb32, x, y, z);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderMultiLayerStar(float tick, PoseStack poseStack, MultiBufferSource buffer,
                                             float baseRadius, float middleRadius, float outerRadius,
                                             RenderCache cache, int argb32, double x, double y, double z) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);

        var rotation0 = cache.rotation0;
        var rotation1 = cache.rotation1;
        var rotation2 = cache.rotation2;

        RenderUtils.renderStarLayer(
                poseStack, buffer, STAR_LAYER_2, middleRadius,
                rotation2.axis, rotation1.getAngle(tick),
                argb32, RenderType.translucent());

        RenderUtils.renderStarLayer(
                poseStack, buffer, STAR_LAYER_0, baseRadius,
                rotation1.axis, rotation0.getAngle(tick),
                argb32, RenderType.solid());

        RenderUtils.renderHaloLayer(
                poseStack, buffer, outerRadius,
                rotation0.axis, rotation0.getAngle(tick),
                HALO_TEX, STAR_LAYER_2,
                1.0F,
                true);

        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static RenderCache getOrCreateCache(long seed) {
        return CACHE_MAP.computeIfAbsent(seed, RenderCache::new);
    }

    @Override
    public void onAdditionalModel(Consumer<ResourceLocation> registry) {
        super.onAdditionalModel(registry);
        registry.accept(STAR_LAYER_0);
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
        return 384;
    }

    @OnlyIn(Dist.CLIENT)
    private static class RenderCache {

        final RotationParams rotation0;
        final RotationParams rotation1;
        final RotationParams rotation2;
        final long seed;

        RenderCache(long seed) {
            this.seed = seed;
            RandomSource random = RandomSource.create(seed);

            this.rotation0 = RenderUtils.createRandomRotation(random, 2.0F, 3.0F);
            this.rotation1 = RenderUtils.createRandomRotation(random, 0.9F, 1.5F);
            this.rotation2 = RenderUtils.createRandomRotation(random, 0.9F, 1.5F);
        }
    }
}
