package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.gtladd.gtladditions.utils.antichrist.AntichristPosHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper

abstract class ForgeOfTheAntichristModuleBase(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleRecipesMachine(
        holder,
        *args
    ),
    IModularMachineModule<ForgeOfTheAntichrist, ForgeOfTheAntichristModuleBase> {
    @field:Persisted
    private var hostPosition: BlockPos? = null
    private var host: ForgeOfTheAntichrist? = null

    override fun getHostPosition(): BlockPos? = hostPosition
    override fun setHostPosition(pos: BlockPos?) {
        hostPosition = pos
    }
    override fun getHost(): ForgeOfTheAntichrist? = host
    override fun setHost(host: ForgeOfTheAntichrist?) {
        this.host = host
    }
    override fun getHostType(): Class<ForgeOfTheAntichrist> = ForgeOfTheAntichrist::class.java
    override fun getHostScanPositions(): Array<BlockPos> = AntichristPosHelper.calculatePossibleHostPositions(pos, frontFacing)

    // ========================================
    // ForgeOfTheAntichrist connection
    // ========================================

    override fun onConnected(host: ForgeOfTheAntichrist) {
        recipeLogic.updateTickSubscription()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        if (!findAndConnectToHost()) {
            removeFromHost(this.host)
        }
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        removeFromHost(this.host)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(this.host)
    }

    override fun onMachineRemoved() {
        removeFromHost(this.host)
    }

    abstract override fun createRecipeLogic(vararg args: Any): ForgeOfTheAntichristModuleBaseLogic

    abstract override fun getRecipeLogic(): ForgeOfTheAntichristModuleBaseLogic

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!this.isFormed) return

        if (isConnectedToHost && getRecipeLogic().enableModify(this.recipeType)) {
            textList.add(
                if (host!!.runningSecs >= ForgeOfTheAntichrist.MAX_EFFICIENCY_SEC) {
                    createLanguageRainbowComponentOnServer(
                        "gtladditions.multiblock.forge_of_the_antichrist.achieve_max_efficiency".toComponent
                    )
                } else {
                    "gtladditions.multiblock.forge_of_the_antichrist.output_multiplier".toComponent(createRainbowComponent(FormattingUtil.DECIMAL_FORMAT_2F.format(host!!.recipeOutputMultiplyPreview)))
                }
            )
        }

        textList.add(
            (if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed").toComponent
        )
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        textList.add(
            "gtceu.multiblock.parallel".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
        textList.add(
            "gtladditions.multiblock.threads".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun needConfirmMEStock(): Boolean = true

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            ForgeOfTheAntichristModuleBase::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.Companion.MANAGED_FIELD_HOLDER
        )

        val FAIL_HOST_NOT_WORKING: RecipeResult = RecipeResult.fail(
            "gtladditions.recipe.fail.host_not_working".toComponent
        )

        val BEFORE_WORKING = Predicate { machine: IRecipeLogicMachine ->
            ((machine as ForgeOfTheAntichristModuleBase).host?.let { it -> return@let it.isActive } ?: false)
                .also { if (!it) RecipeResult.of(machine, FAIL_HOST_NOT_WORKING) }
        }

        fun copyAndModifyRecipe(recipe: GTRecipe, modifier: ContentModifier): GTRecipe {
            val copy = GTRecipe(
                recipe.recipeType,
                recipe.id,
                recipe.inputs,
                modifyOutputContents(recipe.outputs, modifier),
                recipe.tickInputs,
                recipe.tickOutputs,
                recipe.inputChanceLogics,
                recipe.outputChanceLogics,
                recipe.tickInputChanceLogics,
                recipe.tickOutputChanceLogics,
                recipe.conditions,
                recipe.ingredientActions,
                recipe.data,
                recipe.duration,
                recipe.isFuel
            )
            IGTRecipe.of(copy).realParallels = IGTRecipe.of(recipe).realParallels
            copy.ocTier = recipe.ocTier
            return copy
        }

        private fun modifyOutputContents(
            before: Map<RecipeCapability<*>, List<Content>>,
            modifier: ContentModifier
        ): Map<RecipeCapability<*>, List<Content>> {
            val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, List<Content>>()
            for (entry in before) {
                val cap = entry.key
                val contentList = entry.value
                val copyList = ObjectArrayList<Content>(contentList.size)
                for (content in contentList) {
                    copyList.add(content.copy(cap, modifier))
                }
                after[cap] = copyList
            }
            return after
        }

        open class ForgeOfTheAntichristModuleBaseLogic(
            parallel: ForgeOfTheAntichristModuleBase,
            beforeWorking: Predicate<IRecipeLogicMachine>
        ) : GTLAddMultipleWirelessRecipesLogic(parallel, beforeWorking) {
            override fun getMachine(): ForgeOfTheAntichristModuleBase = machine as ForgeOfTheAntichristModuleBase
            override fun getEuMultiplier(): Double =
                getMachine().host?.let { ForgeOfTheAntichrist.Companion.getEuReduction(it) * super.getEuMultiplier() }
                    ?: super.getEuMultiplier()

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                val modifier = ContentModifier.multiplier(getMachine().host!!.recipeOutputMultiply)

                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes,
                    machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel,
                    modifyRecipe = { recipe ->
                        if (enableModify(recipe.recipeType)) copyAndModifyRecipe(recipe, modifier) else recipe
                    }
                )
            }

            override fun checkRecipe(recipe: GTRecipe): Boolean = RecipeRunnerHelper.matchRecipe(machine, recipe)

            open fun enableModify(recipeType: GTRecipeType): Boolean = false
        }
    }
}