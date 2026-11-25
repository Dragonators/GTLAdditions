package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level

interface IWirelessBindableSource<T : IWirelessBindableTarget> {

    val bindingType: ResourceLocation
    var boundTargetPos: BlockPos?
    var boundTarget: T?

    // ========================================
    // Binding contract
    // ========================================

    fun getLevel(): Level?

    fun getPos(): BlockPos

    fun isFormed(): Boolean

    fun asValidTarget(machine: MetaMachine?): T?

    fun isBoundToTarget(targetPos: BlockPos): Boolean = boundTargetPos == targetPos

    fun getBindingName(): Component = bindingType.toLanguageKey("block").toComponent

    // ========================================
    // Recording
    // ========================================

    fun tryRecording(): DataModuleBindingResult {
        if (!isFormed()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.SOURCE_NOT_FORMED,
                Component.translatable("gtladditions.message.suprachronal_data_module.source_not_formed", getBindingName())
            )
        }

        return DataModuleBindingResult.Success
    }

    fun getRecordedMessage(pos: BlockPos): Component =
        "gtladditions.message.suprachronal_data_module.recorded_source".toComponent(
            getBindingName(),
            pos.x,
            pos.y,
            pos.z
        )

    // ========================================
    // Binding lifecycle
    // ========================================

    fun onBound(target: T) {
        if (boundTarget !== target) {
            boundTarget?.unbindSource()
        }

        boundTarget = target
        boundTargetPos = target.getPos()
    }

    fun onUnbound(target: IWirelessBindableTarget?) = disconnectTarget(target, clearBinding = true)

    fun disconnectTarget(target: IWirelessBindableTarget?, clearBinding: Boolean) {
        if (target != null && boundTarget !== target && boundTargetPos != target.getPos()) return

        boundTarget = null
        if (clearBinding) boundTargetPos = null
    }

    fun detachTarget() {
        boundTarget?.disconnectSource(clearBinding = false)
        boundTarget = null
    }

    fun unbindTarget() {
        boundTarget?.unbindSource()
        disconnectTarget(null, clearBinding = true)
    }

    fun reconnectTarget(): Boolean {
        if (!isFormed()) return false

        val targetPos = boundTargetPos ?: return false
        val serverLevel = getLevel() as? ServerLevel ?: return false
        if (!serverLevel.isLoaded(targetPos)) return false

        val machine = (serverLevel.getBlockEntity(targetPos) as? MetaMachineBlockEntity)?.metaMachine
        val target = asValidTarget(machine)
        if (target == null || !target.isBoundToSource(getPos())) {
            boundTargetPos = null
            return false
        }

        return target.isFormed() && target.bindResolvedSource(this).isSuccess
    }

    fun resolveTarget(): T? {
        val cached = boundTarget
        if (cached != null && cached.isFormed()) return cached

        boundTarget = null
        return if (reconnectTarget()) boundTarget else null
    }
}
