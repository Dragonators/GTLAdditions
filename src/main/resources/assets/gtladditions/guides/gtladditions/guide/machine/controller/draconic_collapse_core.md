---
navigation:
  title: Draconic Collapse Core
  icon: draconic_collapse_core
  parent: controller/multiblock_controller.md
  position: 24
categories:
  - multiblock controller
item_ids:
  - gtladditions:draconic_collapse_core
---

# Draconic Collapse Core

<BlockImage id="gtladditions:draconic_collapse_core" scale="8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:draconic_collapse_core" /> processes Fusion Crafting recipes.
* It supports perfect overclocking, laser input, and parallel processing.

</Column>

<Column gap="2" fullWidth={true}>

### Hatch Support

* Supports laser input hatches. At least 1 is required in the structure, and up to 2 are accepted.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.
* Input positions only support normal item input buses, huge item input buses, <ItemLink id="gtladditions:me_super_pattern_buffer" />, and <ItemLink id="gtladditions:me_super_pattern_buffer_proxy" />.

</Column>

<Column gap="2" fullWidth={true}>

### Parallel Calculation

* Maximum parallel is determined by the machine voltage tier. For each tier above UEV, maximum parallel is multiplied by 8.

<Latex math = "Maximum\ parallel = 8^{Voltage\ tier - 10}" />

* UEV is counted as 10 in the formula. Therefore UEV is 1 parallel, UIV is 8 parallel, and UXV is 64 parallel.
* After forming, the UI displays the current parallel.

</Column>

</Column>