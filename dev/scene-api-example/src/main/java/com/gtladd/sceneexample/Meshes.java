package com.gtladd.sceneexample;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.function.Consumer;

/**
 * Unit meshes centred on the origin, with counter-clockwise outer faces. Built on the render thread at first use and
 * kept for the client's lifetime.
 */
final class Meshes {

    private static final int STACKS = 16;
    private static final int SLICES = 32;
    private static VertexBuffer sphere;
    private static VertexBuffer cube;

    /** Radius 1. */
    static VertexBuffer sphere() {
        if (sphere == null) sphere = upload(Meshes::writeSphere);
        return sphere;
    }

    /** Edge 1. */
    static VertexBuffer cube() {
        if (cube == null) cube = upload(Meshes::writeCube);
        return cube;
    }

    private static VertexBuffer upload(Consumer<BufferBuilder> write) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        write.accept(builder);
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(builder.end());
        VertexBuffer.unbind();

        return buffer;
    }

    private static void writeSphere(BufferBuilder builder) {
        for (int stack = 0; stack < STACKS; stack++) {
            double upper = Math.PI * stack / STACKS, lower = Math.PI * (stack + 1) / STACKS;

            for (int slice = 0; slice < SLICES; slice++) {
                double from = 2 * Math.PI * slice / SLICES, to = 2 * Math.PI * (slice + 1) / SLICES;
                point(builder, upper, from);
                point(builder, upper, to);
                point(builder, lower, to);
                point(builder, upper, from);
                point(builder, lower, to);
                point(builder, lower, from);
            }
        }
    }

    private static void point(BufferBuilder builder, double polar, double azimuth) {
        builder.vertex(Math.sin(polar) * Math.cos(azimuth), Math.cos(polar), Math.sin(polar) * Math.sin(azimuth)).endVertex();
    }

    private static void writeCube(BufferBuilder builder) {
        float h = 0.5f;
        face(builder, h, -h, h, h, -h, -h, h, h, -h, h, h, h);
        face(builder, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h);
        face(builder, -h, h, h, h, h, h, h, h, -h, -h, h, -h);
        face(builder, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h);
        face(builder, -h, -h, h, h, -h, h, h, h, h, -h, h, h);
        face(builder, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h);
    }

    /** Corners a, b, c, d counter-clockwise as seen from outside. */
    private static void face(BufferBuilder builder, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx,
                             float dy, float dz) {
        builder.vertex(ax, ay, az).endVertex();
        builder.vertex(bx, by, bz).endVertex();
        builder.vertex(cx, cy, cz).endVertex();
        builder.vertex(ax, ay, az).endVertex();
        builder.vertex(cx, cy, cz).endVertex();
        builder.vertex(dx, dy, dz).endVertex();
    }
}