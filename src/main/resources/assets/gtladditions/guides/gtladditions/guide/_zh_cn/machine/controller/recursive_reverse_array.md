---
navigation:
  title: 递归反演阵列
  icon: recursive_reverse_array
  parent: machine/controller/multiblock_controller.md
  position: 40
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:recursive_reverse_array
  - gtladditions:suprachronal_data_module
  - gtladditions:catalytic_cascade_array
  - gtladditions:magnetorheological_convergence_core
  - gtladditions:spacetime_stasis_device
  - gtladditions:supratemporal_boosting_engine
---

# 递归反演阵列

<BlockImage id="recursive_reverse_array" scale="8"/>

递归反演阵列不是独立生产机器，而是与一台已成型的 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 绑定，并读取自身已成型模块的状态，为绑定主机提供增益。

使用 <ItemLink id="gtladditions:suprachronal_data_module" /> 进行绑定：

* 先右键一台已成型的 <ItemLink id="gtladditions:forge_of_the_antichrist" />，记录绑定源。
* 再右键同维度内一台已成型的递归反演阵列，建立绑定。
* 新的成功绑定会覆盖两台机器上已有的旧绑定。
* 生存模式下，绑定到阵列成功后会消耗该数据模块。创造模式下，只会清除模块内的记录。
* 潜行右键会清除已储存的记录。
* 跨维度绑定会被拒绝。

递归反演增益包要求绑定的 <ItemLink id="gtladditions:forge_of_the_antichrist" /> 已成型、阵列已成型，并且已连接的 <ItemLink id="gtladditions:supratemporal_boosting_engine" /> 正在运行且未过热。随后会按完美时空助推参数对本次增益包进行一次概率判定。温度位于 93000K-97000K 时，该参数为 1.0，增益包必定生效；区间外参数小于 1.0，增益包按概率生效。

增益概率判定失败时，催化迭升阵列的产出增益和 EU 增益、磁流聚敛核心聚焦、时空静滞装置、超时空助推引擎 EU 减免都不会生效。绑定主机自身基于运行时间的基础产出倍率和 EU 减免仍按原逻辑运行。星辰仪式入口与该概率判定分离：它严格要求助推引擎正在运行、未过热且温度位于 93000K-97000K。

## 模块

每个模块都必须在阵列模块位置成型并连接到阵列。模块在未连接到阵列时会中断自身运行。

<Column gap="20" fullWidth={true}>
<Column gap="2" fullWidth={true}>

### 催化迭升阵列

<BlockImage id="catalytic_cascade_array" scale="4"/>

* 催化循环成功时，为绑定主机提供 2 倍产出倍率。
* EU 增益有效时，额外提供 0.15 倍 EU 倍率。
* 产出倍率会在磁流聚敛核心完成聚焦后再应用。
* 配方循环容器的输出本体不会被倍增；与之匹配的循环输入会随倍率增加。

模块循环为 32 秒：

* 第 0 秒通过 <ItemLink id="gtladditions:vientiane_transcription_node" /> 输出 1-15 的红石信号，并选定本轮所需催化剂。
* 第 7 秒起直到本轮重置前，每秒检查一次 LV 巨型流体输入仓。
* 正确催化剂按 40B/s 消耗，并启用产出增益和 EU 增益。
* 输入错误流体时会排空该流体，本轮剩余时间不再提供产出增益；只有后续仍持续输入催化剂时，EU 增益才会保留。
* 检查时没有催化剂输入，则产出增益和 EU 增益都会关闭。

信号对应：

> 1-3：<FluidLink id="gtceu:dimensionallytranscendentcrudecatalyst" /> \
> 4-6：<FluidLink id="gtceu:dimensionallytranscendentprosaiccatalyst" /> \
> 7-9：<FluidLink id="gtceu:dimensionallytranscendentresplendentcatalyst" /> \
> 10-12：<FluidLink id="gtceu:dimensionallytranscendentexoticcatalyst" /> \
> 13-15：<FluidLink id="gtceu:dimensionallytranscendentstellarcatalyst" />

</Column>
<Column gap="2" fullWidth={true}>

### 磁流聚敛核心

<BlockImage id="magnetorheological_convergence_core" scale="4"/>

* 对绑定主机中可参与聚焦的流体输出执行聚焦。
* 当前只聚焦流体输出。第一个流体输出会获得全部流体输出的总量，然后再应用绑定主机和催化迭升阵列的产出倍率。
* 少于两个流体输出的配方不会被聚焦。

模块拥有 12 秒燃料周期。放置时会随机选择两种物品需求和一种流体需求：

> 物品从 <ItemLink id="kubejs:black_body_naquadria_supersolid" />、<ItemLink id="kubejs:quantum_anomaly" />、<ItemLink id="kubejs:hyper_stable_self_healing_adhesive" /> 中选择，每种数量为 1-16384。 \
> 流体从 <FluidLink id="gtceu:exciteddtec" /> 或 <FluidLink id="gtceu:exciteddtsc" /> 中选择，数量为 1-1638400 mB。

激活聚焦需要：

* 通过两个 ULV 巨型物品输入总线精确输入两组被选中的物品。
* 通过一个 LV 巨型流体输入仓精确输入被选中的流体。
* 通过物品输入持续提供 2 个/s 磁物质块。
* 数量过多、过少或缺失时，模块 GUI 会提示失败项，聚焦不会启用。

</Column>
<Column gap="2" fullWidth={true}>

### 时空静滞装置

<BlockImage id="spacetime_stasis_device" scale="4"/>

* 以 OpV 电压持续运行，并消耗 70B/s <FluidLink id="gtceu:spacetime" />。
* 当自身配方正在运行且公共增益门控开启时，绑定主机空闲时不会衰减运行时间。
* 它不会主动增加运行时间，也不会提供产出倍率或 EU 增益。
* 绑定主机工作时，运行时间仍会正常增加。

</Column>
<Column gap="2" fullWidth={true}>

### 超时空助推引擎

<BlockImage id="supratemporal_boosting_engine" scale="4"/>

* 以 UXV 电压持续运行。
* 它是公共增益门控的必要模块。
* 递归反演增益包生效时，它会为绑定主机提供额外 EU 倍率。
* 结构内安装 <ItemLink id="gtladditions:vientiane_transcription_node" /> 时，节点会接收当前温度对应的红石值。

温度行为：

> 初始温度：48000K \
> 最佳区间：93000K-97000K \
> 过热阈值：超过 105000K \
> 工作升温：引擎自身运行中且阵列主机有效时 +1300K/s \
> 空闲降温：-900K/s，不低于 48000K \
> 加载时如果引擎未过热且温度已经位于最佳区间，会保护固定当前温度 5 秒，然后再恢复正常升降温。

控温流体按 100B/s 消耗：

> 加热：<FluidLink id="minecraft:lava" /> +2500K/s，<FluidLink id="gtceu:blaze" /> +4600K/s，<FluidLink id="gtceu:raw_star_matter_plasma" /> +14000K/s \
> 冷却：<FluidLink id="gtceu:ice" /> -1900K/s，<FluidLink id="gtceu:liquid_helium" /> -3400K/s，<FluidLink id="kubejs:gelid_cryotheum" /> -6700K/s

过热后，引擎会停止提供公共门控。加热流体不会被使用，冷却流体会叠加默认 -7125K/s 的过热降温，直到温度回到 48000K 后才解除过热保护。

EU 倍率使用如下公式：

> 最佳温度区间内，完美时空助推参数为 1.0。

<Latex math = "P = 0.5 + 0.5 * (\frac{temperature - 48000}{45000})^{28}, \quad temperature \lt 93000K" />

<Latex math = "P = 1 - 0.85 * (\frac{temperature - 97000}{4000})^{0.42}, \quad temperature \gt 97000K" />

> 完美时空助推参数会限制在 0.0-1.0。EU 倍率为：

<Latex math = "EU倍率 = \min(0.8, 0.05 + 0.7932 * e^{-0.8473 * P^{2.326}})" />

温度位于最佳区间外时，完美时空助推参数同时也是每次绑定主机执行配方时递归反演增益生效的概率。若概率判定失败，本次执行不会获得任何递归反演模块增益。

</Column>
</Column>