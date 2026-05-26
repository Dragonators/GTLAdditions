---
navigation:
  title: 终极目标
  position: 4
categories:
  - 星辰仪式
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

# <Color color="#FFAA00">**终极目标：星辰仪式**</Color>

<Column gap="18" fullWidth={true}>

<Column gap="2" fullWidth={true}>

星辰仪式是 GTLAdditions 的最终路线。你需要先完成经典星门，拨号进入四个指定维度取得神器，最后把这些神器投入 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 的中子星。

四件神器全部被中子星收集后，星体会进入彩虹与坍缩动画；动画结束时，中央会生成一个悬浮、不自然消失的 <ItemLink id="gtceu:creative_chest" />。这就是本路线的终点。

</Column>

<Column gap="2" fullWidth={true}>

## 星门部件

星门路线从下列部件开始：

* <ItemLink id="gtladditions:stargate_frame_part" /> 和 <ItemLink id="gtladditions:stargate_shielding_foil" />：用于继续合成经典星门部件。
* <ItemLink id="gtladditions:stargate_chevron_upgrade" link={false} />：<ItemLink id="sgjourney:classic_stargate_chevron_block" link={false} /> 的核心升级件。
* <ItemLink id="sgjourney:classic_stargate_ring_block" link={false} />、<ItemLink id="sgjourney:classic_stargate_chevron_block" link={false} />、<ItemLink id="sgjourney:classic_stargate_base_block" link={false} />：用于搭建可激活的经典星门。
* <ItemLink id="sgjourney:pegasus_dhd" link={false} />：用于拨号。DHD 的 tooltip 中会列出四个目标维度的地址。

经典星门部件的梦魇合成配方见 [梦魇合成相关配方](recipe/nightmare_crafting_recipe.md)。

</Column>

<Column gap="2" fullWidth={true}>

## 经典星门结构

下方场景为正面结构。结构宽 7 格、高 7 格、厚 1 格；底部中央是 <ItemLink id="sgjourney:classic_stargate_base_block" link={false} />。

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

图例：

* <Color color="#66CCFF">■</Color>：14 个 <ItemLink id="sgjourney:classic_stargate_ring_block" link={false} />
* <Color color="#FFAA00">◆</Color>：9 个 <ItemLink id="sgjourney:classic_stargate_chevron_block" link={false} />
* <Color color="#55FFFF">★</Color>：1 个 <ItemLink id="sgjourney:classic_stargate_base_block" link={false} />

搭好后激活 <ItemLink id="sgjourney:classic_stargate_base_block" link={false} />，结构会转为可拨号的星门。把 <ItemLink id="sgjourney:pegasus_dhd" link={false} /> 放在可连接的位置后，即可按地址拨号。

</Column>

<Column gap="2" fullWidth={true}>

## 四个维度与神器

只有通过星门虫洞进入下列维度时，传送才会被允许；直接跨维度传送会被退回主世界。目标维度的星门结构会在 <ItemLink id="sgjourney:pegasus_dhd" link={false} /> 前方展示对应的神器。

* <Color color="#FFAA00">Abydos</Color>：地址 `-26-6-14-31-11-29-`，取得 <ItemLink id="avaritia:neutron_ring" link={false} />。
* <Color color="#55FF55">Chulak</Color>：地址 `-8-1-22-14-36-19-`，取得 <ItemLink id="avaritia:infinity_umbrella" link={false} />。
* <Color color="#555555">Cavum Tenebrae</Color>：地址 `-18-7-3-36-25-15-`，取得 <ItemLink id="avaritia:infinity_ring" link={false} />。
* <Color color="#55FFFF">Lantea</Color>：地址 `-18-20-1-15-14-7-19-`，取得 <ItemLink id="sgjourney:universe_stargate" link={false} />。

</Column>

<Column gap="2" fullWidth={true}>

## 星辰仪式

仪式发生在 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 的中子星处。开始前需要满足这些状态：

* <ItemLink id="gtladditions:forge_of_the_antichrist" /> 已达到最大效率。
* 它已与 <ItemLink id="gtladditions:recursive_reverse_array" /> 绑定。
* 递归反演阵列的仪式门控处于有效状态：相关机器就绪，<ItemLink id="gtladditions:supratemporal_boosting_engine" /> 正在运行，未过热，并处在 93000K-97000K 的最佳温度区间。

满足条件后，把四件神器丢向伪神之煅炉中央的中子星。中子星每次收集一件未收集过的神器；第一件神器被收集后，星体会转为彩虹状态。四件全部集齐后，星体开始坍缩，约 7 秒后在中央生成 <ItemLink id="gtceu:creative_chest" />。

<Row>
    <BlockImage id="gtceu:creative_chest" scale="4" />
</Row>

</Column>

</Column>