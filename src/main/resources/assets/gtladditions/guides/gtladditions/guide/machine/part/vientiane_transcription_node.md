---
navigation:
  title: Vientiane Transcription Node
  icon: vientiane_transcription_node
  parent: part/machine_part_index.md
  position: 5
categories:
  - machine part hatch
item_ids:
  - gtladditions:vientiane_transcription_node
---

# Vientiane Transcription Node

<BlockImage id="gtladditions:vientiane_transcription_node" scale="4" />

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

Vientiane Transcription Node is the redstone output part used by <ItemLink id="gtladditions:recursive_reverse_array" /> modules. The redstone signal is emitted from the node's front face.

</Column>

<Column gap="2" fullWidth={true}>

## Catalytic Cascade Array

When installed in <ItemLink id="gtladditions:catalytic_cascade_array" />, it outputs a redstone signal from 1 to 15 at the start of each catalytic cycle. This signal indicates the catalyst group required for the current cycle.

</Column>

<Column gap="2" fullWidth={true}>

## Supratemporal Boosting Engine

When installed in <ItemLink id="gtladditions:supratemporal_boosting_engine" />, opening the node GUI after the structure forms lets you configure minimum temperature, maximum temperature, and inverted redstone mode. The node calculates redstone output from the current engine temperature and configured range.

* Normal mode: emits redstone when the temperature is between the configured minimum and maximum values, and stops emitting when below the minimum.
* Inverted mode: emits redstone when the temperature is outside the configured range, and also emits when below the minimum.
* Configurable temperature range is 48000K to 105000K.

</Column>

</Column>