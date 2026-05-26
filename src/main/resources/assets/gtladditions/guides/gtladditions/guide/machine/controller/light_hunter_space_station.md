---
navigation:
  title: Light Hunter Space Station
  icon: light_hunter_space_station
  parent: controller/multiblock_controller.md
  position: 34
categories:
  - multiblock controller
item_ids:
  - gtladditions:light_hunter_space_station
---

# Light Hunter Space Station

<BlockImage id="gtladditions:light_hunter_space_station" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Light Hunter Space Station does not process recipes directly. It acts as the main station for <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> series modules.
* The main station can connect up to 20 <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> series modules.
* Connected modules require the main station to remain in a valid working state before they can continue progressing their own recipes.

</Column>

<Column gap="2" fullWidth={true}>

### Module Connection

* <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> series modules inherit the running state from the main station.
* If the main station is not formed, not powered, or not in a valid state, modules stop working.
* Under normal conditions, modules provide parallel capability based on their own tier and structure.

</Column>

<Column gap="2" fullWidth={true}>

### Astral Arrays

* Light Hunter Space Station can embed up to 64 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />.
* One <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> exceeds this cap and fills it to 64 Astral Arrays.
* Embedded compressed arrays decohere. Breaking the host returns embedded count as ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> stacks.
* After embedding a full 64 Astral Arrays, Light Hunter Space Station unlocks Paradoxical Attainment Theory.
* In this mode, Light Hunter Space Station provides infinite parallel and infinite threads to connected modules.

</Column>

</Column>