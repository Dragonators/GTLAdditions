---
navigation:
  title: 炽隙裂炼穹
  icon: inferno_cleft_smelting_vault
  parent: controller/multiblock_controller.md
  position: 32
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:inferno_cleft_smelting_vault
---

# 炽隙裂炼穹

<BlockImage id = "gtladditions:inferno_cleft_smelting_vault" scale = "8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 炽隙裂炼穹能够处理热解和裂化配方。
* 机器支持线圈并行、激光输入和跨配方模式。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。
* 结构需要 <Color color="#00AAAA">**UV**</Color> 消声仓，并使用热容线圈参与并行计算。

</Column>

<Column gap="2" fullWidth={true}>

### 并行计算

* 炽隙裂炼穹的最大并行由当前线圈温度决定：

<Latex math = "最大并行 = \min(2147483647, 2^{\frac{线圈温度}{900}})" />

* 公式中的线圈温度为结构内已安装线圈的温度。
* 机器成型后，界面会显示当前计算出的最大并行数。

</Column>

</Column>