---
navigation:
  title: 亚原子置换核心
  icon: subatomic_transmutatioon_core
  parent: controller/multiblock_controller.md
  position: 25
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:subatomic_transmutatioon_core
---

# 亚原子置换核心

<BlockImage id="gtladditions:subatomic_transmutatioon_core" scale="8" />

* 结构中需要安装 <ItemLink id="gtladditions:me_block_conservation" />
* 只能使用泰坦钢或以上线圈
* 需要能够提供 UHV 电压的能源输入仓；每个工作 tick 固定消耗 UHV 级功率
* 主机 GUI 的卡片槽内需要放入一张转换卡或普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />；<ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 的压缩态过于不稳定，无法放入该直接槽位。
* 结构成型后机器会持续运行，并在每轮结束时执行方块转换

## 转换卡

并行数由安装的线圈温度计算。设：

<Latex math = "base = \frac{线圈温度}{100}" size="24" />

> 转换卡：<Latex math = "并行 = base^2" size="24" /> 每轮 60 tick，每轮转换一种匹配输入 \
> 高速转换卡：<Latex math = "并行 = base^{3.5}" size="24" /> 每轮 40 tick，每轮转换一种匹配输入 \
> 极限转换卡：<Latex math = "并行 = base^{4.2}" size="24" /> 每轮 30 tick，可在同一轮继续转换多种匹配输入 \
> 星阵：<Latex math = "并行 = base^{6.35}" size="24" /> 每轮 20 tick，可在同一轮继续转换多种匹配输入

<Row>
    <Recipe id="gtladditions:em_resonance_conversion_field/block.kubejs.draconium_block_charged" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.moss_block" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.warped_stem" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.sculk" />
</Row>

<Row>
    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.crimson_stem" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.minecraft.bone_block" />

    <Recipe id="gtladditions:em_resonance_conversion_field/block.kubejs.essence_block" />
</Row>