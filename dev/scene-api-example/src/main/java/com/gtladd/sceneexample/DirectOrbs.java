package com.gtladd.sceneexample;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import com.gtladd.sceneexample.Placements.Placement;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * The orbs drawn by the example itself, without the scheduler: the only path without GTL Additions, and with it the
 * `mode direct` one. Its program imports nothing from GTL Additions.
 */
final class DirectOrbs {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static ShaderInstance shader;

    static void registerShaders(RegisterShadersEvent event) {
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(SceneExample.MOD_ID, "orb_direct"),
                    DefaultVertexFormat.POSITION), loaded -> shader = loaded);
        } catch (IOException error) {
            LOGGER.error("Unable to load the direct orb shader", error);
        }
    }

    /** AFTER_PARTICLES is dispatched from renderLevel only, so never during a shaderpack's shadow pass. */
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ExampleClient.sceneOrbs || Placements.ORBS.isEmpty()) return;
        ShaderInstance shader = DirectOrbs.shader;
        if (shader == null) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableCull();
        // Set here too: a ShaderInstance skips its blend state while it believes that state is still applied.
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        shader.safeGetUniform("Phase").set((event.getRenderTick() + event.getPartialTick()) * 0.1f);
        VertexBuffer mesh = Meshes.sphere();
        mesh.bind();

        for (Placement orb : Placements.ORBS) {
            float radius = (float) orb.radius();
            pose.pushPose();
            pose.translate(orb.x() - camera.x, orb.y() - camera.y, orb.z() - camera.z);
            pose.scale(radius, radius, radius);
            mesh.drawWithShader(pose.last().pose(), event.getProjectionMatrix(), shader);
            pose.popPose();
        }

        VertexBuffer.unbind();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }
}