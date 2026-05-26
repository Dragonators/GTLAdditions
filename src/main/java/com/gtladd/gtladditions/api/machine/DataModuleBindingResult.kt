package com.gtladd.gtladditions.api.machine

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

sealed interface DataModuleBindingResult {
    val message: Component?

    val isSuccess: Boolean
        get() = this is Success || this is BoundSuccess

    data object Success : DataModuleBindingResult {
        override val message: Component? = null
    }

    data class BoundSuccess(
        val source: IWirelessBindableSource<*>,
        val sourcePos: BlockPos,
        override val message: Component
    ) : DataModuleBindingResult

    data class Failure(
        val error: DataModuleBindingError,
        override val message: Component
    ) : DataModuleBindingResult
}