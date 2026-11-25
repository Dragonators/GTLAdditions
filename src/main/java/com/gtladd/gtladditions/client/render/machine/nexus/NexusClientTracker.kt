package com.gtladd.gtladditions.client.render.machine.nexus

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.multiblock.controller.SubspaceCorridorNexusIndustrialArray
import com.gtladd.gtladditions.utils.SubspaceNexusLayout
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.Collections
import java.util.WeakHashMap

/**
 * A Nexus surrounds the camera while its controller can be behind it. Submit loaded controllers before block entities,
 * even before the controller's chunk section has been compiled by the terrain renderer.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object NexusClientTracker {
    private val machines = Collections.newSetFromMap(WeakHashMap<SubspaceCorridorNexusIndustrialArray, Boolean>())

    fun track(machine: SubspaceCorridorNexusIndustrialArray) {
        machines.add(machine)
    }

    fun untrack(machine: SubspaceCorridorNexusIndustrialArray) {
        machines.remove(machine)
    }

    @JvmStatic
    @SubscribeEvent
    fun render(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        val level = Minecraft.getInstance().level ?: return

        val iterator = machines.iterator()
        while (iterator.hasNext()) {
            val machine = iterator.next()
            val blockEntity = machine.holder.self()
            if (blockEntity.isRemoved ||
                blockEntity.level !== level ||
                !level.isLoaded(machine.pos) ||
                MetaMachine.getMachine(level, machine.pos) !== machine
            ) {
                iterator.remove()
                continue
            }

            if (!machine.isFormed || !star(machine).closerThan(event.camera.position, VIEW_DISTANCE)) continue

            NexusScene.enqueue(blockEntity, machine, event.partialTick)
        }
    }

    /** The star in the world; the view distance is measured from it, not from the controller at the host's edge. */
    private fun star(machine: SubspaceCorridorNexusIndustrialArray): Vec3 {
        val star = NexusFrame.STAR
        return SubspaceNexusLayout.point(star.x, star.y, star.z, machine.frontFacing).add(Vec3.atLowerCornerOf(machine.pos))
    }

    private const val VIEW_DISTANCE = 384.0
}
