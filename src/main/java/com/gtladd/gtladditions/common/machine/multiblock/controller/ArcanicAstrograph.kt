package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.api.machine.IHarmonyMachineAccessor
import com.gtladd.gtladditions.common.machine.trait.AstralArrayCompressionTrait
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.handleRecipeOutput
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipe
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipeOutput
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.COSMOS_SIMULATION_RECIPES
import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

class ArcanicAstrograph(holder: IMachineBlockEntity, vararg args: Any?) :
    HarmonyMachine(holder, *args),
    IAstralArrayInteractionMachine,
    IHarmonyMachineAccessor {

    @field:Persisted
    @field:DescSynced
    override var astralArrayCount: Int = 0

    @field:Persisted
    @field:DescSynced
    var parallelAmount: Int = BASE_PARALLEL_AMOUNT
        private set

    @field:Persisted
    val astralArrayCompression: AstralArrayCompressionTrait = AstralArrayCompressionTrait(this)

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = ArcanicAstrographRecipeLogic(this)

    override fun getRecipeLogic(): ArcanicAstrographRecipeLogic = super.getRecipeLogic() as ArcanicAstrographRecipeLogic

    override fun getRecipeType(): GTRecipeType = COSMOS_SIMULATION_RECIPES

    override fun setWorkingEnabled(workingEnabled: Boolean) {
        if (!workingEnabled) astralArrayCompression.resetCompression()
        super.setWorkingEnabled(workingEnabled)
    }

    override fun addDisplayText(textList: MutableList<Component>) {
        super.addDisplayText(textList)
        if (isFormed) {
            textList.add(
                "tooltip.gtladditions.astral_array_count".toComponent(astralArrayCount.toString().literal.withStyle(ChatFormatting.GOLD))
            )
            textList.add(
                "tooltip.gtladditions.arcanic_parallel_amount".toComponent(parallelAmount.toString().literal.withStyle(ChatFormatting.GOLD))
            )
            textList.add(
                "tooltip.gtladditions.compressed_astral_array_output_chance".toComponent(
                    (FormattingUtil.DECIMAL_FORMAT_2F.format(astralArrayCompression.compressedAstralArrayOutputChance * 100) + "%")
                        .literal
                        .withStyle(ChatFormatting.AQUA)
                )
            )
        }
    }

    override fun increaseAstralArrayCount(amount: Int): Int {
        astralArrayCount += amount
        parallelAmount = calculateParallelAmount(astralArrayCount)
        return amount
    }

    override fun onWorking(): Boolean {
        if (!super.onWorking()) return false
        if (recipeLogic.lastRecipe?.recipeType == GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY &&
            recipeLogic.progress % 10 == 0
        ) {
            astralArrayCompression.handleCompressionWorking()
        }
        return true
    }

    override fun afterWorking() {
        val recipe = recipeLogic.lastRecipe
        if (recipe?.recipeType == GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY) {
            astralArrayCompression.finishCompression()
        }
        super.afterWorking()
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private const val BASE_PARALLEL_AMOUNT = 2048

        /**
         * Formula: parallelAmount = 2048 + 2^floor(log_{1.7}(8 * count)) * 128
         * Uses Long to prevent overflow, clamped to Int.MAX_VALUE
         */
        private fun calculateParallelAmount(count: Int): Int {
            if (count <= 0) return BASE_PARALLEL_AMOUNT

            val logBase17 = calculateParallelExponent(count)

            // 2^logBase17 * 128
            val additional = 2.0.pow(logBase17) * 128

            val result = BASE_PARALLEL_AMOUNT.toLong() + additional.toLong()
            return Ints.saturatedCast(result)
        }

        private fun calculateParallelExponent(count: Int): Int {
            // floor(log_{1.7}(8 * count)) = floor(ln(8 * count) / ln(1.7))
            return floor(ln(8.0 * count) / ln(1.7)).toInt()
        }

        private val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            ArcanicAstrograph::class.java,
            HarmonyMachine.MANAGED_FIELD_HOLDER
        )

        class ArcanicAstrographRecipeLogic(machine: ArcanicAstrograph) :
            RecipeLogic(machine),
            IRecipeStatus {
            override fun getMachine(): ArcanicAstrograph = super.getMachine() as ArcanicAstrograph

            override fun findAndHandleRecipe() {
                lastRecipe = null
                recipeStatus = null
                val match = getRecipe()
                val isCompress = match?.recipeType == GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY

                if (!isCompress) getMachine().astralArrayCompression.resetCompression()
                if (match == null) return

                RecipeResult.of(machine, RecipeResult.SUCCESS)
                if (!matchRecipeOutput(machine, match)) {
                    if (isCompress) {
                        getMachine().astralArrayCompression.resetCompression()
                    }
                    return
                }
                setupRecipe(match)
            }

            override fun onRecipeFinish() {
                val finishedCompressedRecipe = lastRecipe?.recipeType == GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY
                machine.afterWorking()
                lastRecipe?.let { handleRecipeOutput(machine, it) }

                val suspendableMachine = machine as? ISuspendableMachine
                if (suspendableMachine?.`gtlcore$isSuspendAfterFinish`() == true) {
                    if (finishedCompressedRecipe) getMachine().astralArrayCompression.resetCompression()
                    status = Status.SUSPEND
                    suspendableMachine.`gtlcore$setSuspendAfterFinish`(false)
                } else {
                    val nextRecipe = getRecipe()?.takeIf { matchRecipeOutput(machine, it) }
                    if (finishedCompressedRecipe &&
                        nextRecipe?.recipeType != GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY
                    ) {
                        getMachine().astralArrayCompression.resetCompression()
                    }
                    nextRecipe?.let {
                        setupRecipe(it)
                        return
                    }
                    status = Status.IDLE
                }
                progress = 0
                duration = 0
            }

            private fun getRecipe(): GTRecipe? {
                if (!machine.hasProxies()) return null
                val astrograph = getMachine()
                return findCosmosSimulationRecipe(astrograph) ?: findCompressedAstralArrayRecipe(astrograph)
            }

            private fun findCompressedAstralArrayRecipe(astrograph: ArcanicAstrograph): GTRecipe? {
                val recipe = GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY.lookup.find(astrograph, this::checkRecipe) ?: return null
                if (!astrograph.consumeAstralStartup()) return null
                return recipe
            }

            private fun findCosmosSimulationRecipe(astrograph: ArcanicAstrograph): GTRecipe? {
                val recipe = COSMOS_SIMULATION_RECIPES.lookup.find(astrograph, this::checkRecipe) ?: return null
                if (!astrograph.consumeCosmosStartup()) return null

                val harmonyRecipe = recipe.copy().also {
                    it.duration = astrograph.getHarmonyDuration()
                }

                return GTRecipeModifiers.accurateParallel(
                    astrograph,
                    harmonyRecipe,
                    astrograph.parallelAmount,
                    false
                ).getFirst()
            }

            private fun checkRecipe(recipe: GTRecipe): Boolean = matchRecipe(machine, recipe)
        }
    }
}