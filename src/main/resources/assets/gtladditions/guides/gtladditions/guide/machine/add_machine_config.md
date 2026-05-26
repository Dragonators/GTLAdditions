---
navigation:
  title: GTLAdditions Machine Settings
  parent: machine/machine_index.md
  position: 4
categories:
  - machine
---

# GTLAdditions Machine Settings

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

This page describes the fixed processing settings used by GTLAdditions machines with cross-recipe, multi-recipe, or special parallel behavior. Even distribution mode and greedy mode are not player-selectable config options; they are fixed by machine registration and recipe logic.

Only minimum recipe duration is adjustable in some machine GUIs. Actual parallel is also affected by machine parallel, thread hatches, coils, modules, wireless energy, or machine-specific state.

</Column>

<Column gap="2" fullWidth={true}>

## Minimum Recipe Duration Setting

> Some GTLAdditions cross-recipe machines can adjust this through the minimum recipe duration button in the GUI.\
> Default value is 20t, with a minimum of 10t and a maximum of 200t.\
> It only controls the minimum duration of the final combined recipe. It does not decide whether the machine belongs to the even distribution setting, greedy setting, or special setting.

</Column>

<Column gap="2" fullWidth={true}>

## Even Distribution Mode

> Machines in even distribution mode attempt to process every recipe that can currently run.\
> When assigning parallel, the machine tries to spread available parallel as evenly as possible across each runnable recipe.\
> The total parallel limit is usually determined by machine parallel and thread count; some machines override base threads, base parallel, or per-recipe limits through their own mechanics.

GTLAdditions-owned machines:

<Row>
    <BlockImage id="gtladditions:lucid_etchdreamer" scale="3" />
    <BlockImage id="gtladditions:atomic_transmutation_core" scale="3" />
    <BlockImage id="gtladditions:astral_convergence_nexus" scale="3" />
    <BlockImage id="gtladditions:nebula_reaper" scale="3" />
    <BlockImage id="gtladditions:arcane_cache_vault" scale="3" />
</Row>

<Row>
    <BlockImage id="gtladditions:biological_simulation_laboratory" scale="3" />
    <BlockImage id="gtladditions:dimensionally_transcendent_chemical_plant" scale="3" />
    <BlockImage id="gtladditions:quantum_syphon_matrix" scale="3" />
    <BlockImage id="gtladditions:fuxi_bagua_heaven_forging_furnace" scale="3" />
    <BlockImage id="gtladditions:antientropy_condensation_center" scale="3" />
</Row>

<Row>
    <BlockImage id="gtladditions:taixu_turbid_array" scale="3" />
    <BlockImage id="gtladditions:inferno_cleft_smelting_vault" scale="3" />
    <BlockImage id="gtladditions:heart_of_the_universe" scale="3" />
    <BlockImage id="gtladditions:dimension_focus_infinity_crafting_array" scale="3" />
</Row>

External machines changed by this mod to use even distribution mode:

<Row>
    <BlockImage id="gtceu:fishing_ground" scale="3" />
    <BlockImage id="gtceu:large_greenhouse" scale="3" />
</Row>

</Column>

<Column gap="2" fullWidth={true}>

## Greedy Mode

> Machines in greedy mode attempt to spend the remaining parallel on the first matched recipe that can actually accept inputs.\
> Only when the current recipe cannot accept more parallel, or lacks inputs, does the machine continue to later recipes.\
> This mode is not meant to spread work evenly; it concentrates work into one or a few recipes whenever possible.

GTLAdditions-owned machines:

<Row>
    <BlockImage id="gtladditions:draconic_collapse_core" scale="3" />
    <BlockImage id="gtladditions:titan_crip_earthbore" scale="3" />
    <BlockImage id="gtladditions:skeleton_shift_rift_engine" scale="3" />
    <BlockImage id="gtladditions:apocalyptic_torsion_quantum_matrix" scale="3" />
</Row>

External machines changed by this mod to use the greedy setting:

<Row>
    <BlockImage id="gtceu:large_recycler" scale="3" />
    <BlockImage id="gtceu:a_mass_fabricator" scale="3" />
    <BlockImage id="gtceu:dimensionally_transcendent_mixer" scale="3" />
    <BlockImage id="gtceu:suprachronal_assembly_line" scale="3" />
    <BlockImage id="gtceu:nano_core" scale="3" />
</Row>

<Row>
    <BlockImage id="gtceu:uev_compressed_fusion_reactor" scale="3" />
    <BlockImage id="gtceu:petrochemical_plant" scale="3" />
    <BlockImage id="gtceu:pcb_factory" scale="3" />
    <BlockImage id="gtceu:advanced_integrated_ore_processor" scale="3" />
    <BlockImage id="gtceu:advanced_neutron_activator" scale="3" />
</Row>

<Row>
    <BlockImage id="gtceu:component_assembly_line" scale="3" />
    <BlockImage id="gtceu:atomic_energy_excitation_plant" scale="3" />
    <BlockImage id="gtceu:huge_incubator" scale="3" />
    <BlockImage id="gtceu:advanced_sps_crafting" scale="3" />
    <BlockImage id="gtceu:wood_distillation" scale="3" />
</Row>

<Row>
    <BlockImage id="gtceu:advanced_rare_earth_centrifugal" scale="3" />
    <BlockImage id="gtceu:gravitation_shockburst" scale="3" />
    <BlockImage id="gtceu:super_particle_collider" scale="3" />
    <BlockImage id="gtceu:matter_fabricator" scale="3" />
</Row>

GTLCore multi-recipe machines whose structure or thread hatch support is extended by this mod:

<Row>
    <BlockImage id="gtceu:cooling_tower" scale="3" />
    <BlockImage id="gtceu:mega_distillery" scale="3" />
    <BlockImage id="gtceu:holy_separator" scale="3" />
    <BlockImage id="gtceu:field_extruder_factory" scale="3" />
    <BlockImage id="gtceu:mega_canner" scale="3" />
</Row>

<Row>
    <BlockImage id="gtceu:mega_wiremill" scale="3" />
    <BlockImage id="gtceu:mega_presser" scale="3" />
    <BlockImage id="gtceu:mega_extractor" scale="3" />
    <BlockImage id="gtceu:mega_fluid_heater" scale="3" />
    <BlockImage id="gtceu:advanced_multi_smelter" scale="3" />
    <BlockImage id="gtceu:super_blast_smelter" scale="3" />
</Row>

</Column>

<Column gap="2" fullWidth={true}>

## Independent Parallel And Special Settings

> These machines are not just total-parallel even distribution or greedy assignment.\
> They calculate runnable recipes based on each recipe, module host state, wireless energy, special hatches, or machine-specific conditions.\
> Some of them behave as special infinite-parallel machines, while still being limited by input, output, energy, or machine state.

GTLAdditions-owned machines:

<Row>
    <BlockImage id="gtladditions:space_scaling_instrument" scale="3" />
    <BlockImage id="gtladditions:forge_of_the_antichrist" scale="3" />
    <BlockImage id="gtladditions:heliofusion_exoticizer" scale="3" />
    <BlockImage id="gtladditions:helioflare_power_forge" scale="3" />
    <BlockImage id="gtladditions:heliofluix_melting_core" scale="3" />
</Row>

<Row>
    <BlockImage id="gtladditions:heliothermal_plasma_fabricator" scale="3" />
    <BlockImage id="gtladditions:heliophase_leyline_crystallizer" scale="3" />
    <BlockImage id="gtladditions:light_hunter_space_station" scale="3" />
    <BlockImage id="gtladditions:nexus_satellite_factory_mk1" scale="3" />
    <BlockImage id="gtladditions:nexus_satellite_factory_mk2" scale="3" />
</Row>

<Row>
    <BlockImage id="gtladditions:nexus_satellite_factory_mk3" scale="3" />
    <BlockImage id="gtladditions:nexus_satellite_factory_mk4" scale="3" />
    <BlockImage id="gtladditions:space_infinity_integrated_ore_processor" scale="3" />
    <BlockImage id="gtladditions:macro_atomic_resonant_fragment_stripper" scale="3" />
</Row>

External machines with special handling added by this mod:

<Row>
    <BlockImage id="gtceu:integrated_ore_processor" scale="3" />
    <BlockImage id="gtceu:door_of_create" scale="3" />
    <BlockImage id="gtceu:create_aggregation" scale="3" />
    <BlockImage id="gtceu:molecular_assembler_matrix" scale="3" />
    <BlockImage id="gtceu:advanced_infinite_driller" scale="3" />
</Row>

Notes:

* Light Hunter Space Station modules use even distribution before the special station state is unlocked; after that, they calculate each recipe independently and use special per-recipe parallel limits.
* Forge of the Antichrist and its modules calculate runnable recipes separately, with behavior affected by the host's output multiplier, wireless energy, and module rules.
* Space Scaling Instrument, Space Infinity Integrated Ore Processor, and Macro Atomic Resonant Fragment Stripper calculate parallel independently for each recipe.
* Dimension Focus Infinity Crafting Array itself belongs to even distribution; as a Molecular Assembler Matrix module, it enables special infinity crafting behavior for that host.
* Door of Creation, Creative Aggregator, Integrated Ore Processor, and Advanced Infinite Driller are special external-machine modifications and should not be read as either of the first two fixed cross-recipe allocation settings.

</Column>

</Column>