---
navigation:
  title: Helio 系列煅炉模块
  icon: heliofusion_exoticizer
  parent: controller/multiblock_controller.md
  position: 47
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:helioflare_power_forge
  - gtladditions:heliofluix_melting_core
  - gtladditions:heliofusion_exoticizer
  - gtladditions:heliophase_leyline_crystallizer
  - gtladditions:heliothermal_plasma_fabricator
---

# Helio 系列煅炉模块

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

Helio 系列是 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 的工作模块。模块需要安装在伪神之煅炉周围的模块位置，并连接到一台已成型且正在工作，或被 <ItemLink id="gtladditions:spacetime_stasis_device" /> 锚定的主机；未连接、主机未运行且未被锚定时，模块不会推进配方。

* 每个模块都直接使用无线能源网络供能，不需要能源仓或激光仓。
* 模块拥有无限并行和无限线程，只要能够满足足够的输入、输出空间和无线能源供应。
* 模块共享主机连续运行带来的 EU 减免。
* 部分模块会在指定配方类别上共享主机的产出倍率；该倍率来自 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 的连续运行效率和 <ItemLink id="gtladditions:recursive_reverse_array" /> 增益。
* 模块运行不会替代主机自身配方；它们是附加的独立处理线路。

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:helioflare_power_forge" />

<BlockImage id="gtladditions:helioflare_power_forge" scale="4"/>

* 用于基础热处理路线，可处理熔炉、高炉、合金炉与合金高炉配方。
* 运行时耗电倍率为 0.2，并共享主机的 EU 减免。
* 执行合金高炉配方时，共享主机的产出倍率。

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:heliofluix_melting_core" />

<BlockImage id="gtladditions:heliofluix_melting_core" scale="4"/>

* 用于恒星射流下的混沌相变，可处理混沌炼金与分子解构配方。
* 运行时耗电倍率为 0.2，并共享主机的 EU 减免。
* 执行混沌炼金配方时，共享主机的产出倍率；分子解构配方只共享 EU 减免。

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:heliofusion_exoticizer" />

<BlockImage id="gtladditions:heliofusion_exoticizer" scale="4"/>

* 用于极端温度下的物质异化，处理物质异化配方。
* 运行时耗电倍率为 0.5，并共享主机的 EU 减免与产出倍率。
* 该模块一次只锁定并运行一种可用配方；当前锁定配方拥有无限并行。

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:heliothermal_plasma_fabricator" />

<BlockImage id="gtladditions:heliothermal_plasma_fabricator" scale="4"/>

* 用于向材料注入恒星级热能，可处理星焰跃迁、核聚变和粒子对撞机配方。
* 运行时耗电倍率为 0.2，并共享主机的 EU 减免。
* 执行聚变或粒子对撞机配方时，共享主机的产出倍率；星焰跃迁配方只共享 EU 减免。

</Column>

<Column gap="2" fullWidth={true}>

### <ItemLink id="gtladditions:heliophase_leyline_crystallizer" />

<BlockImage id="gtladditions:heliophase_leyline_crystallizer" scale="4"/>

* 用于借助群星连接龙脉回廊，处理龙脉结晶配方。
* 只有连接的 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 已达到最大效率，并正在运行或被 <ItemLink id="gtladditions:spacetime_stasis_device" /> 锚定时才会工作。
* 运行时耗电倍率为 256，并共享主机的 EU 减免。
* 该模块不共享主机产出倍率。

</Column>

</Column>