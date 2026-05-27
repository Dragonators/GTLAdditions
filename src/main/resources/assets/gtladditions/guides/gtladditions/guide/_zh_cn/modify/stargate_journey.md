---
navigation:
  title: 星门之旅
  parent: modify/modify_index.md
  position: 6
categories:
  - 魔改
---

# 星门之旅

<GameScene zoom="1.45" background="transparent" interactive={true} fullWidth={true}>
  <Entity id="minecraft:interaction" x="0" y="-2" z="-2" data="{width:9.0f,height:9.0f,response:0b}" />
  <Block id="sgjourney:pegasus_stargate" x="0" y="0" z="0" p:facing="north" />
  <Block id="sgjourney:pegasus_dhd" x="0" y="0" z="-4" p:facing="south" />
  <IsometricCamera yaw="180" pitch="18" />
</GameScene>

<Column gap="2" fullWidth={true}>

* 特定目标维度只能通过星门通行进入。尝试用其他方式进入时，会被送回主世界。
* 星门 DHD 不需要能量即可拨号。
* 星门连接不会自动关闭，会保持开启直到玩家手动关闭。
* 生存模式玩家可以从连接两端双向通行。
* 目标维度生成的星门附近会放置上古神器奖励物品；例如 <ItemLink id="sgjourney:universe_stargate" link={false} />、<ItemLink id="avaritia:neutron_ring" link={false} />、<ItemLink id="avaritia:infinity_umbrella" link={false} /> 与 <ItemLink id="avaritia:infinity_ring" link={false} />。

</Column>
