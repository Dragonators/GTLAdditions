---
navigation:
  title: 物料谱解析仓
  icon: spectral_analysis_hatch
  parent: machine/part/machine_part_index.md
  position: 20
categories:
  - 机器仓室
item_ids:
  - gtladditions:spectral_analysis_hatch
---

# 物料谱解析仓

~~别名op仓~~

<BlockImage id="gtladditions:spectral_analysis_hatch" scale="4" />

<Column gap="15" fullWidth={true}>
<Column gap="2" fullWidth={true}>

* 物料谱解析仓是额外的附属仓室，可以安装在 <ItemLink id="gtceu:integrated_ore_processor" />、<ItemLink id="gtceu:advanced_integrated_ore_processor" />、<ItemLink id="gtladditions:space_infinity_integrated_ore_processor" /> 和 <ItemLink id="gtceu:advanced_infinite_driller" /> 中。
* 最多只能安装一个物料谱解析仓。
* GUI 中有 3 个频段，每个频段各有一个随机生成的隐藏目标频号（0–255）和 3 盏指示灯。
* 可以直接输入数值，也可以拖动文本框下方的滑动条来调节频号。
* 指示灯点亮的数量代表该频段的激活等级：<Color color="#00FF00">绿色为激活状态</Color>，<Color color="#FF0000">红色为未激活状态</Color>。
* 激活等级 1 的误差范围为 **±80**，等级 2 为 **±35**，等级 3 为 **±5**。
* 当三个频段的设定值均与各自目标频号完全一致（零误差）时，触发**完美调频**。

</Column>

## 矿石处理厂效果

<Column gap="2" fullWidth={true}>

* **频段 1**：提高并行数或额外跨配方线程。
> 对 <ItemLink id="gtceu:integrated_ore_processor" />，它会在常规并行仓生效之后，按倍率进一步提高配方并行：\
> 等级 0：×4 并行；等级 1：×6 并行；等级 2：×8 并行；等级 3：×10 并行。
>
> 对 <ItemLink id="gtceu:advanced_integrated_ore_processor" />，它会在基础跨配方线程与 <ItemLink id="gtladditions:thread_modifier_hatch" /> 线程之外，额外增加跨配方处理线程：\
> 等级 0：+72 线程；等级 1：+96 线程；等级 2：+128 线程；等级 3：+144 线程。
>
> 对 <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" /> 没有实际收益，因为该机器本身已拥有无限并行与无限线程。

</Column>
<Column gap="2" fullWidth={true}>

* **频段 2**：将物品输出的电压概率加成乘以「频段等级 + 1」（三台矿石处理厂均生效）。
> 例如：在等级 2 时，若某输出物品的基础概率为 30%、基础电压加成为 5%，加成后的每级电压加成为：

<Latex math = "5 * (2 + 1) = 15" />

</Column>
<Column gap="2" fullWidth={true}>

* **频段 3**：根据机器类型提供耗时或流体消耗减免倍率。
> <ItemLink id="gtceu:integrated_ore_processor" />：配方耗时倍率；<ItemLink id="gtceu:advanced_integrated_ore_processor" />：输入流体消耗倍率；<ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />：没有实际收益。\
> 等级 0：×0.8；等级 1：×0.65；等级 2：×0.5；等级 3：×0.4。

</Column>
<Column gap="2" fullWidth={true}>

* **完美调频**：
> 对 <ItemLink id="gtceu:integrated_ore_processor" />：超频方式由有损超频切换为无损超频。\
> 对 <ItemLink id="gtceu:advanced_integrated_ore_processor" />：在基础跨配方线程、<ItemLink id="gtladditions:thread_modifier_hatch" /> 线程和频段 1 额外线程叠加后，将跨配方总线程翻倍（×2）。\
> 对 <ItemLink id="gtladditions:space_infinity_integrated_ore_processor" />：配方输出翻倍（×2）。

</Column>

## 进阶无尽钻机效果

<Column gap="2" fullWidth={true}>

安装在 <ItemLink id="gtceu:advanced_infinite_driller" /> 中时，物料谱解析仓会改为提供钻井专属加成：

* **频段 1（产量加成）**：提升流体开采产量，并可与 <ItemLink id="gtladditions:thread_modifier_hatch" /> 提供的线程数相乘叠加。
> 等级 0：×1.5；等级 1：×2.3；等级 2：×3.2；等级 3：×4。

</Column>
<Column gap="2" fullWidth={true}>

* **频段 2（耗能减免）**：降低单倍采掘耗能倍率。
> 等级 0：×0.8；等级 1：×0.6；等级 2：×0.2；等级 3：×0.05。

</Column>
<Column gap="2" fullWidth={true}>

* **频段 3（工作范围）**：扩大钻机的区块开采范围（未安装解析仓时默认为 5×5 区块）。
> 等级 0：7×7；等级 1：9×9；等级 2：11×11；等级 3：13×13。\
> 调整频段等级后，工作范围会自动同步更新。

</Column>
<Column gap="2" fullWidth={true}>

* **完美调频**：
> 温控免维护：装入钻头且线圈达标后，不再需要通入升温流体与冷却液，温度会自动保持在当前线圈允许的最高安全温度，钻头也不会过热损坏。\
> 额外超频：满足常规产量耗电后若仍有富余供电，最多可额外进行 15 级超频。可用输入电力每达到当前所需功率的 4 倍即可提升 1 级，每级使实际流体产量与实际耗电各翻倍。

</Column>
</Column>
