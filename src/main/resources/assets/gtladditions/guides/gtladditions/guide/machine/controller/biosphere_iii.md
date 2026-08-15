---
navigation:
  title: Biosphere III
  icon: biosphere_iii
  parent: controller/multiblock_controller.md
  position: 10
item_ids:
  - biosphere_iii
  - garden_of_hermes
---

# Biosphere III

<Row>
    <BlockImage id="biosphere_iii" scale="4"/>
    <BlockImage id="garden_of_hermes" scale="4"/>
</Row>

<Column gap="8" fullWidth={true}>

## Centralized Power Supply

Biosphere III may install any number of the following energy components:

* energy hatches;
* <Color color="#00AA00">**UIV**</Color>–<Color color="#FF0000">**MAX**</Color> laser input hatches;
* <ItemLink id="gtladditions:wireless_energy_network_input_terminal" />.

When a wireless grid input terminal is present, the host prioritizes the wireless grid.

With wired power, the host's total input power is divided evenly among all Garden of Hermes modules that are connected, structurally valid, and enabled. Wireless mode does not divide power.

Garden of Hermes cannot install energy components. Modules still process item and fluid inputs and outputs locally, but all module energy consumption is deducted centrally by Biosphere III.

The host accumulates successfully consumed EU and calculates the average EU/t, settling once per second.

## Module Threads

Without <ItemLink id="gtladditions:thread_modifier_hatch" />, each Garden of Hermes module has a fixed base of 128 threads. Biosphere III may install <ItemLink id="gtladditions:thread_modifier_hatch" />, whose base thread multiplier is 1024.

The engine's extra thread count remains `Astral Array count × 64 × current multiplier`. With an engine installed, each module gains additional threads according to:

`32 × max(host tier - UEV, 0) × host engine extra threads`

The final number of threads per module is `128 + additional threads`.

## Recipe Processing

Garden of Hermes processes Greenhouse and Fishing Ground recipes together without changing recipe types. Recipe tier and overclock voltage come from the host; parallelism doubles for every 1100 K of host coil temperature.

</Column>
