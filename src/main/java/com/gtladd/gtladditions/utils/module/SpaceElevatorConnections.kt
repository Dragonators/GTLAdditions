package com.gtladd.gtladditions.utils.module

import com.gregtechceu.gtceu.api.machine.MetaMachine
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine

typealias ElevatorModule = IModularMachineModule<SpaceElevatorMachine, *>

object SpaceElevatorConnections {
    val registry = ModuleConnectionRegistry<SpaceElevatorMachine, ElevatorModule>()

    @JvmStatic
    fun registerHost(host: SpaceElevatorMachine, registeredSlots: Array<BlockPos>?): Array<BlockPos>? {
        val level = host.level as? ServerLevel ?: return null
        val dimension = level.dimension()
        val slots = registeredSlots ?: host.moduleScanPositions.also {
            registry.register(dimension, host, it)
        }

        for (pos in slots) {
            val module = registry.moduleAt(dimension, pos) ?: continue
            connectRegistered(level, pos, module)
        }

        return slots
    }

    @JvmStatic
    fun releaseHost(host: SpaceElevatorMachine, slots: Array<BlockPos>) {
        val level = host.level as ServerLevel
        val dimension = level.dimension()

        registry.unregister(dimension, host, slots)
        host.safeClearModules()

        for (pos in slots) {
            val module = registry.moduleAt(dimension, pos) ?: continue
            if (module.host == null) connectRegistered(level, pos, module)
        }
    }

    @JvmStatic
    fun register(module: ElevatorModule) {
        val level = module.level as? ServerLevel ?: return
        val pos = (module as MetaMachine).pos

        registry.addModule(level.dimension(), pos, module)
        connectRegistered(level, pos, module)
    }

    @JvmStatic
    fun connectRegistered(level: ServerLevel, pos: BlockPos, module: ElevatorModule): Boolean {
        val dimension = level.dimension()
        if (registry.moduleAt(dimension, pos) !== module) return false

        val slots = registry.slotsAt(dimension, pos)
        if (slots.isEmpty()) return false

        val saved = module.hostPosition
        val host = slots.firstOrNull { it.host.pos == saved }?.host ?: slots.first().host
        if (module.host !== host) module.connectToHost(host)

        return true
    }

    @JvmStatic
    fun unregister(module: ElevatorModule) {
        val level = module.level as? ServerLevel ?: return
        registry.removeModule(level.dimension(), (module as MetaMachine).pos, module)
    }

    @JvmStatic
    fun <M : IModularMachineModule<SpaceElevatorMachine, M>> unload(module: M) {
        unregister(module)

        val host = module.host ?: return

        // Retain the saved host position for overlapping slots on reload.
        module.host = null
        host.removeModule(module)
        module.onDisconnected()
    }
}