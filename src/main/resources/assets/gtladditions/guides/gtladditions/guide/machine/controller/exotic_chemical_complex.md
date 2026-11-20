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

* Exotic Chemical Complex processes Large Chemical Reactor recipes with coil parallel, cross-recipe parallelization, and a tiered expandable structure.
* It draws power directly from the wireless energy network.
* The structure requires a maintenance hatch and can install <ItemLink id="gtladditions:thread_modifier_hatch" />, located on the left and right sides of the controller.
* Input and output hatches can replace <ItemLink id="gtladditions:nanochip_casing" link={false} /> in their respective regions.
* It consists of a central tower and 12 outer process units, each with isolated item and fluid inputs and outputs.

</Column>

<Column gap="2" fullWidth={true}>

### Structure Tiers

The Exotic Chemical Complex has 3 structure tiers.

| Tier | Process units | Parallel rules | Recipe multipliers |
| --- | --- | --- | --- |
| 1 | Central tower only | All recipes share the total parallel pool | EU/t 1.0, duration 1.0 |
| 2 | Unlocks 12 outer process units | By default only <Color color="#00AAAA">**UV**</Color> and higher recipes use the total parallel pool | EU/t 0.8, duration 0.6 |
| 3 | As tier 2 | Unlocks infinite parallel | EU/t 0.8, duration 0.6 |

</Column>

<Column gap="2" fullWidth={true}>

### Upgrading

1. Open the Structure Upgrade page on the left of the controller GUI and choose the tier to view.
2. The left side lists the required items. Once the upgrade conditions are met, put the items into the input on the right and press Unlock to consume them and unlock the next tier.
3. Once the next tier is unlocked, its structure can be previewed in the world as usual or built with the terminal. After building it, press Activate to make it take effect.
4. Coils across different structure tiers must also be of the same type.
5. Unlocked tiers are kept when the controller is dismantled.

</Column>

<Column gap="2" fullWidth={true}>

### Parallel Allocation

* Base coil parallel is determined by coil temperature:

<Latex math = "Coil\ parallel = \min(2147483647, \lfloor 2^{\frac{Coil\ temperature}{900}} \rfloor)" />

* At tiers 1 and 2, the total parallel pool is determined by 256 base threads, extra threads from <ItemLink id="gtladditions:thread_modifier_hatch" />, and coil parallel:

<Latex math = "Total\ parallel\ pool = (256 + Extra\ threads) \times Coil\ parallel" />

* At tier 1, the central tower uses the entire pool and distributes parallel evenly across recipes.
* At tier 2, the total parallel pool is shared between the central tower and outer units with "Run independently" enabled:
  * By default only <Color color="#00AAAA">**UV**</Color> and higher recipes consume parallel; turning on "Count all recipes in parallel" limits all recipes in that unit to its configured parallel limit combined.
  * By default, the central tower uses the remaining parallel left over by independent units.
* At tier 3, both the central tower and independent units have infinite parallel; turning on "Count all recipes in parallel" limits that outer unit to its manually set parallel limit instead.
* Parallel limits can be adjusted in the input box or with the buttons on both sides and take effect automatically.

</Column>

<Column gap="2" fullWidth={true}>

### Process Units and Controls

* **Region isolation**: The central tower and 12 outer process units have isolated item and fluid hatch regions; central only reads and writes hatches on the central platform.
* **Independent mode and return**: From tier 2 onward, select an outer unit (01–12) on the main blueprint to toggle "Run independently". Turning it off returns an idle unit immediately and releases its parallel allocation; a busy unit returns after finishing and outputting its current run.
* **Status and products**: Select central or any independent unit to view its progress bar and "Products" list.

</Column>

</Column>
