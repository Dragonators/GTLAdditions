---
navigation:
  title: Subspace Corridor Nexus
  icon: subspace_corridor_nexus_industrial_array
  parent: machine/controller/multiblock_controller.md
  position: 34
categories:
  - multiblock controller
item_ids:
  - gtladditions:subspace_corridor_nexus_industrial_array
---

# Subspace Corridor Nexus

<BlockImage id="gtladditions:subspace_corridor_nexus_industrial_array" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Subspace Corridor Nexus does not process recipes directly. It acts as the host for <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> series modules.
* The controller's Y coordinate must be between −42 and 51 (it's just way too big!).

</Column>

<Column gap="2" fullWidth={true}>

### Module Connection

* It can connect up to 144 <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> series modules.
* Modules try to connect to the host when they form; the host also attempts to connect modules when it forms.
* The host must keep running for its modules to process their own recipes.

</Column>

<Column gap="2" fullWidth={true}>

### Astral Arrays

* Subspace Corridor Nexus can embed up to 512 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />.
* One <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> exceeds this cap and fills it to 512 Astral Arrays.
* Embedded compressed arrays decohere. Breaking the host returns embedded count as ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> stacks.
* After embedding a full 512 Astral Arrays, Subspace Corridor Nexus unlocks Paradoxical Attainment Theory.
* In this mode, Subspace Corridor Nexus provides infinite parallel and infinite threads to connected modules.

</Column>

</Column>
