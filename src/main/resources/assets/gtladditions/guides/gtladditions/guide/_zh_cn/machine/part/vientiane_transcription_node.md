---
navigation:
  title: 万象转录节点
  icon: vientiane_transcription_node
  parent: part/machine_part_index.md
  position: 5
categories:
  - 机器仓室
item_ids:
  - gtladditions:vientiane_transcription_node
---

# 万象转录节点

<BlockImage id="gtladditions:vientiane_transcription_node" scale="4" />

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

万象转录节点是 <ItemLink id="gtladditions:recursive_reverse_array" /> 模块使用的红石输出部件。红石信号从节点正面输出。

</Column>

<Column gap="2" fullWidth={true}>

## 催化迭升阵列

安装在 <ItemLink id="gtladditions:catalytic_cascade_array" /> 中时，它会在每轮催化循环开始时输出 1-15 的红石信号。该信号用于表示本轮所需的催化剂分组。

</Column>

<Column gap="2" fullWidth={true}>

## 超时空助推引擎

安装在 <ItemLink id="gtladditions:supratemporal_boosting_engine" /> 中时，结构成型后打开节点 GUI 可以配置最低温度、最高温度和反转红石模式。节点会根据当前引擎温度和设定区间计算红石输出。

* 普通模式：温度介于设定最小值和最大值之间时发出红石信号，小于最小值时停止发出红石信号。
* 反转模式：温度介于设定范围之外时发出红石信号，小于最小值时也会发出红石信号。
* 可配置温度范围为 48000K 到 105000K。

</Column>

</Column>