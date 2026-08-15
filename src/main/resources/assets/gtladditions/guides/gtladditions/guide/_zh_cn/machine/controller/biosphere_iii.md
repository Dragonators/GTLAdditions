---
navigation:
  title: 生态圈3号
  icon: biosphere_iii
  parent: controller/multiblock_controller.md
  position: 10
item_ids:
  - biosphere_iii
  - garden_of_hermes
---

# 生态圈3号

<Row>
    <BlockImage id="biosphere_iii" scale="4"/>
    <BlockImage id="garden_of_hermes" scale="4"/>
</Row>

<Column gap="8" fullWidth={true}>

## 集中供能

生态圈3号可以安装任意数量的下列能源部件：

* 能源仓；
* <Color color="#00AA00">**UIV**</Color>–<Color color="#FF0000">**MAX**</Color> 激光靶仓；
* <ItemLink id="gtladditions:wireless_energy_network_input_terminal" />。

存在无线电网输入终端时，主机优先使用无线电网。

使用有线能源时，主机总输入功率会在所有已连接、结构有效且自身开启的赫尔墨斯模块之间均分。无线模式不均分功率。

赫尔墨斯之圃本身不得安装能源部件。模块仍在本地处理物品和流体输入输出，但所有模块的能耗都由生态圈3号统一扣取。

主机会将成功消耗的EU累积并计算平均EU/t，每秒结算一次。

## 模块线程

未安装 <ItemLink id="gtladditions:thread_modifier_hatch" /> 时，每个赫尔墨斯模块固定拥有 128 个基础线程。生态圈3号允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />，其基础线程倍率为 1024。

引擎额外线程仍为 `星阵数量 × 64 × 当前倍率`。安装引擎后，每个模块按下式获得额外线程：

`32 × max(主机等级 - UEV, 0) × 主机引擎额外线程`

最终模块线程数为 `128 + 额外线程`。

## 配方处理

赫尔墨斯之圃同时处理温室与渔场配方，无需切换配方类型。配方等级、超频电压都取自主机；本体线圈温度每达到 1100 K，并行数翻倍。

</Column>
