package com.gtladd.gtladditions.utils

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object ComponentExtensions {
    val String.translatable: String get() = this.toComponent.string
    val String.toComponent: MutableComponent get() = Component.translatable(this)
    fun String.toComponent(vararg args: Any?): MutableComponent = Component.translatable(this, *args)
    val String.literal: MutableComponent get() = Component.literal(this)
    val Number.literal: MutableComponent get() = this.toString().literal
}