---
navigation:
  title: Transmutation Bus Hatch
  icon: me_block_conservation
  parent: part/machine_part_index.md
  position: 10
categories:
  - machine part hatch
item_ids:
  - gtladditions:me_block_conservation
---

# Transmutation Bus Hatch

<BlockImage id="gtladditions:me_block_conservation" scale="4" />

* Serves as the block conversion input and output bus for <ItemLink id="gtladditions:subatomic_transmutatioon_core" />.
* An AE item storage cell must be placed in the internal slot. Converted outputs are written to that cell first.
* When it can write to an AE network, the Transmutation Bus Hatch returns outputs from the internal cell to the connected AE network.
* Configure the input block and amount to request in the Transmutation Bus Hatch GUI. It pulls the matching items from the AE network like an ME Input Bus.
* The internal storage slot accepts AE item storage cells and infinity item storage cells.