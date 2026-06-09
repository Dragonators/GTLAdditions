package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.common.data.GCyMBlocks
import com.gregtechceu.gtceu.common.data.GTBlocks
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gtladd.gtladditions.api.machine.EBFChecks
import com.gtladd.gtladditions.api.machine.GTLAddPartAbility
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import com.gtladd.gtladditions.api.machine.mutable.AddMutableElectricParallelHatchMultiblockMachine
import com.gtladd.gtladditions.api.machine.mutable.MutableCoilElectricParallelHatchMultiblockMachine
import com.gtladd.gtladditions.api.machine.mutable.MutableElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.mutable.MutableElectricParallelHatchMultiblockMachine
import com.gtladd.gtladditions.api.pattern.patchPatternPredicates
import com.gtladd.gtladditions.api.pattern.patternPredicateSelector
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.ANTIENTROPY_CONDENSATION_CENTER
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.APOCALYPTIC_TORSION_QUANTUM_MATRIX
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.ARCANE_CACHE_VAULT
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.ASTRAL_CONVERGENCE_NEXUS
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.ATOMIC_TRANSMUTATIOON_CORE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.BIOLOGICAL_SIMULATION_LABORATORY
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.DRACONIC_COLLAPSE_CORE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.FUXI_BAGUA_HEAVEN_FORGING_FURNACE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.INFERNO_CLEFT_SMELTING_VAULT
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.LUCID_ETCHDREAMER
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEBULA_REAPER
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKI
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIV
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.QUANTUM_SYPHON_MATRIX
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.SKELETON_SHIFT_RIFT_ENGINE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.TITAN_CRIP_EARTHBORE
import com.gtladd.gtladditions.common.machine.multiblock.controller.OreProcessorMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.MutableFusionReactorMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.MutablePCBFactoryMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.MutableSuprachronalAssemblyLineMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.MutableTierCasingMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.ThreadMultiplierStrategy
import it.unimi.dsi.fastutil.longs.LongLongPair
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.common.block.GTLFusionCasingBlock
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineB
import java.util.function.BiConsumer
import kotlin.math.pow
import kotlin.math.roundToInt

object MutableMultiBlockModify {

    fun init() {
        val mutableMachines = listOf(
            MultiBlockMachineA.FISHING_GROUND,
            MultiBlockMachineA.LARGE_GREENHOUSE,
            MultiBlockMachineA.A_MASS_FABRICATOR,
            AdditionalMultiBlockMachine.HUGE_INCUBATOR,
            MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_MIXER,
            AdvancedMultiBlockMachine.SUPRACHRONAL_ASSEMBLY_LINE,
            MultiBlockMachineA.NANO_CORE,
            AdvancedMultiBlockMachine.COMPRESSED_FUSION_REACTOR[GTValues.UEV],
            MultiBlockMachineA.LARGE_RECYCLER,
            MultiBlockMachineA.ADVANCED_SPS_CRAFTING,
            MultiBlockMachineA.PETROCHEMICAL_PLANT,
            MultiBlockMachineB.WOOD_DISTILLATION,
            AdvancedMultiBlockMachine.PCB_FACTORY,
            AdditionalMultiBlockMachine.ADVANCED_RARE_EARTH_CENTRIFUGAL,
            MultiBlockMachineB.GRAVITATION_SHOCKBURST,
            AdditionalMultiBlockMachine.ADVANCED_NEUTRON_ACTIVATOR,
            MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE,
            MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT,
            MultiBlockMachineA.SUPER_PARTICLE_COLLIDER,
            MultiBlockMachineA.MATTER_FABRICATOR
        )

        val multipleMachines = listOf(
            MultiBlockMachineA.ADVANCED_INTEGRATED_ORE_PROCESSOR,
            MultiBlockMachineA.COOLING_TOWER,
            MultiBlockMachineA.MEGA_DISTILLERY,
            MultiBlockMachineA.HOLY_SEPARATOR,
            MultiBlockMachineA.FIELD_EXTRUDER_FACTORY,
            MultiBlockMachineA.MEGA_CANNER,
            MultiBlockMachineA.MEGA_WIREMILL,
            MultiBlockMachineA.MEGA_PRESSER,
            MultiBlockMachineA.MEGA_EXTRACTOR,
            MultiBlockMachineA.MEGA_FLUID_HEATER,
            MultiBlockMachineA.ADVANCED_MULTI_SMELTER,
            MultiBlockMachineA.SUPER_BLAST_SMELTER
        )

        val addDefinitions = listOf(
            NEXUS_SATELLITE_FACTORY_MKI,
            NEXUS_SATELLITE_FACTORY_MKII,
            NEXUS_SATELLITE_FACTORY_MKIII,
            NEXUS_SATELLITE_FACTORY_MKIV,
            LUCID_ETCHDREAMER,
            ATOMIC_TRANSMUTATIOON_CORE,
            ASTRAL_CONVERGENCE_NEXUS,
            NEBULA_REAPER,
            ARCANE_CACHE_VAULT,
            DRACONIC_COLLAPSE_CORE,
            TITAN_CRIP_EARTHBORE,
            BIOLOGICAL_SIMULATION_LABORATORY,
            DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT,
            QUANTUM_SYPHON_MATRIX,
            FUXI_BAGUA_HEAVEN_FORGING_FURNACE,
            ANTIENTROPY_CONDENSATION_CENTER,
            INFERNO_CLEFT_SMELTING_VAULT,
            SKELETON_SHIFT_RIFT_ENGINE,
            APOCALYPTIC_TORSION_QUANTUM_MATRIX,
            DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY
        )

        enableThreadModifier()

        setParallelHatchMutable(
            AdditionalMultiBlockMachine.HUGE_INCUBATOR,
            MultiBlockMachineA.ADVANCED_SPS_CRAFTING,
            MultiBlockMachineB.WOOD_DISTILLATION,
            AdditionalMultiBlockMachine.ADVANCED_RARE_EARTH_CENTRIFUGAL,
            MultiBlockMachineB.GRAVITATION_SHOCKBURST,
            MultiBlockMachineA.SUPER_PARTICLE_COLLIDER,
            MultiBlockMachineA.MATTER_FABRICATOR
        )
        setOtherMutable()

        val mutableWithExtra = mutableMachines + listOf(
            AdvancedMultiBlockMachine.CREATE_AGGREGATION,
            AdvancedMultiBlockMachine.DOOR_OF_CREATE,
            AdvancedMultiBlockMachine.ADVANCED_INFINITE_DRILLER
        )
        for (definition in mutableWithExtra) {
            addTooltips(
                definition,
                "gtladditions.multiblock.thread.tooltip.0".toComponent,
                "gtladditions.multiblock.thread.tooltip.1".toComponent(
                    ThreadMultiplierStrategy.getAdditionalMultiplier(definition).toString().literal
                        .withStyle(ChatFormatting.GOLD)
                ),
                when (definition) {
                    MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE ->
                        "gtladditions.multiblock.thread.below.tooltip.0".toComponent("IV")
                    MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT ->
                        "gtladditions.multiblock.thread.atomic_energy_excitation_plant.tooltip.0".toComponent
                    AdvancedMultiBlockMachine.PCB_FACTORY ->
                        "gtladditions.multiblock.thread.below.tooltip.0".toComponent("LuV")
                    else -> null
                }
            )
        }

        val multipleWithAdd = multipleMachines + addDefinitions
        for (definition in multipleWithAdd) {
            addTooltips(
                definition,
                "gtladditions.multiblock.thread.tooltip.2".toComponent,
                "gtladditions.multiblock.thread.tooltip.1".toComponent(
                    ThreadMultiplierStrategy.getAdditionalMultiplier(definition).toString().literal
                        .withStyle(ChatFormatting.GOLD)
                ),
                when (definition) {
                    DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT ->
                        "gtladditions.multiblock.thread.below.tooltip.0".toComponent("UV")
                    else -> null
                }
            )
        }
    }

    fun enableThreadModifier() {
        val threadModifierPredicate = { Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1) }
        val threadModifierTargets = mapOf(
            MultiBlockMachineA.FISHING_GROUND to patternPredicateSelector({ GTLBlocks.ALUMINIUM_BRONZE_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.LARGE_GREENHOUSE to patternPredicateSelector({ GTBlocks.CASING_STAINLESS_CLEAN.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.A_MASS_FABRICATOR to patternPredicateSelector({ GTBlocks.MACHINE_CASING_UXV.get() }, PartAbility.PARALLEL_HATCH),
            AdditionalMultiBlockMachine.HUGE_INCUBATOR to patternPredicateSelector({ GTBlocks.CASING_PTFE_INERT.get() }, PartAbility.INPUT_LASER),
            MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_MIXER to patternPredicateSelector({ GTLBlocks.DIMENSIONALLY_TRANSCENDENT_CASING.get() }, PartAbility.PARALLEL_HATCH),
            AdvancedMultiBlockMachine.SUPRACHRONAL_ASSEMBLY_LINE to patternPredicateSelector({ GTLBlocks.MOLECULAR_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.NANO_CORE to patternPredicateSelector({ GTLBlocks.NAQUADAH_ALLOY_CASING.get() }, PartAbility.INPUT_LASER),
            AdvancedMultiBlockMachine.COMPRESSED_FUSION_REACTOR[GTValues.UEV] to patternPredicateSelector({ GTLFusionCasingBlock.getCasingState(GTValues.UEV) }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.LARGE_RECYCLER to patternPredicateSelector({ GTBlocks.STEEL_HULL.get() }, PartAbility.MAINTENANCE),
            MultiBlockMachineA.ADVANCED_SPS_CRAFTING to patternPredicateSelector({ GTBlocks.FUSION_CASING_MK2.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.PETROCHEMICAL_PLANT to patternPredicateSelector({ GTBlocks.CASING_STAINLESS_CLEAN.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineB.WOOD_DISTILLATION to patternPredicateSelector({ GTBlocks.CASING_INVAR_HEATPROOF.get() }, PartAbility.PARALLEL_HATCH),
            AdvancedMultiBlockMachine.PCB_FACTORY to patternPredicateSelector({ GCyMBlocks.CASING_WATERTIGHT.get() }, PartAbility.PARALLEL_HATCH),
            AdditionalMultiBlockMachine.ADVANCED_RARE_EARTH_CENTRIFUGAL to patternPredicateSelector({ GTLBlocks.SPS_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineB.GRAVITATION_SHOCKBURST to patternPredicateSelector({ GTLBlocks.CREATE_CASING.get() }, PartAbility.PARALLEL_HATCH),
            AdditionalMultiBlockMachine.ADVANCED_NEUTRON_ACTIVATOR to patternPredicateSelector({ GTLBlocks.SPS_CASING.get() }, PartAbility.INPUT_LASER),
            MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE to patternPredicateSelector({ GTLBlocks.IRIDIUM_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT to patternPredicateSelector({ GTLBlocks.DIMENSIONALLY_TRANSCENDENT_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.SUPER_PARTICLE_COLLIDER to patternPredicateSelector({ GTLBlocks.LAFIUM_MECHANICAL_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.MATTER_FABRICATOR to patternPredicateSelector({ GTBlocks.HIGH_POWER_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.ADVANCED_INTEGRATED_ORE_PROCESSOR to patternPredicateSelector({ GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get() }, PartAbility.INPUT_LASER),
            MultiBlockMachineA.COOLING_TOWER to patternPredicateSelector({ GTBlocks.CASING_ALUMINIUM_FROSTPROOF.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.MEGA_DISTILLERY to patternPredicateSelector({ GTBlocks.CASING_STAINLESS_CLEAN.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.HOLY_SEPARATOR to patternPredicateSelector({ GTLBlocks.IRIDIUM_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.FIELD_EXTRUDER_FACTORY to patternPredicateSelector({ GTLBlocks.IRIDIUM_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.MEGA_CANNER to patternPredicateSelector({ GTLBlocks.LAFIUM_MECHANICAL_CASING.get() }, PartAbility.PARALLEL_HATCH),
            MultiBlockMachineA.MEGA_WIREMILL to patternPredicateSelector({ GTLBlocks.OXIDATION_RESISTANT_HASTELLOY_N_MECHANICAL_CASING.get() }, PartAbility.INPUT_LASER),
            MultiBlockMachineA.MEGA_PRESSER to patternPredicateSelector({ GTLBlocks.MOLECULAR_CASING.get() }, PartAbility.INPUT_LASER),
            MultiBlockMachineA.MEGA_EXTRACTOR to patternPredicateSelector({ GTLBlocks.HYPER_MECHANICAL_CASING.get() }, PartAbility.INPUT_LASER),
            MultiBlockMachineA.MEGA_FLUID_HEATER to patternPredicateSelector({ GTLBlocks.IRIDIUM_CASING.get() }, PartAbility.INPUT_LASER),
            MultiBlockMachineA.ADVANCED_MULTI_SMELTER to patternPredicateSelector({ GTBlocks.CASING_INVAR_HEATPROOF.get() }, PartAbility.INPUT_LASER),
            MultiBlockMachineA.SUPER_BLAST_SMELTER to patternPredicateSelector({ GCyMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get() }, PartAbility.PARALLEL_HATCH)
        )
        for ((definition, selector) in threadModifierTargets) {
            definition.patchPatternPredicates(
                "thread_modifier",
                selector,
                threadModifierPredicate
            )
        }
    }

    fun setParallelHatchMutable(vararg definitions: MultiblockMachineDefinition) {
        for (definition in definitions) {
            definition.setMachineSupplier { blockEntity: IMachineBlockEntity ->
                MutableElectricParallelHatchMultiblockMachine(blockEntity)
            }
        }
    }

    fun addTooltips(definition: MultiblockMachineDefinition, vararg newTooltips: Component?) {
        val oldBuilder = definition.tooltipBuilder
        definition.tooltipBuilder = BiConsumer { stack: ItemStack?, components: MutableList<Component> ->
            oldBuilder?.accept(stack, components)
            components.addAll(
                if (components.isNotEmpty()) components.size - 1 else 0,
                newTooltips.filterNotNull().toList()
            )
        }
    }

    fun setOtherMutable() {
        MultiBlockMachineA.FISHING_GROUND.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            AddMutableElectricParallelHatchMultiblockMachine(blockEntity)
        }

        MultiBlockMachineA.LARGE_GREENHOUSE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            AddMutableElectricParallelHatchMultiblockMachine(blockEntity)
        }

        MultiBlockMachineA.LARGE_RECYCLER.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricMultiblockMachine(blockEntity) {
                override fun getMaxParallel(): Int = 4.0.pow(tier - 4).roundToInt()
            }
        }

        MultiBlockMachineA.A_MASS_FABRICATOR.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun createRecipeLogic(vararg args: Any?): RecipeLogic = MutableRecipesLogic(this, 0.4)
            }
        }

        MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_MIXER.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun createRecipeLogic(vararg args: Any?): RecipeLogic = object : MutableRecipesLogic<MutableElectricParallelHatchMultiblockMachine>(this) {
                    override val euMultiplier: Double
                        get() = if (machine.recipeType == GTRecipeTypes.MIXER_RECIPES) super.euMultiplier * 0.2 else super.euMultiplier
                }
            }
        }

        AdvancedMultiBlockMachine.SUPRACHRONAL_ASSEMBLY_LINE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            MutableSuprachronalAssemblyLineMachine(blockEntity)
        }

        MultiBlockMachineA.NANO_CORE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun getMaxParallel(): Int = if (getRecipeLogic().isMultipleRecipeMode()) 67108864 else 8192

                override fun createRecipeLogic(vararg args: Any?): RecipeLogic = MutableRecipesLogic(this, 0.05)
            }
        }

        AdvancedMultiBlockMachine.COMPRESSED_FUSION_REACTOR[GTValues.UEV].setMachineSupplier { blockEntity: IMachineBlockEntity ->
            MutableFusionReactorMachine(blockEntity, GTValues.UEV)
        }

        MultiBlockMachineA.PETROCHEMICAL_PLANT.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            MutableCoilElectricParallelHatchMultiblockMachine(blockEntity)
        }

        AdvancedMultiBlockMachine.PCB_FACTORY.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            MutablePCBFactoryMachine(blockEntity)
        }

        MultiBlockMachineA.ADVANCED_INTEGRATED_ORE_PROCESSOR.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            OreProcessorMachine(blockEntity)
        }

        AdditionalMultiBlockMachine.ADVANCED_NEUTRON_ACTIVATOR.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun createRecipeLogic(vararg args: Any?): RecipeLogic = object : MutableRecipesLogic<MutableElectricParallelHatchMultiblockMachine>(this) {
                    override fun getRecipeEut(recipe: GTRecipe): Long = recipe.data.getInt("evt") * 2000L
                }
            }
        }

        MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableTierCasingMachine(blockEntity, "CATier") {
                override fun createRecipeLogic(vararg args: Any): RecipeLogic = object : MutableRecipesLogic<MutableTierCasingMachine>(this, TIER_CHECK) {
                    override fun calculateParallel(
                        machine: IRecipeLogicMachine,
                        match: GTRecipe,
                        remain: Long
                    ): LongLongPair = if (RecipeHelper.getInputEUt(match) <= GTValues.V[GTValues.IV]) {
                        LongLongPair.of(
                            IParallelLogic.getMaxParallel(
                                machine,
                                match,
                                Long.MAX_VALUE
                            ),
                            0
                        )
                    } else {
                        super.calculateParallel(machine, match, remain)
                    }
                }
            }
        }

        MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableCoilElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun createRecipeLogic(vararg args: Any?): RecipeLogic = object : MutableRecipesLogic<MutableCoilElectricParallelHatchMultiblockMachine>(
                    this,
                    EBFChecks.ATOMIC_ENERGY_EXCITATION_PLANT_CHECK
                ) {
                    override fun calculateParallel(
                        machine: IRecipeLogicMachine,
                        match: GTRecipe,
                        remain: Long
                    ): LongLongPair = if (match.recipeType == GTLRecipeTypes.FUEL_REFINING_RECIPES) {
                        LongLongPair.of(
                            IParallelLogic.getMaxParallel(
                                machine,
                                match,
                                Long.MAX_VALUE
                            ),
                            0
                        )
                    } else {
                        super.calculateParallel(machine, match, remain)
                    }
                }
            }
        }
    }
}