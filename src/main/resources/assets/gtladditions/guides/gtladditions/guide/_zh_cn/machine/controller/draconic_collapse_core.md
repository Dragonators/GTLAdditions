---
navigation:
  title: 龙怒坍缩核心
  icon: draconic_collapse_core
  parent: controller/multiblock_controller.md
  position: 24
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:draconic_collapse_core
---

# 龙怒坍缩核心

<BlockImage id="gtladditions:draconic_collapse_core" scale="8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:draconic_collapse_core" /> 处理聚合装置配方。
* 它支持无损超频、激光输入和并行处理。

</Column>

<Column gap="2" fullWidth={true}>

### 舱室支持

* 支持激光输入仓，结构中至少需要 1 个，最多接受 2 个。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。
* 输入位置只支持普通物品输入总线、巨型物品输入总线、<ItemLink id="gtladditions:me_super_pattern_buffer" /> 与 <ItemLink id="gtladditions:me_super_pattern_buffer_proxy" />。

</Column>

<Column gap="2" fullWidth={true}>

### 并行计算

* 最大并行由机器电压等级决定；电压等级每高出 UEV 一级，最大并行乘以 8。

<Latex math = "最大并行 = 8^{电压等级 - 10}" />

* 公式中的 UEV 记为 10；因此 UEV 为 1 并行，UIV 为 8 并行，UXV 为 64 并行。
* 成型后，界面会显示当前并行。

</Column>

</Column>