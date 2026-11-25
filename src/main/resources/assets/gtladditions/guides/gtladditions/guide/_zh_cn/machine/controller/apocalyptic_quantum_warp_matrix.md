---
navigation:
  title: 终焉曲折量子波动矩阵
  icon: apocalyptic_quantum_warp_matrix
  parent: machine/controller/multiblock_controller.md
  position: 42
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:apocalyptic_quantum_warp_matrix
---

# 终焉曲折量子波动矩阵

<BlockImage id="gtladditions:apocalyptic_quantum_warp_matrix" scale="8"/>

<Column gap="20" fullWidth={true}>
<Column gap="2" fullWidth={true}>

* 逆向演算未来，让概率成为必然。可以同时处理量子操纵者与奇点压缩配方。
* 机器使用无线能源网络供能，无需安装能源仓或激光仓。
* 基础 1024 线程，自带 Int.MAX 并行，无视一切配方维护条件。
* 不支持 ME 配方类型统合样板总成。

</Column>
<Column gap="2" fullWidth={true}>

### 嵌入星阵

* 手持 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 潜行右键主机，可以提高线程。主机最多容纳 256 颗星阵。
* 也可以嵌入 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />。足以一次填满主机，但超出容量的部分不会返还。

</Column>
<Column gap="2" fullWidth={true}>

### 持续运行

* 机器工作时会逐渐预热，能同时处理的工作量也随之增加。
* 持续运行时间积累达到 <Color color="#55FFFF">4 小时</Color> 后，线程提升至初始的 <Color color="#FFAA00">16 倍</Color>。
* 还能逐步降低时空扭曲者的额外材料需求：从基础用量的 80% 逐渐降至 40%。见 <ItemLink id="gtladditions:time_space_distorter" />。
* 机器空闲时会逐步失去已积累的运行时间

线程计算如下：

<Latex math="T = \left\lfloor (1024 + 64KN) \times 16^{\min(t/4, 1)} \right\rfloor" />

* T 为线程数，N 为嵌入的星阵数量，t 为界面显示的持续运行时间。
* K 为星阵线程倍率，受整合包的配方时间倍率设置影响。

</Column>
<Column gap="2" fullWidth={true}>

### 概率材料与配方记忆

* 概率产物必定产出；对于概率消耗的原料，其消耗几率降低至原来的 1/10。
* 每种配方都有独立的运行记忆。加工得越久，消耗几率就越低；积累 <Color color="#55FFFF">1 小时</Color> 后，消耗几率最低降至原来的 <Color color="#FFAA00">1/20</Color>。
* 配方空闲时会逐步失去已积累的运行时间。
* 点击主机GUI左侧的时钟页签，可以查看不同配方的记忆时长和当前概率减免。

消耗几率计算如下，s 为该配方积累的运行小时数：

<Latex math="p = \frac{p_0}{10 + 10\min(s, 1)}" />

p₀ 为配方原本的消耗几率，p 为矩阵中的消耗几率。

</Column>
<Column gap="2" fullWidth={true}>

### 无限并行与线程

满足以下条件后，矩阵会自动进入无限模式：

* 持续运行达到 4 小时。
* 主机嵌入满 256 颗星阵。
* 已绑定一台成型且正在运行的 <ItemLink id="gtladditions:time_space_distorter" />。

在无限模式下，机器耗电量将会提升4倍。

</Column>
<Column gap="2" fullWidth={true}>

### 维度曲折

* 在无限模式下，打开机器GUI中的维度曲折开关可以进一步提高产出。
* 开启后，以 <Color color="#FF5555">6 倍额外材料</Color> 将时空扭曲者的产出倍率再翻倍，总计最高可达到 <Color color="#FFAA00">6.4 倍产出</Color>。

</Column>
</Column>