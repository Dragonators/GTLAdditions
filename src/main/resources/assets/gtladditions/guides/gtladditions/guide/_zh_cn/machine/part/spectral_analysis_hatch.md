---
navigation:
  title: 物料谱解析仓
  icon: spectral_analysis_hatch
  parent: part/machine_part_index.md
  position: 20
categories:
  - 机器仓室
item_ids:
  - gtladditions:spectral_analysis_hatch
---

# 物料谱解析仓

~~别名op仓~~

<BlockImage id="gtladditions:spectral_analysis_hatch" scale="4" />

* 物料谱解析仓可以安装在 <ItemLink id="gtceu:integrated_ore_processor" />、<ItemLink id="gtceu:advanced_integrated_ore_processor" />、<ItemLink id="gtladditions:space_infinity_integrated_ore_processor" /> 中。
* 它是额外的附属仓室。每个兼容结构最多只能安装一个物料谱解析仓。
* GUI 中有三个频段。每个频段都有一个随机隐藏目标值和三个指示灯。
* 频段后的显示灯激活数量代表对应的激活等级, <Color color="#00FF00">绿色为激活状态</Color>, <Color color="#FF0000">红色则是未激活状态</Color>
* 激活等级 1 的范围是 **+/-80**，等级 2 的范围是 **+/-35**，等级 3 的范围是 **+/-5**。
* 可以直接输入频段值，也可以拖动文本框下方的滑动条来调节频号。

## 频段效果

<Column gap="15" fullWidth={true}>
<Column gap="2" fullWidth={true}>

* 频段 1 同时作用于 <ItemLink id="gtceu:integrated_ore_processor" /> 与 <ItemLink id="gtceu:advanced_integrated_ore_processor" />。
> 对 <ItemLink id="gtceu:integrated_ore_processor" />，它会在正常并行仓已经生效后，继续按倍率提高配方并行。 \
> 等级 0：x4 并行。等级 1：x6 并行。等级 2：x8 并行。等级 3：x10 并行。
>
> 对 <ItemLink id="gtceu:advanced_integrated_ore_processor" />，它会在基础跨配方线程与 <ItemLink id="gtladditions:thread_modifier_hatch" /> 线程之外，额外增加跨配方处理线程。\
> 等级 0：+72 线程。等级 1：+96 线程。等级 2：+128 线程。等级 3：+144 线程。
>
> 频段 1 对 <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" /> 没有实际收益，因为该机器已经拥有无限并行与无限线程。

</Column>
<Column gap="2" fullWidth={true}>

* 频段 2 会把物品输出的电压概率加成乘以该频段等级加一。
> 例如: 在等级2的情况下, 输出物品A, 基础出产概率为30%, 基础电压加成概率为5%, 则经过该频段加成后基础电压加成概率为:

<Latex math = "5 * (2 + 1) = 15" />

</Column>
<Column gap="2" fullWidth={true}>

* 频段 3 根据机器类型提供不同倍率。
> <ItemLink id="gtceu:integrated_ore_processor" />：配方耗时倍率。<ItemLink id="gtceu:advanced_integrated_ore_processor" />：输入流体消耗倍率。<ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />：没有实际收益。\
> 等级 0：x0.8。等级 1：x0.65。等级 2：x0.5。等级 3：x0.4。

</Column>
</Column>

## 完美调频

* 完美调频表示三个频段的输入值都与实际频号相同(即零误差)。
* 对 <ItemLink id="gtceu:integrated_ore_processor" />，完美调频会把超频逻辑从有损超频切换为无损超频。
* 对 <ItemLink id="gtceu:advanced_integrated_ore_processor" />，完美调频会在基础跨配方线程、<ItemLink id="gtladditions:thread_modifier_hatch" /> 线程和频段 1 线程已经生效后，把跨配方总线程乘以 2。
* 对 <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />，完美调频会让配方输出翻倍。