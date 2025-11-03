package com.gtladd.gtladditions.common.machine.muiltblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.data.ParallelData
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.utils.AntichristPosHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper

open class ForgeOfTheAntichristModuleBase(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleRecipesMachine(
        holder,
        *args
    ), IModularMachineModule<ForgeOfTheAntichrist, ForgeOfTheAntichristModuleBase> {
    @field:Persisted
    private var hostPosition: BlockPos? = null
    private var host: ForgeOfTheAntichrist? = null

    override fun getHostPosition(): BlockPos? = hostPosition
    override fun setHostPosition(pos: BlockPos?) { hostPosition = pos }
    override fun getHost(): ForgeOfTheAntichrist? = host
    override fun setHost(host: ForgeOfTheAntichrist?) { this.host = host }
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

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!this.isFormed) return

        if (isConnectedToHost) {
            textList.add(
                if (host!!.runningSecs >= ForgeOfTheAntichrist.MAX_EFFICIENCY_SEC) {
                    GTLAddMachines.createRainbowComponent(
                        Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.achieve_max_efficiency").string
                    )
                } else {
                    Component.translatable(
                        "gtladditions.multiblock.forge_of_the_antichrist.output_multiplier",
                        GTLAddMachines.createRainbowComponent(FormattingUtil.DECIMAL_FORMAT_2F.format(host!!.recipeOutputMultiply))
                    )
                }
            )
        }

        textList.add(
            Component.translatable(
                if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed"
            )
        )
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        textList.add(
            Component.translatable(
                "gtceu.multiblock.parallel",
                GTLAddMachines.createRainbowComponent(
                    Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                )
            ).withStyle(ChatFormatting.GRAY)
        )
        textList.add(
            Component.translatable(
                "gtladditions.multiblock.threads",
                GTLAddMachines.createRainbowComponent(
                    Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            ForgeOfTheAntichristModuleBase::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.Companion.MANAGED_FIELD_HOLDER
        )

        val BEFORE_WORKING = Predicate { machine: IRecipeLogicMachine ->
            (machine as ForgeOfTheAntichristModuleBase).host?.let { it -> return@let it.isActive } ?: false
        }

        @JvmStatic
        protected fun copyAndModifyRecipe(recipe: GTRecipe, modifier: ContentModifier): GTRecipe {
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
            parallel: ForgeOfTheAntichristModuleBase
        ) : GTLAddMultipleWirelessRecipesLogic(parallel, BEFORE_WORKING) {
            override fun getMachine(): ForgeOfTheAntichristModuleBase = machine as ForgeOfTheAntichristModuleBase
            override fun getEuMultiplier(): Double =
                getMachine().host?.let { ForgeOfTheAntichrist.Companion.getEuReduction(it) * super.getEuMultiplier() }
                    ?: super.getEuMultiplier()

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                if (recipes.isEmpty()) return null

                val recipeList = ObjectArrayList<GTRecipe>(recipes.size)
                val parallelsList = LongArrayList(recipes.size)
                val modifier = ContentModifier.multiplier(getMachine().host!!.recipeOutputMultiply)

                for (recipe in recipes) {
                    recipe ?: continue
                    val modified = if (enableModify(recipe)) copyAndModifyRecipe(recipe, modifier) else recipe
                    val parallel = getMaxParallel(modified, Long.MAX_VALUE)
                    if (parallel > 0) {
                        recipeList.add(modified)
                        parallelsList.add(parallel)
                    }
                }

                return if (recipeList.isEmpty()) null
                else ParallelData(recipeList, parallelsList.toLongArray())
            }

            override fun checkRecipe(recipe: GTRecipe): Boolean {
                return RecipeRunnerHelper.matchRecipe(machine, recipe)
            }

            open fun enableModify(recipe: GTRecipe): Boolean {
                return false
            }
        }
    }
}