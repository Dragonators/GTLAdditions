---
navigation:
  title: 伪神之煅炉
  icon: forge_of_the_antichrist
  parent: controller/multiblock_controller.md
  position: 43
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:forge_of_the_antichrist
---

# 伪神之煅炉

<BlockImage id="gtladditions:forge_of_the_antichrist" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 伪神之煅炉可以在超维度熔炼、恒星热能熔炼和终极物质锻造配方之间进行跨配方模式并行处理。
* 机器直接使用无线能源网络供能，并不需要安装任何能源/激光仓。
* 伪神之煅炉拥有无限并行和无限线程，只要能够满足足够的输入、输出空间和无线能源供应。

</Column>

<Column gap="2" fullWidth={true}>

### 连续运行

* 伪神之煅炉连续工作时会积累运行时间。
* 运行时间越长，产出倍率越高，耗电倍率越低。
* 连续运行 4 小时后达到最大效率：产出最高可达 15 倍，耗电最低可降至 0.2 倍。
* 机器空闲时会逐步失去已积累的运行时间。

</Column>

<Column gap="2" fullWidth={true}>

### 容器配方倍增

* 伪神之煅炉提高产出倍率时，普通物品输出和流体输出会随 <Color color="#55FFFF">当前产出倍率</Color> 提高，但部分容器不会被简单复制。
* 在超维度熔炼和恒星热能熔炼配方中，<ItemLink id="kubejs:extremely_durable_plasma_cell" />、<ItemLink id="kubejs:time_dilation_containment_unit" /> 和 <ItemLink id="kubejs:plasma_containment_cell" /> 会被视为循环容器。
* 如果循环容器出现在输入侧，它会按 <Color color="#55FFFF">当前产出倍率</Color> 一起增加消耗；如果循环容器出现在输出侧，它会保持原配方数量，不会随产出倍率复制。
* 可以把这理解为：额外产出需要额外消耗对应容器，但配方返还的空容器或循环容器不会凭空变多。

#### 受循环容器规则影响的配方

以下展示的是原始配方；伪神之煅炉实际运行时会在原始配方基础上套用上面的容器倍增规则。

超维度熔炼：

* 无尽催化剂：<ItemLink id="kubejs:time_dilation_containment_unit" /> 是循环容器输出，保持原配方的 64 个，不随产出倍率复制。

<Recipe id="avaritia:dimensionally_transcendent_plasma_forge/infinity_catalyst" />

* 宇宙中子等离子单元：输入的 <ItemLink id="kubejs:extremely_durable_plasma_cell" /> 按 <Color color="#55FFFF">当前产出倍率</Color> 倍消耗，产出的 <ItemLink id="kubejs:cosmic_neutron_plasma_cell" /> 按产出倍率增加。

<Recipe id="kubejs:dimensionally_transcendent_plasma_forge/cosmic_neutron_plasma_cell" />

* 致密中子等离子：<ItemLink id="kubejs:plasma_containment_cell" /> 是循环容器输出，保持原配方数量，不随产出倍率复制。

<Recipe id="gtceu:dimensionally_transcendent_plasma_forge/dense_neutron_plasma" />

恒星热能熔炼：

* 中子等离子密闭容器：输入的 <ItemLink id="kubejs:plasma_containment_cell" /> 按 <Color color="#55FFFF">当前产出倍率</Color> 倍消耗，产出按产出倍率增加。

<Recipe id="kubejs:stellar_forge/neutron_plasma_containment_cell" />

* 自由质子气：<ItemLink id="kubejs:time_dilation_containment_unit" /> 是循环容器输出，保持原配方数量，不随产出倍率复制。

<Recipe id="gtceu:stellar_forge/free_proton_gas" />

* 传奇等离子：<ItemLink id="kubejs:plasma_containment_cell" /> 是循环容器输出，保持原配方数量，不随产出倍率复制。

<Recipe id="gtceu:stellar_forge/legendarium_plasma" />

* 高密度质子物质容器：输入的 <ItemLink id="kubejs:time_dilation_containment_unit" /> 按 <Color color="#55FFFF">当前产出倍率</Color> 倍消耗，产出按产出倍率增加。

<Recipe id="kubejs:stellar_forge/contained_high_density_protonic_matter" />

* 赖斯纳-诺德斯特伦奇点容器：输入的 <ItemLink id="kubejs:time_dilation_containment_unit" /> 按 <Color color="#55FFFF">当前产出倍率</Color> 倍消耗，产出按产出倍率增加。

<Recipe id="kubejs:stellar_forge/contained_reissner_nordstrom_singularity" />

#### 额外追加输入的配方

* 奇异物质容器：在原配方输入之外，额外消耗相当于 <Color color="#55FFFF">当前产出倍率 - 1</Color> 倍的 <ItemLink id="kubejs:time_dilation_containment_unit" />。

<Recipe id="kubejs:stellar_forge/contained_exotic_matter" />

* 高耐久等离子体容器：在原配方输入之外，额外消耗相当于 <Color color="#55FFFF">当前产出倍率 - 1</Color> 倍的 <ItemLink id="kubejs:extremely_durable_plasma_cell" />；配方输出中的 <ItemLink id="kubejs:extremely_durable_plasma_cell" /> 是循环容器，保持原配方数量，不随产出倍率复制。

<Recipe id="kubejs:stellar_forge/extremely_durable_plasma_cell" />

* 克尔-纽曼黑洞奇点容器：在原配方输入之外，额外消耗相当于 <Color color="#55FFFF">当前产出倍率 - 1</Color> 倍的 <ItemLink id="kubejs:time_dilation_containment_unit" />；配方输出中的 <ItemLink id="kubejs:time_dilation_containment_unit" /> 是循环容器，保持原配方的 63 个，不随产出倍率复制。

<Recipe id="kubejs:stellar_forge/contained_kerr_newmann_singularity" />

* 当 <Color color="#55FFFF">当前产出倍率</Color> 小于 2 时，上述三条配方不会追加这部分额外输入。

</Column>

<Column gap="2" fullWidth={true}>

### Helio 模块

* 伪神之煅炉可以连接 Helio 系列模块，扩展出额外的材料处理线路。
* 模块需要安装在主机周围的模块位置，并连接到正在运行或被 <ItemLink id="gtladditions:spacetime_stasis_device" /> 锚定的伪神之煅炉；未连接、主机未运行且未被锚定时，模块不会工作。
* 已连接模块共享主机连续运行带来的 EU 减免；部分模块在指定配方类别上还会共享主机的产出倍率。
* <ItemLink id="gtladditions:heliophase_leyline_crystallizer" /> 要求主机已经达到最大效率才会工作。
* 五台模块的配方类别和共享倍率规则请查看 <ItemLink id="gtladditions:heliofusion_exoticizer" /> 页面。

</Column>

<Column gap="2" fullWidth={true}>

### 递归反演

* 伪神之煅炉可以与 <ItemLink id="gtladditions:recursive_reverse_array" /> 绑定，获得递归反演阵列和其模块提供的增益。
* <ItemLink id="gtladditions:recursive_reverse_array" /> 可帮助保留空闲时的运行时间，让 Helio 模块将空闲主机视为被 <ItemLink id="gtladditions:spacetime_stasis_device" /> 锚定，并在条件满足时进一步影响产出、耗电和部分输出处理。
* 相关模块的具体需求和效果请查看 <ItemLink id="gtladditions:recursive_reverse_array" /> 页面。

</Column>

</Column>