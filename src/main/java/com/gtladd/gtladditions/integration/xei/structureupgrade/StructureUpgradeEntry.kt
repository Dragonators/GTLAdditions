package com.gtladd.gtladditions.integration.xei.structureupgrade

import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.feature.TierRequirement
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class StructureUpgradeEntry(
    val machine: MachineDefinition,
    val toTier: Int,
    val maxTier: Int,
    val requirement: TierRequirement,
    val theme: StructureUpgradeTheme
) {
    val fromTier: Int
        get() = toTier - 1

    val id: ResourceLocation = GTLAdditions.id("structure_upgrade/${machine.id.path}/tier_$toTier")

    /** AE2 needs an output to encode a processing pattern; like GTLCore's multiblock pages, the page itself shows none. */
    fun patternOutput(): ItemStack = ItemStack(Items.WRITTEN_BOOK).apply {
        setHoverName(
            "gtladditions.structure_tier.xei.output".toComponent(Component.translatable(machine.descriptionId), toTier)
                .withStyle { it.withItalic(false).withColor(theme.accentText and 0xFFFFFF) }
        )
        hideTooltipPart(ItemStack.TooltipPart.ADDITIONAL)
    }
}
