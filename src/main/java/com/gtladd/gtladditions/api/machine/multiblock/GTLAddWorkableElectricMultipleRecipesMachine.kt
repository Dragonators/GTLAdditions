package com.gtladd.gtladditions.api.machine.multiblock

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipeMachine
import com.gtladd.gtladditions.api.machine.gui.LimitedDurationConfigurator
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.utils.CommonUtils
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
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
    WorkableElectricMultipleRecipesMachine(holder, *args),
    IGTLAddMultiRecipeMachine {
    @field:Persisted(key = "drLimit")
    private var limitedDuration = 20

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    public override fun createRecipeLogic(vararg args: Any): RecipeLogic = GTLAddMultipleRecipesLogic(this)

    override fun getRecipeLogic(): GTLAddMultipleRecipesLogic = super.getRecipeLogic() as GTLAddMultipleRecipesLogic

    override fun setLimitedDuration(duration: Int) {
        if (duration != limitedDuration) limitedDuration = duration
    }

    override fun getLimitedDuration(): Int = this.limitedDuration

    override fun needConfirmMEStock(): Boolean = false

    // ========================================
    // GUI SYSTEM
    // ========================================

    protected open fun createConfigurators(): IFancyConfigurator? = LimitedDurationConfigurator(this)

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        createConfigurators()?.let { it -> configuratorPanel.attachConfigurators(it) }
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
                "gtceu.multiblock.invalid_structure".toComponent
                    .withStyle(
                        Style.EMPTY.withColor(ChatFormatting.RED)
                            .withHoverEvent(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    "gtceu.multiblock.invalid_structure.tooltip".toComponent
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
        getWirelessNetworkEnergyHandler()?.let { networkEnergyHandler ->
            if (!networkEnergyHandler.isOnline) return@let
            val totalEu = networkEnergyHandler.maxAvailableEnergy
            val longEu = NumberUtils.getLongValue(totalEu)
            val energyTier = if (longEu == Long.MAX_VALUE) GTValues.MAX_TRUE else NumberUtils.getFakeVoltageTier(longEu)

            // Max energy per tick
            textList.add(
                "gtceu.multiblock.max_energy_per_tick".toComponent(CommonUtils.formatBigIntegerFixed(totalEu), NewGTValues.VNF[energyTier].literal)
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                "gtceu.multiblock.max_energy_per_tick_hover".toComponent
                                    .withStyle(ChatFormatting.GRAY)
                            )
                        )
                    }
            )

            // Max recipe tier
            textList.add(
                "gtceu.multiblock.max_recipe_tier".toComponent(GTValues.VNF[energyTier.coerceAtMost(14)].literal)
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                "gtceu.multiblock.max_recipe_tier_hover".toComponent
                                    .withStyle(ChatFormatting.GRAY)
                            )
                        )
                    }
            )

            return@addEnergyDisplay
        }

        // Max energy per tick
        if (energyContainer != null && energyContainer.energyCapacity > 0) {
            val maxVoltage = max(energyContainer.inputVoltage, energyContainer.outputVoltage)
            textList.add(
                "gtceu.multiblock.max_energy_per_tick".toComponent(
                    FormattingUtil.formatNumbers(maxVoltage),
                    NewGTValues.VNF[
                        if (maxVoltage == Long.MAX_VALUE) {
                            GTValues.MAX_TRUE
                        } else {
                            NumberUtils.getFakeVoltageTier(
                                maxVoltage
                            )
                        }
                    ].literal
                )
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                "gtceu.multiblock.max_energy_per_tick_hover".toComponent
                                    .withStyle(ChatFormatting.GRAY)
                            )
                        )
                    }
            )
        }

        // Max recipe tier
        if (tier >= GTValues.ULV) {
            textList.add(
                "gtceu.multiblock.max_recipe_tier".toComponent(GTValues.VNF[tier.coerceAtMost(14)].literal)
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                "gtceu.multiblock.max_recipe_tier_hover".toComponent
                                    .withStyle(ChatFormatting.GRAY)
                            )
                        )
                    }
            )
        }
    }

    protected open fun addMachineModeDisplay(textList: MutableList<Component?>) {
        textList.add(
            "gtceu.gui.machinemode".toComponent(recipeType.registryName.toLanguageKey().toComponent)
                .withStyle(ChatFormatting.AQUA)
        )
    }

    protected open fun addParallelDisplay(textList: MutableList<Component?>) {
        if (maxParallel > 1) {
            textList.add(
                "gtceu.multiblock.parallel".toComponent(FormattingUtil.formatNumbers(maxParallel).literal.withStyle(ChatFormatting.DARK_PURPLE)).withStyle(ChatFormatting.GRAY)
            )
        }
        if (getRecipeLogic().getMultipleThreads() > 1) {
            textList.add(
                "gtladditions.multiblock.threads".toComponent((FormattingUtil.formatNumbers(getRecipeLogic().getMultipleThreads())).literal.withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY)
            )
        }
    }

    protected open fun addWorkingStatus(textList: MutableList<Component?>) {
        when {
            !isWorkingEnabled -> textList.add("gtceu.multiblock.work_paused".toComponent.withStyle(ChatFormatting.GOLD))

            isActive -> {
                textList.add("gtceu.multiblock.running".toComponent.withStyle(ChatFormatting.GREEN))
                textList.add("gtceu.multiblock.progress".toComponent((recipeLogic.progressPercent * 100).toInt()))
            }

            else -> textList.add("gtceu.multiblock.idling".toComponent)
        }
    }

    protected open fun addRecipeLockDisplay(textList: MutableList<Component?>, iLockRecipe: ILockRecipe) {
        val text = if (iLockRecipe.isLock && iLockRecipe.lockRecipe != null) {
            "gui.gtlcore.recipe_lock.recipe".toComponent.withStyle {
                it.withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        RecipeText.getRecipeInputText(iLockRecipe.lockRecipe)
                            .append(RecipeText.getRecipeOutputText(iLockRecipe.lockRecipe))
                    )
                )
            }
        } else {
            "gui.gtlcore.recipe_lock.no_recipe".toComponent
        }
        textList.add(text)
    }

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(
                GTLAddWorkableElectricMultipleRecipesMachine::class.java,
                WorkableMultiblockMachine.MANAGED_FIELD_HOLDER
            )
    }
}