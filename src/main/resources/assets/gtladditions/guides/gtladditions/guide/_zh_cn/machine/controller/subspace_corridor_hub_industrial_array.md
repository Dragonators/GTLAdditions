---
navigation:
  title: 亚空间航道枢纽
  icon: subspace_corridor_hub_industrial_array
  parent: controller/multiblock_controller.md
  position: 34
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:subspace_corridor_hub_industrial_array
---

# 亚空间航道枢纽

<BlockImage id="gtladditions:subspace_corridor_hub_industrial_array" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 亚空间航道枢纽并不实际处理配方，而是作为 <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> 系列模块的主站。
* 主站最多可连接 138 个 <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> 系列模块。
* 已连接模块需要主站保持有效工作状态，才能继续推进自身配方。

</Column>

<Column gap="2" fullWidth={true}>

### 模块连接

* <ItemLink id="gtladditions:nexus_satellite_factory_mk1" /> 系列模块会从主站继承运行状态。
* 主站未成型、未供能或未处于有效状态时，模块会停止工作。
* 常规状态下，模块按自身等级和结构提供并行能力。

</Column>

<Column gap="2" fullWidth={true}>

### 星规矩阵

* 亚空间航道枢纽可嵌入最多 512 个 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />。
* 1 个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 会超过该上限，因此会直接填充至 512 个星阵。
* 已嵌入的压缩星阵会退相干；打掉主机时，嵌入数量只会以普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 形式返还。
* 嵌入满 512 个星阵后，亚空间航道枢纽会解锁悖论实现理论。
* 这一模式下，亚空间航道枢纽会向已连接模块提供无限并行和无限线程。

</Column>

</Column>
