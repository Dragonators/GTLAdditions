package com.gtladd.gtladditions.common.data

import com.gtladd.gtladditions.api.machine.IWirelessBindableSource
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.core.BlockPos
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

data class DataModuleBindingRecord(
    val sourcePos: BlockPos,
    val sourceDimension: String,
    val bindingType: ResourceLocation
) {
    fun getSourceName(): Component = bindingType.toLanguageKey("block").toComponent

    companion object {
        private const val NBT_SOURCE_POS = "BindingSourcePos"
        private const val NBT_SOURCE_DIMENSION = "BindingSourceDimension"
        private const val NBT_BINDING_TYPE = "BindingType"

        fun read(stack: ItemStack): DataModuleBindingRecord? {
            val tag = stack.tag ?: return null
            if (!tag.contains(NBT_SOURCE_POS, Tag.TAG_LONG.toInt())) return null
            val dimension = tag.getString(NBT_SOURCE_DIMENSION).takeIf { it.isNotBlank() } ?: return null
            val type = tag.getString(NBT_BINDING_TYPE).takeIf { it.isNotBlank() }?.let { ResourceLocation(it) } ?: return null
            return DataModuleBindingRecord(
                BlockPos.of(tag.getLong(NBT_SOURCE_POS)),
                dimension,
                type
            )
        }

        fun write(stack: ItemStack, source: IWirelessBindableSource<*>, level: Level) {
            val pos = source.getPos()
            val tag = stack.orCreateTag
            tag.putLong(NBT_SOURCE_POS, pos.asLong())
            tag.putString(NBT_SOURCE_DIMENSION, level.dimension().location().toString())
            tag.putString(NBT_BINDING_TYPE, source.bindingType.toString())
            tag.remove("BindingName")
        }

        fun clear(stack: ItemStack) {
            val tag = stack.tag ?: return
            tag.remove(NBT_SOURCE_POS)
            tag.remove(NBT_SOURCE_DIMENSION)
            tag.remove(NBT_BINDING_TYPE)
            tag.remove("BindingName")
            if (tag.isEmpty) stack.tag = null
        }

        fun hasRecord(stack: ItemStack): Boolean = read(stack) != null
    }
}