---
navigation:
  title: Subatomic Transmutatioon Core
  icon: subatomic_transmutatioon_core
  parent: controller/multiblock_controller.md
  position: 25
categories:
  - multiblock controller
item_ids:
  - gtladditions:subatomic_transmutatioon_core
---

# Subatomic Transmutatioon Core

<BlockImage id="gtladditions:subatomic_transmutatioon_core" scale="8" />

* The structure must install <ItemLink id="gtladditions:me_block_conservation" />.
* Only Titansteel-tier coils or above can be used.
* It requires an energy input hatch that can provide UHV voltage. Each working tick consumes fixed UHV-tier power.
* A conversion card or ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> must be placed in the card slot of the controller GUI. <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> is too unstable for this direct slot and is not accepted.
* After the structure forms, the machine runs continuously and performs block conversion at the end of each cycle.

## Conversion Cards

Parallel is calculated from the installed coil temperature. Let:

<Latex math = "base = \frac{Coil\ temperature}{100}" size="24" />

> Conversion Card: <Latex math = "Parallel = base^2" size="24" /> 60 ticks per cycle, converts one matching input per cycle \
> Advanced Conversion Card: <Latex math = "Parallel = base^{3.5}" size="24" /> 40 ticks per cycle, converts one matching input per cycle \
> Ultimate Conversion Card: <Latex math = "Parallel = base^{4.2}" size="24" /> 30 ticks per cycle, can continue converting multiple matching inputs in the same cycle \
> Astral Array: <Latex math = "Parallel = base^{6.35}" size="24" /> 20 ticks per cycle, can continue converting multiple matching inputs in the same cycle

<Row>
    <Recipe id="gtladditions:em_resonance_conversion_field/block.kubejs.draconium_block_charged" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.moss_block" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.warped_stem" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.sculk" />
</Row>

<Row>
    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.crimson_stem" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.bone_block" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.kubejs.essence_block" />
</Row>