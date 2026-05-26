---
navigation:
  title: 超维度化工厂
  icon: dimensionally_transcendent_chemical_plant
  parent: controller/multiblock_controller.md
  position: 27
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:dimensionally_transcendent_chemical_plant
---

# 超维度化工厂

<BlockImage id = "gtladditions:dimensionally_transcendent_chemical_plant" scale = "8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 跨配方版本的大型化学反应釜。
* 它支持线圈并行和激光输入。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。

</Column>

<Column gap="2" fullWidth={true}>

### 并行计算

* 机器的基础并行由线圈温度决定：

<Latex math = "基础并行 = \min(2147483647, \lfloor 2^{\frac{线圈温度}{900}} \rfloor)" />

</Column>

<Column gap="2" fullWidth={true}>

### 跨配方分配

* 跨配方模式下，机器会把总并行预算分配给可运行的化学配方。
* <Color color="#00AAAA">**UV**</Color> 及以下配方不消耗有限并行预算；高于 <Color color="#00AAAA">**UV**</Color> 的配方会占用预算。

</Column>

</Column>