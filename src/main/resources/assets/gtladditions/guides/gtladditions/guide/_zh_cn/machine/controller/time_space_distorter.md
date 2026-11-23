---
navigation:
  title: 时空扭曲者
  icon: time_space_distorter
  parent: machine/controller/multiblock_controller.md
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

* 时空扭曲者可与 <ItemLink id="gtladditions:apocalyptic_quantum_warp_matrix" /> 或 <ItemLink id="gtladditions:exotic_chemical_complex" /> 建立一对一绑定。
* 使用 <ItemLink id="gtladditions:suprachronal_data_module" link={false} />，先右键已成型的主机，再右键同一维度内已成型的时空扭曲者建立绑定。
* 想要获得增益，需要让本机保持运行，并输入充足的额外材料。
* 不支持 ME 配方类型统合样板总成。

</Column>

<Column gap="2" fullWidth={true}>

### 因果扭曲等级

使用界面中的加减按钮调整等级。等级越高，产出越多，需要的额外材料种类也越多：

* 1 级：<Color color="#FFAA00">1.3 倍产出</Color>。额外消耗 <FluidLink id="gtceu:infinity" />，用量为每 17 并行 1 mB。
* 2 级：<Color color="#FFAA00">1.7 倍产出</Color>。在等级 1 基础上额外消耗 <FluidLink id="gtceu:hypogen" />，用量为每 28 并行 1 mB。
* 3 级：<Color color="#FFAA00">2.6 倍产出</Color>。在等级 2 基础上额外消耗 <FluidLink id="gtceu:spacetime" />，用量为每 44 并行 1 mB。
* 4 级：<Color color="#FFAA00">3.2 倍产出</Color>。在等级 3 基础上，每 730 并行额外消耗 1 个 <ItemLink id="kubejs:quantum_anomaly" />，以及每 873 并行 1 个 <ItemLink id="kubejs:hypercube" />。

</Column>
<Column gap="2" fullWidth={true}>

### 额外材料用量

* 终焉曲折量子波动矩阵刚开始工作时，额外材料需求为基础用量的 80%。持续运行 4 小时后降至 40%。
* 异星化工复合体仅在三级结构的化学扭曲单元启动并产出时结算，额外材料用量固定为基础用量的 10%。
* 每项材料的用量向下取整。

每项额外材料的用量计算如下：

<Latex math="Q = \left\lfloor \frac{P \times c \times F}{d} \right\rfloor" />

<Latex math="c = 0.8 \times 2^{-\min(t/4, 1)}" />

* P 为矩阵或化扭单元本轮实际启动的并行数。
* d 对应上面的基础用量，依次为 17、28、44、730、873。
* c 为额外材料倍率。对于矩阵，t 为持续运行小时数，c 按上式计算。对于异星化工复合体，c 固定为 0.1。
* F 在矩阵的维度曲折生效时为 6，其余情况为 1。

</Column>
<Column gap="2" fullWidth={true}>

### 维度曲折

* 矩阵进入无限模式后，可以开启维度曲折，以 6 倍辅料换取翻倍的产出倍率，并与矩阵运行时间带来的额外材料减免乘算。
* 四个等级的产出倍率将分别变为 2.6、3.4、5.2、6.4 倍。

</Column>

</Column>