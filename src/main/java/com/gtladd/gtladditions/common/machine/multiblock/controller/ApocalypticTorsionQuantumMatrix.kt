package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.IWirelessBindableSource
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine
import com.gtladd.gtladditions.api.recipe.ChanceParallelLogic
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class ApocalypticTorsionQuantumMatrix(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
        holder,
        GTLAddRecipesTypes.QUANTUM_OSCILLATION,
        *args
    ),
    IWirelessBindableSource<TimeSpaceDistorter> {

    @field:DescSynced
    @field:Persisted
    private var timeSpaceDistorterPos: BlockPos? = null

    private var timeSpaceDistorter: TimeSpaceDistorter? = null

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

    override fun onBound(target: TimeSpaceDistorter) {
        if (timeSpaceDistorter != target) {
            timeSpaceDistorter?.unbindSource()
        }
        timeSpaceDistorter = target
        timeSpaceDistorterPos = target.pos
    }

    override fun onUnbound(target: TimeSpaceDistorter?) {
        disconnectTSD(target, clearBinding = true)
    }

    fun disconnectTSD(target: TimeSpaceDistorter?, clearBinding: Boolean) {
        if (target != null && timeSpaceDistorter != target && timeSpaceDistorterPos != target.pos) return
        timeSpaceDistorter = null
        if (clearBinding) timeSpaceDistorterPos = null
    }

    private fun reconnectTSD(): Boolean {
        if (!isFormed) return false
        val distorterPos = timeSpaceDistorterPos ?: return false
        val machine = ((level as? ServerLevel)?.getBlockEntity(distorterPos) as? MetaMachineBlockEntity)
            ?.metaMachine as? TimeSpaceDistorter
            ?: return false
        if (!machine.isFormed()) return false
        return machine.bindResolvedSource(this).isSuccess
    }

    private fun resolveTSD(): TimeSpaceDistorter? {
        val cached = timeSpaceDistorter
        if (cached != null && cached.isFormed()) return cached
        timeSpaceDistorter = null
        if (!reconnectTSD()) return null
        return timeSpaceDistorter
    }

    fun consumeTSDAndGetMultiplier(totalParallels: Long): Double =
        resolveTSD()?.tryConsumeAndGetOutputMultiplier(totalParallels) ?: 1.0

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        super.onStructureFormed()
        reconnectTSD()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        timeSpaceDistorter?.disconnectATQM(clearBinding = false)
        timeSpaceDistorter = null
    }

    override fun onMachineRemoved() {
        timeSpaceDistorter?.unbindSource()
        timeSpaceDistorter = null
    }

    override fun onUnload() {
        super.onUnload()
        timeSpaceDistorter?.disconnectATQM(clearBinding = false)
        timeSpaceDistorter = null
    }

    // ========================================
    // Logic
    // ========================================

    companion object {
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
                                ContentModifier.multiplier(p.toDouble()),
                                INPUT_CHANCE_RATIO
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
                            for (content in contents) {
                                content.content = capability.copyContent(content.content, modifier)
                            }
                        }
                    }
                }

                private fun modifyContents(
                    before: Map<RecipeCapability<*>, MutableList<Content>>,
                    isInput: Boolean
                ): Reference2ReferenceOpenHashMap<RecipeCapability<*>, MutableList<Content>> {
                    val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, MutableList<Content>>()
                    for (entry in before) {
                        val cap = entry.key
                        val contentList = after.computeIfAbsent(cap) { ObjectArrayList() }
                        for (cont in entry.value) {
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
                    IGTRecipe.of(copy).realParallels = IGTRecipe.of(recipe).realParallels
                    copy.ocTier = recipe.ocTier
                    return copy
                }
            }
        }
    }
}