---
navigation:
  title: Apocalyptic Torsion Quantum Matrix
  icon: apocalyptic_torsion_quantum_matrix
  parent: controller/multiblock_controller.md
  position: 42
categories:
  - multiblock controller
item_ids:
  - gtladditions:apocalyptic_torsion_quantum_matrix
---

# Apocalyptic Torsion Quantum Matrix

<BlockImage id="gtladditions:apocalyptic_torsion_quantum_matrix" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:apocalyptic_torsion_quantum_matrix" /> can process Quantum Manipulator, Deep Chemical Distortion, and Singularity Collapse related recipes in cross-recipe mode.
* The machine draws power from the wireless energy network and does not need any energy or laser hatches.
* It has 1024 base threads and supports parallel control hatches.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.
* Runtime EU multiplier is 0.2.
* Record this machine with a <ItemLink id="gtladditions:suprachronal_data_module" link={false} />, then bind it to a <ItemLink id="gtladditions:time_space_distorter" />. When that machine successfully consumes its resources, this cycle's processed recipe outputs receive the causality distortion multiplier.

</Column>

<Column gap="2" fullWidth={true}>

### Chance Materials

* Chance outputs that participate in processing are treated as guaranteed outputs.
* Chance inputs that participate in processing are consumed at one tenth of their original requirement.
* For recipes with chance inputs, the machine first checks the currently available items and fluids, then calculates how much parallel those chance inputs can support in this cycle.
* Each runnable recipe tries to fill its own maximum material-supported parallel according to the expected chance consumption, instead of conservatively calculating as if those inputs were consumed at 100%.

</Column>

<Column gap="2" fullWidth={true}>

### Time Space Distorter

* <ItemLink id="gtladditions:time_space_distorter" /> reads this machine's actual successful parallels after input consumption for the cycle.
* If the <ItemLink id="gtladditions:time_space_distorter" /> is running and all required <FluidLink id="gtceu:infinity" />, <FluidLink id="gtceu:hypogen" />, <FluidLink id="gtceu:spacetime" />, <ItemLink id="kubejs:quantum_anomaly" />, and <ItemLink id="kubejs:hypercube" /> costs are available for the configured level, this cycle's outputs are multiplied by its causality distortion level and those costs are consumed together.
* This buff only applies output multipliers. Chance input and chance output rules are unchanged.

</Column>

</Column>