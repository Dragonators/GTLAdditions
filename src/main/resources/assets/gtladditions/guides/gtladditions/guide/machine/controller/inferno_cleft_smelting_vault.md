---
navigation:
  title: Inferno Cleft Smelting Vault
  icon: inferno_cleft_smelting_vault
  parent: controller/multiblock_controller.md
  position: 32
categories:
  - multiblock controller
item_ids:
  - gtladditions:inferno_cleft_smelting_vault
---

# Inferno Cleft Smelting Vault

<BlockImage id = "gtladditions:inferno_cleft_smelting_vault" scale = "8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Inferno Cleft Smelting Vault can process pyrolysis and cracking recipes.
* The machine supports coil parallel, laser input, and cross-recipe mode.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.
* The structure requires a <Color color="#00AAAA">**UV**</Color> Muffler Hatch and uses thermal capacity coils for parallel calculation.

</Column>

<Column gap="2" fullWidth={true}>

### Parallel Calculation

* Inferno Cleft Smelting Vault's maximum parallel is determined by current coil temperature:

<Latex math = "Maximum\ parallel = \min(2147483647, 2^{\frac{Coil\ temperature}{900}})" />

* Coil temperature in the formula is the temperature of the coils installed in the structure.
* After forming, the machine UI displays the currently calculated maximum parallel.

</Column>

</Column>