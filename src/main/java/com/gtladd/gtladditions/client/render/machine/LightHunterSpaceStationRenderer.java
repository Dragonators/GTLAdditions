package com.gtladd.gtladditions.client.render.machine;

import org.gtlcore.gtlcore.client.ClientUtil;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.LightHunterSpaceStation;
import com.gtladd.gtladditions.common.record.CircularMotionParams;
import com.gtladd.gtladditions.common.record.RotationParams;
import com.gtladd.gtladditions.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LightHunterSpaceStationRenderer extends WorkableCasingMachineRenderer {

    private static final double BEAM_OFFSET_X = -174.0;
    private static final double STAR_OFFSET_X = -206.0;
    private static final float MIN_RADIUS = 22.0f;
    private static final float MAX_RADIUS = 53.0f;
    private static final float MIN_TILT_ANGLE = 70f;
    private static final float MAX_TILT_ANGLE = 90f;
    private static final ResourceLocation STAR_LAYER = GTLAdditions.id("obj/star_layer_1");
    private static final ResourceLocation SPACE_MODEL = GTLAdditions.id("obj/heart_of_universe");
    private static final ResourceLocation HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex1.png");
    private static final List<ResourceLocation> ORBIT_OBJECTS = List.of(
            GTLAdditions.id("obj/planets/the_nether"),
            GTLAdditions.id("obj/planets/overworld"),
            GTLAdditions.id("obj/planets/the_end"),
            GTLAdditions.id("obj/planets/ceres"),
            GTLAdditions.id("obj/planets/enceladus"),
            GTLAdditions.id("obj/planets/ganymede"),
            GTLAdditions.id("obj/planets/io"),
            GTLAdditions.id("obj/planets/mars"),
            GTLAdditions.id("obj/planets/mercury"),
            GTLAdditions.id("obj/planets/moon"),
            GTLAdditions.id("obj/planets/pluto"),
            GTLAdditions.id("obj/planets/titan"),
            GTLAdditions.id("obj/planets/venus"));

    @OnlyIn(Dist.CLIENT)
    private static final ConcurrentHashMap<Long, RenderCache> CACHE_MAP = new ConcurrentHashMap<>();

    public LightHunterSpaceStationRenderer() {
        super(GTCEu.id("block/casings/hpca/high_power_casing"), GTCEu.id("block/multiblock/data_bank"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        if (blockEntity instanceof IMachineBlockEntity machineBlockEntity &&
                machineBlockEntity.getMetaMachine() instanceof LightHunterSpaceStation machine && machine.isActive()) {
            final float tick = machine.getOffsetTimer() + partialTicks;
            final long seed = blockEntity.getBlockPos().asLong();
            final Direction facing = machine.getFrontFacing();
            final Vec3 beamEnd = RenderUtils.getRotatedRenderPosition(Direction.EAST, facing, BEAM_OFFSET_X, 0.5, 0.0);
            final Vec3 starPos = RenderUtils.getRotatedRenderPosition(Direction.EAST, facing, STAR_OFFSET_X, 0.5, 0.0);

            renderBeam(poseStack, buffer, blockEntity, new Vec3(0.5, 0.5, 0.5), beamEnd, tick);
            if (machine.unlockParadoxical()) renderBlackHole(poseStack, buffer, facing, starPos, tick, seed);
            else renderStar(poseStack, buffer, facing, starPos, tick, seed);
            renderOrbit(poseStack, buffer, facing, starPos, tick, seed);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderBeam(PoseStack poseStack, MultiBufferSource buffer, BlockEntity entity, Vec3 from, Vec3 to, float tick) {
        RenderUtils.drawBeacon(poseStack, buffer, from, to,
                FastColor.ARGB32.color(140, 130, 168, 192), tick, entity,
                1.7f, 0.0f, 0.0f, 1.0f);
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderStar(PoseStack poseStack, MultiBufferSource buffer, Direction facing, Vec3 starPos, float tick, long seed) {
        RenderCache cache = getOrCreateCache(seed, facing, starPos);

        poseStack.pushPose();
        poseStack.translate(starPos.x, starPos.y, starPos.z);

        var rotation = cache.starRotation;

        RenderUtils.renderStarLayer(poseStack, buffer, STAR_LAYER, 0.20F,
                rotation.axis, rotation.getAngle(tick),
                FastColor.ARGB32.color(255, 255, 255, 255),
                RenderType.solid());

        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderBlackHole(PoseStack poseStack, MultiBufferSource buffer, Direction facing, Vec3 starPos, float tick, long seed) {
        RenderCache cache = getOrCreateCache(seed, facing, starPos);

        poseStack.pushPose();
        poseStack.translate(starPos.x, starPos.y, starPos.z);

        var rotation = cache.starRotation;

        RenderUtils.renderHaloLayer(poseStack, buffer, 0.20F * 1.02F,
                rotation.axis, rotation.getAngle(tick),
                HALO_TEX, SPACE_MODEL);

        RenderUtils.renderStarLayer(poseStack, buffer, SPACE_MODEL, 0.20F,
                rotation.axis, rotation.getAngle(tick),
                FastColor.ARGB32.color(255, 255, 255, 255),
                RenderType.solid());

        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderOrbit(PoseStack poseStack, MultiBufferSource buffer, Direction facing, Vec3 starPos, float tick, long seed) {
        RenderCache cache = getOrCreateCache(seed, facing, starPos);

        for (int i = 0; i < ORBIT_OBJECTS.size(); i++) {
            var motionParams = cache.orbitParams.get(i);
            RenderUtils.renderOrbitRing(poseStack, buffer, motionParams, 128);
        }

        var planetConsumer = buffer.getBuffer(RenderType.solid());

        for (int i = 0; i < ORBIT_OBJECTS.size(); i++) {
            var motionParams = cache.orbitParams.get(i);
            var rotationParams = cache.planetRotations.get(i);
            float planetSize = cache.planetSizes.get(i);

            Quaternionf rotation = new Quaternionf().fromAxisAngleDeg(
                    rotationParams.axis.x,
                    rotationParams.axis.y,
                    rotationParams.axis.z,
                    rotationParams.getAngle(tick));

            RenderUtils.renderCircularMotionModelDirect(
                    poseStack,
                    planetConsumer,
                    ClientUtil.getBakedModel(ORBIT_OBJECTS.get(i)),
                    motionParams,
                    tick,
                    planetSize,
                    FastColor.ARGB32.color(255, 255, 255, 255),
                    RenderType.solid(),
                    true,
                    rotation,
                    true);
        }
    }

    @SuppressWarnings("SameParameterValue")
    @OnlyIn(Dist.CLIENT)
    private static float getPlanetSize(long seed, int index, float minSize, float maxSize) {
        Random sizeRandom = new Random(seed + index * 500L);
        return minSize + sizeRandom.nextFloat() * (maxSize - minSize);
    }

    @SuppressWarnings("SameParameterValue")
    @OnlyIn(Dist.CLIENT)
    private static @NotNull CircularMotionParams getCircularMotionParams(
                                                                         long seed, int i, Vec3 centerPos, float minRadius, float maxRadius,
                                                                         float minTiltAngle, float maxTiltAngle) {
        Random objRandom = new Random(seed + i * 1000L);

        float radius = minRadius + objRandom.nextFloat() * (maxRadius - minRadius);
        float speed = 0.5f + objRandom.nextFloat();
        float angleOffset = objRandom.nextFloat() * 360f;
        float tiltAngle = minTiltAngle + objRandom.nextFloat() * (maxTiltAngle - minTiltAngle);
        float thetaTilt = objRandom.nextFloat() * 2.0f * (float) Math.PI;
        Vector3f tiltDirection = new Vector3f(
                (float) Math.cos(thetaTilt),
                0.0f,
                (float) Math.sin(thetaTilt));

        return new CircularMotionParams(
                centerPos,
                radius,
                speed,
                angleOffset,
                tiltAngle,
                tiltDirection);
    }

    @OnlyIn(Dist.CLIENT)
    private static long createCacheKey(long seed, Direction facing) {
        return (seed & 0x1FFFFFFFFFFFFFFFL) | ((long) facing.ordinal() << 61);
    }

    @OnlyIn(Dist.CLIENT)
    private static RenderCache getOrCreateCache(long seed, Direction facing, Vec3 centerPos) {
        long key = createCacheKey(seed, facing);
        return CACHE_MAP.computeIfAbsent(key, k -> new RenderCache(seed, centerPos));
    }

    @Override
    public void onAdditionalModel(Consumer<ResourceLocation> registry) {
        super.onAdditionalModel(registry);
        registry.accept(STAR_LAYER);
        registry.accept(SPACE_MODEL);
        ORBIT_OBJECTS.forEach(registry);
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

        final List<RotationParams> planetRotations = new ObjectArrayList<>(13);
        final List<CircularMotionParams> orbitParams = new ObjectArrayList<>(13);
        final List<Float> planetSizes = new ObjectArrayList<>(13);
        final RotationParams starRotation;
        final long seed;

        RenderCache(long seed, Vec3 centerPos) {
            this.seed = seed;

            for (int i = 0; i < ORBIT_OBJECTS.size(); i++) {
                planetRotations.add(RenderUtils.createRandomRotation(
                        RandomSource.create(seed + i * 2000L), 0.1F, 0.5F));
                orbitParams.add(getCircularMotionParams(seed, i, centerPos, MIN_RADIUS, MAX_RADIUS, MIN_TILT_ANGLE, MAX_TILT_ANGLE));
                planetSizes.add(getPlanetSize(seed, i, 1.3f, 2.7f));
            }

            starRotation = RenderUtils.createRandomRotation(RandomSource.create(seed), 0.5F, 2.0F);
        }
    }
}
