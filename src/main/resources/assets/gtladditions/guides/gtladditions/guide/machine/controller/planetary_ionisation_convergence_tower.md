---
navigation:
  title: Planetary Ionisation Convergence Tower
  icon: stone
  parent: controller/multiblock_controller.md
  position: 10
categories:
  - multiblock controller
item_ids:
  - gtladditions:planetary_ionisation_convergence_tower
---

# Planetary Ionisation Convergence Tower

<BlockImage id = "gtladditions:planetary_ionisation_convergence_tower" scale = "8"/>

* Only Titansteel-tier coils or above can be used.
* Each work cycle lasts 3 seconds.
* If energy still enters after the internal energy buffer is full, a massive explosion occurs centered on the machine.
* At the start of a work cycle, it generates an instant extremely high-power EU pulse for 1 tick into the internal energy buffer, then smoothly discharges lower power into the internal energy buffer during the remaining time.
* After the pulse ends, the internal energy buffer outputs power to the outside through dynamo hatches or laser source hatches.
* Stellar thermal containment tier affects internal energy buffer capacity.
> Basic: 54,120,000,000,000 EU \
> Advanced: 3,475,000,000,000,000 EU \
> Ultimate: 1,160,000,000,000,000,000 EU
* Coil tier affects the consumed fluid type, consumption per cycle, and generated power.
> Titansteel to Adamantium: <FluidLink id="gtceu:rhenium" /> 73728 mB, <FluidLink id="gtceu:ice" /> 8 KB, <ItemLink id="kubejs:space_drone_mk2" /> 2x10^-4 items \
> Instant - 4096A MAX, discharge - 16A MAX \
> Superpower Silicon Rock to Astralium: <FluidLink id="gtceu:promethium" /> 36864 mB, <FluidLink id="gtceu:liquid_helium" /> 4 KB, <ItemLink id="kubejs:space_drone_mk4" /> 1x10^-4 items \
> Instant - 524288A MAX, discharge - 256A MAX \
> Infinity to Eternity: <FluidLink id="gtceu:crystalmatrix" /> 9216 mB, <FluidLink id="kubejs:gelid_cryotheum" /> 1 KB, <ItemLink id="kubejs:space_drone_mk6" /> 2.5x10^-5 items \
> Instant - 268435456A MAX, discharge - 131072A MAX