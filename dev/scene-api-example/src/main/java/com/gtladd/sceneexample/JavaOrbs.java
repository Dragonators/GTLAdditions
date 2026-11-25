package com.gtladd.sceneexample;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import com.gtladd.gtladditions.api.client.scene.CompositionPoint;
import com.gtladd.gtladditions.api.client.scene.PartBlend;
import com.gtladd.gtladditions.api.client.scene.PartBounds;
import com.gtladd.gtladditions.api.client.scene.PartFeature;
import com.gtladd.gtladditions.api.client.scene.PartKey;
import com.gtladd.gtladditions.api.client.scene.PartOutcome;
import com.gtladd.gtladditions.api.client.scene.PartRenderer;
import com.gtladd.gtladditions.api.client.scene.ProviderHandle;
import com.gtladd.gtladditions.api.client.scene.SceneFrame;
import com.gtladd.gtladditions.api.client.scene.ScenePart;
import com.gtladd.gtladditions.api.client.scene.SceneProvider;
import com.gtladd.gtladditions.api.client.scene.SceneRendering;
import com.gtladd.sceneexample.Placements.Placement;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * The Java provider, push style: each frame it submits one translucent part per orb from a stage event, as a block
 * entity renderer would from its render method. The parts sample the scene depth to fade where they meet the world.
 */
final class JavaOrbs {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PART_ORB = 0;
    private static ProviderHandle provider;
    private static ShaderInstance shader;
    private static int resourceGeneration = -1;
    private static int pipelineGeneration = -1;
    private static int levelChanges;
    private static CompositionPoint composition;

    /** One call per run of adjacent orbs: they share the batch key. */
    private static final PartRenderer<Placement> RENDERER = (context, parts) -> {
        ShaderInstance shader = JavaOrbs.shader;
        if (shader == null) {
            for (ScenePart<Placement> part : parts) context.report(part, PartOutcome.RESOURCE_UNAVAILABLE);
            return;
        }

        SceneFrame frame = context.getFrame();
        PoseStack pose = context.getPoseStack();
        // Non-null here: the parts declare SCENE_DEPTH and are translucent.
        java.util.Objects.requireNonNull(context.getSceneDepth()).bind(shader);
        shader.safeGetUniform("Phase").set((float) (frame.getRenderTicks() * 0.1));
        VertexBuffer mesh = Meshes.sphere();
        mesh.bind();
        try {
            for (ScenePart<Placement> part : parts) {
                Placement orb = part.getPayload();
                float radius = (float) orb.radius();
                pose.pushPose();
                try {
                    pose.translate(orb.x() - frame.getCameraX(), orb.y() - frame.getCameraY(), orb.z() - frame.getCameraZ());
                    pose.scale(radius, radius, radius);
                    mesh.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            VertexBuffer.unbind();
        }
    };

    /** This program imports GTL Additions' scene_depth.glsl, so it is registered only through {@link SceneBridge}. */
    static void registerShaders(RegisterShadersEvent event) {
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(SceneExample.MOD_ID, "orb_scene"),
                    DefaultVertexFormat.POSITION), loaded -> shader = loaded);
        } catch (IOException error) {
            LOGGER.error("Unable to load the scene orb shader", error);
        }
    }

    /** Once, on the render thread. The notifications also reach a provider that pushes its parts. */
    static void register() {
        provider = SceneRendering.register(new ResourceLocation(SceneExample.MOD_ID, "orb"), new SceneProvider() {

            @Override
            public void onResourcesReloaded(int generation) {
                resourceGeneration = generation;
            }

            @Override
            public void onPipelineChanged(int generation) {
                pipelineGeneration = generation;
            }

            @Override
            public void onLevelChanged() {
                levelChanges++;
                Placements.clear();
            }
        });
    }

    /** AFTER_ENTITIES lies inside the submission window; frame() is null during a shaderpack's shadow pass. */
    static void submit(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !ExampleClient.sceneOrbs) return;
        SceneFrame frame = SceneRendering.frame();
        if (frame == null) return;
        composition = frame.getComposition();

        for (Placement orb : Placements.ORBS) {
            frame.submit(ScenePart.builder(key(orb), RENDERER, orb)
                    .blend(PartBlend.PREMULTIPLIED)
                    .bounds(PartBounds.sphere(orb.x(), orb.y(), orb.z(), orb.radius()))
                    .features(PartFeature.SCENE_DEPTH)
                    .batchKey(RENDERER)
                    .build());
        }
    }

    static String status() {
        StringBuilder text = new StringBuilder("Java orbs: resources ").append(resourceGeneration).append(", pipeline ").append(pipelineGeneration)
                .append(", level changes ").append(levelChanges).append(", composition ").append(composition).append(", shader ")
                .append(shader == null ? "none" : Integer.toHexString(System.identityHashCode(shader)));
        // The outcome of the last frame that ended.
        for (Placement orb : Placements.ORBS) text.append("\n  orb ").append(orb.id()).append(": ").append(SceneRendering.lastOutcome(key(orb)));
        return text.toString();
    }

    private static PartKey key(Placement orb) {
        return new PartKey(provider, orb.id(), PART_ORB);
    }
}
