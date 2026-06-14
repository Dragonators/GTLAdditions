---
navigation:
  title: 超级样板总成
  icon: me_super_pattern_buffer
  parent: part/machine_part_index.md
  position: 9
categories:
  - 机器仓室
item_ids:
  - gtladditions:me_super_pattern_buffer
  - gtladditions:me_super_pattern_buffer_proxy
---

# 超级样板总成

<Column gap="15" fullWidth={true}>

<Row>
    <BlockImage id="gtladditions:me_super_pattern_buffer" scale="6" />
    <BlockImage id="gtladditions:me_super_pattern_buffer_proxy" scale="6" />
</Row>

<Column gap="2" fullWidth={true}>

* 你需要在 <Color color="#FFFF00">**UXV**</Color> 阶段才能制作它。
* 超级样板总成是更大的 ME 样板总成，拥有可配置的每行样板数、每页行数和最大页数。
* 它提供 <ItemLink id="gtceu:me_extend_pattern_buffer" /> 拥有的所有能力。
* GUI 中的分页由配置决定，适合集中管理大量终端样板。
* 它也支持 GTLCore 的 <ItemLink id="gtlcore:me_pattern_buffer_copy" /> 与 <ItemLink id="gtlcore:me_pattern_buffer_cut" />。

</Column>

<Column gap="2" fullWidth={true}>

## 神锻样板模式

* 神锻样板模式配置页默认关闭，只会改变此 <ItemLink id="gtladditions:me_super_pattern_buffer" /> 向 AE 终端暴露的样板。
* 样板输出倍率范围为 1 到 30，默认值为 15，对应 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 最大效率。
* 启用后，原始样板输入输出会按照 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 的配方倍增规则进行修改。

</Column>

<Column gap="2" fullWidth={true}>

## 闪存绑定

* 手持 <ItemLink id="gtceu:data_stick" /> 右键 <ItemLink id="gtladditions:me_super_pattern_buffer" />，会记录该总成的绑定信息。
* 手持记录过绑定信息的 <ItemLink id="gtceu:data_stick" /> 右键 <ItemLink id="gtladditions:me_super_pattern_buffer_proxy" />，会让镜像绑定到对应的 <ItemLink id="gtladditions:me_super_pattern_buffer" />。
* 镜像只接收 <ItemLink id="gtladditions:me_super_pattern_buffer" /> 坐标；普通样板总成坐标不会被作为有效目标。

</Column>



</Column>