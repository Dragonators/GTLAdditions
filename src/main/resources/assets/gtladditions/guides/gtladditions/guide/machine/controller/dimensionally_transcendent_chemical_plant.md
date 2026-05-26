---
navigation:
  title: Advanced Chemical Plant
  icon: dimensionally_transcendent_chemical_plant
  parent: controller/multiblock_controller.md
  position: 27
categories:
  - multiblock controller
item_ids:
  - gtladditions:dimensionally_transcendent_chemical_plant
---

# Advanced Chemical Plant

<BlockImage id = "gtladditions:dimensionally_transcendent_chemical_plant" scale = "8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* A cross-recipe version of the Large Chemical Reactor.
* It supports coil parallel and laser input.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.

</Column>

<Column gap="2" fullWidth={true}>

### Parallel Calculation

* The machine's base parallel is determined by coil temperature:

<Latex math = "Base\ parallel = \min(2147483647, \lfloor 2^{\frac{Coil\ temperature}{900}} \rfloor)" />

</Column>

<Column gap="2" fullWidth={true}>

### Cross-Recipe Allocation

* In cross-recipe mode, the machine allocates its total parallel budget to runnable chemical recipes.
* Recipes of <Color color="#00AAAA">**UV**</Color> tier or below do not consume the limited parallel budget. Recipes above <Color color="#00AAAA">**UV**</Color> occupy that budget.

</Column>

</Column>