---
navigation:
  title: Helio Forge Modules
  icon: heliofusion_exoticizer
  parent: controller/multiblock_controller.md
  position: 47
categories:
  - multiblock controller
item_ids:
  - gtladditions:helioflare_power_forge
  - gtladditions:heliofluix_melting_core
  - gtladditions:heliofusion_exoticizer
  - gtladditions:heliophase_leyline_crystallizer
  - gtladditions:heliothermal_plasma_fabricator
---

# Helio Forge Modules

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

The Helio series are work modules for <ItemLink id="gtladditions:forge_of_the_antichrist" />. A module must be installed in a module position around the Forge of the Antichrist and connected to a formed, currently working host. If it is not connected or the host is not running, the module will not progress recipes.

* Each module draws power directly from the wireless energy network and does not need energy hatches or laser hatches.
* Modules have infinite parallel and infinite threads, as long as sufficient inputs, output space, and wireless energy are available.
* Modules share the EU reduction from the host's continuous operation.
* Some modules share the host's output multiplier for specified recipe categories. This multiplier comes from <ItemLink id="gtladditions:forge_of_the_antichrist" /> continuous operation efficiency and <ItemLink id="gtladditions:recursive_reverse_array" /> buffs.
* Module operation does not replace the host's own recipes. They are additional independent processing lines.

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:helioflare_power_forge" />

<BlockImage id="gtladditions:helioflare_power_forge" scale="4"/>

* Used for basic heat-treatment lines. It can process Furnace, Blast Furnace, Alloy Smelter, and Alloy Blast recipes.
* Runtime EU multiplier is 0.2, and it shares the host's EU reduction.
* When running Alloy Blast recipes, it shares the host's output multiplier.

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:heliofluix_melting_core" />

<BlockImage id="gtladditions:heliofluix_melting_core" scale="4"/>

* Used for chaotic phase transformation under stellar jets. It can process Chaotic Alchemy and Molecular Deconstruction recipes.
* Runtime EU multiplier is 0.2, and it shares the host's EU reduction.
* When running Chaotic Alchemy recipes, it shares the host's output multiplier. Molecular Deconstruction recipes only share EU reduction.

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:heliofusion_exoticizer" />

<BlockImage id="gtladditions:heliofusion_exoticizer" scale="4"/>

* Used for matter exoticization at extreme temperatures. It processes Matter Exotic recipes.
* Runtime EU multiplier is 0.5, and it shares both the host's EU reduction and output multiplier.
* This module locks and runs only one available recipe at a time. The currently locked recipe has infinite parallel.

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:heliothermal_plasma_fabricator" />

<BlockImage id="gtladditions:heliothermal_plasma_fabricator" scale="4"/>

* Used to inject stellar-grade thermal energy into materials. It can process Stellar Ignition, Fusion, and Super Particle Collider recipes.
* Runtime EU multiplier is 0.2, and it shares the host's EU reduction.
* When running Fusion or Super Particle Collider recipes, it shares the host's output multiplier. Stellar Ignition recipes only share EU reduction.

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:heliophase_leyline_crystallizer" />

<BlockImage id="gtladditions:heliophase_leyline_crystallizer" scale="4"/>

* Used to connect leyline corridors through stellar power. It processes Leyline Crystallize recipes.
* It only works when the connected <ItemLink id="gtladditions:forge_of_the_antichrist" /> has reached maximum efficiency and is currently running.
* Runtime EU multiplier is 256, and it shares the host's EU reduction.
* This module does not share the host's output multiplier.

</Column>

</Column>