---
navigation:
  title: Time Space Distorter
  icon: time_space_distorter
  parent: controller/multiblock_controller.md
  position: 43
categories:
  - multiblock controller
item_ids:
  - gtladditions:time_space_distorter
---

# Time Space Distorter

<BlockImage id="gtladditions:time_space_distorter" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:time_space_distorter" /> is a bound buff machine for the <ItemLink id="gtladditions:apocalyptic_torsion_quantum_matrix" />.
* Use a <ItemLink id="gtladditions:suprachronal_data_module" link={false} /> on the <ItemLink id="gtladditions:apocalyptic_torsion_quantum_matrix" /> first to record it, then use it on the <ItemLink id="gtladditions:time_space_distorter" /> to bind them.
* The <ItemLink id="gtladditions:time_space_distorter" /> must run its circuit 1 idle recipe. The recipe uses OpV power and lasts 30 seconds.
* After the <ItemLink id="gtladditions:apocalyptic_torsion_quantum_matrix" /> finishes consuming inputs for this cycle, it counts the total successful parallels and asks the bound <ItemLink id="gtladditions:time_space_distorter" /> to consume buff resources. All required buff resources are checked together before anything is consumed.
* If the buff resource consumption succeeds, only this cycle's recipe outputs are multiplied. Chance inputs, chance outputs, and parallel calculation are unchanged.

</Column>

<Column gap="2" fullWidth={true}>

### Causality Distortion Levels

* Level 1: 1.3x output multiplier, consumes <FluidLink id="gtceu:infinity" /> equal to total parallels / 17.
* Level 2: 1.7x output multiplier, additionally consumes <FluidLink id="gtceu:hypogen" /> equal to total parallels / 28.
* Level 3: 2.6x output multiplier, additionally consumes <FluidLink id="gtceu:spacetime" /> equal to total parallels / 44.
* Level 4: 3.2x output multiplier, additionally consumes <ItemLink id="kubejs:quantum_anomaly" /> equal to total parallels / 730 and <ItemLink id="kubejs:hypercube" /> equal to total parallels / 873.
* Amounts use integer division. A result of 0 consumes nothing for that entry.

</Column>

<Recipe id="gtladditions:time_space_distortion/time_space_distortion" />

</Column>