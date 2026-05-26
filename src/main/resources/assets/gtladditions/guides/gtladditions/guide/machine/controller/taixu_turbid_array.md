---
navigation:
  title: Taixu Turbid Array
  icon: taixu_turbid_array
  parent: controller/multiblock_controller.md
  position: 31
categories:
  - multiblock controller
item_ids:
  - gtladditions:taixu_turbid_array
---

# Taixu Turbid Array

<BlockImage id = "gtladditions:taixu_turbid_array" scale = "8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Taixu Turbid Array processes Chaos Weaving recipes, serving as a higher-tier replacement for <ItemLink id="gtceu:large_recycler" link={false} />.
* The structure can only use one laser hatch.
* Machine energy use is fixed at 524288 A at the current voltage.
* Normal mode has a fixed duration of 5 seconds. In cross-recipe mode, final recipe duration is fixed to 1 second and uses an additional 16x total EU.
* The machine has a fixed 128 cross-recipe threads.

</Column>

<Column gap="2" fullWidth={true}>

### Slot And Embedded Items

* The lower-right controller slot can hold up to 64 valid items.
* Each <ItemLink id="gtceu:enderium_nanoswarm" /> provides +0.01 success chance.
* Each <ItemLink id="gtceu:draconium_nanoswarm" /> provides +0.05 success chance.
* Each <ItemLink id="gtceu:spacetime_nanoswarm" /> provides +0.1 success chance.
* Each <ItemLink id="gtceu:eternity_nanoswarm" /> provides +0.2 success chance.
* When <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> is placed as the slot item, <FluidLink id="gtceu:uu_amplifier" /> and <FluidLink id="gtceu:uu_matter" /> success chances directly become 100%. This slot does not accept <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />.
* When extra <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> are embedded, up to 2048 Astral Arrays can be embedded. Each <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> counts as 1024, and embedding the first Astral Array enables cross-recipe mode.

</Column>

<Column gap="2" fullWidth={true}>

### Output Unlocks

* At <Color color="#FFFF00">**UXV**</Color> voltage tier, Chaos Weaving recipes additionally attempt to output <FluidLink id="gtceu:uu_amplifier" />.
* At <Color color="#FF0000">**MAX**</Color> voltage tier, Chaos Weaving recipes additionally attempt to output <FluidLink id="gtceu:uu_matter" />.
* Final output amount is current parallel multiplied by base output amount.

</Column>

<Column gap="2" fullWidth={true}>

### Formulas

* <ItemLink id="gtlcore:stellar_containment_casing" />, <ItemLink id="gtlcore:advanced_stellar_containment_casing" />, or <ItemLink id="gtlcore:ultimate_stellar_containment_casing" /> in the structure provide a tier bonus:

<Latex math = "\alpha = 8 * (2^{Structure\ tier} - 1) * \sqrt{Voltage\ tier + 1}" />

* Coil bonus:

<Latex math = "\beta = 3.8 * 1.3^{Coil\ tier} * (\frac{Coil\ temperature}{36000})^{0.7}" />

* <FluidLink id="gtceu:uu_amplifier" /> success chance:

<Latex math = "\frac{100}{1 + e^{-0.1 * (\frac{\alpha}{50} + \frac{\beta}{100} + \frac{Height}{3})}}" />

* <FluidLink id="gtceu:uu_amplifier" /> base output:

<Latex math = "40960 * \tanh(0.007 * (\alpha * \frac{Height}{9} + \sqrt{\beta} * \ln(Voltage\ tier + 2)))" />

* <FluidLink id="gtceu:uu_matter" /> success chance:

<Latex math = "100 * (1 - e^{-0.02 * (\frac{\alpha + \beta}{20} + \sqrt[3]{Height} * \frac{Voltage\ tier}{3})})" />

* <FluidLink id="gtceu:uu_matter" /> base output:

<Latex math = "22500 * \tanh(\sqrt{\alpha * \beta} * \frac{(Height + Voltage\ tier) * 0.045}{200})" />

* Normal parallel without embedded <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> or <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />:

<Latex math = "4096 * 1.621^{\frac{Coil\ temperature}{6400}}" />

* Parallel after embedding <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> or <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />:

<Latex math = "3^{16} * count^{1.392}" />

* In the formula, count is the current number of extra embedded <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />. Each <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> counts as 1024.
* Embedded <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> loses its compressed state. Breaking the host returns embedded count as ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> stacks.
* Height refers to the height of repeatable layers in the structure.

</Column>

</Column>