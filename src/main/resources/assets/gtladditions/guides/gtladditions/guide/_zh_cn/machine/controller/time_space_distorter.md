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

* 时空扭曲者是 <ItemLink id="gtladditions:apocalyptic_quantum_warp_matrix" /> 的绑定增益机器。
* 使用 <ItemLink id="gtladditions:suprachronal_data_module" link={false} />，先右键已成型的矩阵，再右键同一维度内已成型的时空扭曲者建立绑定。
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

### 持续运行与额外材料用量

* 矩阵刚开始工作时，额外材料需求为上述基础用量的 80%；持续运行 4 小时后，降至 40%。
* 消耗量向下取整。

每项额外材料的用量计算如下：

<Latex math="Q = \left\lfloor \frac{P \times c \times F}{d} \right\rfloor" />

<Latex math="c = 0.8 \times 2^{-\min(t/4, 1)}" />

* P 为矩阵本轮的实际总并行数，t 为矩阵的持续运行时间(小时)。
* d 对应上面的基础用量，依次为 17、28、44、730、873。
* c 为持续时间带来的额外材料倍率；F 在维度曲折关闭时为 1，生效时为 6。

</Column>
<Column gap="2" fullWidth={true}>

### 维度曲折

* 矩阵进入无限模式后，可以开启维度曲折，以 6 倍辅料换取翻倍的产出倍率，并与矩阵运行时间带来的额外材料减免乘算。
* 四个等级的产出倍率将分别变为 2.6、3.4、5.2、6.4 倍。

</Column>

</Column>