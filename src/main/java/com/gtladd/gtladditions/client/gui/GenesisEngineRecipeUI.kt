package com.gtladd.gtladditions.client.gui

import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheCosmos
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheUniverse
import com.gtladd.gtladditions.mixin.ldlib.TankWidgetAccessor
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.widget.TankWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.jei.IngredientIO
import com.lowdragmc.lowdraglib.utils.CycleFluidTransfer
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.MutableComponent

object GenesisEngineRecipeUI {
    fun build(recipe: GTRecipe, page: WidgetGroup) {
        val boosted = recipe.id == HeartOfTheUniverse.ANNIHILATION_RECIPE
        aux(page, "aux_stabilizer", boosted, HeartOfTheCosmos.RECOVERY_FLUIDS, "gtladditions.genesis_engine.stabilizer", 2) {
            "gtladditions.genesis_engine.recovery".toComponent(it / 100)
        }

        aux(page, "aux_amplifier", boosted, HeartOfTheCosmos.POWER_FLUIDS, "gtladditions.genesis_engine.amplifier", 3) {
            "gtladditions.genesis_engine.power".toComponent(it)
        }

        // Without the fluid row the inputs move half a slot down, level with the arrow and the output.
        if (!boosted) {
            for (id in listOf("item_in_0", "item_in_1")) {
                find(page, id)?.second?.addSelfPosition(0, 9)
            }
        }
    }

    private fun aux(
        page: WidgetGroup,
        id: String,
        boosted: Boolean,
        fluids: Reference2IntArrayMap<Material>,
        kind: String,
        tier: Int,
        effect: (Int) -> MutableComponent
    ) {
        val (group, tank) = find(page, id) ?: return
        if (!boosted) {
            group.removeWidget(tank)
            find(page, "${id}_mark")?.let { (parent, mark) -> parent.removeWidget(mark) }
            return
        }

        (tank as TankWidget)
            .setFluidTank(
                CycleFluidTransfer(listOf(fluids.keys.map { it.getFluid(HeartOfTheCosmos.AUXILIARY_PER_PLANE) })),
                0
            )
            .setIngredientIO(IngredientIO.INPUT)
            .setAllowClickFilled(false)
            .setAllowClickDrained(false)
            .setOnAddedTooltips { widget, lines ->
                val shown = (widget as TankWidgetAccessor).currentJEIRenderedIngredient ?: return@setOnAddedTooltips
                val value = fluids.reference2IntEntrySet().firstOrNull { it.key.fluid == shown.fluid }?.intValue ?: return@setOnAddedTooltips
                lines.add(kind.toComponent.withStyle(ChatFormatting.AQUA))
                lines.add("gtladditions.structure_tier.recipe".toComponent(tier).withStyle(ChatFormatting.GRAY))
                lines.add(effect(value).withStyle(ChatFormatting.GRAY))
            }
    }

    private fun find(group: WidgetGroup, id: String): Pair<WidgetGroup, Widget>? {
        for (child in group.widgets) {
            if (child.id == id) return group to child
            if (child is WidgetGroup) find(child, id)?.let { return it }
        }
        return null
    }
}