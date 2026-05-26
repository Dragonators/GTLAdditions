---
navigation:
  title: Final Goal
  position: 4
categories:
  - star ritual
item_ids:
  - gtladditions:stargate_frame_part
  - gtladditions:stargate_shielding_foil
  - gtladditions:stargate_chevron_upgrade
  - gtladditions:dimension_focus_infinity_crafting_array
  - gtladditions:forge_of_the_antichrist
  - gtladditions:recursive_reverse_array
  - gtladditions:supratemporal_boosting_engine
  - gtceu:creative_chest
  - sgjourney:classic_stargate_ring_block
  - sgjourney:classic_stargate_chevron_block
  - sgjourney:classic_stargate_base_block
  - sgjourney:pegasus_dhd
  - sgjourney:universe_stargate
  - avaritia:neutron_ring
  - avaritia:infinity_ring
  - avaritia:infinity_umbrella
---

# <Color color="#FFAA00">**Final Goal: Star Ritual**</Color>

<Column gap="18" fullWidth={true}>

<Column gap="2" fullWidth={true}>

The Star Ritual is the final GTLAdditions route. Build the classic Stargate, dial four specific dimensions to obtain their relics, then throw those relics into the neutron star of the <ItemLink id="gtladditions:forge_of_the_antichrist" />.

After all four relics are collected, the star enters its rainbow and collapse animation. When the animation ends, a floating <ItemLink id="gtceu:creative_chest" /> appears at the center. This is the endpoint of the route.

</Column>

<Column gap="2" fullWidth={true}>

## Stargate Parts

The route starts from these parts:

* <ItemLink id="gtladditions:stargate_frame_part" /> and <ItemLink id="gtladditions:stargate_shielding_foil" />: used to craft the classic Stargate parts.
* <ItemLink id="gtladditions:stargate_chevron_upgrade" link={false} />: the core upgrade for <ItemLink id="sgjourney:classic_stargate_chevron_block" link={false} />.
* <ItemLink id="sgjourney:classic_stargate_ring_block" link={false} />, <ItemLink id="sgjourney:classic_stargate_chevron_block" link={false} />, and <ItemLink id="sgjourney:classic_stargate_base_block" link={false} />: used to build the activatable classic Stargate.
* <ItemLink id="sgjourney:pegasus_dhd" link={false} />: used for dialing. Its tooltip lists the four target addresses.

The Nightmare Crafting recipes for the classic Stargate parts are listed on [Nightmare Crafting Recipes](recipe/nightmare_crafting_recipe.md).

</Column>

<Column gap="2" fullWidth={true}>

## Classic Stargate Structure

The scene below shows the front-facing structure. It is 7 blocks wide, 7 blocks tall, and 1 block thick. The bottom-center block is the <ItemLink id="sgjourney:classic_stargate_base_block" link={false} />.

<GameScene zoom="4" background="transparent" interactive={true} fullWidth={true}>
  <Block id="sgjourney:classic_stargate_chevron_block" x="1" y="6" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="2" y="6" z="0" />
  <Block id="sgjourney:classic_stargate_chevron_block" x="3" y="6" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="4" y="6" z="0" />
  <Block id="sgjourney:classic_stargate_chevron_block" x="5" y="6" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="0" y="5" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="1" y="5" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="5" y="5" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="6" y="5" z="0" />
  <Block id="sgjourney:classic_stargate_chevron_block" x="0" y="4" z="0" />
  <Block id="sgjourney:classic_stargate_chevron_block" x="6" y="4" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="0" y="3" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="6" y="3" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="0" y="2" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="6" y="2" z="0" />
  <Block id="sgjourney:classic_stargate_chevron_block" x="0" y="1" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="1" y="1" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="5" y="1" z="0" />
  <Block id="sgjourney:classic_stargate_chevron_block" x="6" y="1" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="1" y="0" z="0" />
  <Block id="sgjourney:classic_stargate_chevron_block" x="2" y="0" z="0" />
  <Block id="sgjourney:classic_stargate_base_block" x="3" y="0" z="0" p:facing="north" />
  <Block id="sgjourney:classic_stargate_chevron_block" x="4" y="0" z="0" />
  <Block id="sgjourney:classic_stargate_ring_block" x="5" y="0" z="0" />
  <IsometricCamera yaw="180" pitch="20" />
</GameScene>

Legend:

* <Color color="#66CCFF">■</Color>: 14x <ItemLink id="sgjourney:classic_stargate_ring_block" link={false} />
* <Color color="#FFAA00">◆</Color>: 9x <ItemLink id="sgjourney:classic_stargate_chevron_block" link={false} />
* <Color color="#55FFFF">★</Color>: 1x <ItemLink id="sgjourney:classic_stargate_base_block" link={false} />

After the structure is built, activate the <ItemLink id="sgjourney:classic_stargate_base_block" link={false} />. The structure becomes a dialable Stargate. Place a connected <ItemLink id="sgjourney:pegasus_dhd" link={false} /> nearby and dial the target addresses.

</Column>

<Column gap="2" fullWidth={true}>

## Dimensions And Relics

These dimensions only allow entry through a Stargate wormhole; direct cross-dimensional teleportation is rejected and returns the player to the Overworld. The generated Stargate structure in each target dimension displays the matching relic in front of its <ItemLink id="sgjourney:pegasus_dhd" link={false} />.

* <Color color="#FFAA00">Abydos</Color>: address `-26-6-14-31-11-29-`, obtain <ItemLink id="avaritia:neutron_ring" link={false} />.
* <Color color="#55FF55">Chulak</Color>: address `-8-1-22-14-36-19-`, obtain <ItemLink id="avaritia:infinity_umbrella" link={false} />.
* <Color color="#555555">Cavum Tenebrae</Color>: address `-18-7-3-36-25-15-`, obtain <ItemLink id="avaritia:infinity_ring" link={false} />.
* <Color color="#55FFFF">Lantea</Color>: address `-18-20-1-15-14-7-19-`, obtain <ItemLink id="sgjourney:universe_stargate" link={false} />.

</Column>

<Column gap="2" fullWidth={true}>

## Star Ritual

The ritual takes place at the neutron star rendered by the <ItemLink id="gtladditions:forge_of_the_antichrist" />. Before it can begin:

* The <ItemLink id="gtladditions:forge_of_the_antichrist" /> must have reached maximum efficiency.
* It must be bound to a <ItemLink id="gtladditions:recursive_reverse_array" />.
* The Recursive Reverse Array's ritual gate must be active: the related machines are ready, the <ItemLink id="gtladditions:supratemporal_boosting_engine" /> is running, it is not overheated, and its temperature is in the 93000K-97000K optimal range.

Once the gate is active, throw the four relics into the neutron star at the center of the Forge of the Antichrist. The star collects each distinct relic once; after the first one, it turns rainbow. After all four are collected, the star collapses and spawns the <ItemLink id="gtceu:creative_chest" /> at the center after roughly 7 seconds.

<Row>
    <BlockImage id="gtceu:creative_chest" scale="4" />
</Row>

</Column>

</Column>