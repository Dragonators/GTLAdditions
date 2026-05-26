---
navigation:
  title: Spectral Analysis Hatch
  icon: spectral_analysis_hatch
  parent: part/machine_part_index.md
  position: 20
categories:
  - machine part hatch
item_ids:
  - gtladditions:spectral_analysis_hatch
---

# Spectral Analysis Hatch

~~Also known as the OP hatch.~~

<BlockImage id="gtladditions:spectral_analysis_hatch" scale="4" />

* Spectral Analysis Hatch can be installed in <ItemLink id="gtceu:integrated_ore_processor" />, <ItemLink id="gtceu:advanced_integrated_ore_processor" />, and <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />.
* It is an extra auxiliary hatch. Each compatible structure can install at most one Spectral Analysis Hatch.
* Its GUI has three frequency bands. Each band has a hidden random target value and three indicator lights.
* The number of active lights after a frequency band represents its activation level: <Color color="#00FF00">green means active</Color>, while <Color color="#FF0000">red means inactive</Color>.
* Activation level 1 has a **+/-80** range, level 2 has a **+/-35** range, and level 3 has a **+/-5** range.
* You can type frequency values directly, or drag the slider under the text box to adjust the frequency number.

## Frequency Effects

<Column gap="15" fullWidth={true}>
<Column gap="2" fullWidth={true}>

* Frequency band 1 affects both <ItemLink id="gtceu:integrated_ore_processor" /> and <ItemLink id="gtceu:advanced_integrated_ore_processor" />.
> For <ItemLink id="gtceu:integrated_ore_processor" />, it further multiplies recipe parallel after the normal parallel hatch has already applied. \
> Level 0: x4 parallel. Level 1: x6 parallel. Level 2: x8 parallel. Level 3: x10 parallel.
>
> For <ItemLink id="gtceu:advanced_integrated_ore_processor" />, it adds extra cross-recipe processing threads beyond base cross-recipe threads and <ItemLink id="gtladditions:thread_modifier_hatch" /> threads.\
> Level 0: +72 threads. Level 1: +96 threads. Level 2: +128 threads. Level 3: +144 threads.
>
> Frequency band 1 has no practical benefit for <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />, because that machine already has infinite parallel and infinite threads.

</Column>
<Column gap="2" fullWidth={true}>

* Frequency band 2 multiplies the voltage chance bonus of item outputs by this band level plus one.
> For example: at level 2, if item A has a base output chance of 30% and a base voltage bonus chance of 5%, then this band changes the base voltage bonus chance to:

<Latex math = "5 * (2 + 1) = 15" />

</Column>
<Column gap="2" fullWidth={true}>

* Frequency band 3 provides different multipliers depending on machine type.
> <ItemLink id="gtceu:integrated_ore_processor" />: recipe duration multiplier. <ItemLink id="gtceu:advanced_integrated_ore_processor" />: input fluid consumption multiplier. <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />: no practical benefit.\
> Level 0: x0.8. Level 1: x0.65. Level 2: x0.5. Level 3: x0.4.

</Column>
</Column>

## Perfect Tuning

* Perfect tuning means all three frequency band inputs match their actual frequency numbers with zero error.
* For <ItemLink id="gtceu:integrated_ore_processor" />, perfect tuning changes overclocking from lossy overclocking to perfect overclocking.
* For <ItemLink id="gtceu:advanced_integrated_ore_processor" />, after base cross-recipe threads, <ItemLink id="gtladditions:thread_modifier_hatch" /> threads, and frequency band 1 threads have already applied, perfect tuning multiplies the total cross-recipe threads by 2.
* For <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />, perfect tuning doubles recipe output.