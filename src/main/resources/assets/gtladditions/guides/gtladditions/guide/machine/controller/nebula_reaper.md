---
navigation:
  title: Nebula Reaper
  icon: nebula_reaper
  parent: controller/multiblock_controller.md
  position: 30
categories:
  - multiblock controller
item_ids:
  - gtladditions:nebula_reaper
---

# Nebula Reaper

<BlockImage id="gtladditions:nebula_reaper" scale="8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:nebula_reaper" /> is a module machine for <ItemLink id="gtceu:space_elevator" /> and mainly processes space mining and space drilling recipes.
* It must establish a connection with <ItemLink id="gtceu:space_elevator" />. If the <ItemLink id="gtceu:space_elevator" /> does not satisfy its running conditions, the module will not continue progressing recipes.
* The machine supports laser power and cross-recipe parallelization.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.

</Column>

<Column gap="2" fullWidth={true}>

### Module Tier

* The maximum parallel of <ItemLink id="gtladditions:nebula_reaper" /> is determined by the tier provided by <ItemLink id="gtlcore:power_module_2" /> through <ItemLink id="gtlcore:power_module_7" /> installed in <ItemLink id="gtceu:space_elevator" />.
* Higher module tier grants this module a higher parallel limit.
* After forming, the UI displays whether it is currently connected to <ItemLink id="gtceu:space_elevator" /> and the available maximum parallel.

</Column>

</Column>