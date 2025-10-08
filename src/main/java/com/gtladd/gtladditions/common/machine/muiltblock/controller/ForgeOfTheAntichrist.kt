package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine
import com.gtladd.gtladditions.client.render.machine.StarGradient
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.network.chat.Component
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ForgeOfTheAntichrist(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
        holder,
        GTLAddRecipesTypes.FORGE_OF_THE_ANTICHRIST,
        *args
    ) {
    @field:Persisted
    @field:DescSynced
    var runningSecs: Long = 0
        private set

    private var runningSecSubs: TickableSubscription? = null

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return ForgeOfTheAntichristLogic(this)
    }

    override fun getRecipeLogic(): ForgeOfTheAntichristLogic {
        return super.getRecipeLogic() as ForgeOfTheAntichristLogic
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        textList.add(Component.translatable("gtceu.multiblock.parallel",
            GTLAddMachines.createRainbowComponent(
                Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
            )))
    }

    // ========================================
    // Running Time
    // ========================================
    override fun onLoad() {
        super.onLoad()
        (level as? ServerLevel)?.server?.tell(TickTask(0, ::updateRunningSecSubscription))
    }

    override fun onWorking(): Boolean {
        if (this.runningSecs == 0L) {
            this.runningSecs = 1
            this.updateRunningSecSubscription()
        }
        return super.onWorking()
    }

    private fun updateRunningSecSubscription() {
        if (this.runningSecs > 0) {
            this.runningSecSubs = this.subscribeServerTick(this.runningSecSubs, ::updateRunningSecs)
        } else if (this.runningSecSubs != null) {
            this.runningSecSubs!!.unsubscribe()
            this.runningSecSubs = null
        }
    }

    private fun updateRunningSecs() {
        if (this.offsetTimer % 20 == 0L) {
            if (this.recipeLogic.isWorking) this.runningSecs = max(runningSecs + 1, 0)
            else this.runningSecs = max(runningSecs - 16, 0)
        }

        this.updateRunningSecSubscription()
    }

    // ========================================
    // Utils
    // ========================================

    val radiusMultiplier: Float
        get() = (1 + 1.7 * (1.0 - exp(-runningSecs.toDouble() / MAX_EFFICIENCY_SEC))).toFloat()

    val rGBFromTime: Int
        get() = StarGradient.getRGBFromTime(
            max(
                0.0,
                min(
                    1.0,
                    1.0 - exp(-runningSecs.toDouble() / MAX_EFFICIENCY_SEC)
                )
            )
        )

    private val recipeOutputMultiply: Double
        // 1 -> MAX_MULTIPLIER
        get() {
            val addition: Double = (MAX_OUTPUT_RATIO - 1) * (min(
                this.runningSecs,
                MAX_EFFICIENCY_SEC.toLong()
            ).toDouble() / MAX_EFFICIENCY_SEC).pow(2.0)
            return 1 + addition
        }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    class ForgeOfTheAntichristLogic(parallel: ForgeOfTheAntichrist?) :
        GTLAddMultipleTypeWirelessRecipesLogic(parallel) {
        init {
            this.setReduction(0.1, 1.0)
        }

        override fun getMachine(): ForgeOfTheAntichrist? {
            return super.getMachine() as ForgeOfTheAntichrist?
        }

        // 1 -> 1 / MAX_MULTIPLIER
        override fun getEuMultiplier(): Double {
            val reduction: Double = 1 - (1 - MIN_EU_RATIO) * (min(
                getMachine()!!.runningSecs,
                MAX_EFFICIENCY_SEC.toLong()
            ).toDouble() / MAX_EFFICIENCY_SEC).pow(2.5)
            return super.getEuMultiplier() * reduction
        }

        override fun calculateParallels(): ParallelData? {
            val recipes = lookupRecipeIterator()
            if (recipes.isEmpty()) return null

            val recipeList = ObjectArrayList<GTRecipe>(recipes.size)
            val parallelsList = ObjectArrayList<Long>()

            for (recipe in recipes) {
                recipe ?: continue
                val parallel = getMaxParallel(recipe, Long.MAX_VALUE)
                if (parallel > 0) {
                    recipeList.add(recipe)
                    parallelsList.add(parallel)
                }
            }

            return if (recipeList.isEmpty()) null
                   else ParallelData(recipeList, parallelsList.toLongArray())
        }

        override fun modifyInputAndOutput(recipe: GTRecipe): GTRecipe {
            val modifiedRecipe = super.modifyInputAndOutput(recipe)
            val modifier = ContentModifier.multiplier(getMachine()!!.recipeOutputMultiply)
            for (entry in modifiedRecipe.outputs) {
                val contentList = entry.value
                val cap = entry.key
                if (contentList != null && !contentList.isEmpty()) {
                    val copy = ObjectArrayList<Content?>(contentList.size)

                    for (content in contentList) {
                        copy.add(content.copy(cap, modifier))
                    }

                    contentList.clear()
                    contentList.addAll(copy)
                }
            }
            return modifiedRecipe
        }
    }

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            ForgeOfTheAntichrist::class.java,
            GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine.MANAGED_FIELD_HOLDER
        )

        const val MAX_EFFICIENCY_SEC = 14400
        private const val MAX_OUTPUT_RATIO = 15
        private const val MIN_EU_RATIO = 0.2
    }
}
