---
navigation:
  title: 太虚浊化阵
  icon: taixu_turbid_array
  parent: controller/multiblock_controller.md
  position: 31
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:taixu_turbid_array
---

# 太虚浊化阵

<BlockImage id = "gtladditions:taixu_turbid_array" scale = "8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 太虚浊化阵处理混沌编织配方，作为 <ItemLink id="gtceu:large_recycler" link={false} /> 的上位替代。
* 结构只能使用一个激光仓。
* 机器固定耗能为当前电压下 524288 A。
* 常规模式固定耗时 5 秒；跨配方模式下最终配方耗时固定为 1 秒，并额外使用 16 倍总耗能。
* 机器固定拥有 128 个跨配方线程。

</Column>

<Column gap="2" fullWidth={true}>

### 槽位与嵌入物

* 主机右下角槽位最多放入 64 个有效物品。
* <ItemLink id="gtceu:enderium_nanoswarm" /> 每个提供 0.01 成功率加成。
* <ItemLink id="gtceu:draconium_nanoswarm" /> 每个提供 0.05 成功率加成。
* <ItemLink id="gtceu:spacetime_nanoswarm" /> 每个提供 0.1 成功率加成。
* <ItemLink id="gtceu:eternity_nanoswarm" /> 每个提供 0.2 成功率加成。
* 放入 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 作为槽位物品时，<FluidLink id="gtceu:uu_amplifier" /> 和 <FluidLink id="gtceu:uu_matter" /> 成功率直接达到 100%。该槽位不接受 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />。
* 额外嵌入 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 时，最多可嵌入 2048 个星阵；每个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 按 1024 个计入，嵌入第一颗星阵后会开启跨配方模式。

</Column>

<Column gap="2" fullWidth={true}>

### 输出解锁

* 电压等级达到 <Color color="#FFFF00">**UXV**</Color> 时，混沌编织配方会额外尝试输出 <FluidLink id="gtceu:uu_amplifier" />。
* 电压等级达到 <Color color="#FF0000">**MAX**</Color> 时，混沌编织配方会额外尝试输出 <FluidLink id="gtceu:uu_matter" />。
* 最终输出量为当前并行数乘以基础产出量。

</Column>

<Column gap="2" fullWidth={true}>

### 公式

* 结构中的 <ItemLink id="gtlcore:stellar_containment_casing" />、<ItemLink id="gtlcore:advanced_stellar_containment_casing" /> 或 <ItemLink id="gtlcore:ultimate_stellar_containment_casing" /> 会提供等级加成：

<Latex math = "\alpha = 8 * (2^{结构等级} - 1) * \sqrt{电压等级 + 1}" />

* 线圈加成：

<Latex math = "\beta = 3.8 * 1.3^{线圈等级} * (\frac{线圈温度}{36000})^{0.7}" />

* <FluidLink id="gtceu:uu_amplifier" /> 成功率：

<Latex math = "\frac{100}{1 + e^{-0.1 * (\frac{\alpha}{50} + \frac{\beta}{100} + \frac{高度}{3})}}" />

* <FluidLink id="gtceu:uu_amplifier" /> 基础产出：

<Latex math = "40960 * \tanh(0.007 * (\alpha * \frac{高度}{9} + \sqrt{\beta} * \ln(电压等级 + 2)))" />

* <FluidLink id="gtceu:uu_matter" /> 成功率：

<Latex math = "100 * (1 - e^{-0.02 * (\frac{\alpha + \beta}{20} + \sqrt[3]{高度} * \frac{电压等级}{3})})" />

* <FluidLink id="gtceu:uu_matter" /> 基础产出：

<Latex math = "22500 * \tanh(\sqrt{\alpha * \beta} * \frac{(高度 + 电压等级) * 0.045}{200})" />

* 未嵌入 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 或 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 时的常规并行：

<Latex math = "4096 * 1.621^{\frac{线圈温度}{6400}}" />

* 嵌入 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 或 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 后的并行：

<Latex math = "3^{16} * count^{1.392}" />

* 公式中的 count 是当前已额外嵌入的 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 数量；每个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 按 1024 个计入。
* 已嵌入的 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 会失去压缩态；打掉主机时，嵌入数量只会以普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 形式返还。
* 公式中的高度指结构中可重复层的高度。

</Column>

</Column>