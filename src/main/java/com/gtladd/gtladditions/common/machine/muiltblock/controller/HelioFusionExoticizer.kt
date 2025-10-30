package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.data.ParallelData
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.utils.AntichristPosHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import java.util.function.Predicate

class HelioFusionExoticizer(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleRecipesMachine(
        holder,
        *args
    ), IModularMachineModule<ForgeOfTheAntichrist, HelioFusionExoticizer> {

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

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return HelioFusionExoticizerLogic(
            this
        ) { machine -> (machine as HelioFusionExoticizer).host?.let { it -> return@let it.isActive } ?: false }
    }

    override fun getRecipeLogic(): HelioFusionExoticizerLogic {
        return super.getRecipeLogic() as HelioFusionExoticizerLogic
    }

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

    // ========================================
    // GUI
    // ========================================

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!this.isFormed) return
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
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        class HelioFusionExoticizerLogic(
            parallel: HelioFusionExoticizer,
            beforeWorking: Predicate<IRecipeLogicMachine>
        ) :
            GTLAddMultipleWirelessRecipesLogic(parallel, beforeWorking) {
            init {
                this.setReduction(0.5, 1.0)
            }

            override fun getMachine(): HelioFusionExoticizer = machine as HelioFusionExoticizer

            override fun getEuMultiplier(): Double =
                getMachine().host?.let { ForgeOfTheAntichrist.getEuReduction(it) * super.getEuMultiplier() }
                    ?: super.getEuMultiplier()

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                if (recipes.isEmpty()) return null

                recipes.first().let { recipe ->
                    val parallel = getMaxParallel(recipe, Long.MAX_VALUE)
                    if (parallel > 0) {
                        return ParallelData(
                            listOf(
                                copyAndModifyRecipe(
                                    recipe,
                                    ContentModifier.multiplier(getMachine().host!!.recipeOutputMultiply)
                                )
                            ), longArrayOf(parallel)
                        )
                    }
                }

                return null
            }

            override fun lookupRecipeIterator(): Set<GTRecipe> {
                lockRecipe?.let {
                    return if (checkRecipe(it)) setOf(it) else setOf()
                }

                return machine.recipeType.lookup
                    .find(machine) { recipe: GTRecipe -> checkRecipe(recipe) }
                    ?.also {
                        isLock = true
                        lockRecipe = it
                    }
                    ?.let { setOf(it) } ?: setOf()
            }
        }

        private fun copyAndModifyRecipe(recipe: GTRecipe, modifier: ContentModifier): GTRecipe {
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

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            HelioFusionExoticizer::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )
    }
}
