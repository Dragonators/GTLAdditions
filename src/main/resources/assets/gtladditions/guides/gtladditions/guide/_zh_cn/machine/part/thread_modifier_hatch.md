---
navigation:
  title: Ω-天球分歧引擎
  icon: thread_modifier_hatch
  parent: part/machine_part_index.md
  position: 19
categories:
  - 机器仓室
item_ids:
  - gtladditions:thread_modifier_hatch
  - gtladditions:astral_array
---

# Ω-天球分歧引擎

<Column gap="15" fullWidth={true}>

<BlockImage id="gtladditions:thread_modifier_hatch" scale="8" />

<Column gap="2" fullWidth={true}>

* Ω-天球分歧引擎提供线程修改能力。
* 它同时支持兼容的非跨配方模式控制器和兼容的跨配方模式控制器。
* 安装到兼容的非跨配方模式控制器后，该机器会获得跨配方并行能力。
* 安装到兼容的跨配方模式控制器后，该机器会获得额外线程。

</Column>

<Column gap="2" fullWidth={true}>

## 星规矩阵

* GUI 中只能放入普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />。该槽位不接受 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />。
* 每个 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 提供 64 基础线程。
* 额外线程总数按以下公式计算，其中 `n` 为 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 数量，`m` 为当前线程倍率：

<Latex math = "额外线程总数 = n * 64 * m" />

* 当前线程倍率由安装目标决定，会显示在该仓室 GUI 中。
* 对非跨配方模式控制器，这个额外线程总数就是启用跨配方并行后的线程数。
* 对已有跨配方模式的控制器，这个额外线程总数会追加到机器原有线程上。

</Column>

<Column gap="2" fullWidth={true}>

> 该仓室不可共享。移除机器时，内部星规矩阵会被清理掉落。

</Column>

</Column>