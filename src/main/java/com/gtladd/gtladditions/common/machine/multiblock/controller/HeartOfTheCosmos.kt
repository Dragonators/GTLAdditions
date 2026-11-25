package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.api.machine.feature.TierCondition
import com.gtladd.gtladditions.api.machine.feature.TierCost
import com.gtladd.gtladditions.api.machine.feature.TierRequirement
import com.gtladd.gtladditions.api.machine.gui.StructureTierPage
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.data.StructureRenderType
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheUniverse.Companion.HeartOfTheUniverseLogic
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.HeartBlockLayout
import com.gtladd.gtladditions.common.machine.trait.StructureModifyTrait
import com.gtladd.gtladditions.common.machine.trait.StructureTierTrait
import com.gtladd.gtladditions.common.material.GTLAddMaterial
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.recipe.MachineUtil
import com.gtladd.gtladditions.utils.recipe.RecipeCalculationHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.machine.IGTLRenderDisabled
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLMaterials.DimensionallyTranscendentResidue
import org.gtlcore.gtlcore.common.data.GTLMaterials.ExcitedDtec
import org.gtlcore.gtlcore.common.data.GTLMaterials.ExcitedDtsc
import org.gtlcore.gtlcore.common.data.GTLMaterials.Miracle
import org.gtlcore.gtlcore.utils.Registries.getItem
import java.math.BigDecimal
import java.math.BigInteger

class HeartOfTheCosmos(holder: IMachineBlockEntity) :
    HeartOfTheUniverse(holder),
    IStructureTierMachine,
    IDropSaveMachine,
    IMachineLife {

    private val structureModify = StructureModifyTrait(this, StructureRenderType.HEART) {
        structureTierTrait.onStructureRestored()
    }

    @field:Persisted
    @field:DescSynced
    override val structureTierTrait: StructureTierTrait = StructureTierTrait(this, HeartBlockLayout.tiers, structureModify)

    @field:Persisted
    @field:DropSaved
    var annihilatedPlanes = 0L

    val structureTier: Int
        get() = structureTierTrait.structureTier

    // ========================================
    // Core overrides
    // ========================================

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = HeartOfTheCosmosLogic(this)

    override fun getRecipeLogic(): HeartOfTheCosmosLogic = super.getRecipeLogic() as HeartOfTheCosmosLogic

    override fun checkPattern(): Boolean = structureTierTrait.checkPattern()

    // ========================================
    // Structure tier
    // ========================================

    override fun tierRequirement(tier: Int): TierRequirement? = REQUIREMENTS.getOrNull(tier - 2)

    class PlaneCountCondition(private val threshold: Long) : TierCondition {
        override fun test(machine: IStructureTierMachine): Boolean = (machine as HeartOfTheCosmos).annihilatedPlanes >= threshold

        override fun describe(machine: IStructureTierMachine): Component =
            "gtladditions.structure_tier.condition.planes".toComponent(
                FormattingUtil.formatNumbers((machine as HeartOfTheCosmos).annihilatedPlanes),
                FormattingUtil.formatNumbers(threshold)
            )
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        super.onStructureFormed()
        structureTierTrait.onStructureFormed()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        structureTierTrait.onStructureInvalid()
    }

    override fun onMachineRemoved() {
        structureTierTrait.onMachineRemoved()
    }

    // ========================================
    // Persistence
    // ========================================

    override fun saveBreak(): Boolean = structureTierTrait.hasDropData || annihilatedPlanes > 0

    override fun savePickClone(): Boolean = false

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    // ========================================
    // GUI
    // ========================================

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        IGTLRenderDisabled.attachConfigurator(configuratorPanel, this as IGTLRenderDisabled)
    }

    override fun attachSideTabs(sideTabs: TabsWidget) {
        super.attachSideTabs(sideTabs)
        sideTabs.attachSubTab(StructureTierPage(this))
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        textList.add(
            "gtladditions.structure_tier.display"
                .toComponent(structureTier, structureTierTrait.layout.maxTier)
                .withStyle(ChatFormatting.AQUA)
        )
        textList.add(
            "gtladditions.structure_tier.planes"
                .toComponent(FormattingUtil.formatNumbers(annihilatedPlanes))
                .withStyle(ChatFormatting.DARK_RED)
        )
    }

    // ========================================
    // Recipe logic
    // ========================================

    companion object {
        val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            HeartOfTheCosmos::class.java,
            GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )

        private const val SEED_CHANCE = 8000
        const val AUXILIARY_PER_PLANE = 100L

        /** Best first: the array map iterates in insertion order. Values are the output chance out of 10000. */
        val RECOVERY_FLUIDS: Reference2IntArrayMap<Material> by lazy {
            Reference2IntArrayMap<Material>(3).apply {
                put(ExcitedDtsc, 9500)
                put(ExcitedDtec, 8500)
                put(DimensionallyTranscendentResidue, 7000)
            }
        }

        /** Best first; values are the power multiplier. */
        val POWER_FLUIDS: Reference2IntArrayMap<Material> by lazy {
            Reference2IntArrayMap<Material>(3).apply {
                put(Miracle, 8)
                put(GTLAddMaterial.PHONON_MEDIUM, 4)
                put(GTLAddMaterial.PROTO_MATTER, 2)
            }
        }

        private fun planeLevel(planes: Long): Int = when {
            planes >= 1L shl 22 -> 3
            planes >= 1L shl 21 -> 2
            planes >= 1L shl 20 -> 1
            else -> 0
        }

        private fun cost(id: String, amount: Long) = TierCost({ Ingredient.of(getItem(id)) }, amount)

        private val REQUIREMENTS = listOf(
            TierRequirement(
                listOf(
                    cost("gtmthings:creative_energy_hatch", 16),
                    cost("gtmthings:creative_laser_hatch", 16),
                    cost("gtlcore:max_field_generator", 64),
                    cost("gtladditions:astral_array", 16),
                    cost("gtladditions:heliothermal_plasma_fabricator", 1),
                    cost("gtlcore:create_casing", 64),
                    cost("gtladditions:wireless_energy_network_output_terminal", 16),
                    cost("gtladditions:super_parallel_hatch", 4),
                    cost("gtladditions:spacetime_lens", 16),
                    cost("gtladditions:primary_soc_wafer", 64)
                ),
                listOf(PlaneCountCondition(1L shl 16))
            ),
            TierRequirement(
                listOf(
                    cost("gtladditions:compressed_astral_array", 2),
                    cost("gtladditions:arcanic_astrograph", 8),
                    cost("kubejs:create_aggregatione_core", 32),
                    cost("kubejs:two_way_foil", 128),
                    cost("kubejs:cosmic_singularity", 64),
                    cost("gtceu:cosmic_nanoswarm", 64),
                    cost("gtladditions:relativistic_heat_capacitor", 16),
                    cost("gtladditions:create_data", 16),
                    cost("gtladditions:heliofusion_exoticizer", 1)
                ),
                listOf(PlaneCountCondition(1L shl 18))
            )
        )

        class HeartOfTheCosmosLogic(private val heart: HeartOfTheCosmos) : HeartOfTheUniverseLogic(heart) {
            private var neededStructureTier = 0

            override fun getGTRecipe(): GTRecipe? {
                neededStructureTier = 0
                val recipe = super.getGTRecipe()
                if (recipe == null && neededStructureTier > 0) {
                    RecipeResult.of(machine, RecipeResult.fail("gtladditions.structure_tier.recipe".toComponent(neededStructureTier)))
                }

                return recipe
            }

            override fun allows(recipe: GTRecipe): Boolean {
                val tier = recipe.data.getInt(IStructureTierMachine.RECIPE_TIER)
                if (tier <= heart.structureTier) return true
                if (neededStructureTier == 0 || tier < neededStructureTier) neededStructureTier = tier
                return false
            }

            // Outputs are rolled in buildFinalWirelessRecipe, once the recovery fluid has set their chance.
            override fun paralleledRecipe(recipe: GTRecipe, parallel: Long): GTRecipe = RecipeCalculationHelper.multipleRecipe(
                if (heart.structureTier == 3 && recipe.id == ANNIHILATION_RECIPE) {
                    val seed = GTLAddItems.BLACK_HOLE_SEED.asStack()
                    withItemChance(recipe, IO.IN, SEED_CHANCE shr planeLevel(heart.annihilatedPlanes)) {
                        ItemRecipeCapability.CAP.of(it.content).test(seed)
                    }
                } else {
                    recipe
                },
                parallel
            )

            override fun buildFinalWirelessRecipe(
                parallelData: ParallelData,
                wirelessTrait: IWirelessNetworkEnergyHandler
            ): WirelessGTRecipe {
                val itemOutputs = ObjectArrayList<Content>()
                val fluidOutputs = ObjectArrayList<Content>()
                var eu = BigInteger.ZERO
                var planes = 0L
                val tier = heart.structureTier
                val planeLevel = planeLevel(heart.annihilatedPlanes)

                for ((i, recipe) in parallelData.originRecipeList.withIndex()) {
                    val parallel = parallelData.parallels[i]
                    parallelData.addInputs(parallelData.processedRecipeList!![i].inputs)
                    var produced = recipe
                    var power = 1L

                    if (tier >= 2 && recipe.id == ANNIHILATION_RECIPE) {
                        drawFirst(RECOVERY_FLUIDS, parallel, parallelData)?.let { chance ->
                            produced = withItemChance(recipe, IO.OUT, chance) { it.chance < it.maxChance }
                        }

                        if (tier == 3) {
                            power = (1L shl planeLevel) * (drawFirst(POWER_FLUIDS, parallel, parallelData) ?: 1)
                        }
                    }

                    RecipeCalculationHelper.collectOutputs(
                        IParallelLogic.getRecipeOutputChance(machine, RecipeCalculationHelper.multipleRecipe(produced, parallel)),
                        itemOutputs,
                        fluidOutputs
                    )
                    eu += BigDecimal.valueOf(recipe.duration * euMultiplier)
                        .multiply(
                            BigDecimal(BigInteger.valueOf(getWirelessRecipeEut(recipe)).multiply(BigInteger.valueOf(parallel * power)))
                        )
                        .toBigInteger()
                    planes += parallel
                }

                heart.annihilatedPlanes += planes
                return buildWirelessRecipe(itemOutputs, fluidOutputs, eu.negate())
            }

            private fun drawFirst(fluids: Reference2IntArrayMap<Material>, planes: Long, parallelData: ParallelData): Int? {
                for (entry in fluids.reference2IntEntrySet()) {
                    val fluid = entry.key.getFluid(AUXILIARY_PER_PLANE * planes)
                    if (MachineUtil.inputFluid(heart, fluid)) {
                        parallelData.addInput(fluid)
                        return entry.intValue
                    }
                }

                return null
            }

            private fun withItemChance(recipe: GTRecipe, io: IO, chance: Int, matches: (Content) -> Boolean): GTRecipe =
                recipe.copy()
                    .also { RecipeExtensionCopier.copy(recipe, it) }
                    .apply {
                        (if (io == IO.IN) inputs else outputs)[ItemRecipeCapability.CAP]?.forEach {
                            if (matches(it)) it.chance = chance
                        }
                    }
        }
    }
}
