package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gtladd.gtladditions.api.machine.data.ParallelData
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.utils.AntichristPosHelper
import com.gtladd.gtladditions.utils.StarGradient
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.utils.Registries.getItem
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ForgeOfTheAntichrist(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
        holder,
        GTLAddRecipesTypes.FORGE_OF_THE_ANTICHRIST,
        *args
    ), IModularMachineHost<ForgeOfTheAntichrist> {
    private val modules: MutableSet<IModularMachineModule<ForgeOfTheAntichrist, *>> =
        ReferenceOpenHashSet<IModularMachineModule<ForgeOfTheAntichrist, *>>()

    @field:Persisted
    @field:DescSynced
    var runningSecs: Long = 0
        private set
    private var mam = 0
    private var runningSecSubs: TickableSubscription? = null

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return ForgeOfTheAntichristLogic(this)
    }

    override fun getRecipeLogic(): ForgeOfTheAntichristLogic {
        return super.getRecipeLogic() as ForgeOfTheAntichristLogic
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (isFormed) textList.add(Component.translatable("tooltip.gtlcore.installed_module_count", getMAM()))
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

    // ========================================
    // HelioFusionExoticizer connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<ForgeOfTheAntichrist, *>> = modules
    override fun getModuleScanPositions(): Array<out BlockPos> = AntichristPosHelper.calculateModulePositions(pos, frontFacing)

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        safeClearModules()
    }

    override fun onMachineRemoved() {
        safeClearModules()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        safeClearModules()
        scanAndConnectModules()
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

    val recipeOutputMultiply: Double
        // 1 -> MAX_MULTIPLIER
        get() {
            val addition: Double = (MAX_OUTPUT_RATIO - 1) * (min(
                this.runningSecs,
                MAX_EFFICIENCY_SEC.toLong()
            ).toDouble() / MAX_EFFICIENCY_SEC).pow(2.0)
            return 1 + addition
        }

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        class ForgeOfTheAntichristLogic(parallel: ForgeOfTheAntichrist) :
            GTLAddMultipleTypeWirelessRecipesLogic(parallel) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): ForgeOfTheAntichrist {
                return super.getMachine() as ForgeOfTheAntichrist
            }

            override fun getEuMultiplier(): Double {
                return super.getEuMultiplier() * getEuReduction(getMachine())
            }

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                if (recipes.isEmpty()) return null

                val recipeList = ObjectArrayList<GTRecipe>(recipes.size)
                val parallelsList = LongArrayList(recipes.size)
                val modifier = ContentModifier.multiplier(getMachine().recipeOutputMultiply)

                for (recipe in recipes) {
                    recipe ?: continue
                    val modified = copyAndModifyRecipe(recipe, modifier)
                    val parallel = getMaxParallel(modified, Long.MAX_VALUE)
                    if (parallel > 0) {
                        recipeList.add(modified)
                        parallelsList.add(parallel)
                    }
                }

                return if (recipeList.isEmpty()) null
                else ParallelData(recipeList, parallelsList.toLongArray())
            }

            private fun copyAndModifyRecipe(recipe: GTRecipe, modifier: ContentModifier): GTRecipe {
                val copy = GTRecipe(
                    recipe.recipeType,
                    recipe.id,
                    modifyInputContents(recipe.inputs, modifier),
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

                    if (cap == ItemRecipeCapability.CAP) {
                        for (content in contentList) {
                            if (content.content is SizedIngredient && (content.content as SizedIngredient).items[0].item in cycleItems) {
                                copyList.add(content)
                            } else {
                                copyList.add(content.copy(ItemRecipeCapability.CAP, modifier))
                            }
                        }
                    } else {
                        for (content in contentList) {
                            copyList.add(content.copy(cap, modifier))
                        }
                    }
                    after[cap] = copyList
                }
                return after
            }

            private fun modifyInputContents(
                before: Map<RecipeCapability<*>, List<Content>>,
                modifier: ContentModifier
            ): Map<RecipeCapability<*>, List<Content>> {
                if (!before.containsKey(ItemRecipeCapability.CAP)) return before

                val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, List<Content>>()
                for (entry in before) {
                    val cap = entry.key
                    val contentList = entry.value

                    if (cap == ItemRecipeCapability.CAP) {
                        val copyList = ObjectArrayList<Content>(contentList.size)
                        for (content in contentList) {
                            if (content.content is SizedIngredient && (content.content as SizedIngredient).items[0].item in cycleItems) {
                                copyList.add(content.copy(ItemRecipeCapability.CAP, modifier))
                            } else {
                                copyList.add(content)
                            }
                        }
                        after[cap] = copyList
                    } else {
                        after[cap] = contentList
                    }
                }
                return after
            }

            companion object {
                val cycleItems = ObjectOpenHashSet<Item>(
                    arrayOf(
                        getItem("kubejs:extremely_durable_plasma_cell"),
                        getItem("kubejs:time_dilation_containment_unit"),
                        getItem("kubejs:plasma_containment_cell")
                    )
                )
            }
        }

        // 1 -> 1 / MAX_MULTIPLIER
        fun getEuReduction(machine: ForgeOfTheAntichrist): Double {
            return 1 - (1 - MIN_EU_RATIO) * (min(
                machine.runningSecs,
                MAX_EFFICIENCY_SEC.toLong()
            ).toDouble() / MAX_EFFICIENCY_SEC).pow(2.5)
        }

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            ForgeOfTheAntichrist::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )

        const val MAX_EFFICIENCY_SEC = 14400
        private const val MAX_OUTPUT_RATIO = 15
        private const val MIN_EU_RATIO = 0.2
    }
}
