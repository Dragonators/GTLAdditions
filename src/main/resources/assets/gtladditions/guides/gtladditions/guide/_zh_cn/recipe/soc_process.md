---
navigation:
  title: SoC 产线
  parent: recipe/recipe_index.md
  position: 20
categories:
  - 配方
item_ids:
  - gtladditions:echo_shard_boule
  - gtladditions:hassium_boule
  - gtladditions:starmetal_boule
  - gtladditions:periodicium_boule
  - gtladditions:infinity_boule
  - gtladditions:gallium_oxide_dust
  - gtladditions:echo_shard_wafer
  - gtladditions:hassium_wafer
  - gtladditions:starmetal_wafer
  - gtladditions:periodicium_wafer
  - gtladditions:infinity_wafer
  - gtladditions:bioware_echo_shard_wafer
  - gtladditions:prepare_extraordinary_soc_wafer
  - gtladditions:dragon_element_starmetal_wafer
  - gtladditions:prepare_spacetime_soc_wafer
  - gtladditions:prepare_primary_soc_wafer
  - gtladditions:outstanding_soc_wafer
  - gtladditions:extraordinary_soc_wafer
  - gtladditions:chaos_soc_wafer
  - gtladditions:spacetime_soc_wafer
  - gtladditions:primary_soc_wafer
  - gtladditions:outstanding_soc
  - gtladditions:extraordinary_soc
  - gtladditions:chaos_soc
  - gtladditions:spacetime_soc
  - gtladditions:primary_soc
---

# SoC 产线

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

GTLAdditions 补充了一条 SoC 产线，用 SoC 芯片简化对应等级的电路组装机配方。

</Column>

<Column gap="2" fullWidth={true}>

## 单晶硅

<Row>
  <ItemImage id="gtladditions:echo_shard_boule" scale="4" />
  <ItemImage id="gtladditions:hassium_boule" scale="4" />
  <ItemImage id="gtladditions:starmetal_boule" scale="4" />
  <ItemImage id="gtladditions:periodicium_boule" scale="4" />
  <ItemImage id="gtladditions:infinity_boule" scale="4" />
</Row>

* 先通过镓、硫酸、氨和氧气制备 <ItemLink id="gtladditions:gallium_oxide_dust" />。
* 使用硅单晶、<ItemLink id="gtladditions:gallium_oxide_dust" />、对应粉和氪气，在电力高炉中制备不同等级的单晶硅。

</Column>

<Column gap="2" fullWidth={true}>

## 晶圆

<Row>
  <ItemImage id="gtladditions:echo_shard_wafer" scale="4" />
  <ItemImage id="gtladditions:hassium_wafer" scale="4" />
  <ItemImage id="gtladditions:starmetal_wafer" scale="4" />
  <ItemImage id="gtladditions:periodicium_wafer" scale="4" />
  <ItemImage id="gtladditions:infinity_wafer" scale="4" />
</Row>

* 将对应单晶硅放入切割机，可以得到对应晶圆。
* 等级较高的切割配方需要更高等级的洁净水。

</Column>

<Column gap="2" fullWidth={true}>

## 处理后晶圆

<Row>
  <ItemImage id="gtladditions:bioware_echo_shard_wafer" scale="4" />
  <ItemImage id="gtladditions:prepare_extraordinary_soc_wafer" scale="4" />
  <ItemImage id="gtladditions:dragon_element_starmetal_wafer" scale="4" />
  <ItemImage id="gtladditions:prepare_spacetime_soc_wafer" scale="4" />
  <ItemImage id="gtladditions:prepare_primary_soc_wafer" scale="4" />
</Row>

* 不同等级晶圆会进入化学浴、大型化学反应、SPS、量子操纵者或超维度搅拌等步骤，制成后续蚀刻用晶圆。
* 高阶步骤会使用维度或法则洁净室条件；精确条件以 JEI 为准。

</Column>

<Column gap="2" fullWidth={true}>

## SoC 晶圆

<Row>
  <ItemImage id="gtladditions:outstanding_soc_wafer" scale="4" />
  <ItemImage id="gtladditions:extraordinary_soc_wafer" scale="4" />
  <ItemImage id="gtladditions:chaos_soc_wafer" scale="4" />
  <ItemImage id="gtladditions:spacetime_soc_wafer" scale="4" />
  <ItemImage id="gtladditions:primary_soc_wafer" scale="4" />
</Row>

* 将处理后晶圆放入维度聚焦激光蚀刻阵列，或使用光子矩阵蚀刻配方，可以得到对应 SoC 晶圆。
* 后两级 SoC 晶圆需要使用时空透镜作为蚀刻工具。

</Column>

<Column gap="2" fullWidth={true}>

## SoC 芯片

<Row>
  <ItemImage id="gtladditions:outstanding_soc" scale="4" />
  <ItemImage id="gtladditions:extraordinary_soc" scale="4" />
  <ItemImage id="gtladditions:chaos_soc" scale="4" />
  <ItemImage id="gtladditions:spacetime_soc" scale="4" />
  <ItemImage id="gtladditions:primary_soc" scale="4" />
</Row>

* 将 SoC 晶圆再次切割，就能得到对应 SoC 芯片。
* SoC 芯片会参与对应等级处理器的电路组装机配方，减少电路合成步骤和材料消耗。

</Column>

</Column>