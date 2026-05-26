---
navigation:
  title: 时空扭曲者
  icon: time_space_distorter
  parent: controller/multiblock_controller.md
  position: 43
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:time_space_distorter
---

# 时空扭曲者

<BlockImage id="gtladditions:time_space_distorter" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:time_space_distorter" /> 是 <ItemLink id="gtladditions:apocalyptic_torsion_quantum_matrix" /> 的绑定增益机器。
* 使用 <ItemLink id="gtladditions:suprachronal_data_module" link={false} /> 先右键 <ItemLink id="gtladditions:apocalyptic_torsion_quantum_matrix" /> 记录数据，再右键 <ItemLink id="gtladditions:time_space_distorter" /> 建立绑定。
* <ItemLink id="gtladditions:time_space_distorter" /> 自身需要运行 1 号电路空转配方，配方耗能为 OpV，耗时 30 秒。
* <ItemLink id="gtladditions:apocalyptic_torsion_quantum_matrix" /> 完成本轮实际并行消耗后，才会统计成功处理的总并行数，并向绑定的 <ItemLink id="gtladditions:time_space_distorter" /> 尝试消耗增益资源；所有所需增益资源会先整体检查，通过后才会扣除。
* 增益资源消耗成功时，只放大本轮配方输出；不改变概率输入、概率输出或并行计算方式。

</Column>

<Column gap="2" fullWidth={true}>

### 因果扭曲等级

* 等级 1：输出倍率 1.3x，额外消耗 <FluidLink id="gtceu:infinity" />，数量为总并行 / 17。
* 等级 2：输出倍率 1.7x，在等级 1 基础上额外消耗 <FluidLink id="gtceu:hypogen" />，数量为总并行 / 28。
* 等级 3：输出倍率 2.6x，在等级 2 基础上额外消耗 <FluidLink id="gtceu:spacetime" />，数量为总并行 / 44。
* 等级 4：输出倍率 3.2x，在等级 3 基础上额外消耗 <ItemLink id="kubejs:quantum_anomaly" />，数量为总并行 / 730，并消耗 <ItemLink id="kubejs:hypercube" />，数量为总并行 / 873。
* 数量计算使用整数除法；结果为 0 时该项不消耗。

</Column>

<Recipe id="gtladditions:time_space_distortion/time_space_distortion" />

</Column>