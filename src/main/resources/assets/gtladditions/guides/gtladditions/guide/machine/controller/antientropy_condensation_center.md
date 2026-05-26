---
navigation:
  title: Antientropy Condensation Center
  icon: antientropy_condensation_center
  parent: controller/multiblock_controller.md
  position: 30
categories:
  - multiblock controller
item_ids:
  - gtladditions:antientropy_condensation_center
  - gtladditions:relativistic_heat_capacitor
---

# Antientropy Condensation Center

<BlockImage id = "gtladditions:antientropy_condensation_center" scale = "4"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Antientropy Condensation Center processes Plasma Condensation and Vacuum Freezer recipes.
* It no longer requires extra <FluidLink id="gtceu:liquid_helium" /> input during normal cooling.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.

</Column>

<Column gap="2" fullWidth={true}>

### Condensation Consumption

* Before each work attempt, it consumes <ItemLink id="kubejs:dust_cryotheum" /> from the input side.
* The consumed amount is calculated from the total parallel of this work cycle:

<Latex math = "Consumption = \frac{5 * (\frac{Total\ parallel}{524288} + 51 * \ln(Total\ parallel))}{\max(1, Voltage\ tier - 9)}" />

* The machine UI displays the <ItemLink id="kubejs:dust_cryotheum" /> requirement calculated during the previous work attempt.

</Column>

<Column gap="2" fullWidth={true}>

### Heat Capacitor Embedding

* Right-click the controller while holding a stack of <ItemLink id="gtladditions:relativistic_heat_capacitor" /> to embed it into the Antientropy Condensation Center.
* After embedding, the EU multiplier becomes 0.35.
* After embedding, a work cycle consumes at most 1 <ItemLink id="kubejs:dust_cryotheum" />. If the total parallel of this cycle does not exceed INT.MAX, it consumes no <ItemLink id="kubejs:dust_cryotheum" />.
* When the controller is broken, the embedded stack of <ItemLink id="gtladditions:relativistic_heat_capacitor" /> is returned.

</Column>

</Column>