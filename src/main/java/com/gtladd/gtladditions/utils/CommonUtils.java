package com.gtladd.gtladditions.utils;

import org.gtlcore.gtlcore.client.ClientUtil;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.text.DecimalFormat;

public class CommonUtils {

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
     * 生成随机旋转参数
     *
     * @param random   随机源
     * @param minSpeed 最小旋转速度
     * @param maxSpeed 最大旋转速度
     * @return 旋转参数
     */
    public static RotationParams createRandomRotation(RandomSource random, float minSpeed, float maxSpeed) {
        float theta = random.nextFloat() * 2.0F * (float) Math.PI;
        float phi = (float) Math.acos(2.0F * random.nextFloat() - 1.0F);

        Vector3f rotationAxis = new Vector3f(
                (float) (Math.sin(phi) * Math.cos(theta)),
                (float) (Math.sin(phi) * Math.sin(theta)),
                (float) Math.cos(phi));

        // 使用平方根分布，使速度更集中在中间范围，避免极端快速旋转
        float speedFactor = (float) Math.sqrt(random.nextFloat());
        float rotationSpeed = minSpeed + speedFactor * (maxSpeed - minSpeed);
        float rotationOffset = random.nextFloat() * 360F;
        return new RotationParams(rotationAxis, rotationSpeed, rotationOffset);
    }

    /**
     * 渲染星体层（带颜色）
     *
     * @param poseStack     渲染位置栈
     * @param buffer        渲染缓冲
     * @param modelLocation 模型资源位置
     * @param size          缩放尺寸
     * @param rotationAxis  旋转轴
     * @param angle         旋转角度
     * @param argb32        ARGB32 颜色值
     */
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
     * 渲染光晕层
     *
     * @param poseStack     渲染位置栈
     * @param buffer        渲染缓冲
     * @param size          缩放尺寸
     * @param rotationAxis  旋转轴
     * @param angle         旋转角度
     * @param haloTexture   光晕纹理
     * @param modelLocation 模型资源位置
     */
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

    public record RotationParams(Vector3f axis, float speed, float offset) {

        public float getAngle(float tick) {
            return (offset + tick * speed) % 360F;
        }
    }
}
