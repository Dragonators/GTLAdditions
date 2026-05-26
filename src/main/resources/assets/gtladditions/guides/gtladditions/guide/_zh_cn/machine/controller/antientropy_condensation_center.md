---
navigation:
  title: 反熵冷凝中枢
  icon: antientropy_condensation_center
  parent: controller/multiblock_controller.md
  position: 30
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:antientropy_condensation_center
  - gtladditions:relativistic_heat_capacitor
---

# 反熵冷凝中枢

<BlockImage id = "gtladditions:antientropy_condensation_center" scale = "4"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 反熵冷凝中枢处理等离子冷凝与真空冷冻机配方。
* 不再需要普通冷却过程中额外的 <FluidLink id="gtceu:liquid_helium" /> 输入。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。

</Column>

<Column gap="2" fullWidth={true}>

### 冷凝消耗

* 每次工作前需要从输入侧消耗 <ItemLink id="kubejs:dust_cryotheum" />。
* 消耗数量按本轮总并行数计算：

<Latex math = "消耗量 = \frac{5 * (\frac{总并行}{524288} + 51 * \ln(总并行))}{\max(1, 电压等级 - 9)}" />

* 机器界面会显示上一次尝试工作时计算出的 <ItemLink id="kubejs:dust_cryotheum" /> 需求。

</Column>

<Column gap="2" fullWidth={true}>

### 热容嵌入

* 手持一组 <ItemLink id="gtladditions:relativistic_heat_capacitor" /> 右键主机，可以将其嵌入反熵冷凝中枢。
* 嵌入后耗能倍率变为 0.35。
* 嵌入后单次工作至多消耗 1 个 <ItemLink id="kubejs:dust_cryotheum" />；如果本轮总并行数不超过 INT.MAX，则不消耗 <ItemLink id="kubejs:dust_cryotheum" />。
* 拆除主机时会返还已嵌入的一组 <ItemLink id="gtladditions:relativistic_heat_capacitor" />。

</Column>

</Column>