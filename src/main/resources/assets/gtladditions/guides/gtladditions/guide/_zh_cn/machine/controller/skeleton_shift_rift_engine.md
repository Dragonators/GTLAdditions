---
navigation:
  title: 骸变速隙机
  icon: skeleton_shift_rift_engine
  parent: controller/multiblock_controller.md
  position: 33
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:skeleton_shift_rift_engine
---

# 骸变速隙机

<BlockImage id="gtladditions:skeleton_shift_rift_engine" scale="8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:skeleton_shift_rift_engine" /> 处理衰变加速配方。
* 它支持激光输入和无损超频。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。
* 结构中的 <ItemLink id="gtlcore:stellar_containment_casing" />、<ItemLink id="gtlcore:advanced_stellar_containment_casing" /> 或 <ItemLink id="gtlcore:ultimate_stellar_containment_casing" /> 会决定恒星热力容器等级。

</Column>

<Column gap="2" fullWidth={true}>

### 公式

* 线圈温度每提高 1200K，最大并行数乘以 2：

<Latex math = "最大并行 = \min(2147483647, \left\lfloor 2^{\frac{线圈温度}{1200}} \right\rfloor)" />

* 恒星热力容器等级越高，普通配方的基础耗时越短，最低为 1 tick：

<Latex math = "基础耗时 = \max(1, \left\lfloor \frac{原配方耗时}{恒星热力容器等级} \right\rfloor)" />

* 处于跨配方模式下时，恒星热力容器等级会转化为总耗能倍率：

<Latex math = "耗能倍率 = \frac{1}{\max(1, 恒星热力容器等级)}" />

* 主机界面会显示当前并行数和恒星热力容器等级；结构重新成型后会重新计算这些数值。

</Column>

</Column>