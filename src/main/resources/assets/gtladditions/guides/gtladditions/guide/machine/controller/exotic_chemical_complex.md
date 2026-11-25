---
navigation:
  title: Exotic Chemical Complex
  icon: exotic_chemical_complex
  parent: machine/controller/multiblock_controller.md
  position: 28
categories:
  - multiblock controller
item_ids:
  - gtladditions:exotic_chemical_complex
---

# Exotic Chemical Complex

<BlockImage id="gtladditions:exotic_chemical_complex" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* The Exotic Chemical Complex processes Large Chemical Reactor recipes. Tier 3 unlocks Deep Chemical Distortion recipes.
* It draws power directly from the wireless energy network.
* The structure requires a maintenance hatch and can install <ItemLink id="gtladditions:thread_modifier_hatch" />, located on the left and right sides of the controller.
* Input and output hatches can replace <ItemLink id="gtladditions:nanochip_casing" link={false} /> in their respective regions.
* It consists of the Synthesis Nexus and 12 outer process units, each with isolated item and fluid inputs and outputs.

</Column>

<Column gap="2" fullWidth={true}>

### Structure Tiers

The Exotic Chemical Complex has 3 structure tiers.

| Tier | Process units | Parallel rules | Recipe multipliers |
| --- | --- | --- | --- |
| 1 | Synthesis Nexus only | Standard parallel | EU/t 1.0, duration 1.0 |
| 2 | Unlocks 12 outer process units | Independent parallel per unit; by default only <Color color="#00AAAA">**UV**</Color> and higher recipes count | EU/t 0.8, duration 0.6 |
| 3 | Unlocks Deep Chemical Distortion recipes | Unlocks infinite parallel; outer units support manual limits | EU/t 0.8, duration 0.6 |

</Column>

<Column gap="2" fullWidth={true}>

### Upgrading

1. Open the "Structure Upgrade" tab in the controller GUI sidebar and switch to view the target tier and required materials.
2. With the required items in your inventory, click "Unlock (From Inventory)" to consume them and unlock the next tier.
3. Once the next tier is unlocked, its structure can be previewed in the world as usual or built with the terminal. After building it, click Activate to make it take effect.
4. Coils across different structure tiers must also be of the same type.
5. Unlocked tiers and payment progress are kept when the controller is dismantled.

</Column>

<Column gap="2" fullWidth={true}>

### Parallel Allocation

* Base coil parallel is determined by coil temperature:

<Latex math = "Coil\ parallel = \min(2147483647, \lfloor 2^{\frac{Coil\ temperature}{900}} \rfloor)" />

* At tiers 1 and 2, each unit's full parallel capacity is determined by 256 base threads, extra threads from the <ItemLink id="gtladditions:thread_modifier_hatch" />, and coil parallel.

<Latex math = "Unit\ capacity = (256 + Extra\ threads) \times Coil\ parallel" />

* At tier 1, the Synthesis Nexus distributes its parallel capacity evenly across all recipes.
* At tier 2, the Synthesis Nexus and each outer unit have their own full parallel capacity. By default, only <Color color="#00AAAA">**UV**</Color> and higher recipes consume parallel.
  * Enabling "Count all recipes in parallel" applies this parallel limit to recipes of all tiers in that unit.
* At tier 3, all units have infinite parallel.
* Parallel limits can be adjusted in the input box or with the buttons on both sides and take effect automatically.

</Column>

<Column gap="2" fullWidth={true}>

### Process Units and Controls

* The Synthesis Nexus and the 12 outer process units have isolated item and fluid hatches.
* Select an unlocked unit on the blueprint to view its progress and "Products", or individually use "Pause"/"Resume" or "Pause after".

</Column>

<Column gap="2" fullWidth={true}>

### Recipe Types and Time Space Distortion

* Tiers 1 and 2 are limited to Large Chemical Reactor recipes. Tier 3 adds the option for Deep Chemical Distortion recipes.
* The <ItemLink id="gtceu:me_recipe_pattern_buffer" /> installed on a unit hatch generates integrated patterns for that unit's active recipe type, refreshing automatically after a mode or structure tier change.
* Use a <ItemLink id="gtladditions:suprachronal_data_module" link={false} /> to bind a <ItemLink id="gtladditions:time_space_distorter" />. Tier 3 Distortion units can gain a 1.3×–3.2× boost to item and fluid outputs.
* Each batch is calculated from the parallels that actually started, with a fixed material cost of 0.1× the Distorter's base material cost.

</Column>

</Column>
