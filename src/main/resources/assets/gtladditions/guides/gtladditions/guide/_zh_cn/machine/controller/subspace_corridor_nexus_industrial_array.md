---
navigation:
  title: 亚空间航道枢纽
  icon: subspace_corridor_nexus_industrial_array
  parent: machine/controller/multiblock_controller.md
  position: 34
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:subspace_corridor_nexus_industrial_array
---

# 亚空间航道枢纽

<BlockImage id="gtladditions:subspace_corridor_nexus_industrial_array" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 亚空间航道枢纽并不实际处理配方，而是作为 <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> 系列模块的主站。
* 控制器 Y 坐标需位于 −42～51（实在是太大了！）。

</Column>

<Column gap="2" fullWidth={true}>

### 模块连接

* 最多可连接 144 个 <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> 系列模块。
* 模块成型后会主动尝试连接主机（主机成型后也是如此）
* 需要保持主机的持续运行，模块才能处理各自配方。

</Column>

<Column gap="2" fullWidth={true}>

### 星阵

* 亚空间航道枢纽可嵌入最多 512 个 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />。
* 1 个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 会超过该上限，因此会直接填充至 512 个星阵。
* 已嵌入的压缩星阵会退相干；打掉主机时，嵌入数量只会以普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 形式返还。
* 嵌入满 512 个星阵后，亚空间航道枢纽会解锁悖论实现理论。
* 这一模式下，亚空间航道枢纽会向已连接模块提供无限并行和无限线程。

</Column>

</Column>
