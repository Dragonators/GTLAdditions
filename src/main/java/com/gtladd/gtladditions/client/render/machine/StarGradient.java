package com.gtladd.gtladditions.client.render.machine;

public class StarGradient {

    public static int getRGBFromTime(double ratio) {
        int orange = 0xFF8C2A;
        int blueWh = 0xBFD8FF;
        int white = 0xFFFFFF;

        int result;
        if (ratio <= 0.5) {
            double p = ratio / 0.5;
            result = lerpSRGB(orange, blueWh, p);
        } else {
            double p = (ratio - 0.5) / 0.5;
            result = lerpSRGB(blueWh, white, p);
        }

        return result;
    }

    private static int lerpSRGB(int colorA, int colorB, double t) {
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;

        double lrA = sRGBToLinear(rA / 255.0);
        double lgA = sRGBToLinear(gA / 255.0);
        double lbA = sRGBToLinear(bA / 255.0);

        double lrB = sRGBToLinear(rB / 255.0);
        double lgB = sRGBToLinear(gB / 255.0);
        double lbB = sRGBToLinear(bB / 255.0);

        double lr = lerp(lrA, lrB, t);
        double lg = lerp(lgA, lgB, t);
        double lb = lerp(lbA, lbB, t);

        int r = (int) Math.round(clamp01(linearToSRGB(lr)) * 255);
        int g = (int) Math.round(clamp01(linearToSRGB(lg)) * 255);
        int b = (int) Math.round(clamp01(linearToSRGB(lb)) * 255);

        return (r << 16) | (g << 8) | b;
    }

    private static double lerp(double x, double y, double t) {
        return x + (y - x) * t;
    }

    private static double sRGBToLinear(double c) {
        return (c <= 0.04045) ? (c / 12.92) : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double linearToSRGB(double c) {
        return (c <= 0.0031308) ? (c * 12.92) : (1.055 * Math.pow(c, 1.0 / 2.4) - 0.055);
    }

    private static double clamp01(double x) {
        return x < 0 ? 0 : (x > 1 ? 1 : x);
    }
}
