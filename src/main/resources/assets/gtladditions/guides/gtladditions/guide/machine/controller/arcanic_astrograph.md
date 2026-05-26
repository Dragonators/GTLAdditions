---
navigation:
  title: Arcanic Astrograph
  icon: arcanic_astrograph
  parent: controller/multiblock_controller.md
  position: 22
categories:
  - multiblock controller
item_ids:
  - gtladditions:arcanic_astrograph
---

# Arcanic Astrograph

<BlockImage id="gtladditions:arcanic_astrograph" scale="8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:arcanic_astrograph" /> is a denser miniature universe than <ItemLink id="gtceu:eye_of_harmony" />, with 2048 base parallel.
* The machine follows <ItemLink id="gtceu:eye_of_harmony" /> consumption rules, but only <ItemLink id="kubejs:quantum_chromodynamic_charge" /> and <FluidLink id="gtceu:cosmic_element" /> scale with parallel. Other resource consumption keeps the original rules.
* After forming, the UI displays the number of embedded <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> and the current maximum parallel.

</Column>

<Column gap="2" fullWidth={true}>

### Astral Array Bonus

* <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> or <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> can be embedded to increase the parallel limit.
* Parallel is calculated as:

<Latex math = "Maximum\ parallel = 2048 + 2^{\lfloor \log_{1.7}(8 * count) \rfloor} * 128" />

* In the formula, count is the current number of embedded <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />. Each <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> counts as 1024.
* <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> is highly unstable after embedding. If the host is broken, embedded compressed arrays return only as ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> stacks.

</Column>

<Column gap="2" fullWidth={true}>

### Astral Array Compression

* The compression recipe only starts after the listed base materials are matched, then draws a one-time 9,223,372,036,854,775,807 EU startup cost from the bound wireless energy network.
* During the 30 seconds compression window, ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> item entities thrown into the cosmic render area are consumed. Every complete 1024 arrays attempts one <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> output; if the compression recipe continues into the next cycle, unmatched arrays are retained.
* If the next recipe check cannot continue compression, the machine is paused, or the output can no longer accept compressed results, stored arrays and failed outputs are annihilated.
* The output chance starts at 30% and reaches 100% at 42,441 embedded arrays. Formula:

<Latex math = "Output\ chance = \min(100\%, 30\% + 70\% * (\frac{count}{42,441})^{1.5})" />

<Recipe id="gtladditions:compressed_astral_array/compressed_astral_array" />

</Column>

</Column>