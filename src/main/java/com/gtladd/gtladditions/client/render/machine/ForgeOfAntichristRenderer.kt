package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristDeferredRenderer
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRenderProfile
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.mojang.blaze3d.vertex.PoseStack
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.api.machine.IGTLRenderDisabled

class ForgeOfAntichristRenderer(
    baseCasing: ResourceLocation,
    workableModel: ResourceLocation,
    partEntry: BlockEntry<Block>,
    partCasing: ResourceLocation
) : PartWorkableCasingMachineRenderer(baseCasing, workableModel, partEntry, partCasing) {

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? ForgeOfTheAntichrist ?: return
        if (IGTLRenderDisabled.isRenderDisabled(machine)) {
            if (!machine.isFormed) return
            val profile = AntichristRenderProfile.staticRings(machine.frontFacing)
            AntichristDeferredRenderer.enqueue(blockEntity, profile, renderRings = true, renderEffects = false)
            return
        }

        val isWorking = machine.isActiveOrStasisAnchored()
        if (!machine.isFormed && !isWorking) return

        val tick = if (isWorking) RenderUtils.getSmoothTickDouble(machine, partialTicks) else 0.0
        val profile = AntichristRenderProfile.create(machine, tick, isWorking)
        AntichristDeferredRenderer.enqueue(
            blockEntity,
            profile,
            renderRings = machine.isFormed,
            renderEffects = isWorking
        )
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384

    @OnlyIn(Dist.CLIENT)
    /** Measured from the star the rings turn about, not from the controller at the end of its beam. */
    override fun shouldRender(blockEntity: BlockEntity, cameraPos: Vec3): Boolean {
        val facing = (blockEntity as IMachineBlockEntity).metaMachine.frontFacing
        val star = getRotatedRenderPosition(AntichristRenderProfile.BASE_DIRECTION, facing, AntichristRenderProfile.STAR_OFFSET_X, 0.0, 0.0)
        return star.add(Vec3.atLowerCornerOf(blockEntity.blockPos)).closerThan(cameraPos, viewDistance.toDouble())
    }
}
