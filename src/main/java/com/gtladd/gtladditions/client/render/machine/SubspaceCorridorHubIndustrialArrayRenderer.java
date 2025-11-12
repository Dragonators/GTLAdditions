package com.gtladd.gtladditions.client.render.machine;

import org.gtlcore.gtlcore.GTLCore;
import org.gtlcore.gtlcore.client.ClientUtil;
import org.gtlcore.gtlcore.client.renderer.RenderBufferHelper;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.SubspaceCorridorHubIndustrialArray;
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

public class SubspaceCorridorHubIndustrialArrayRenderer extends WorkableCasingMachineRenderer {

    private static final ResourceLocation STAR_LAYER = GTLAdditions.id("obj/star_layer_1");
    private static final ResourceLocation CLIMBER_MODEL = GTLCore.id("obj/climber");
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
    private static final int[][] BEAM_CYLINDER_OFFSETS = new int[][] {
            { -24, -109, -30 },
            { -120, -109, -83 },
            { -171, -109, -53 },
            { -171, -109, 53 },
            { -121, -109, 83 },
            { -24, -109, 31 }
    };

    @OnlyIn(Dist.CLIENT)
    private static final ConcurrentHashMap<Long, RenderCache> CACHE_MAP = new ConcurrentHashMap<>();

    public SubspaceCorridorHubIndustrialArrayRenderer() {
        super(GTCEu.id("block/casings/hpca/high_power_casing"), GTCEu.id("block/multiblock/data_bank"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
                       int combinedLight, int combinedOverlay) {
        if (blockEntity instanceof IMachineBlockEntity machineBlockEntity &&
                machineBlockEntity.getMetaMachine() instanceof SubspaceCorridorHubIndustrialArray machine && machine.isActive()) {
            float tick = machine.getOffsetTimer() + partialTicks;
            long seed = blockEntity.getBlockPos().asLong();

            double x = 0.5, y = -94.5, z = 0.5;
            switch (machine.getFrontFacing()) {
                case NORTH -> z = 105.5;
                case SOUTH -> z = -104.5;
                case WEST -> x = 105.5;
                case EAST -> x = -104.5;
            }

            RenderCache cache = getOrCreateCache(seed, machine.getFrontFacing(), new Vec3(x, y, z));

            renderBeamCylinders(poseStack, buffer, machine.getFrontFacing(), tick);

            RenderUtils.drawBeaconToSky(poseStack, buffer, x, y - 36, z, FastColor.ARGB32.color(255, 255, 255, 255), tick, blockEntity, 2.9f);

            renderStar(tick, poseStack, buffer, cache, x, y, z);

            renderOrbit(poseStack, buffer, cache, tick);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderBeamCylinders(PoseStack poseStack, MultiBufferSource buffer, Direction facing, float tick) {
        for (int[] offset : BEAM_CYLINDER_OFFSETS) {
            poseStack.pushPose();

            double x, z;
            switch (facing) {
                case NORTH -> {
                    x = 0.5 - offset[2];
                    z = 0.5 - offset[0];
                }
                case SOUTH -> {
                    x = 0.5 + offset[2];
                    z = 0.5 + offset[0];
                }
                case WEST -> {
                    x = 0.5 - offset[0];
                    z = 0.5 - offset[2];
                }
                default -> {
                    x = 0.5 + offset[0];
                    z = 0.5 + offset[2];
                }
            }
            double y = 0.5 + offset[1];

            RenderBufferHelper.renderCylinder(
                    poseStack,
                    buffer.getBuffer(GTRenderTypes.getLightRing()),
                    (float) x, (float) y, (float) z,
                    0.3F,
                    400,
                    20,
                    255, 255, 255, 255);

            long positionSeed = new BlockPos(offset[0], offset[1], offset[2]).asLong();
            RandomSource random = RandomSource.create(positionSeed);

            float tickOffset = random.nextFloat() * 320.0f;
            float offsetTick = tick + tickOffset;

            double climberY = y + 180 + (140 * Math.sin(offsetTick / 160));
            poseStack.translate(x, climberY, z);

            renderClimber(poseStack, buffer);
            poseStack.popPose();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderClimber(PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        poseStack.scale(4F, 4F, 4F);
        ClientUtil.modelRenderer().renderModel(poseStack.last(), buffer.getBuffer(RenderType.solid()), null, ClientUtil.getBakedModel(CLIMBER_MODEL), 1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderStar(float tick, PoseStack poseStack, MultiBufferSource buffer, RenderCache cache,
                                   double x, double y, double z) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);

        var rotation = cache.starRotation;

        RenderUtils.renderStarLayer(poseStack, buffer, STAR_LAYER, 0.23F,
                rotation.axis, rotation.getAngle(tick),
                FastColor.ARGB32.color(255, 255, 255, 255),
                RenderType.solid());

        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderOrbit(PoseStack poseStack, MultiBufferSource buffer, RenderCache cache, float tick) {
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
        registry.accept(CLIMBER_MODEL);
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
                orbitParams.add(getCircularMotionParams(seed, i, centerPos, 27.0f, 55.0f, 0f, 30f));
                planetSizes.add(getPlanetSize(seed, i, 1.3f, 2.7f));
            }

            starRotation = RenderUtils.createRandomRotation(RandomSource.create(seed), 0.5F, 2.0F);
        }
    }
}
