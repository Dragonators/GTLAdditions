package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.pattern.BlockPattern
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.utils.SupplierMemoizer
import com.gtladd.gtladditions.api.machine.EBFChecks
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import com.gtladd.gtladditions.api.machine.mutable.AddMutableElectricParallelHatchMultiblockMachine
import com.gtladd.gtladditions.api.machine.mutable.MutableCoilElectricParallelHatchMultiblockMachine
import com.gtladd.gtladditions.api.machine.mutable.MutableElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.mutable.MutableElectricParallelHatchMultiblockMachine
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.ANTIENTROPY_CONDENSATION_CENTER
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.APOCALYPTIC_TORSION_QUANTUM_MATRIX
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.ARCANE_CACHE_VAULT
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.ASTRAL_CONVERGENCE_NEXUS
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.ATOMIC_TRANSMUTATIOON_CORE
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.BIOLOGICAL_SIMULATION_LABORATORY
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.DRACONIC_COLLAPSE_CORE
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.FUXI_BAGUA_HEAVEN_FORGING_FURNACE
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.INFERNO_CLEFT_SMELTING_VAULT
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.LUCID_ETCHDREAMER
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEBULA_REAPER
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKI
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKII
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIII
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIV
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.QUANTUM_SYPHON_MATRIX
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.SKELETON_SHIFT_RIFT_ENGINE
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.TITAN_CRIP_EARTHBORE
import com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable.MutableFusionReactorMachine
import com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable.MutablePCBFactoryMachine
import com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable.MutableSuprachronalAssemblyLineMachine
import com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable.MutableTierCasingMachine
import com.gtladd.gtladditions.common.modify.multiblockMachine.*
import com.gtladd.gtladditions.utils.ThreadMultiplierStrategy
import it.unimi.dsi.fastutil.longs.LongLongPair
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineB
import java.util.function.BiConsumer
import java.util.function.Function
import kotlin.math.pow
import kotlin.math.roundToInt

object MutableMultiBlockModify {

    fun init() {
        val mutableMachines = mapOf(
            MultiBlockMachineA.FISHING_GROUND to MutableMultiBlocksA.FISHING_GROUND,
            MultiBlockMachineA.LARGE_GREENHOUSE to MutableMultiBlocksA.LARGE_GREENHOUSE,
            MultiBlockMachineA.A_MASS_FABRICATOR to MutableMultiBlocksA.A_MASS_FABRICATOR,
            AdditionalMultiBlockMachine.HUGE_INCUBATOR to MutableMultiBlocksB.HUGE_INCUBATOR,
            MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_MIXER to MutableMultiBlocksB.DIMENSIONALLY_TRANSCENDENT_MIXER,
            AdvancedMultiBlockMachine.SUPRACHRONAL_ASSEMBLY_LINE to MutableMultiBlocksB.SUPRACHRONAL_ASSEMBLY_LINE,
            MultiBlockMachineA.NANO_CORE to MutableMultiBlocksC.NANO_CORE,
            AdvancedMultiBlockMachine.COMPRESSED_FUSION_REACTOR[GTValues.UEV] to MutableMultiBlocksC.COMPRESSED_FUSION_REACTOR,
            MultiBlockMachineA.LARGE_RECYCLER to MutableMultiBlocksA.LARGE_RECYCLER,
            MultiBlockMachineA.ADVANCED_SPS_CRAFTING to MutableMultiBlocksA.ADVANCED_SPS_CRAFTING,
            MultiBlockMachineA.PETROCHEMICAL_PLANT to MutableMultiBlocksA.PETROCHEMICAL_PLANT,
            MultiBlockMachineB.WOOD_DISTILLATION to MutableMultiBlocksA.WOOD_DISTILLATION,
            AdvancedMultiBlockMachine.PCB_FACTORY to MutableMultiBlocksA.PCB_FACTORY,
            AdditionalMultiBlockMachine.ADVANCED_RARE_EARTH_CENTRIFUGAL to MutableMultiBlocksD.ADVANCED_RARE_EARTH_CENTRIFUGAL,
            MultiBlockMachineB.GRAVITATION_SHOCKBURST to MutableMultiBlocksA.GRAVITATION_SHOCKBURST,
            AdditionalMultiBlockMachine.ADVANCED_NEUTRON_ACTIVATOR to MutableMultiBlocksA.ADVANCED_NEUTRON_ACTIVATOR,
            MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE to MutableMultiBlocksD.COMPONENT_ASSEMBLY_LINE,
            MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT to MutableMultiBlocksA.ATOMIC_ENERGY_EXCITATION_PLANT,
            MultiBlockMachineA.SUPER_PARTICLE_COLLIDER to MutableMultiBlocksD.SUPER_PARTICLE_COLLIDER,
            MultiBlockMachineA.MATTER_FABRICATOR to MutableMultiBlocksD.MATTER_FABRICATOR
        )

        val multipleMachines = mapOf(
            MultiBlockMachineA.ADVANCED_INTEGRATED_ORE_PROCESSOR to MultiRecipeMultiBlocks.ADVANCED_INTEGRATED_ORE_PROCESSOR,
            MultiBlockMachineA.COOLING_TOWER to ElectricMultiRecipeMultiBlocks.COOLING_TOWER,
            MultiBlockMachineA.MEGA_DISTILLERY to ElectricMultiRecipeMultiBlocks.MEGA_DISTILLERY,
            MultiBlockMachineA.HOLY_SEPARATOR to ElectricMultiRecipeMultiBlocks.HOLY_SEPARATOR,
            MultiBlockMachineA.FIELD_EXTRUDER_FACTORY to ElectricMultiRecipeMultiBlocks.FIELD_EXTRUDER_FACTORY,
            MultiBlockMachineA.MEGA_CANNER to ElectricMultiRecipeMultiBlocks.MEGA_CANNER,
            MultiBlockMachineA.MEGA_WIREMILL to CoilMultiRecipeMultiBlocks.MEGA_WIREMILL,
            MultiBlockMachineA.MEGA_PRESSER to CoilMultiRecipeMultiBlocks.MEGA_PRESSER,
            MultiBlockMachineA.MEGA_EXTRACTOR to CoilMultiRecipeMultiBlocks.MEGA_EXTRACTOR,
            MultiBlockMachineA.MEGA_FLUID_HEATER to CoilMultiRecipeMultiBlocks.MEGA_FLUID_HEATER,
            MultiBlockMachineA.ADVANCED_MULTI_SMELTER to CoilMultiRecipeMultiBlocks.ADVANCED_MULTI_SMELTER,
            MultiBlockMachineA.SUPER_BLAST_SMELTER to CoilMultiRecipeMultiBlocks.SUPER_BLAST_SMELTER
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

        enableThreadModifier(mutableMachines + multipleMachines)

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

        val mutableWithExtra = mutableMachines.keys + listOf(
            AdvancedMultiBlockMachine.CREATE_AGGREGATION,
            AdvancedMultiBlockMachine.DOOR_OF_CREATE
        )
        for (definition in mutableWithExtra) {
            addTooltips(
                definition,
                Component.translatable("gtladditions.multiblock.thread.tooltip.0"),
                Component.translatable(
                    "gtladditions.multiblock.thread.tooltip.1",
                    Component.literal(ThreadMultiplierStrategy.getAdditionalMultiplier(definition).toString())
                        .withStyle(ChatFormatting.GOLD)
                ),
                when (definition) {
                    MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE ->
                        Component.translatable("gtladditions.multiblock.thread.component_assembly_line.tooltip.0")
                    MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT ->
                        Component.translatable("gtladditions.multiblock.thread.atomic_energy_excitation_plant.tooltip.0")
                    else -> null
                }
            )
        }

        val multipleWithAdd = multipleMachines.keys + addDefinitions
        for (definition in multipleWithAdd) {
            addTooltips(
                definition,
                Component.translatable("gtladditions.multiblock.thread.tooltip.2"),
                Component.translatable(
                    "gtladditions.multiblock.thread.tooltip.1",
                    Component.literal(ThreadMultiplierStrategy.getAdditionalMultiplier(definition).toString())
                        .withStyle(ChatFormatting.GOLD)
                )
            )
        }
    }

    fun enableThreadModifier(
        machinePatternMap: Map<MultiblockMachineDefinition, Function<MultiblockMachineDefinition, BlockPattern>>
    ) {
        for ((definition, pattern) in machinePatternMap) {
            definition.patternFactory = SupplierMemoizer.memoize { pattern.apply(definition) }
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
                override fun getMaxParallel(): Int {
                    return 4.0.pow(tier - 4).roundToInt()
                }
            }
        }

        MultiBlockMachineA.A_MASS_FABRICATOR.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
                    return MutableRecipesLogic(this, 0.4)
                }
            }
        }

        MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_MIXER.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
                    return object : MutableRecipesLogic<MutableElectricParallelHatchMultiblockMachine>(this) {
                        override val euMultiplier: Double
                            get() = if (machine.recipeType == GTRecipeTypes.MIXER_RECIPES) super.euMultiplier * 0.2 else super.euMultiplier
                    }
                }
            }
        }

        AdvancedMultiBlockMachine.SUPRACHRONAL_ASSEMBLY_LINE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            MutableSuprachronalAssemblyLineMachine(blockEntity)
        }

        MultiBlockMachineA.NANO_CORE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun getMaxParallel(): Int {
                    return 8192
                }

                override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
                    return MutableRecipesLogic(this, 0.05)
                }
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

        AdditionalMultiBlockMachine.ADVANCED_NEUTRON_ACTIVATOR.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
                    return object : MutableRecipesLogic<MutableElectricParallelHatchMultiblockMachine>(this) {
                        override fun getRecipeEut(recipe: GTRecipe): Long {
                            return recipe.data.getInt("evt") * 2000L
                        }
                    }
                }
            }
        }

        MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableTierCasingMachine(blockEntity, "CATier") {
                override fun createRecipeLogic(vararg args: Any): RecipeLogic {
                    return object : MutableRecipesLogic<MutableTierCasingMachine>(this, TIER_CHECK) {
                        override fun calculateParallel(
                            machine: IRecipeLogicMachine,
                            match: GTRecipe,
                            remain: Long
                        ): LongLongPair {
                            return if (RecipeHelper.getInputEUt(match) <= GTValues.V[GTValues.IV]) {
                                LongLongPair.of(
                                    IParallelLogic.getMaxParallel(
                                        machine,
                                        match,
                                        Long.MAX_VALUE
                                    ), 0
                                )
                            } else super.calculateParallel(machine, match, remain)
                        }
                    }
                }
            }
        }

        MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            object : MutableCoilElectricParallelHatchMultiblockMachine(blockEntity) {
                override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
                    return object : MutableRecipesLogic<MutableCoilElectricParallelHatchMultiblockMachine>(
                        this,
                        EBFChecks.ATOMIC_ENERGY_EXCITATION_PLANT_CHECK
                    ) {
                        override fun calculateParallel(
                            machine: IRecipeLogicMachine,
                            match: GTRecipe,
                            remain: Long
                        ): LongLongPair {
                            return if (match.recipeType == GTLRecipeTypes.FUEL_REFINING_RECIPES) {
                                LongLongPair.of(
                                    IParallelLogic.getMaxParallel(
                                        machine,
                                        match,
                                        Long.MAX_VALUE
                                    ), 0
                                )
                            } else super.calculateParallel(machine, match, remain)
                        }
                    }
                }
            }
        }
    }
}