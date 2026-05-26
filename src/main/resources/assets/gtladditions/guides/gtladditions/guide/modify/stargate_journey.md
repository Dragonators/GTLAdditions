---
navigation:
  title: Stargate Journey
  parent: modify/modify_index.md
  position: 6
categories:
  - modify
item_ids:
  - sgjourney:pegasus_dhd
  - sgjourney:universe_stargate
  - avaritia:neutron_ring
  - avaritia:infinity_umbrella
  - avaritia:infinity_ring
---

# Stargate Journey

<GameScene zoom="1.45" background="transparent" interactive={true} fullWidth={true}>
  <Entity id="minecraft:interaction" x="0" y="-2" z="-2" data="{width:9.0f,height:9.0f,response:0b}" />
  <Block id="sgjourney:pegasus_stargate" x="0" y="0" z="0" p:facing="north" />
  <Block id="sgjourney:pegasus_dhd" x="0" y="0" z="-4" p:facing="south" />
  <IsometricCamera yaw="180" pitch="18" />
</GameScene>

<Column gap="2" fullWidth={true}>

* Certain target dimensions can only be entered through Stargate travel. Entering them by other means sends the player back to the Overworld.
* Stargate DHDs can dial without energy.
* Stargate connections do not autoclose and stay open until a player manually closes them.
* Survival-mode players can travel both ways through an active connection.
* Stargates generated in target dimensions place ancient artifact rewards nearby, such as <ItemLink id="sgjourney:universe_stargate" link={false} />, <ItemLink id="avaritia:neutron_ring" link={false} />, <ItemLink id="avaritia:infinity_umbrella" link={false} />, and <ItemLink id="avaritia:infinity_ring" link={false} />.

</Column>