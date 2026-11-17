---
navigation:
  title: Apocalyptic Quantum Warp Matrix
  icon: apocalyptic_quantum_warp_matrix
  parent: machine/controller/multiblock_controller.md
  position: 42
categories:
  - multiblock controller
item_ids:
  - gtladditions:apocalyptic_quantum_warp_matrix
---

# Apocalyptic Quantum Warp Matrix

<BlockImage id="gtladditions:apocalyptic_quantum_warp_matrix" scale="8"/>

<Column gap="20" fullWidth={true}>
<Column gap="2" fullWidth={true}>

* Work backward through the future and turn probability into certainty. The Matrix processes Quantum Manipulator, Deep Chemical Distortion and Singularity Collapse recipes together.
* It draws power from the wireless energy network and needs no energy or laser hatches.
* It starts with 1024 threads, provides Int.MAX parallels and ignores all recipe maintenance requirements.
* This machine cannot use the ME Recipe Type Integrated Pattern Buffer.

</Column>
<Column gap="2" fullWidth={true}>

### Installing Astral Arrays

* Sneak-right-click the controller with an <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> to add threads. It holds up to 256 arrays.
* You can also install a <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />. One is enough to fill the controller, but any excess capacity is not refunded.

</Column>
<Column gap="2" fullWidth={true}>

### Continuous Operation

* As the Matrix works, it gradually warms up and can process more work at once.
* After <Color color="#55FFFF">4 hours</Color> of accumulated running time, its thread count reaches <Color color="#FFAA00">16 times</Color> its starting value.
* Running time also gradually reduces the Time Space Distorter's additional material requirements from 80% to 40% of the base amounts. See <ItemLink id="gtladditions:time_space_distorter" />.
* The Matrix gradually loses its accumulated running time while idle.

Thread count is calculated as follows:

<Latex math="T = \left\lfloor (1024 + 64KN) \times 16^{\min(t/4, 1)} \right\rfloor" />

* T is the thread count, N is the number of installed arrays, and t is the continuous running time shown in the interface.
* K is the array thread multiplier, which depends on the modpack's recipe-duration multiplier setting.

</Column>
<Column gap="2" fullWidth={true}>

### Material Probabilities and Recipe Memory

* Chance outputs are guaranteed. The consumption probability of chance inputs is reduced to 1/10 of its original value.
* Each recipe has its own running memory. The longer it is processed, the lower its input consumption probability becomes. After <Color color="#55FFFF">1 hour</Color>, it falls to as little as <Color color="#FFAA00">1/20</Color> of its original value.
* A recipe gradually loses its accumulated running time while idle.
* Open the clock tab on the left of the controller interface to see each recipe's remembered running time and current consumption probability reduction.

Consumption probability is calculated as follows, with s representing that recipe's accumulated running time in hours:

<Latex math="p = \frac{p_0}{10 + 10\min(s, 1)}" />

p₀ is the recipe's original consumption probability, and p is its consumption probability in the Matrix.

</Column>
<Column gap="2" fullWidth={true}>

### Infinite Parallels and Threads

The Matrix enters infinite mode automatically when all of these conditions are met:

* It has accumulated 4 hours of continuous running time.
* The controller is filled with 256 Astral Arrays.
* It is bound to a formed, running <ItemLink id="gtladditions:time_space_distorter" />.

In infinite mode, the machine consumes four times as much power.

</Column>
<Column gap="2" fullWidth={true}>

### Dimension Folding

* In infinite mode, enable Dimension Folding in the machine interface to increase output further.
* When enabled, it consumes <Color color="#FF5555">6 times the additional materials</Color> to double the Time Space Distorter's output multiplier, reaching up to <Color color="#FFAA00">6.4x output</Color>.

</Column>
</Column>