package com.gtladd.gtladditions.utils;

import org.gtlcore.gtlcore.client.ClientUtil;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import com.gtladd.gtladditions.api.machine.data.ParallelData;
import com.gtladd.gtladditions.api.machine.data.RecipeData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ftb.mods.ftbchunks.client.FTBChunksRenderTypes;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class CommonUtils {

    // ===================================================
    // Recipe Calculation
    // ===================================================

    public static @Nullable ParallelData getParallelData(int length, long remaining, long[] parallels, ObjectArrayFIFOQueue<RecipeData> queue, ObjectArrayList<GTRecipe> recipeList) {
        if (recipeList.isEmpty()) return null;

        var remainingWants = new long[length];
        var activeIndices = new IntArrayList(queue.size());
        while (!queue.isEmpty()) {
            var data = queue.dequeue();
            remainingWants[data.index()] = data.remainingWant();
            activeIndices.add(data.index());
        }

        while (remaining > 0 && !activeIndices.isEmpty()) {
            long perRecipe = remaining / activeIndices.size();
            if (perRecipe == 0) break;

            long distributed = 0;
            for (var it = activeIndices.iterator(); it.hasNext();) {
                int idx = it.nextInt();
                long give = Math.min(remainingWants[idx], perRecipe);
                parallels[idx] += give;
                distributed += give;
                remainingWants[idx] -= give;
                if (remainingWants[idx] == 0) {
                    it.remove();
                }
            }
            remaining -= distributed;
        }

        return new ParallelData(recipeList, parallels);
    }

    public static GTRecipe copyFixRecipe(GTRecipe origin, @NotNull ContentModifier modifier, int fixMultiplier) {
        return new GTRecipe(origin.recipeType, origin.id,
                copyFixContents(origin.inputs, modifier, fixMultiplier), copyFixContents(origin.outputs, modifier, fixMultiplier),
                copyFixContents(origin.tickInputs, modifier, fixMultiplier), copyFixContents(origin.tickOutputs, modifier, fixMultiplier),
                new Reference2ReferenceArrayMap<>(origin.inputChanceLogics), new Reference2ReferenceArrayMap<>(origin.outputChanceLogics),
                new Reference2ReferenceArrayMap<>(origin.tickInputChanceLogics), new Reference2ReferenceArrayMap<>(origin.tickOutputChanceLogics),
                new ObjectArrayList<>(origin.conditions),
                new ObjectArrayList<>(origin.ingredientActions), origin.data, origin.duration, origin.isFuel);
    }

    public static Map<RecipeCapability<?>, List<Content>> copyFixContents(Map<RecipeCapability<?>, List<Content>> contents,
                                                                          @NotNull ContentModifier modifier, int fixMultiplier) {
        Map<RecipeCapability<?>, List<Content>> copyContents = new Reference2ReferenceArrayMap<>();
        for (var entry : contents.entrySet()) {
            var contentList = entry.getValue();
            var cap = entry.getKey();
            if (contentList != null && !contentList.isEmpty()) {
                List<Content> contentsCopy = new ObjectArrayList<>();
                for (Content content : contentList) {
                    contentsCopy.add(copyFixBoost(content, cap, modifier, fixMultiplier));
                }
                copyContents.put(entry.getKey(), contentsCopy);
            }
        }
        return copyContents;
    }

    public static Content copyFixBoost(Content content, RecipeCapability<?> capability, @NotNull ContentModifier modifier, int fixMultiplier) {
        final var result = content.chance != 0 ? new Content(capability.copyContent(content.content, modifier), content.chance, content.maxChance, content.tierChanceBoost, content.slotName, content.uiName) : new Content(capability.copyContent(content.content), content.chance, content.maxChance, content.tierChanceBoost, content.slotName, content.uiName);
        result.tierChanceBoost /= fixMultiplier;
        return result;
    }

    // ===================================================
    // Format
    // ===================================================

    private static final String[] EXTENDED_UNITS = new String[] {
            "",    // 10^0
            "K",   // 10^3 - Kilo
            "M",   // 10^6 - Mega
            "G",   // 10^9 - Giga
            "T",   // 10^12 - Tera
            "P",   // 10^15 - Peta
            "E",   // 10^18 - Exa
            "Z",   // 10^21 - Zetta
            "Y",   // 10^24 - Yotta
            "R",   // 10^27 - Ronna
            "Q",   // 10^30 - Quetta
            // Beyond standard SI prefixes, use scientific notation style
            "e33", "e36", "e39", "e42", "e45", "e48", "e51", "e54", "e57", "e60",
            "e63", "e66", "e69", "e72", "e75", "e78", "e81", "e84", "e87", "e90",
            "e93", "e96", "e99", "e102", "e105", "e108", "e111", "e114", "e117", "e120",
            "e123", "e126", "e129", "e132", "e135", "e138", "e141", "e144", "e147", "e150",
            "e153", "e156", "e159", "e162", "e165", "e168", "e171", "e174", "e177", "e180",
            "e183", "e186", "e189", "e192", "e195", "e198", "e201", "e204", "e207", "e210",
            "e213", "e216", "e219", "e222", "e225", "e228", "e231", "e234", "e237", "e240",
            "e243", "e246", "e249", "e252", "e255", "e258", "e261", "e264", "e267", "e270",
            "e273", "e276", "e279", "e282", "e285", "e288", "e291", "e294", "e297", "e300",
            "e303", "e306", "e309"  // Covers up to 10^309+
    };

    private static final DecimalFormat DECIMAL2_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final double LOG_1000 = Math.log10(1000.0); // 3.0

    public static String format2Double(double number) {
        // Clamp unit index to valid range [0, EXTENDED_UNITS.length - 1]
        // Handles edge cases: number < 1, Infinity, NaN
        int unitIndex = Math.min(EXTENDED_UNITS.length - 1,
                Math.max(0, (int) (Math.log10(number) / LOG_1000)));

        double scaledValue = number / Math.pow(1000.0, unitIndex);

        return DECIMAL2_FORMAT.format(scaledValue) + EXTENDED_UNITS[unitIndex];
    }

    public static String formatDouble(double number) {
        int unitIndex = Math.min(EXTENDED_UNITS.length - 1,
                Math.max(0, (int) (Math.log10(number) / LOG_1000)));

        double scaledValue = number / Math.pow(1000.0, unitIndex);

        return DECIMAL_FORMAT.format(scaledValue) + EXTENDED_UNITS[unitIndex];
    }

    // ===================================================
    // Renderer
    // ===================================================

    /**
     * Generates random rotation parameters with uniformly distributed axis and speed
     *
     * @param random   Random source for generating rotation parameters
     * @param minSpeed Minimum rotation speed in degrees per tick
     * @param maxSpeed Maximum rotation speed in degrees per tick
     * @return Rotation parameters containing axis, speed, and offset
     */
    @OnlyIn(Dist.CLIENT)
    public static RotationParams createRandomRotation(RandomSource random, float minSpeed, float maxSpeed) {
        float theta = random.nextFloat() * 2.0F * (float) Math.PI;
        float phi = (float) Math.acos(2.0F * random.nextFloat() - 1.0F);

        Vector3f rotationAxis = new Vector3f(
                (float) (Math.sin(phi) * Math.cos(theta)),
                (float) (Math.sin(phi) * Math.sin(theta)),
                (float) Math.cos(phi));

        // Use square root distribution to concentrate speeds in the middle range, avoiding extreme fast rotation
        float speedFactor = (float) Math.sqrt(random.nextFloat());
        float rotationSpeed = minSpeed + speedFactor * (maxSpeed - minSpeed);
        float rotationOffset = random.nextFloat() * 360F;
        return new RotationParams(rotationAxis, rotationSpeed, rotationOffset);
    }

    /**
     * Renders a star layer with color tinting and rotation
     *
     * @param poseStack     Pose stack for transformations
     * @param buffer        Multi-buffer source for rendering
     * @param modelLocation Resource location of the model to render
     * @param size          Scale size of the star layer
     * @param rotationAxis  Rotation axis vector
     * @param angle         Rotation angle in degrees
     * @param argb32        ARGB32 color value for tinting
     * @param type          Render type to use
     */
    @OnlyIn(Dist.CLIENT)
    public static void renderStarLayer(PoseStack poseStack, MultiBufferSource buffer,
                                       ResourceLocation modelLocation, float size,
                                       Vector3f rotationAxis, float angle, int argb32, RenderType type) {
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(
                rotationAxis.x, rotationAxis.y, rotationAxis.z, angle));

        ClientUtil.modelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(type),
                null,
                ClientUtil.getBakedModel(modelLocation),
                FastColor.ARGB32.red(argb32) / 255F,
                FastColor.ARGB32.green(argb32) / 255F,
                FastColor.ARGB32.blue(argb32) / 255F,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                type);
        poseStack.popPose();
    }

    /**
     * Renders a halo layer with glow effect using eyes render type
     *
     * @param poseStack     Pose stack for transformations
     * @param buffer        Multi-buffer source for rendering
     * @param size          Scale size of the halo layer
     * @param rotationAxis  Rotation axis vector
     * @param angle         Rotation angle in degrees
     * @param haloTexture   Texture resource location for the halo glow effect
     * @param modelLocation Resource location of the model to render
     */
    @OnlyIn(Dist.CLIENT)
    public static void renderHaloLayer(PoseStack poseStack, MultiBufferSource buffer, float size,
                                       Vector3f rotationAxis, float angle,
                                       ResourceLocation haloTexture, ResourceLocation modelLocation) {
        poseStack.pushPose();
        poseStack.scale(size, size, size);
        poseStack.mulPose(new Quaternionf().fromAxisAngleDeg(
                rotationAxis.x, rotationAxis.y, rotationAxis.z, angle));

        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(haloTexture));

        ClientUtil.modelRenderer().renderModel(
                poseStack.last(),
                consumer,
                null,
                ClientUtil.getBakedModel(modelLocation),
                1.0F, 1.0F, 1.0F,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                RenderType.eyes(haloTexture));
        poseStack.popPose();
    }

    /**
     * Draws a beacon from the machine position to the star center
     * with fade effect near the star, always facing the camera
     *
     * @param poseStack   Pose stack for transformations
     * @param buffer      Multi-buffer source for rendering
     * @param starX       X coordinate of the star center (block-relative)
     * @param starY       Y coordinate of the star center (block-relative)
     * @param starZ       Z coordinate of the star center (block-relative)
     * @param argb32      ARGB32 color value for the beacon
     * @param tick        Current tick count with partial ticks for animation
     * @param blockEntity Block entity to get the machine position from
     * @param outerRadius Outer radius of the star, used to calculate beam width multiplier
     */
    @OnlyIn(Dist.CLIENT)
    public static void drawBeaconToStar(PoseStack poseStack, MultiBufferSource buffer,
                                        double starX, double starY, double starZ,
                                        int argb32, float tick, BlockEntity blockEntity, float outerRadius) {
        VertexConsumer vertexConsumer = buffer.getBuffer(FTBChunksRenderTypes.WAYPOINTS_DEPTH);

        // Extract RGB from ARGB
        int r = (argb32 >> 16) & 0xFF;
        int g = (argb32 >> 8) & 0xFF;
        int b = argb32 & 0xFF;

        // Get camera position and convert to block-relative coordinates
        Vec3 cameraWorldPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 blockWorldPos = Vec3.atLowerCornerOf(blockEntity.getBlockPos());
        Vec3 playerPos = cameraWorldPos.subtract(blockWorldPos); // Convert to block-relative coordinates

        // Beacon parameters
        float beaconWidth = 2.9F;
        float fadeRatio = 0.1F;
        float expandRatio = 0.4F;
        float endWidthMultiplier = outerRadius / beaconWidth * 144;

        // Calculate alpha with pulsing effect
        int baseAlpha = 150;
        float pulse = (float) (Math.sin(tick * 0.05) * 0.2 + 0.8);
        int alpha = (int) (baseAlpha * pulse);
        alpha = Math.max(0, Math.min(255, alpha));

        // Define start and end points (both in block-relative coordinates)
        Vec3 from = new Vec3(0.5, 0.5, 0.5); // Block center
        Vec3 to = new Vec3(starX, starY, starZ); // Star position

        // Render the beacon
        drawBeaconBetweenPoints(
                poseStack,
                from,
                to,
                playerPos,
                vertexConsumer,
                r, g, b, alpha,
                beaconWidth,
                fadeRatio,
                expandRatio,
                endWidthMultiplier);
    }

    /**
     * Renders a beacon beam between two arbitrary 3D points, facing the camera.
     * The beam consists of a solid section and a fading section for smooth visual effect.
     *
     * @param poseStack          The pose stack for transformations (must already be in camera-relative space)
     * @param from               Starting point of the beacon beam (world coordinates)
     * @param to                 Ending point of the beacon beam (world coordinates)
     * @param playerPos          Camera position (world coordinates, used for billboard calculation)
     * @param buffer             Vertex consumer for rendering
     * @param r                  Red color component (0-255)
     * @param g                  Green color component (0-255)
     * @param b                  Blue color component (0-255)
     * @param alpha              Alpha value at the solid end (0-255)
     * @param width              Width of the beacon beam
     * @param fadeRatio          Ratio of the beam length that fades out (0.0-1.0, e.g., 0.3 = 30% fade)
     * @param expandRatio        Ratio of the beam length where expansion starts (0.0-1.0, e.g., 0.4 = last 40% expands)
     * @param endWidthMultiplier Multiplier for the beam width at the end point
     */
    @OnlyIn(Dist.CLIENT)
    public static void drawBeaconBetweenPoints(
                                               PoseStack poseStack,
                                               Vec3 from,
                                               Vec3 to,
                                               Vec3 playerPos,
                                               VertexConsumer buffer,
                                               int r, int g, int b, int alpha,
                                               float width,
                                               float fadeRatio,
                                               float expandRatio,
                                               float endWidthMultiplier) {
        Vec3 beamDirection = to.subtract(from);
        double beamLength = beamDirection.length();
        if (beamLength < 0.01) return;

        Vec3 normalizedBeamDir = beamDirection.normalize();

        Vec3 fromToPlayer = playerPos.subtract(from);

        double projectionLength = fromToPlayer.dot(normalizedBeamDir);

        projectionLength = Math.max(0, Math.min(beamLength, projectionLength));

        Vec3 perpFootPoint = from.add(normalizedBeamDir.scale(projectionLength));

        Vec3 perpToPlayer = playerPos.subtract(perpFootPoint);
        double perpLength = perpToPlayer.length();

        Vec3 billboardNormal;
        if (perpLength < 0.001) {
            Vec3 arbitrary = Math.abs(normalizedBeamDir.y) > 0.9 ?
                    new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            billboardNormal = normalizedBeamDir.cross(arbitrary).normalize();
        } else {
            billboardNormal = perpToPlayer.normalize();
        }

        Vec3 widthDirection = normalizedBeamDir.cross(billboardNormal).normalize();

        double fadeStartDist = beamLength * (1.0 - fadeRatio);
        double expandStartDist = beamLength * (1.0 - expandRatio);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        float baseWidthHalf = width * 0.5f;
        float endWidthHalf = width * endWidthMultiplier * 0.5f;

        final int segments = 32;
        for (int i = 0; i < segments; i++) {
            double segStart = i * beamLength / segments;
            double segEnd = (i + 1) * beamLength / segments;

            Vec3 segStartPos = from.add(normalizedBeamDir.scale(segStart));
            Vec3 segEndPos = from.add(normalizedBeamDir.scale(segEnd));

            float segStartWidth = calculateSegmentWidth(segStart, expandStartDist, beamLength,
                    baseWidthHalf, endWidthHalf, expandRatio);
            float segEndWidth = calculateSegmentWidth(segEnd, expandStartDist, beamLength,
                    baseWidthHalf, endWidthHalf, expandRatio);

            int segStartAlpha = calculateSegmentAlpha(segStart, fadeStartDist, beamLength,
                    alpha, fadeRatio);
            int segEndAlpha = calculateSegmentAlpha(segEnd, fadeStartDist, beamLength,
                    alpha, fadeRatio);

            float v1 = (float) (segStart / beamLength);
            float v2 = (float) (segEnd / beamLength);

            Vec3 p1 = segStartPos.subtract(widthDirection.scale(segStartWidth));
            Vec3 p2 = segStartPos.add(widthDirection.scale(segStartWidth));
            Vec3 p3 = segEndPos.add(widthDirection.scale(segEndWidth));
            Vec3 p4 = segEndPos.subtract(widthDirection.scale(segEndWidth));

            renderQuadSimple(buffer, matrix, p1, p2, p3, p4,
                    r, g, b, segStartAlpha, segEndAlpha,
                    0.0f, v1, 1.0f, v2);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static float calculateSegmentWidth(double position, double expandStart, double beamLength,
                                               float baseWidth, float endWidth, float expandRatio) {
        if (position <= expandStart) {
            return baseWidth;
        }

        double expandLength = beamLength * expandRatio;
        double progress = (position - expandStart) / expandLength;
        progress = Math.min(1.0, progress);

        float t = (float) (progress * progress * (3.0 - 2.0 * progress));
        return baseWidth + (endWidth - baseWidth) * t;
    }

    @OnlyIn(Dist.CLIENT)
    private static int calculateSegmentAlpha(double position, double fadeStart, double beamLength,
                                             int maxAlpha, float fadeRatio) {
        if (position <= fadeStart) {
            return maxAlpha;
        }

        double fadeLength = beamLength * fadeRatio;
        double progress = (position - fadeStart) / fadeLength;
        progress = Math.min(1.0, progress);

        return (int) (maxAlpha * (1.0 - progress));
    }

    @SuppressWarnings({ "DuplicatedCode", "SameParameterValue" })
    @OnlyIn(Dist.CLIENT)
    private static void renderQuadSimple(
                                         VertexConsumer buffer, org.joml.Matrix4f matrix,
                                         Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                                         int r, int g, int b, int alphaStart, int alphaEnd,
                                         float u1, float v1, float u2, float v2) {
        buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                .color(r, g, b, alphaStart)
                .uv(u1, v1)
                .endVertex();

        buffer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                .color(r, g, b, alphaStart)
                .uv(u2, v1)
                .endVertex();

        buffer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                .color(r, g, b, alphaEnd)
                .uv(u2, v2)
                .endVertex();

        buffer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z)
                .color(r, g, b, alphaEnd)
                .uv(u1, v2)
                .endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    public record RotationParams(Vector3f axis, float speed, float offset) {

        public float getAngle(float tick) {
            return (offset + tick * speed) % 360F;
        }
    }
}
