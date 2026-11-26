---
navigation:
  title: Spectral Analysis Hatch
  icon: spectral_analysis_hatch
  parent: machine/part/machine_part_index.md
  position: 20
categories:
  - machine part hatch
item_ids:
  - gtladditions:spectral_analysis_hatch
---

# Spectral Analysis Hatch

~~Also known as the OP hatch.~~

<BlockImage id="gtladditions:spectral_analysis_hatch" scale="4" />

<Column gap="15" fullWidth={true}>
<Column gap="2" fullWidth={true}>

* Spectral Analysis Hatch is an auxiliary hatch that can be installed in <ItemLink id="gtceu:integrated_ore_processor" />, <ItemLink id="gtceu:advanced_integrated_ore_processor" />, <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />, and <ItemLink id="gtceu:advanced_infinite_driller" />.
* Each compatible multiblock structure can install at most one Spectral Analysis Hatch.
* Its GUI has three channels, each with a hidden random target frequency (0–255) and three indicator lights.
* You can type values directly or drag the slider below each text box to tune the frequency.
* The number of lit lights represents the channel's activation level: <Color color="#00FF00">green means active</Color>, while <Color color="#FF0000">red means inactive</Color>.
* Level 1 requires being within **±80** of the target, level 2 within **±35**, and level 3 within **±5**; with no lights active, the channel is at level 0 (still providing its base bonus).
* Matching all three channels to their exact target values (zero error) activates **Perfect Tuning**.

</Column>

## Ore Processor Effects

<Column gap="2" fullWidth={true}>

* **Channel 1**: Increases recipe parallel or adds extra cross-recipe threads.
> For <ItemLink id="gtceu:integrated_ore_processor" />, it further multiplies recipe parallel after the normal parallel hatch has applied:\
> Level 0: ×4 parallel; Level 1: ×6 parallel; Level 2: ×8 parallel; Level 3: ×10 parallel.
>
> For <ItemLink id="gtceu:advanced_integrated_ore_processor" />, it adds extra cross-recipe processing threads on top of base threads and <ItemLink id="gtladditions:thread_modifier_hatch" /> threads:\
> Level 0: +72 threads; Level 1: +96 threads; Level 2: +128 threads; Level 3: +144 threads.
>
> For <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />, it provides no practical benefit because the machine already has infinite parallel and infinite threads.

</Column>
<Column gap="2" fullWidth={true}>

* **Channel 2**: Multiplies the voltage chance bonus of item outputs by (channel level + 1) across all three ore processors.
> For example: at level 2, if an output item has a 30% base chance and a 5% base voltage bonus chance, the boosted voltage bonus chance becomes:

<Latex math = "5 * (2 + 1) = 15" />

</Column>
<Column gap="2" fullWidth={true}>

* **Channel 3**: Applies a duration or fluid consumption multiplier depending on the machine.
> <ItemLink id="gtceu:integrated_ore_processor" />: recipe duration multiplier; <ItemLink id="gtceu:advanced_integrated_ore_processor" />: input fluid consumption multiplier; <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />: no practical benefit.\
> Level 0: ×0.8; Level 1: ×0.65; Level 2: ×0.5; Level 3: ×0.4.

</Column>
<Column gap="2" fullWidth={true}>

* **Perfect Tuning**:
> For <ItemLink id="gtceu:integrated_ore_processor" />: switches overclocking from lossy overclocking to perfect overclocking.\
> For <ItemLink id="gtceu:advanced_integrated_ore_processor" />: doubles total cross-recipe threads (×2) after base threads, <ItemLink id="gtladditions:thread_modifier_hatch" /> threads, and Channel 1 bonus threads are combined.\
> For <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />: doubles recipe outputs (×2).

</Column>

## Advanced Infinite Driller Effects

<Column gap="2" fullWidth={true}>

When installed in <ItemLink id="gtceu:advanced_infinite_driller" />, Spectral Analysis Hatch instead grants fluid drilling bonuses:

* **Channel 1 (Yield Bonus)**: Multiplies fluid drilling yield and stacks multiplicatively with threads from <ItemLink id="gtladditions:thread_modifier_hatch" /> (final output remains bounded by available input power).
> Level 0: ×1.5; Level 1: ×2.3; Level 2: ×3.2; Level 3: ×4.

</Column>
<Column gap="2" fullWidth={true}>

* **Channel 2 (Energy Reduction)**: Reduces the base energy consumption multiplier per unit of output.
> Level 0: ×0.8; Level 1: ×0.6; Level 2: ×0.2; Level 3: ×0.05.

</Column>
<Column gap="2" fullWidth={true}>

* **Channel 3 (Working Range)**: Expands the driller's chunk scanning range (defaults to 5×5 chunks without the hatch).
> Level 0: 7×7; Level 1: 9×9; Level 2: 11×11; Level 3: 13×13.\
> Adjusting the channel level or removing the hatch automatically updates the working range (removing the hatch restores 5×5).

</Column>
<Column gap="2" fullWidth={true}>

* **Perfect Tuning**:
> Maintenance-Free Temperature: once a drill bit is installed and the coil tier meets the requirement, neither heating fluid nor coolant is needed—temperature stays locked at the coil's maximum safe temperature without overheating or damaging the drill bit.\
> Extra Overclocking: when spare input power remains beyond normal production needs, the driller gains up to 15 extra overclock levels. Each 4× increase in available input power grants 1 extra level, doubling both actual fluid yield and actual energy consumption per level.

</Column>
</Column>
