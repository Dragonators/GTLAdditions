---
navigation:
  title: Subspace Corridor Hub
  icon: subspace_corridor_hub_industrial_array
  parent: controller/multiblock_controller.md
  position: 34
categories:
  - multiblock controller
item_ids:
  - gtladditions:subspace_corridor_hub_industrial_array
---

# Subspace Corridor Hub

<BlockImage id="gtladditions:subspace_corridor_hub_industrial_array" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* Subspace Corridor Hub does not process recipes directly. It acts as the host for <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> series modules.
* The hub can connect up to 138 <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> series modules.
* Connected modules require the hub to remain in a valid working state before they can continue progressing their own recipes.

</Column>

<Column gap="2" fullWidth={true}>

### Module Connection

* <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> series modules inherit the running state from the hub.
* If the hub is not formed, not powered, or not in a valid state, modules stop working.
* Under normal conditions, modules provide parallel capability based on their own tier and structure.

</Column>

<Column gap="2" fullWidth={true}>

### Astral Arrays

* Subspace Corridor Hub can embed up to 512 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />.
* One <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> exceeds this cap and fills it to 512 Astral Arrays.
* Embedded compressed arrays decohere. Breaking the host returns embedded count as ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> stacks.
* After embedding a full 512 Astral Arrays, Subspace Corridor Hub unlocks Paradoxical Attainment Theory.
* In this mode, Subspace Corridor Hub provides infinite parallel and infinite threads to connected modules.

</Column>

</Column>
