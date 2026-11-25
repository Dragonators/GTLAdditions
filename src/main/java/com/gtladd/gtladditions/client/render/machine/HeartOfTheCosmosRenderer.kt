package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderProfile
import com.gtladd.gtladditions.client.render.machine.heart.HeartFleetRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartShellRenderer
import com.gtladd.gtladditions.client.render.scene.SceneVolumes
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheCosmos
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.HeartBlockLayout
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.api.machine.IGTLRenderDisabled
import org.gtlcore.gtlcore.common.data.GTLBlocks.IRIDIUM_CASING
import org.gtlcore.gtlcore.config.ConfigHolder

class HeartOfTheCosmosRenderer :
    PartWorkableCasingMachineRenderer(
        GTCEu.id("block/casings/hpca/high_power_casing"),
        GTCEu.id("block/multiblock/cosmos_simulation"),
        IRIDIUM_CASING,
        GTLCore.id("block/casings/iridium_casing")
    ),
    MachineRenderBox {

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? HeartOfTheCosmos ?: return
        if (!machine.isFormed) return
        val frame = SceneRendering.frame() ?: return
        if (IGTLRenderDisabled.isRenderDisabled(machine)) {
            HeartShellRenderer.submitStatic(blockEntity, machine)
            return
        }

        val smooth = ConfigHolder.INSTANCE.graphics.enableSmoothAnimations
        val tick = RenderUtils.getSmoothTickDouble(machine, partialTicks)
        val working = machine.recipeLogic.isWorking
        val motion = HeartShellRenderer.motion(machine)
        motion.advance(tick / 20.0, working, smooth)
        HeartShellRenderer.submit(frame, blockEntity, machine, motion)
        if (machine.structureTier == HeartBlockLayout.tiers.maxTier) HeartFleetRenderer.submit(frame, machine, motion)
        if (!working) return
        val profile = HeartBlackHoleRenderProfile.create(RenderUtils.continuousRenderTick(machine, tick, smooth), machine.frontFacing, starOffset(machine))
        SceneVolumes.submit(frame, blockEntity, profile)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    /** The fleet reaches about 171 blocks from the black hole; leave room to view its far side. */
    override fun getViewDistance(): Int = 384

    @OnlyIn(Dist.CLIENT)
    /** Measured from the black hole, not the far-off controller, so the Heart drops out at one distance all round. */
    override fun shouldRender(blockEntity: BlockEntity, cameraPos: Vec3): Boolean {
        val machine = (blockEntity as IMachineBlockEntity).metaMachine
        return starOffset(machine).add(Vec3.atLowerCornerOf(blockEntity.blockPos)).closerThan(cameraPos, viewDistance.toDouble())
    }

    @OnlyIn(Dist.CLIENT)
    /** The shells, and the fleet once its plan is built, around the black hole. */
    override fun renderBox(blockEntity: BlockEntity): AABB {
        val center = starOffset((blockEntity as IMachineBlockEntity).metaMachine).add(Vec3.atLowerCornerOf(blockEntity.blockPos))
        return AABB(center, center).inflate(HeartFleetRenderer.reach ?: HeartShellRenderer.RADIUS)
    }

    /** Relative to the controller block's corner. */
    private fun starOffset(machine: MetaMachine): Vec3 {
        val c = HeartBlockLayout.centerOffset
        return getRotatedRenderPosition(Direction.SOUTH, machine.frontFacing, c.x.toDouble(), c.y.toDouble(), c.z.toDouble())
    }
}
