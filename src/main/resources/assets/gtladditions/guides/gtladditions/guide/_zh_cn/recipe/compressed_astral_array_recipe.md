---
navigation:
  title: 星阵压缩配方
  parent: recipe/recipe_index.md
  position: 16
categories:
  - 配方
item_ids:
  - gtladditions:compressed_astral_array
---

# 星阵压缩配方

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 这是 <ItemLink id="gtladditions:arcanic_astrograph" /> 独有配方。
* 压缩配方只在基础材料匹配后启动，随后从已绑定的无线电网一次性抽取 9,223,372,036,854,775,807 EU 作为启动耗能。
* 在 30 秒的压缩窗口内，投入宇宙渲染区域的普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 实体会被收束。每完整 1024 个星阵尝试产出 1 个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} />；若压缩配方连续接上下一轮，未凑整的星阵会继续保留。
* 若下一次检测未能继续运行压缩配方、机器被暂停，或输出条件无法继续接收压缩结果，暂存的星阵和未能输出的结果都会湮灭。
* 产出概率基础为 30%，并在嵌入 42,441 个星阵时达到 100%。公式为：

<Latex math = "产出概率 = \min(100\%, 30\% + 70\% * (\frac{count}{42,441})^{1.5})" />

<Recipe id="gtladditions:compressed_astral_array/compressed_astral_array" />

</Column>

</Column>