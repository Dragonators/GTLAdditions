---
navigation:
  title: Skeleton Shift Rift Engine
  icon: skeleton_shift_rift_engine
  parent: controller/multiblock_controller.md
  position: 33
categories:
  - multiblock controller
item_ids:
  - gtladditions:skeleton_shift_rift_engine
---

# Skeleton Shift Rift Engine

<BlockImage id="gtladditions:skeleton_shift_rift_engine" scale="8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:skeleton_shift_rift_engine" /> processes Decay Hastener recipes.
* It supports laser input and perfect overclocking.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.
* <ItemLink id="gtlcore:stellar_containment_casing" />, <ItemLink id="gtlcore:advanced_stellar_containment_casing" />, or <ItemLink id="gtlcore:ultimate_stellar_containment_casing" /> in the structure determine the stellar thermal containment tier.

</Column>

<Column gap="2" fullWidth={true}>

### Formulas

* For every 1200K increase in coil temperature, maximum parallel is multiplied by 2:

<Latex math = "Maximum\ parallel = \min(2147483647, \left\lfloor 2^{\frac{Coil\ temperature}{1200}} \right\rfloor)" />

* Higher stellar thermal containment tier shortens normal recipe base duration, down to a minimum of 1 tick:

<Latex math = "Base\ duration = \max(1, \left\lfloor \frac{Original\ recipe\ duration}{Stellar\ thermal\ containment\ tier} \right\rfloor)" />

* In cross-recipe mode, stellar thermal containment tier is converted into a total EU multiplier:

<Latex math = "EU\ multiplier = \frac{1}{\max(1, Stellar\ thermal\ containment\ tier)}" />

* The controller UI displays current parallel and stellar thermal containment tier. These values are recalculated after the structure reforms.

</Column>

</Column>