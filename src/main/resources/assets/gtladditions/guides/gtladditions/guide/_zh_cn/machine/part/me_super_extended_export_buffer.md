---
navigation:
  title: 超级增广输出总成
  icon: me_super_extended_export_buffer
  parent: part/machine_part_index.md
  position: 11
categories:
  - 机器仓室
item_ids:
  - gtladditions:me_super_extended_export_buffer
---

# 超级增广输出总成

<Column gap="15" fullWidth={true}>

<BlockImage id="gtladditions:me_super_extended_export_buffer" scale="6" />

<Column gap="2" fullWidth={true}>

* 你需要在 <Color color="#FFFF00">**UIV**</Color> 阶段才能制作它。
* 它提供 <ItemLink id="gtceu:me_extended_export_buffer" /> 拥有的所有能力。
* 在 <ItemLink id="gtceu:me_extended_export_buffer" /> 的基础上，将物品和流体的累计输出缓存提升至 **Int128**，每种物料上限为 **2^127 − 1**。

</Column>

<Column gap="2" fullWidth={true}>

## 输出循环上限

* 允许调整每轮向 ME 网络尝试插入物料的轮数，范围为 **1～4096**，默认 **64**。
* 每轮插入最多能处理单种 **long.max** 数量的物品或流体

</Column>

</Column>
