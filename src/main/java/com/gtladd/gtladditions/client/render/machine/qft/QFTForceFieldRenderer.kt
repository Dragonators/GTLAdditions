package com.gtladd.gtladditions.client.render.machine.qft

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.config.ConfigHolder
import org.gtlcore.gtlcore.utils.RenderUtil
import org.lwjgl.opengl.GL11C

@OnlyIn(Dist.CLIENT)
internal object QFTForceFieldRenderer {
    private val texture = GTLAdditions.id("textures/effect/qft/force_field.png")
    private val builder = BufferBuilder(4096)
    private val vertexBuffer by lazy { VertexBuffer(VertexBuffer.Usage.DYNAMIC) }
    var shader: ShaderInstance? = null

    fun enqueue(blockEntity: BlockEntity, machine: WorkableMultiblockMachine, partialTicks: Float) {
        DeferredMachineRenderer.enqueue(Entry(blockEntity, machine, partialTicks))
    }

    private fun renderBatch(context: DeferredRenderContext, entries: List<DeferredRenderEntry>) {
        val currentShader = shader ?: return
        val first = entries.firstOrNull() as? Entry ?: return
        val ticks = RenderUtil.getSmoothTick(first.machine, first.partialTicks)
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        for (entry in entries) {
            val field = entry as Entry
            QFTForceFieldGeometry.write(builder, field.center.subtract(context.cameraPosition))
        }
        // Sort every face in this batch, including the back faces seen through the field.
        builder.setQuadSorting(VertexSorting.DISTANCE_TO_ORIGIN)
        RenderUtils.withSavedRenderState {
            RenderUtils.withBoundBuffer(vertexBuffer) {
                vertexBuffer.upload(builder.end())
                RenderSystem.enableBlend()
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
                RenderSystem.disableCull()
                RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11C.GL_LEQUAL)
                RenderSystem.depthMask(false)
                RenderSystem.setShaderTexture(0, texture)
                currentShader.getUniform("AnimationFrame")?.set(QFTForceFieldGeometry.animationFrame((ticks.toDouble() * 50.0).toLong()))

                DeferredOculusCompat.withDeferredShaderPass {
                    vertexBuffer.drawWithShader(context.poseStack.last().pose(), RenderSystem.getProjectionMatrix(), currentShader)
                }
            }
        }
    }

    private class Entry(
        override val blockEntity: BlockEntity,
        val machine: WorkableMultiblockMachine,
        val partialTicks: Float
    ) : DeferredRenderEntry {
        val center = QFTForceFieldGeometry.center(blockEntity.blockPos, machine.frontFacing)
        private val bounds = QFTForceFieldGeometry.bounds(center)

        // A shared shader uniform is valid only when the machines share an animation clock.
        override val batchKey: Any? = if (ConfigHolder.INSTANCE.graphics.enableSmoothAnimations) QFTForceFieldRenderer else null
        override val renderOrder: Int = 20

        override fun isVisible(context: DeferredRenderContext): Boolean =
            !blockEntity.isRemoved &&
                machine.isFormed &&
                machine.isActive &&
                machine.isWorkingEnabled &&
                (context.frustum?.isVisible(bounds) != false)

        override fun render(context: DeferredRenderContext) {
            renderBatch(context, listOf(this))
        }

        override fun renderBatch(context: DeferredRenderContext, entries: List<DeferredRenderEntry>) {
            QFTForceFieldRenderer.renderBatch(context, entries)
        }
    }
}