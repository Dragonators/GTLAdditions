package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gtladd.gtladditions.common.data.DataModuleBindingRecord
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

interface IWirelessBindableTarget {

    val bindingTargetType: ResourceLocation
    var boundSourcePos: BlockPos?
    val boundSource: IWirelessBindableSource<*>?

    // ========================================
    // Binding contract
    // ========================================

    fun acceptsBindingSource(type: ResourceLocation): Boolean

    fun isValidSource(source: IWirelessBindableSource<*>): Boolean

    private fun acceptsBindingSource(source: IWirelessBindableSource<*>): Boolean =
        isValidSource(source) && acceptsBindingSource(source.bindingType)

    /** Stores the validated source in the controller's typed cache and notifies it of the binding. */
    fun updateBoundSource(source: IWirelessBindableSource<*>?)

    fun onSourceDisconnected() = Unit

    fun getLevel(): Level?

    fun getPos(): BlockPos?

    fun isFormed(): Boolean

    fun isBoundToSource(sourcePos: BlockPos): Boolean = boundSourcePos == sourcePos

    fun getBindingTargetName(): Component = bindingTargetType.toLanguageKey("block").toComponent

    // ========================================
    // Binding lifecycle
    // ========================================

    fun tryBindSource(level: Level, stack: ItemStack): DataModuleBindingResult {
        val record = DataModuleBindingRecord.read(stack) ?: return DataModuleBindingResult.Failure(
            DataModuleBindingError.NO_RECORD,
            "gtladditions.message.suprachronal_data_module.no_record".toComponent
        )

        if (!acceptsBindingSource(record.bindingType)) return targetMismatch()
        if (record.sourceDimension != level.dimension().location().toString()) return dimensionMismatch()
        if (!level.isLoaded(record.sourcePos)) return sourceUnloaded(record)

        val source = (level.getBlockEntity(record.sourcePos) as? MetaMachineBlockEntity)
            ?.metaMachine as? IWirelessBindableSource<*>
        if (source == null || source.bindingType != record.bindingType) return sourceUnloaded(record)

        return bindResolvedSource(source)
    }

    fun bindResolvedSource(source: IWirelessBindableSource<*>): DataModuleBindingResult {
        if (!acceptsBindingSource(source)) return targetMismatch()
        if (source.getLevel()?.dimension() != getLevel()?.dimension()) return dimensionMismatch()

        if (!source.isFormed()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.SOURCE_NOT_FORMED,
                "gtladditions.message.suprachronal_data_module.source_not_formed".toComponent(source.getBindingName())
            )
        }

        if (!isFormed()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.TARGET_NOT_FORMED,
                "gtladditions.message.suprachronal_data_module.target_not_formed".toComponent(getBindingTargetName())
            )
        }

        val sourcePos = source.getPos()
        if (boundSource !== source) {
            boundSource?.onUnbound(this)
        }

        boundSourcePos = sourcePos
        updateBoundSource(source)

        return DataModuleBindingResult.BoundSuccess(source, sourcePos, getBoundMessage(source, sourcePos))
    }

    fun unbindSource() = disconnectSource(clearBinding = true)

    fun disconnectSource(clearBinding: Boolean) {
        onSourceDisconnected()

        val oldSource = boundSource
        updateBoundSource(null)
        if (clearBinding) boundSourcePos = null

        oldSource?.disconnectTarget(this, clearBinding)
    }

    fun reconnectSource(): Boolean {
        if (!isFormed()) return false

        val selfPos = getPos() ?: return false
        val sourcePos = boundSourcePos ?: return false
        val serverLevel = getLevel() as? ServerLevel ?: return false
        if (!serverLevel.isLoaded(sourcePos)) return false

        val source = (serverLevel.getBlockEntity(sourcePos) as? MetaMachineBlockEntity)
            ?.metaMachine as? IWirelessBindableSource<*>
        if (source == null || !acceptsBindingSource(source) || !source.isBoundToTarget(selfPos)) {
            boundSourcePos = null
            return false
        }

        return source.isFormed() && bindResolvedSource(source).isSuccess
    }

    // ========================================
    // Messages & failure results
    // ========================================

    private fun targetMismatch() = DataModuleBindingResult.Failure(
        DataModuleBindingError.TARGET_MISMATCH,
        "gtladditions.message.suprachronal_data_module.target_mismatch".toComponent
    )

    private fun dimensionMismatch() = DataModuleBindingResult.Failure(
        DataModuleBindingError.DIMENSION_MISMATCH,
        "gtladditions.message.suprachronal_data_module.dimension_mismatch".toComponent
    )

    private fun sourceUnloaded(record: DataModuleBindingRecord) = DataModuleBindingResult.Failure(
        DataModuleBindingError.SOURCE_UNLOADED,
        "gtladditions.message.suprachronal_data_module.source_unloaded".toComponent(record.getSourceName())
    )

    fun getBoundMessage(source: IWirelessBindableSource<*>, sourcePos: BlockPos): Component =
        "gtladditions.message.suprachronal_data_module.bound_target_to_source".toComponent(
            getBindingTargetName(),
            source.getBindingName(),
            sourcePos.x,
            sourcePos.y,
            sourcePos.z
        )
}
