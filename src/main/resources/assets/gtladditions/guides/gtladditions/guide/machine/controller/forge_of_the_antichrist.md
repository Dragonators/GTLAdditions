---
navigation:
  title: Forge of the Antichrist
  icon: forge_of_the_antichrist
  parent: controller/multiblock_controller.md
  position: 43
categories:
  - multiblock controller
item_ids:
  - gtladditions:forge_of_the_antichrist
---

# Forge of the Antichrist

<BlockImage id="gtladditions:forge_of_the_antichrist" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Forge of the Antichrist can process Dimensionally Transcendent Plasma Forge, Stellar Forge, and Ultimate Material Forge recipes in cross-recipe mode.
* The machine draws power directly from the wireless energy network and does not need any energy or laser hatches.
* Forge of the Antichrist has infinite parallel and infinite threads, as long as sufficient inputs, output space, and wireless energy are available.

</Column>

<Column gap="2" fullWidth={true}>

### Continuous Operation

* Forge of the Antichrist accumulates running time while it works continuously.
* The longer it runs, the higher its output multiplier becomes and the lower its EU multiplier becomes.
* After 4 hours of continuous operation, it reaches maximum efficiency: output can reach up to 15x, and EU consumption can drop to 0.2x.
* When the machine is idle, it gradually loses accumulated running time.

</Column>

<Column gap="2" fullWidth={true}>

### Container Recipe Multiplication

* When Forge of the Antichrist increases its output multiplier, ordinary item outputs and fluid outputs scale with the <Color color="#55FFFF">current output multiplier</Color>, but some containers are not simply duplicated.
* In Dimensionally Transcendent Smelting and Stellar Thermal Smelting recipes, <ItemLink id="kubejs:extremely_durable_plasma_cell" />, <ItemLink id="kubejs:time_dilation_containment_unit" />, and <ItemLink id="kubejs:plasma_containment_cell" /> are treated as cycle containers.
* If a cycle container appears on the input side, it is consumed according to the <Color color="#55FFFF">current output multiplier</Color>. If a cycle container appears on the output side, it keeps the original recipe amount and is not copied by the output multiplier.
* In practice, extra output requires extra containers, but returned empty containers or cycle containers are not created from nothing.

#### Recipes Affected By Cycle Containers

The recipes below are the original recipes. Forge of the Antichrist applies the container multiplication rules on top of them when it runs them.

Dimensionally Transcendent Smelting:

* Infinity Catalyst: <ItemLink id="kubejs:time_dilation_containment_unit" /> is a cycle-container output, so it stays at the original 64 and is not copied by the output multiplier.

<Recipe id="avaritia:dimensionally_transcendent_plasma_forge/infinity_catalyst" />

* Cosmic Neutron Plasma Cell: the input <ItemLink id="kubejs:extremely_durable_plasma_cell" /> is consumed according to the <Color color="#55FFFF">current output multiplier</Color>, while the output <ItemLink id="kubejs:cosmic_neutron_plasma_cell" /> scales with the output multiplier.

<Recipe id="kubejs:dimensionally_transcendent_plasma_forge/cosmic_neutron_plasma_cell" />

* Dense Neutron Plasma: <ItemLink id="kubejs:plasma_containment_cell" /> is a cycle-container output, so it keeps the original recipe amount and is not copied by the output multiplier.

<Recipe id="gtceu:dimensionally_transcendent_plasma_forge/dense_neutron_plasma" />

Stellar Thermal Smelting:

* Neutron Plasma Containment Cell: the input <ItemLink id="kubejs:plasma_containment_cell" /> is consumed according to the <Color color="#55FFFF">current output multiplier</Color>, and the output scales with the output multiplier.

<Recipe id="kubejs:stellar_forge/neutron_plasma_containment_cell" />

* Free Proton Gas: <ItemLink id="kubejs:time_dilation_containment_unit" /> is a cycle-container output, so it keeps the original recipe amount and is not copied by the output multiplier.

<Recipe id="gtceu:stellar_forge/free_proton_gas" />

* Legendarium Plasma: <ItemLink id="kubejs:plasma_containment_cell" /> is a cycle-container output, so it keeps the original recipe amount and is not copied by the output multiplier.

<Recipe id="gtceu:stellar_forge/legendarium_plasma" />

* Contained High Density Protonic Matter: the input <ItemLink id="kubejs:time_dilation_containment_unit" /> is consumed according to the <Color color="#55FFFF">current output multiplier</Color>, and the output scales with the output multiplier.

<Recipe id="kubejs:stellar_forge/contained_high_density_protonic_matter" />

* Contained Reissner-Nordstrom Singularity: the input <ItemLink id="kubejs:time_dilation_containment_unit" /> is consumed according to the <Color color="#55FFFF">current output multiplier</Color>, and the output scales with the output multiplier.

<Recipe id="kubejs:stellar_forge/contained_reissner_nordstrom_singularity" />

#### Recipes With Extra Added Inputs

* Contained Exotic Matter: in addition to the original recipe inputs, it consumes an extra amount equal to <Color color="#55FFFF">current output multiplier - 1</Color> times <ItemLink id="kubejs:time_dilation_containment_unit" />.

<Recipe id="kubejs:stellar_forge/contained_exotic_matter" />

* Extremely Durable Plasma Cell: in addition to the original recipe inputs, it consumes an extra amount equal to <Color color="#55FFFF">current output multiplier - 1</Color> times <ItemLink id="kubejs:extremely_durable_plasma_cell" />. The output <ItemLink id="kubejs:extremely_durable_plasma_cell" /> is a cycle container, so it keeps the original recipe amount and is not copied by the output multiplier.

<Recipe id="kubejs:stellar_forge/extremely_durable_plasma_cell" />

* Contained Kerr-Newman Singularity: in addition to the original recipe inputs, it consumes an extra amount equal to <Color color="#55FFFF">current output multiplier - 1</Color> times <ItemLink id="kubejs:time_dilation_containment_unit" />. The output <ItemLink id="kubejs:time_dilation_containment_unit" /> is a cycle container, so it stays at the original 63 and is not copied by the output multiplier.

<Recipe id="kubejs:stellar_forge/contained_kerr_newmann_singularity" />

* If the <Color color="#55FFFF">current output multiplier</Color> is below 2, the three recipes above do not add these extra inputs.

</Column>

<Column gap="2" fullWidth={true}>

### Helio Modules

* Forge of the Antichrist can connect Helio series modules to extend additional material processing lines.
* Modules must be installed in module positions around the host and connected to a Forge of the Antichrist that is running or anchored by <ItemLink id="gtladditions:spacetime_stasis_device" />. If they are not connected, or the host is neither running nor anchored, modules do not work.
* Connected modules share the EU reduction from the host's continuous operation. Some modules also share the host's output multiplier for specified recipe categories.
* <ItemLink id="gtladditions:heliophase_leyline_crystallizer" /> requires the host to have reached maximum efficiency before it can work.
* For the recipe categories and shared multiplier rules of the five modules, see the <ItemLink id="gtladditions:heliofusion_exoticizer" /> page.

</Column>

<Column gap="2" fullWidth={true}>

### Recursive Reversal

* Forge of the Antichrist can bind to <ItemLink id="gtladditions:recursive_reverse_array" /> and receive buffs from the Recursive Reverse Array and its modules.
* <ItemLink id="gtladditions:recursive_reverse_array" /> can help preserve running time while the Forge is idle, let Helio modules treat the idle host as anchored by <ItemLink id="gtladditions:spacetime_stasis_device" />, and under valid conditions further affect output, EU consumption, and some output handling.
* See the <ItemLink id="gtladditions:recursive_reverse_array" /> page for detailed module requirements and effects.

</Column>

</Column>