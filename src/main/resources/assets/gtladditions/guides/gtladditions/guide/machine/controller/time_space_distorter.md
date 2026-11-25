---
navigation:
  title: Time Space Distorter
  icon: time_space_distorter
  parent: machine/controller/multiblock_controller.md
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

* A Time Space Distorter can bind one-to-one with an <ItemLink id="gtladditions:apocalyptic_quantum_warp_matrix" /> or an <ItemLink id="gtladditions:exotic_chemical_complex" />.
* Use a <ItemLink id="gtladditions:suprachronal_data_module" link={false} /> to right-click the formed host, then the formed Time Space Distorter in the same dimension.
* Keep the Time Space Distorter running and supply enough additional materials to receive the boost.
* This machine cannot use the ME Recipe Type Integrated Pattern Buffer.

</Column>

<Column gap="2" fullWidth={true}>

### Causality Distortion Levels

Use the plus and minus buttons in the interface to adjust the level. Higher levels give more output and require more kinds of additional materials:

* Level 1: <Color color="#FFAA00">1.3x output</Color>. Consumes additional <FluidLink id="gtceu:infinity" /> at 1 mB per 17 parallels.
* Level 2: <Color color="#FFAA00">1.7x output</Color>. Keeps the level 1 requirement and adds <FluidLink id="gtceu:hypogen" /> at 1 mB per 28 parallels.
* Level 3: <Color color="#FFAA00">2.6x output</Color>. Keeps the level 2 requirements and adds <FluidLink id="gtceu:spacetime" /> at 1 mB per 44 parallels.
* Level 4: <Color color="#FFAA00">3.2x output</Color>. Keeps the level 3 requirements and adds one <ItemLink id="kubejs:quantum_anomaly" /> per 730 parallels and one <ItemLink id="kubejs:hypercube" /> per 873 parallels.

</Column>
<Column gap="2" fullWidth={true}>

### Additional Material Costs

* The Apocalyptic Quantum Warp Matrix starts at 80% of the base material cost. After 4 hours of continuous operation, this falls to 40%.
* The Exotic Chemical Complex incurs costs only when a tier 3 Distortion unit starts a batch with outputs, at a fixed 10% of the base material cost.
* Each material amount is rounded down.

Each additional material's amount is calculated as follows:

<Latex math="Q = \left\lfloor \frac{P \times c \times F}{d} \right\rfloor" />

<Latex math="c = 0.8 \times 2^{-\min(t/4, 1)}" />

* P is the number of parallels that actually started in the Matrix or Distortion unit for that batch.
* d is the base-cost divisor listed above: 17, 28, 44, 730 or 873.
* c is the material cost multiplier. For the Matrix, use the formula above with t as the continuous running time in hours. The Exotic Chemical Complex uses a fixed c of 0.1.
* F is 6 while the Matrix has Dimension Folding active and 1 in all other cases.

</Column>
<Column gap="2" fullWidth={true}>

### Dimension Folding

* Once the Matrix enters infinite mode, Dimension Folding can trade 6 times the additional materials for twice the output multiplier. Its material cost multiplier combines multiplicatively with the reduction from the Matrix's running time.
* The four levels then provide 2.6x, 3.4x, 5.2x and 6.4x output.

</Column>

</Column>