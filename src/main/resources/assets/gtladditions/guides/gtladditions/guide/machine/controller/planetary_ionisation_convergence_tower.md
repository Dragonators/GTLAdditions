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
> Titansteel to Adamantine: <FluidLink id="gtceu:rhenium" /> 73,728 mB, <FluidLink id="gtceu:ice" /> 8 KB, <ItemLink id="kubejs:space_drone_mk2" /> 2×10⁻⁴ items \
> Naquadriatic Taranium to Star Metal: <FluidLink id="gtceu:promethium" /> 36,864 mB, <FluidLink id="gtceu:liquid_helium" /> 4 KB, <ItemLink id="kubejs:space_drone_mk4" /> 1×10⁻⁴ items \
> Infinity to Eternity: <FluidLink id="gtceu:crystalmatrix" /> 9,216 mB, <FluidLink id="kubejs:gelid_cryotheum" /> 1 KB, <ItemLink id="kubejs:space_drone_mk6" /> 2.5×10⁻⁵ items \
>
> | Coil tier | Instantaneous (A MAX) | Discharge (A MAX) |
> |---|---:|---:|
> | Titansteel | 4,096 | 16 |
> | Adamantine | 32,768 | 128 |
> | Naquadriatic Taranium | 524,288 | 256 |
> | Star Metal | 4,194,304 | 2,048 |
> | Infinity | 8,388,608 | 4,096 |
> | Hypogen | 67,108,864 | 32,768 |
> | Eternity | 268,435,456 | 131,072 |
