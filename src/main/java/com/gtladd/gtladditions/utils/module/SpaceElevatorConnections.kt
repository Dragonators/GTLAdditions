package com.gtladd.gtladditions.utils.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.SpaceElevatorMKII
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine

typealias ElevatorModule = IModularMachineModule<SpaceElevatorMachine, *>

object SpaceElevatorConnections {
    val registry = ModuleConnectionRegistry<SpaceElevatorMKII, ElevatorModule>()

    @JvmStatic
    fun register(module: ElevatorModule) {
        val level = module.level as? ServerLevel ?: return
        val machine = module as MetaMachine

        registry.addModule(level.dimension(), machine.pos, module)
        connectRegistered(level, machine.pos, module)
    }

    fun connectRegistered(level: ServerLevel, pos: BlockPos, module: ElevatorModule): Boolean {
        val current = module.host
        if (current != null && current.isFormed && !current.isInValid) return true

        val slots = registry.slotsAt(level.dimension(), pos)
        if (slots.isEmpty()) return false

        // When slots overlap, preserve the saved connection to a loaded host.
        val host = module.hostPosition?.let { position ->
            slots.firstOrNull { it.host.pos == position }?.host
                ?: loadedOriginalHost(level, position)?.takeIf { it.isFormed }
        } ?: slots.first().host

        module.connectToHost(host)
        return true
    }

    @JvmStatic
    fun unregister(module: ElevatorModule) {
        val level = module.level as? ServerLevel ?: return
        registry.removeModule(level.dimension(), (module as MetaMachine).pos, module)
    }

    /** Only the original elevator uses world lookup. Pending chunks are left to its native lifecycle. */
    @JvmStatic
    fun loadedOriginalHost(level: ServerLevel, pos: BlockPos): SpaceElevatorMachine? {
        val chunk = level.chunkSource.getChunkNow(pos.x shr 4, pos.z shr 4) ?: return null
        val host = (chunk.getBlockEntity(pos) as? IMachineBlockEntity)?.metaMachine as? SpaceElevatorMachine
        return host?.takeUnless { it is SpaceElevatorMKII }
    }
}