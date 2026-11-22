package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.ITimeSpaceDistorterSource
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine
import com.gtladd.gtladditions.api.recipe.ChanceParallelLogic
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.utils.recipe.RecipeCalculationHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import org.gtlcore.gtlcore.api.recipe.IAdvancedContentModifier
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class ApocalypticTorsionQuantumMatrix(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
        holder,
        GTLAddRecipesTypes.QUANTUM_OSCILLATION,
        *args
    ),
    ITimeSpaceDistorterSource {

    @field:Persisted
    override var timeSpaceDistorterPos: BlockPos? = null

    override var timeSpaceDistorter: TimeSpaceDistorter? = null

    // ========================================
    // Core overrides
    // ========================================

    override fun getMaxParallel(): Int = GTLRecipeModifiers.getHatchParallel(this)

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = ApocalypticTorsionQuantumMatrixLogic(this)

    override fun getRecipeLogic(): ApocalypticTorsionQuantumMatrixLogic = super.getRecipeLogic() as ApocalypticTorsionQuantumMatrixLogic

    override fun needConfirmMEStock(): Boolean = true

    // ========================================
    // Wireless binding
    // ========================================

    override val bindingType: ResourceLocation = BINDING_TYPE

    fun consumeTSDAndGetMultiplier(totalParallels: Long): Double =
        resolveTSD()?.tryConsumeAndGetOutputMultiplier(totalParallels) ?: 1.0

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        super.onStructureFormed()
        reconnectTarget()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        detachTarget()
    }

    override fun onMachineRemoved() {
        unbindTarget()
    }

    override fun onUnload() {
        super.onUnload()
        detachTarget()
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    // ========================================
    // Logic
    // ========================================

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            ApocalypticTorsionQuantumMatrix::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )
        val BINDING_TYPE: ResourceLocation = ResourceLocation("gtladditions", "apocalyptic_torsion_quantum_matrix")

        class ApocalypticTorsionQuantumMatrixLogic(parallel: ApocalypticTorsionQuantumMatrix) : GTLAddMultipleTypeWirelessRecipesLogic(parallel) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): ApocalypticTorsionQuantumMatrix = super.getMachine() as ApocalypticTorsionQuantumMatrix

            override fun getMultipleThreads(): Int = Ints.saturatedCast(MAX_THREADS + getMachine().getAdditionalThread())

            override fun getMaxParallel(recipe: GTRecipe, limit: Long): Long = ChanceParallelLogic.getMaxParallel(
                getMachine(),
                recipe,
                limit,
                chanceCaches,
                recipe.type.chanceFunction,
                IGTRecipe.of(recipe).euTier,
                getMachine().tier
            )

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                val totalParallel = getMachine().maxParallel.toLong() * getMultipleThreads()

                var remain = totalParallel
                var consumedParallels = 0L
                val recipeList = ObjectArrayList<GTRecipe>()
                val processedRecipeList = ObjectArrayList<GTRecipe>()
                val parallelsList = LongArrayList()

                for (match in recipes) {
                    if (remain <= 0L) break
                    val modified = modifyChance(match)
                    val p = getMaxParallel(modified, remain)
                    if (p <= 0L) continue

                    val paralleledRecipe = IParallelLogic.getRecipeOutputChance(
                        machine,
                        RecipeCalculationHelper.multipleRecipe(modified, p) { recipe ->
                            RecipeCalculationHelper.copyFixRecipe(
                                recipe,
                                IAdvancedContentModifier.preciseMultiplier(p)
                            )
                        }
                    )

                    if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                        remain -= p
                        consumedParallels += p
                        recipeList.add(match)
                        processedRecipeList.add(paralleledRecipe)
                        parallelsList.add(p)
                    }
                }

                if (recipeList.isEmpty) return null

                val multiplier = getMachine().consumeTSDAndGetMultiplier(consumedParallels)
                if (multiplier > 1.0) {
                    applyOutputMultiplier(processedRecipeList, multiplier)
                }

                return ParallelData(recipeList, parallelsList.toLongArray(), false, processedRecipeList)
            }

            companion object {
                private const val MAX_THREADS: Long = 1024
                private const val INPUT_CHANCE_RATIO = 10

                private fun applyOutputMultiplier(recipes: Iterable<GTRecipe>, multiplier: Double) {
                    val modifier = ContentModifier.multiplier(multiplier)
                    for (recipe in recipes) {
                        for ((capability, contents) in recipe.outputs) {
                            for (i in contents.indices) {
                                contents[i] = contents[i].copy(capability, modifier)
                            }
                        }
                    }
                }

                private fun modifyContents(
                    before: Map<RecipeCapability<*>, MutableList<Content>>,
                    isInput: Boolean
                ): Reference2ReferenceOpenHashMap<RecipeCapability<*>, MutableList<Content>> {
                    val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, MutableList<Content>>()

                    for ((cap, value) in before) {
                        val contentList = after.computeIfAbsent(cap) { ObjectArrayList() }

                        for (cont in value) {
                            if (cont.chance >= cont.maxChance) {
                                contentList.add(cont)
                            } else if (cont.chance != 0) {
                                val copy = cont.copy(cap, null)
                                if (isInput) {
                                    copy.maxChance = cont.maxChance * INPUT_CHANCE_RATIO
                                } else {
                                    copy.chance = cont.maxChance
                                }
                                contentList.add(copy)
                            }
                        }
                        if (contentList.isEmpty()) after.remove(cap)
                    }
                    return after
                }

                private fun modifyChance(recipe: GTRecipe): GTRecipe {
                    val copy = GTRecipe(
                        recipe.recipeType,
                        recipe.id,
                        modifyContents(recipe.inputs, true),
                        modifyContents(recipe.outputs, false),
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
                    RecipeExtensionCopier.copy(recipe, copy)
                    return copy
                }
            }
        }
    }
}