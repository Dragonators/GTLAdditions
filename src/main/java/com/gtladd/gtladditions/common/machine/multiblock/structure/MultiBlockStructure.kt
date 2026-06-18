package com.gtladd.gtladditions.common.machine.multiblock.structure

import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection

object MultiBlockStructure {
    val EYE_OF_HARMONY_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/eye_of_harmony.bin", "eye_of_harmony")
    }

    val FUXI_BAGUA_HEAVEN_FORGING_FURNACE_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/fuxi_bagua_heaven_forging_furnace.bin", "fuxi_bagua_heaven_forging_furnace")
    }

    val BIOLOGICAL_SIMULATION_LABORATORY_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/biological_simulation_laboratory.bin", "biological_simulation_laboratory")
    }

    val TITAN_CRIP_EARTHBORE_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/titan_crip_earthbore.bin", "titan_crip_earthbore")
    }

    val DRACONIC_COLLAPSE_CORE_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/draconic_collapse_core.bin", "draconic_collapse_core")
    }

    val LUCID_ETCHDREAMER_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/lucid_etchdreamer.bin", "lucid_etchdreamer")
    }

    val QUANTUM_SYPHON_MATRIX_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/quantum_syphon_matrix.bin", "quantum_syphon_matrix")
    }

    val ANTIENTROPY_CONDENSATION_CENTER_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/antientropy_condensation_center.bin", "antientropy_condensation_center")
    }

    val TAIXU_TURBID_ARRAY_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/taixu_turbid_array.bin",
            "taixu_turbid_array",
            listOf(StructureResourceLoader.RepeatableAisle(10, 1, 16)),
            RelativeDirection.RIGHT,
            RelativeDirection.UP,
            RelativeDirection.BACK
        )
    }

    val INFERNO_CLEFT_SMELTING_VAULT: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/inferno_cleft_smelting_vault.bin", "inferno_cleft_smelting_vault")
    }

    val SKELETON_SHIFT_RIFT_ENGINE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/skeleton_shift_rift_engine.bin",
            "skeleton_shift_rift_engine",
            RelativeDirection.FRONT,
            RelativeDirection.UP,
            RelativeDirection.RIGHT
        )
    }

    val TIME_SPACE_DISTORTER_STRUCTURE: FactoryBlockPattern by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/time_space_distorter.bin", "time_space_distorter")
    }

    val APOCALYPTIC_TORSION_QUANTUM_MATRIX: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/apocalyptic_torsion_quantum_matrix.bin",
            "apocalyptic_torsion_quantum_matrix",
            RelativeDirection.LEFT,
            RelativeDirection.BACK,
            RelativeDirection.DOWN
        )
    }

    val FORGE_OF_THE_ANTICHRIST: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/forge_of_the_antichrist.bin",
            "forge_of_the_antichrist",
            RelativeDirection.LEFT,
            RelativeDirection.DOWN,
            RelativeDirection.BACK
        )
    }

    val ANNIHILATE_GENERATOR_STRUCTURE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/annihilate_generator.bin", "annihilate_generator")
    }

    val FORGE_OF_THE_ANTICHRIST_MODULE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/forge_of_the_antichrist_module.bin", "forge_of_the_antichrist_module")
    }

    val DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/dimension_focus_infinity_crafting_array.bin",
            "dimension_focus_infinity_crafting_array",
            RelativeDirection.LEFT,
            RelativeDirection.UP,
            RelativeDirection.BACK
        )
    }

    val SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/subspace_corridor_hub_industrial_array.bin",
            "subspace_corridor_hub_industrial_array",
            RelativeDirection.LEFT,
            RelativeDirection.UP,
            RelativeDirection.BACK
        )
    }

    val SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY_MODULE: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/subspace_corridor_hub_industrial_array_module.bin",
            "subspace_corridor_hub_industrial_array_module",
            RelativeDirection.LEFT,
            RelativeDirection.UP,
            RelativeDirection.BACK
        )
    }

    val SPACE_INFINITY_INTEGRATED_ORE_PROCESSOR: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/space_infinity_integrated_ore_processor.bin",
            "space_infinity_integrated_ore_processor",
            RelativeDirection.LEFT,
            RelativeDirection.BACK,
            RelativeDirection.DOWN
        )
    }

    val MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER: FactoryBlockPattern? by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/macro_atomic_resonant_fragment_stripper.bin",
            "macro_atomic_resonant_fragment_stripper",
            RelativeDirection.LEFT,
            RelativeDirection.BACK,
            RelativeDirection.DOWN
        )
    }

    val PLANETARY_IONISATION_CONVERGENCE_TOWER: FactoryBlockPattern by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/planetary_ionisation_convergence_tower.bin", "planetary_ionisation_convergence_tower")
    }

    val ARCANE_CACHE_VAULT_STRUCTURE: FactoryBlockPattern by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/arcane_cache_vault.bin", "arcane_cache_vault")
    }

    val SUBATOMIC_TRANSMUTATIOON_CORE: FactoryBlockPattern by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/subatomic_transmutatioon_core.bin", "subatomic_transmutatioon_core")
    }

    val RECURSIVE_REVERSE_FORGE_STRUCTURE: FactoryBlockPattern by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern("multiblock/recursive_reverse_forge.bin", "recursive_reverse_forge")
    }

    val RECURSIVE_REVERSE_FORGE_MODULE_STRUCTURE: FactoryBlockPattern by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadFactoryPattern(
            "multiblock/recursive_reverse_forge_module.bin",
            "recursive_reverse_forge_module",
            RelativeDirection.BACK,
            RelativeDirection.UP,
            RelativeDirection.RIGHT
        )
    }
}