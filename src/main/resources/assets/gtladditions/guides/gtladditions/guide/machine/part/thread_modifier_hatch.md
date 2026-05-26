---
navigation:
  title: Ω-Spheres Divergence Engine
  icon: thread_modifier_hatch
  parent: part/machine_part_index.md
  position: 19
categories:
  - machine part hatch
item_ids:
  - gtladditions:thread_modifier_hatch
  - gtladditions:astral_array
---

# Ω-Spheres Divergence Engine

<Column gap="15" fullWidth={true}>

<BlockImage id="gtladditions:thread_modifier_hatch" scale="8" />

<Column gap="2" fullWidth={true}>

* Ω-Spheres Divergence Engine provides thread modification capability.
* It supports both compatible non-cross-recipe controllers and compatible cross-recipe controllers.
* When installed on a compatible non-cross-recipe controller, that machine gains cross-recipe parallelization capability.
* When installed on a compatible controller that already has cross-recipe mode, that machine gains extra threads.

</Column>

<Column gap="2" fullWidth={true}>

## Astral Arrays

* Only ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> can be placed in the GUI. <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> is not accepted by this slot.
* Each <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> provides 64 base threads.
* The total extra threads are calculated by the following formula, where `n` is the number of <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> and `m` is the current thread multiplier:

<Latex math = "Extra\ threads = n * 64 * m" />

* The current thread multiplier is determined by the installation target and is displayed in this hatch's GUI.
* For non-cross-recipe controllers, this extra thread total becomes the thread count after cross-recipe parallelization is enabled.
* For controllers that already have cross-recipe mode, this extra thread total is added to the machine's original threads.

</Column>

<Column gap="2" fullWidth={true}>

> This hatch cannot be shared. When the machine is removed, internal Astral Arrays are cleared and dropped.

</Column>

</Column>