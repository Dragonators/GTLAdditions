---
navigation:
  title: Biological Simulation Laboratory
  icon: biological_simulation_laboratory
  parent: controller/multiblock_controller.md
  position: 26
categories:
  - multiblock controller
item_ids:
  - gtladditions:biological_simulation_laboratory
---

# Biological Simulation Laboratory

<BlockImage id = "gtladditions:biological_simulation_laboratory" scale = "8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Biological Simulation Laboratory processes biological data simulation recipes for efficient biological drops.
* The lower-right internal item slot can only accept <ItemLink id="gtceu:rhenium_nanoswarm" />, <ItemLink id="gtceu:orichalcum_nanoswarm" />, <ItemLink id="gtceu:infuscolium_nanoswarm" />, or <ItemLink id="gtceu:nan_certificate" />.
* The structure can use laser hatches, but only accepts laser hatches of <Color color="#FFFF00">**UXV**</Color> tier or above.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />. After inserting <ItemLink id="gtceu:nan_certificate" /> to unlock cross-recipe mode, it can use the threads provided by that hatch.
* Without a boost item inserted, the machine starts with 64 parallel.

</Column>

<Column gap="2" fullWidth={true}>

### Nanoswarm Slot Bonus

> Insert <ItemLink id="gtceu:rhenium_nanoswarm" />: maximum parallel 2048, EU multiplier 0.9, duration multiplier 0.9.\
> Insert <ItemLink id="gtceu:orichalcum_nanoswarm" />: maximum parallel 16384, EU multiplier 0.8, duration multiplier 0.6.\
> Insert <ItemLink id="gtceu:infuscolium_nanoswarm" />: maximum parallel 262144, EU multiplier 0.6, duration multiplier 0.4.\
> Insert <ItemLink id="gtceu:nan_certificate" />: maximum parallel 4194304, EU multiplier 0.25, duration multiplier 0.1.

</Column>

<Column gap="2" fullWidth={true}>

### Certificate Bonus

> After inserting <ItemLink id="gtceu:nan_certificate" />, the machine unlocks cross-recipe parallel processing.\
> Biological simulation recipes containing <ItemLink id="avaritia:infinity_sword" /> also require this item to unlock.\
> Without this item, the machine only processes one available recipe.

</Column>

</Column>