package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.gui.LimitedDurationConfigurator
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.RecipeText
import org.gtlcore.gtlcore.common.machine.multiblock.electric.WorkableElectricMultipleRecipesMachine
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues
import org.gtlcore.gtlcore.utils.NumberUtils
import kotlin.math.max

open class GTLAddWorkableElectricMultipleRecipesMachine(holder: IMachineBlockEntity, vararg args: Any?) :
    WorkableElectricMultipleRecipesMachine(holder, *args), IGTLAddMultiRecipe {
    @field:Persisted(key = "drLimit")
    private var limitedDuration = 20

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    public override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this)
    }

    override fun getRecipeLogic(): GTLAddMultipleRecipesLogic {
        return super.getRecipeLogic() as GTLAddMultipleRecipesLogic
    }

    override fun setLimitedDuration(number: Int) {
        if (number != limitedDuration) limitedDuration = number
    }

    override fun getLimitedDuration(): Int {
        return this.limitedDuration
    }

    // ========================================
    // GUI SYSTEM
    // ========================================

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(LimitedDurationConfigurator(this))
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        if (isFormed()) {
            // Energy display
            addEnergyDisplay(textList)

            // Machine mode
            addMachineModeDisplay(textList)

            // Parallel display
            addParallelDisplay(textList)

            // Working status
            addWorkingStatus(textList)

            // Recipe/Working status errors
            (recipeLogic as IRecipeStatus).let { status ->
                status.recipeStatus?.reason?.copy()?.withStyle(ChatFormatting.RED)?.let(textList::add)
                status.workingStatus?.reason?.copy()?.withStyle(ChatFormatting.RED)?.let(textList::add)
            }
        } else {
            textList.add(
                Component.translatable("gtceu.multiblock.invalid_structure")
                    .withStyle(
                        Style.EMPTY.withColor(ChatFormatting.RED)
                            .withHoverEvent(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("gtceu.multiblock.invalid_structure.tooltip")
                                        .withStyle(ChatFormatting.GRAY)
                                )
                            )
                    )
            )
        }

        // Recipe lock display
        (recipeLogic as? ILockRecipe)?.let { addRecipeLockDisplay(textList, it) }

        definition.additionalDisplay.accept(this, textList)
        parts.forEach { it.addMultiText(textList) }
    }

    protected open fun addEnergyDisplay(textList: MutableList<Component?>) {

        // Max energy per tick
        if (energyContainer != null && energyContainer.energyCapacity > 0) {
            val maxVoltage = max(energyContainer.inputVoltage, energyContainer.outputVoltage)
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.max_energy_per_tick",
                    FormattingUtil.formatNumbers(maxVoltage),
                    Component.literal(
                        NewGTValues.VNF[if (maxVoltage == Long.MAX_VALUE) GTValues.MAX_TRUE else NumberUtils.getFakeVoltageTier(maxVoltage)]
                    )
                )
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("gtceu.multiblock.max_energy_per_tick_hover").withStyle(ChatFormatting.GRAY)
                            )
                        )
                    })
        }

        // Max recipe tier
        if (tier >= GTValues.ULV && tier <= GTValues.MAX_TRUE) {
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.max_recipe_tier",
                    Component.literal(GTValues.VNF[tier.coerceAtMost(14)])
                )
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("gtceu.multiblock.max_recipe_tier_hover").withStyle(ChatFormatting.GRAY)
                            )
                        )
                    })
        }
    }

    protected open fun addMachineModeDisplay(textList: MutableList<Component?>) {
        textList.add(
            Component.translatable("gtceu.gui.machinemode", Component.translatable(recipeType.registryName.toLanguageKey()))
                .withStyle(ChatFormatting.AQUA)
        )
    }

    protected open fun addParallelDisplay(textList: MutableList<Component?>) {
        if (maxParallel > 1) {
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.parallel",
                    Component.literal(FormattingUtil.formatNumbers(maxParallel)).withStyle(ChatFormatting.DARK_PURPLE)
                ).withStyle(ChatFormatting.GRAY)
            )
        }
        if(getRecipeLogic().multipleThreads > 1) {
            textList.add(
                Component.translatable(
                    "gtladditions.multiblock.threads",
                    Component.literal(FormattingUtil.formatNumbers(getRecipeLogic().multipleThreads)).withStyle(ChatFormatting.GOLD)
                ).withStyle(ChatFormatting.GRAY)
            )
        }
    }

    protected open fun addWorkingStatus(textList: MutableList<Component?>) {
        when {
            !isWorkingEnabled -> textList.add(Component.translatable("gtceu.multiblock.work_paused").withStyle(ChatFormatting.GOLD))

            isActive -> {
                textList.add(Component.translatable("gtceu.multiblock.running").withStyle(ChatFormatting.GREEN))
                textList.add(Component.translatable("gtceu.multiblock.progress", (recipeLogic.progressPercent * 100).toInt()))
            }

            else -> textList.add(Component.translatable("gtceu.multiblock.idling"))
        }
    }

    protected open fun addRecipeLockDisplay(textList: MutableList<Component?>, iLockRecipe: ILockRecipe) {
        val text = if (iLockRecipe.isLock && iLockRecipe.lockRecipe != null) {
            Component.translatable("gui.gtlcore.recipe_lock.recipe").withStyle {
                it.withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        RecipeText.getRecipeInputText(iLockRecipe.lockRecipe)
                            .append(RecipeText.getRecipeOutputText(iLockRecipe.lockRecipe))
                    )
                )
            }
        } else {
            Component.translatable("gui.gtlcore.recipe_lock.no_recipe")
        }
        textList.add(text)
    }

    companion object {
        @JvmStatic
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(
                GTLAddWorkableElectricMultipleRecipesMachine::class.java,
                WorkableMultiblockMachine.MANAGED_FIELD_HOLDER
            )
    }
}
