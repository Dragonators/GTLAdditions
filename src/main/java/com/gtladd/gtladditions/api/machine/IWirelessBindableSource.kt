package com.gtladd.gtladditions.api.machine

import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

interface IWirelessBindableSource<T : IWirelessBindableTarget> {
    val bindingType: ResourceLocation

    fun getPos(): BlockPos

    fun isFormed(): Boolean

    fun tryRecording(): DataModuleBindingResult {
        if (!isFormed()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.SOURCE_NOT_FORMED,
                Component.translatable("gtladditions.message.suprachronal_data_module.source_not_formed", getBindingName())
            )
        }
        return DataModuleBindingResult.Success
    }

    fun getBindingName(): Component = bindingType.toLanguageKey("block").toComponent

    fun getRecordedMessage(pos: BlockPos): Component = "gtladditions.message.suprachronal_data_module.recorded_source".toComponent(
        getBindingName(),
        pos.x,
        pos.y,
        pos.z
    )

    fun onBound(target: T) = Unit

    fun onUnbound(target: T?) = Unit
}