---
navigation:
  title: Lucid Etchdreamer
  icon: lucid_etchdreamer
  parent: controller/multiblock_controller.md
  position: 20
categories:
  - multiblock controller
item_ids:
  - gtladditions:lucid_etchdreamer
---

# Lucid Etchdreamer

<BlockImage id = "gtladditions:lucid_etchdreamer" scale = "8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Lucid Etchdreamer processes photonic crystal matrix etching recipes derived from laser engraving.
* The machine supports coil parallel, laser hatch input, and cross-recipe parallelization.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.
> Photonic crystal matrix etching recipes do not need computation input or data research.

</Column>

<Column gap="2" fullWidth={true}>

### Parallel Calculation

* The machine's base parallel is determined by coil temperature:

<Latex math = "Base\ parallel = \min(2147483647, \lfloor 2^{\frac{Coil\ temperature}{900}} \rfloor)" />

</Column>

</Column>