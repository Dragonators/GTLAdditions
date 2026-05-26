---
navigation:
  title: 终焉曲折量子波动矩阵
  icon: apocalyptic_torsion_quantum_matrix
  parent: controller/multiblock_controller.md
  position: 42
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:apocalyptic_torsion_quantum_matrix
---

# 终焉曲折量子波动矩阵

<BlockImage id="gtladditions:apocalyptic_torsion_quantum_matrix" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:apocalyptic_torsion_quantum_matrix" /> 可以在量子操纵者、深度化学扭曲仪和奇点压缩相关配方之间进行跨配方模式并行处理。
* 机器使用无线能源网络供能，并不需要安装任何能源/激光仓。
* 基础线程为 1024，支持安装并行控制仓。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。
* 运行时耗电倍率为 0.2。
* 可以使用 <ItemLink id="gtladditions:suprachronal_data_module" link={false} /> 记录本机，并绑定到 <ItemLink id="gtladditions:time_space_distorter" />；在其资源消耗成功时，本轮成功处理的配方输出会获得因果扭曲倍率。

</Column>

<Column gap="2" fullWidth={true}>

### 概率材料

* 参与处理的概率输出会视为必定产出。
* 参与处理的概率输入会按原需求的十分之一消耗。
* 对带有概率输入的配方，机器会先检查当前可用的物品与流体，再计算这些概率输入在本轮最多能支撑多少并行。
* 每条可运行配方都会尽量按照预计概率吃满自身可承受的最大材料并行，而不是按照100%消耗的情况保守计算。

</Column>

<Column gap="2" fullWidth={true}>

### 时空扭曲者

* <ItemLink id="gtladditions:time_space_distorter" /> 会在本机完成本轮输入消耗后读取实际成功并行数。
* 若 <ItemLink id="gtladditions:time_space_distorter" /> 正在运行，且对应等级所需的 <FluidLink id="gtceu:infinity" />、<FluidLink id="gtceu:hypogen" />、<FluidLink id="gtceu:spacetime" />、<ItemLink id="kubejs:quantum_anomaly" />、<ItemLink id="kubejs:hypercube" /> 均可用，本轮输出会按其因果扭曲等级进行倍率放大，并一次性扣除这些消耗。
* 该增益只处理输出倍率，不改变概率输入或概率输出规则。

</Column>

</Column>