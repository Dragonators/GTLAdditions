package com.gtladd.gtladditions.utils.recipe

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipeBuilder
import com.gtladd.gtladditions.common.data.ParallelData
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import it.unimi.dsi.fastutil.longs.LongLongPair
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.recipe.*
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.utils.Registries
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.roundToInt

object RecipeCalculationHelper {

    // ========================================
    // Process Recipe
    // ========================================

    inline fun multipleRecipe(
        recipe: GTRecipe,
        parallel: Long,
        crossinline copyRecipe: (GTRecipe) -> GTRecipe
    ): GTRecipe {
        val processed = if (parallel > 1) {
            copyRecipe(recipe).also { RecipeExtensionCopier.copy(recipe, it) }
        } else {
            recipe
        }
        IGTRecipe.of(processed).realParallels = parallel
        return processed
    }

    fun multipleRecipe(
        recipe: GTRecipe,
        parallel: Long
    ): GTRecipe = multipleRecipe(recipe, parallel) { recipe -> recipe.copy(IAdvancedContentModifier.preciseMultiplier(parallel), false) }

    inline fun processParallelDataNormal(
        parallelData: ParallelData,
        machine: IRecipeLogicMachine,
        maxEUt: Long,
        euMultiplier: Double,
        getTotalRecipeEu: (GTRecipe) -> Double,
        crossinline shouldBreak: (Double) -> Boolean = { totalEu -> totalEu / maxEUt > 20 * 500 }
    ): Triple<ObjectArrayList<Content>, ObjectArrayList<Content>, Double> {
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()
        var totalEu = 0.0

        for (i in parallelData.originRecipeList.indices) {
            val r = parallelData.originRecipeList[i]
            val p = parallelData.parallels[i]
            if (parallelData.shouldProcess) {
                val processedRecipe = IParallelLogic.getRecipeOutputChance(machine, multipleRecipe(r, p))

                if (RecipeRunnerHelper.matchRecipeInput(
                        machine,
                        processedRecipe
                    ) &&
                    RecipeRunnerHelper.handleRecipeInput(
                        machine,
                        processedRecipe
                    )
                ) {
                    totalEu += getTotalRecipeEu(r) * p * euMultiplier
                    parallelData.addInputs(processedRecipe.inputs)
                    collectOutputs(processedRecipe, itemOutputs, fluidOutputs)
                }

                if (shouldBreak(totalEu)) break
            } else {
                val processedRecipe = parallelData.processedRecipeList!![i]
                totalEu += getTotalRecipeEu(r) * p * euMultiplier
                parallelData.addInputs(processedRecipe.inputs)
                collectOutputs(processedRecipe, itemOutputs, fluidOutputs)
            }
        }

        return Triple(itemOutputs, fluidOutputs, totalEu)
    }

    inline fun processParallelDataWireless(
        parallelData: ParallelData,
        machine: IRecipeLogicMachine,
        maxTotalEu: BigInteger,
        euMultiplier: Double,
        getRecipeEut: (GTRecipe) -> Long,
        isEnergyConsumer: Boolean = true,
        crossinline onStarted: (GTRecipe, Long) -> Unit = { _, _ -> }
    ): Triple<ObjectArrayList<Content>, ObjectArrayList<Content>, BigInteger> {
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()
        var accumulatedEu = BigInteger.ZERO

        for (i in parallelData.originRecipeList.indices) {
            val r = parallelData.originRecipeList[i]
            val p = parallelData.parallels[i]

            var parallelEUt = BigInteger.valueOf(getRecipeEut(r))
            if (p > 1) parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p))

            val tempAccumulatedEu = accumulatedEu.add(
                BigDecimal.valueOf(r.duration * euMultiplier)
                    .multiply(BigDecimal(parallelEUt)).toBigInteger()
            )

            if (parallelData.shouldProcess) {
                if (isEnergyConsumer && tempAccumulatedEu > maxTotalEu) {
                    if (accumulatedEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                    break
                }

                val paralleledRecipe = IParallelLogic.getRecipeOutputChance(machine, multipleRecipe(r, p))

                if (RecipeRunnerHelper.matchRecipeInput(
                        machine,
                        paralleledRecipe
                    ) &&
                    RecipeRunnerHelper.handleRecipeInput(
                        machine,
                        paralleledRecipe
                    )
                ) {
                    accumulatedEu = tempAccumulatedEu
                    parallelData.addInputs(paralleledRecipe.inputs)
                    collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs)
                    onStarted(paralleledRecipe, p)
                }
            } else {
                val processedRecipe = parallelData.processedRecipeList!![i]
                accumulatedEu = tempAccumulatedEu
                parallelData.addInputs(processedRecipe.inputs)
                collectOutputs(processedRecipe, itemOutputs, fluidOutputs)
                onStarted(processedRecipe, p)
            }
        }

        val totalEu = if (isEnergyConsumer) accumulatedEu else accumulatedEu.negate()
        return Triple(itemOutputs, fluidOutputs, totalEu)
    }

    fun buildNormalRecipe(
        itemOutputs: List<Content>,
        fluidOutputs: List<Content>,
        totalEu: Double,
        maxEUt: Long,
        minDuration: Int
    ): GTRecipe {
        val recipe = GTRecipeBuilder.ofRaw().buildRawRecipe()
        recipe.outputs[ItemRecipeCapability.CAP] = itemOutputs
        recipe.outputs[FluidRecipeCapability.CAP] = fluidOutputs

        val d = totalEu / maxEUt
        val eut = if (d > minDuration) maxEUt else (totalEu / minDuration).toLong()
        recipe.tickInputs[EURecipeCapability.CAP] = listOf(Content(eut, 10000, 10000, 0, null, null))
        recipe.duration = maxOf(d, minDuration.toDouble()).roundToInt()
        IGTRecipe.of(recipe).setHasTick(true)
        IGTRecipe.of(recipe).isBatchProcessed = true
        return recipe
    }

    fun buildWirelessRecipe(
        itemOutputs: List<Content>,
        fluidOutputs: List<Content>,
        duration: Int,
        totalEu: BigInteger,
        recipeType: GTRecipeType = GTRecipeTypes.DUMMY_RECIPES
    ): WirelessGTRecipe {
        val eut = totalEu.divide(BigInteger.valueOf(duration.toLong())).negate()
        val recipe = WirelessGTRecipeBuilder
            .ofRaw(recipeType)
            .output(ItemRecipeCapability.CAP, itemOutputs)
            .output(FluidRecipeCapability.CAP, fluidOutputs)
            .duration(duration)
            .setWirelessEut(eut)
            .buildRawRecipe()
        IGTRecipe.of(recipe).isBatchProcessed = true
        return recipe
    }

    fun collectOutputs(
        recipe: GTRecipe,
        itemOutputs: MutableList<Content>,
        fluidOutputs: MutableList<Content>
    ) {
        recipe.outputs[ItemRecipeCapability.CAP]?.let { itemOutputs.addAll(it) }
        recipe.outputs[FluidRecipeCapability.CAP]?.let { fluidOutputs.addAll(it) }
    }

    fun hasOutputs(itemOutputs: List<Content>, fluidOutputs: List<Content>): Boolean = itemOutputs.isNotEmpty() || fluidOutputs.isNotEmpty()

    // ========================================
    // ParallelData Calculation
    // ========================================

    inline fun calculateParallelsWithFairAllocation(
        recipes: Collection<GTRecipe>,
        totalParallel: Long,
        crossinline getParallelAndIfConsumption: (GTRecipe) -> LongBooleanPair
    ): ParallelData? {
        val capacity = recipes.size
        if (capacity == 0) return null

        val recipeList = ObjectArrayList<GTRecipe>(capacity)
        val wants = LongArray(capacity)
        val budgeted = BooleanArray(capacity)
        var budgetedCount = 0
        var available = totalParallel
        var allFit = true

        for (recipe in recipes) {
            val pair = getParallelAndIfConsumption(recipe)
            val want = pair.firstLong()
            if (want <= 0) continue

            val index = recipeList.size
            recipeList.add(recipe)
            wants[index] = want
            budgeted[index] = pair.secondBoolean()
            if (budgeted[index]) {
                budgetedCount++
                if (allFit) {
                    if (want > available) {
                        allFit = false
                    } else {
                        available -= want
                    }
                }
            }
        }

        return positiveParallels(
            recipeList,
            FairShare.allocate(wants, budgeted, totalParallel, recipeList.size, budgetedCount, allFit)
        )
    }

    @PublishedApi
    internal fun positiveParallels(recipeList: ObjectArrayList<GTRecipe>, parallels: LongArray): ParallelData? {
        val count = recipeList.size
        var size = 0
        while (size < count && parallels[size] > 0) size++

        for (i in size + 1 until count) {
            if (parallels[i] > 0) {
                recipeList[size] = recipeList[i]
                parallels[size++] = parallels[i]
            }
        }

        if (size == 0) return null
        if (size < count) recipeList.size(size)

        val compact = when (size) {
            parallels.size -> parallels
            1 -> longArrayOf(parallels[0])
            else -> parallels.copyOf(size)
        }

        return ParallelData(recipeList, compact)
    }

    inline fun calculateParallelsWithGreedyAllocation(
        recipes: Collection<GTRecipe>,
        totalParallel: Long,
        machine: IRecipeLogicMachine,
        crossinline getParallelAndConsumption: (GTRecipe, Long) -> LongLongPair,
        crossinline createParalleledRecipe: (GTRecipe, Long) -> GTRecipe = { r, p -> IParallelLogic.getRecipeOutputChance(machine, multipleRecipe(r, p)) }
    ): ParallelData? {
        var remain = totalParallel

        val recipeList = ObjectArrayList<GTRecipe>()
        val processedRecipeList = ObjectArrayList<GTRecipe>()
        val parallelsList = LongArrayList()

        for (match in recipes) {
            if (remain <= 0) break
            val pair = getParallelAndConsumption(match, remain)
            val p = pair.firstLong()
            if (p <= 0) continue

            val paralleledRecipe = createParalleledRecipe(match, p)
            if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                remain -= pair.secondLong()
                recipeList.add(match)
                processedRecipeList.add(paralleledRecipe)
                parallelsList.add(p)
            }
        }

        return if (recipeList.isEmpty) {
            null
        } else {
            ParallelData(recipeList, parallelsList.toLongArray(), false, processedRecipeList)
        }
    }

    inline fun calculateParallelsWithProcessing(
        recipes: Collection<GTRecipe>,
        machine: IRecipeLogicMachine,
        crossinline getParallelLimitForRecipe: (GTRecipe) -> Long,
        crossinline getMaxParallelForRecipe: (GTRecipe, Long) -> Long,
        crossinline modifyRecipe: (GTRecipe) -> GTRecipe = { it },
        crossinline createParalleledRecipe: (GTRecipe, Long) -> GTRecipe = { r, p -> multipleRecipe(r, p) },
        useModifiedRecipe: Boolean = false,
        preProcessRecipes: Boolean = true
    ): ParallelData? {
        val length = recipes.size
        if (length == 0) return null

        val recipeList = ObjectArrayList<GTRecipe>(length)
        val processedRecipeList = if (preProcessRecipes) ObjectArrayList<GTRecipe>(length) else null
        val parallelsList = LongArrayList(length)

        for (recipe in recipes) {
            val modified = modifyRecipe(recipe)
            val limit = getParallelLimitForRecipe(modified)
            val parallel = getMaxParallelForRecipe(modified, limit)

            if (parallel > 0) {
                if (preProcessRecipes) {
                    val paralleledRecipe = IParallelLogic.getRecipeOutputChance(
                        machine,
                        createParalleledRecipe(modified, parallel)
                    )
                    if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                        recipeList.add(if (useModifiedRecipe) modified else recipe)
                        processedRecipeList!!.add(paralleledRecipe)
                        parallelsList.add(parallel)
                    }
                } else {
                    recipeList.add(if (useModifiedRecipe) modified else recipe)
                    parallelsList.add(parallel)
                }
            }
        }

        return if (recipeList.isEmpty) {
            null
        } else {
            ParallelData(recipeList, parallelsList.toLongArray(), !preProcessRecipes, processedRecipeList)
        }
    }

    // ========================================
    // Recipe Copy Boost Fix ( > 10000 max chance)
    // ========================================

    fun copyFixRecipe(origin: GTRecipe, modifier: ContentModifier): GTRecipe =
        RecipeExtensionCopier.copy(
            origin,
            GTRecipe(
                origin.recipeType,
                origin.id,
                copyFixContents(origin.inputs, modifier),
                copyFixContents(origin.outputs, modifier),
                copyFixContents(origin.tickInputs, modifier),
                copyFixContents(origin.tickOutputs, modifier),
                Reference2ReferenceArrayMap(origin.inputChanceLogics),
                Reference2ReferenceArrayMap(origin.outputChanceLogics),
                Reference2ReferenceArrayMap(origin.tickInputChanceLogics),
                Reference2ReferenceArrayMap(origin.tickOutputChanceLogics),
                ObjectArrayList(origin.conditions),
                ObjectArrayList(origin.ingredientActions),
                origin.data,
                origin.duration,
                origin.isFuel
            )
        )

    fun copyFixContents(
        contents: Map<RecipeCapability<*>, List<Content>>,
        modifier: ContentModifier
    ): Map<RecipeCapability<*>, List<Content>> =
        Reference2ReferenceArrayMap<RecipeCapability<*>, List<Content>>().apply {
            contents.forEach { (cap, contentList) ->
                if (contentList.isNotEmpty()) {
                    put(
                        cap,
                        ObjectArrayList(
                            contentList.map { content ->
                                copyFixBoost(content, cap, modifier)
                            }
                        )
                    )
                }
            }
        }

    fun copyFixBoost(
        content: Content,
        capability: RecipeCapability<*>,
        modifier: ContentModifier?
    ): Content {
        val newContent = if (content.chance != 0 && modifier != null) {
            capability.copyContent(content.content, modifier)
        } else {
            capability.copyContent(content.content)
        }

        return Content(
            newContent,
            content.chance,
            content.maxChance,
            0,
            content.slotName,
            content.uiName
        ).also {
            // Content's constructor rescales boosts for maxChance; copies must retain the actual field value.
            it.tierChanceBoost = content.tierChanceBoost
        }
    }

    // ========================================
    // Recycle Recipe
    // ========================================

    const val RECIPE_CYCLE_CONTAINER = "recipe_cycle_container"

    val RECIPE_CYCLE_CONTAINER_ITEM_IDS: Set<ResourceLocation> = setOf(
        ResourceLocation("kubejs", "extremely_durable_plasma_cell"),
        ResourceLocation("kubejs", "time_dilation_containment_unit"),
        ResourceLocation("kubejs", "plasma_containment_cell")
    )

    val RECIPE_CYCLE_CONTAINER_ITEMS: Set<Item> by lazy {
        RECIPE_CYCLE_CONTAINER_ITEM_IDS.mapTo(mutableSetOf()) { id -> Registries.getItem(id.toString()) }
    }

    val FORGE_OF_THE_ANTICHRIST_SPECIAL_INPUT_RULES: Map<ResourceLocation, Content> by lazy {
        mapOf(
            ResourceLocation("kubejs", "stellar_forge/contained_exotic_matter") to Content(
                LongIngredient.create(Ingredient.of(Registries.getItemStack("kubejs:time_dilation_containment_unit"))),
                ChanceLogic.getMaxChancedValue(),
                ChanceLogic.getMaxChancedValue(),
                0,
                null,
                null
            ),
            ResourceLocation("kubejs", "stellar_forge/extremely_durable_plasma_cell") to Content(
                LongIngredient.create(Ingredient.of(Registries.getItemStack("kubejs:extremely_durable_plasma_cell"))),
                ChanceLogic.getMaxChancedValue(),
                ChanceLogic.getMaxChancedValue(),
                0,
                null,
                null
            ),
            ResourceLocation("kubejs", "stellar_forge/contained_kerr_newmann_singularity") to Content(
                LongIngredient.create(Ingredient.of(Registries.getItemStack("kubejs:time_dilation_containment_unit"))),
                ChanceLogic.getMaxChancedValue(),
                ChanceLogic.getMaxChancedValue(),
                0,
                null,
                null
            )
        )
    }

    fun isRecipeCycleContainerItem(item: Item): Boolean = item in RECIPE_CYCLE_CONTAINER_ITEMS

    fun isRecipeCycleContainerContent(content: Content): Boolean = content.slotName == RECIPE_CYCLE_CONTAINER

    fun appendSpecificInput(copyList: MutableList<Content>, id: ResourceLocation, modifier: ContentModifier) {
        FORGE_OF_THE_ANTICHRIST_SPECIAL_INPUT_RULES[id]?.let {
            if (modifier.multiplier >= 2) {
                copyList.add(
                    it.copy(
                        ItemRecipeCapability.CAP,
                        ContentModifier.multiplier(modifier.multiplier - 1)
                    )
                )
            }
        }
    }
}
