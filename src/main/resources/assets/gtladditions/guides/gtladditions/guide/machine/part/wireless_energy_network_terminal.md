---
navigation:
  title: Wireless Energy Network Terminals
  icon: wireless_energy_network_input_terminal
  parent: part/machine_part_index.md
  position: 20
categories:
  - machine part hatch
item_ids:
  - gtladditions:wireless_energy_network_input_terminal
  - gtladditions:wireless_energy_network_output_terminal
---

# Wireless Energy Network Terminals

<Column gap="15" fullWidth={true}>

<Row>
    <BlockImage id="gtladditions:wireless_energy_network_input_terminal" scale="6" />
    <BlockImage id="gtladditions:wireless_energy_network_output_terminal" scale="6" />
</Row>

<Column gap="2" fullWidth={true}>

* Wireless Energy Network Terminals are divided into <ItemLink id="gtladditions:wireless_energy_network_input_terminal" /> and <ItemLink id="gtladditions:wireless_energy_network_output_terminal" />.
* When placed, they automatically bind to the placer. Right-click with a <ItemLink id="gtceu:data_stick" /> to rebind to the current player, or left-click with a <ItemLink id="gtceu:data_stick" /> to unbind.
* Both terminals use the matching UUID energy account in the wireless energy network as their energy source.

</Column>

<Column gap="2" fullWidth={true}>

## Input Terminal

* <ItemLink id="gtladditions:wireless_energy_network_input_terminal" /> provides Energy Input Hatch and Laser Target Hatch capability.
* Normal energy input limit is Long.MAX level.
* In cross-recipe mode, it lets compatible wireless machines read available energy from the wireless energy network.

</Column>

<Column gap="2" fullWidth={true}>

## Output Terminal

* <ItemLink id="gtladditions:wireless_energy_network_output_terminal" /> provides Dynamo Hatch and Laser Source Hatch capability.
* Normal energy output limit is Long.MAX level.
* It can write energy output by compatible machines into the bound player's wireless energy network. When installed on <ItemLink id="gtladditions:heart_of_the_universe" />, it can output infinite power.

</Column>

</Column>