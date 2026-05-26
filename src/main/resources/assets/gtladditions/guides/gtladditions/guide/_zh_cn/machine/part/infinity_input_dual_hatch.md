---
navigation:
  title: 无尽输入总成
  icon: infinity_input_dual_hatch
  parent: part/machine_part_index.md
  position: 8
categories:
  - 机器仓室
item_ids:
  - gtladditions:infinity_input_dual_hatch
---

# 无尽输入总成

<Column gap="15" fullWidth={true}>

<BlockImage id="gtladditions:infinity_input_dual_hatch" scale="8" />

<Column gap="2" fullWidth={true}>

* 你需要在 <Color color="#FFFF00">**UXV**</Color> 阶段后期才能制作它。
* 无尽输入总成是面向后期多方块的输入总成，提供无尽的物品输入与流体输入能力。
* 它按物品或流体种类聚合存储内容，而不是使用固定数量的普通槽位。
* 单种物品数量与单种流体容量均可达到 Long.MAX 级别。

</Column>

<Column gap="2" fullWidth={true}>

## ME 网络优化

* 无尽输入总成针对 ME 网络做了特殊优化。
* 与 <ItemLink id="gtladditions:super_input_dual_hatch" /> 不同，它能够像未标记的 ME 接口一样，一次性把内部所有内容返回到 ME 网络。
* AE 样板供应器能够一次性向其输入 Long.MAX 级别的物品与流体，不会遭到截断。

</Column>

</Column>