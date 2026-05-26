---
navigation:
  title: 光之蚀梦者
  icon: lucid_etchdreamer
  parent: controller/multiblock_controller.md
  position: 20
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:lucid_etchdreamer
---

# 光之蚀梦者

<BlockImage id = "gtladditions:lucid_etchdreamer" scale = "8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 光之蚀梦者处理由激光蚀刻派生的光子晶阵蚀刻配方。
* 机器支持线圈并行、激光仓输入和跨配方并行。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。
> 光子晶阵蚀刻配方不需要算力输入，也不需要数据研究。

</Column>

<Column gap="2" fullWidth={true}>

### 并行计算

* 机器的基础并行由线圈温度决定：

<Latex math = "基础并行 = \min(2147483647, \lfloor 2^{\frac{线圈温度}{900}} \rfloor)" />

</Column>

</Column>