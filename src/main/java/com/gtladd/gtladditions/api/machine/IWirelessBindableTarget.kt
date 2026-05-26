package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gtladd.gtladditions.common.data.DataModuleBindingRecord
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

interface IWirelessBindableTarget {
    val bindingTargetType: ResourceLocation

    fun acceptsBindingSource(type: ResourceLocation): Boolean

    fun isFormed(): Boolean

    fun bindResolvedSource(source: IWirelessBindableSource<*>): DataModuleBindingResult

    fun unbindSource()

    fun getPos(): BlockPos?

    fun getBindingTargetName(): Component = bindingTargetType.toLanguageKey("block").toComponent

    fun tryBindSource(level: Level, stack: ItemStack): DataModuleBindingResult {
        val record = DataModuleBindingRecord.read(stack) ?: return DataModuleBindingResult.Failure(
            DataModuleBindingError.NO_RECORD,
            "gtladditions.message.suprachronal_data_module.no_record".toComponent
        )

        if (!acceptsBindingSource(record.bindingType)) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.TARGET_MISMATCH,
                "gtladditions.message.suprachronal_data_module.target_mismatch".toComponent
            )
        }
        if (record.sourceDimension != level.dimension().location().toString()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.DIMENSION_MISMATCH,
                "gtladditions.message.suprachronal_data_module.dimension_mismatch".toComponent
            )
        }

        val source = (level.getBlockEntity(record.sourcePos) as? MetaMachineBlockEntity)?.metaMachine as? IWirelessBindableSource<*>
        if ((source == null) || (source.bindingType != record.bindingType)) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.SOURCE_UNLOADED,
                "gtladditions.message.suprachronal_data_module.source_unloaded".toComponent(record.getSourceName())
            )
        }

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

        return bindResolvedSource(source)
    }

    fun getBoundMessage(source: IWirelessBindableSource<*>, sourcePos: BlockPos): Component = "gtladditions.message.suprachronal_data_module.bound_target_to_source".toComponent(
        getBindingTargetName(),
        source.getBindingName(),
        sourcePos.x,
        sourcePos.y,
        sourcePos.z
    )
}