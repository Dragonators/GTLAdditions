---
navigation:
  title: Super Extended ME Export Buffer
  icon: me_super_extended_export_buffer
  parent: part/machine_part_index.md
  position: 11
categories:
  - machine part hatch
item_ids:
  - gtladditions:me_super_extended_export_buffer
---

# Super Extended ME Export Buffer

<Column gap="15" fullWidth={true}>

<BlockImage id="gtladditions:me_super_extended_export_buffer" scale="6" />

<Column gap="2" fullWidth={true}>

* You need to reach the <Color color="#FFFF00">**UIV**</Color> stage before you can craft it.
* It provides every capability of <ItemLink id="gtceu:me_extended_export_buffer" />.
* Extends <ItemLink id="gtceu:me_extended_export_buffer" /> with **Int128** accumulated output capacity for items and fluids, up to **2^127 − 1** per material.

</Column>

<Column gap="2" fullWidth={true}>

## Output Transfer Limit

* Set the maximum number of ME insertion attempts per cycle: **1–4096**, with a default of **64**.
* Each insertion can transfer up to **Long.MAX_VALUE** units of a single item or fluid type.

</Column>

</Column>
