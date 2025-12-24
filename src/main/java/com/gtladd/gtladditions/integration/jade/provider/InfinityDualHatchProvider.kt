package com.gtladd.gtladditions.integration.jade.provider

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.integration.jade.GTElementHelper
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.Registries
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig
import snownee.jade.api.fluid.JadeFluidObject

class InfinityDualHatchProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val blockEntity = accessor.blockEntity as? IMachineBlockEntity ?: return
        if (blockEntity.metaMachine is InfinityDualHatchPartMachine) {
            readStorageContents(tooltip, accessor.serverData)
        }
    }

    override fun appendServerData(tag: CompoundTag, accessor: BlockAccessor) {
        val blockEntity = accessor.blockEntity as? IMachineBlockEntity ?: return
        val hatch = blockEntity.metaMachine as? InfinityDualHatchPartMachine ?: return
        putTag(tag, hatch)
    }

    override fun getUid(): ResourceLocation = GTCEu.id("infinity_dual_hatch")

    companion object {
        private fun readStorageContents(tooltip: ITooltip, serverData: CompoundTag) {
            val itemTags = serverData.getList("items", Tag.TAG_COMPOUND.toInt())
            for (t in itemTags) {
                val itemTag = t as? CompoundTag ?: continue
                val item = Registries.getItem(itemTag.getString("item"))
                val count = itemTag.getLong("count")
                if (count > 0) {
                    val stack = ItemStack(item)
                    tooltip.add(tooltip.elementHelper.smallItem(stack))
                    val text = Component.literal(" ")
                        .append(Component.literal(NumberUtils.formatLong(count)).withStyle(ChatFormatting.DARK_PURPLE))
                        .append(Component.literal(" × ").withStyle(ChatFormatting.WHITE))
                        .append(stack.hoverName.copy().withStyle(ChatFormatting.GOLD))
                    tooltip.append(text)
                }
            }

            val fluidTags = serverData.getList("fluids", Tag.TAG_COMPOUND.toInt())
            for (t in fluidTags) {
                val fluidTag = t as? CompoundTag ?: continue
                val fluid = Registries.getFluid(fluidTag.getString("fluid"))
                val amount = fluidTag.getLong("amount")
                if (amount > 0) {
                    tooltip.add(GTElementHelper.smallFluid(JadeFluidObject.of(fluid)))
                    val text = Component.literal(" ")
                        .append(if (amount < 1000L) "${amount}mB" else "${NumberUtils.formatLong(amount / 1000)}B")
                        .withStyle(ChatFormatting.DARK_PURPLE)
                        .append(Component.literal(" ").withStyle(ChatFormatting.WHITE))
                        .append(fluid.fluidType.description.copy().withStyle(ChatFormatting.DARK_AQUA))
                    tooltip.append(text)
                }
            }
        }

        private fun putTag(tag: CompoundTag, dualHatch: InfinityDualHatchPartMachine) {
            val itemTags = ListTag()
            for (entry in dualHatch.itemIterator) {
                val stack = entry.key
                val itemTag = CompoundTag()
                itemTag.putString("item", Registries.getItemId(stack))
                itemTag.putLong("count", entry.longValue)
                itemTags.add(itemTag)
            }
            tag.put("items", itemTags)

            val fluidTags = ListTag()
            for (fluidStack in dualHatch.fluidIterator) {
                val fluidTag = CompoundTag()
                fluidTag.putString("fluid", Registries.getFluidId(fluidStack))
                fluidTag.putLong("amount", fluidStack.amount)
                fluidTags.add(fluidTag)
            }
            tag.put("fluids", fluidTags)
        }
    }
}