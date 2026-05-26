---
navigation:
  title: 奥术星图
  icon: arcanic_astrograph
  parent: controller/multiblock_controller.md
  position: 22
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:arcanic_astrograph
---

# 奥术星图

<BlockImage id="gtladditions:arcanic_astrograph" scale="8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:arcanic_astrograph" /> 是比 <ItemLink id="gtceu:eye_of_harmony" /> 更加致密的微缩宇宙，基础并行为 2048。
* 机器沿用 <ItemLink id="gtceu:eye_of_harmony" /> 的消耗逻辑：但只有 <ItemLink id="kubejs:quantum_chromodynamic_charge" /> 和 <FluidLink id="gtceu:cosmic_element" /> 会随并行增加而增加，其他资源消耗保持原有规则。
* 成型后会在界面中显示已嵌入的 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 数量和当前最大并行数。

</Column>

<Column gap="2" fullWidth={true}>

### 星规矩阵加成

* 可嵌入 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 或 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 来提高并行上限。
* 并行计算公式为：

<Latex math = "最大并行 = 2048 + 2^{\lfloor \log_{1.7}(8 * count) \rfloor} * 128" />

* 公式中的 count 是当前已嵌入的 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 数量；每个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 按 1024 个计入。
* <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 在嵌入后极不稳定；打掉主机时，已嵌入的压缩星阵只会以普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 形式析出。

</Column>

<Column gap="2" fullWidth={true}>

### 星阵压缩

* 压缩配方只在基础材料匹配后启动，随后从已绑定的无线电网一次性抽取 9,223,372,036,854,775,807 EU 作为启动耗能。
* 在 30 秒的压缩窗口内，投入宇宙渲染区域的普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 实体会被收束。每完整 1024 个星阵尝试产出 1 个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />；若压缩配方连续接上下一轮，未凑整的星阵会继续保留。
* 若下一次检测未能继续运行压缩配方、机器被暂停，或输出条件无法继续接收压缩结果，暂存的星阵和未能输出的结果都会湮灭。
* 产出概率基础为 30%，并在嵌入 42,441 个星阵时达到 100%。公式为：

<Latex math = "产出概率 = \min(100\%, 30\% + 70\% * (\frac{count}{42,441})^{1.5})" />

<Recipe id="gtladditions:compressed_astral_array/compressed_astral_array" />

</Column>

</Column>