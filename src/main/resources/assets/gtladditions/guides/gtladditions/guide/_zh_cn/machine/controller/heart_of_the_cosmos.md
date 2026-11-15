---
navigation:
  title: 宇宙之心
  icon: heart_of_the_cosmos
  parent: controller/multiblock_controller.md
  position: 44
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:heart_of_the_cosmos
  - gtladditions:strange_annihilation_fuel_rod
  - gtladditions:eternity_dark_matter_fuel_rod
---

# 宇宙之心

<BlockImage id="gtladditions:heart_of_the_cosmos" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 发电机之终，燃烧燃料棒将位面湮灭为无线能源网络中的能量。
* 结构中需要 <ItemLink id="gtladditions:wireless_energy_network_output_terminal" />。
* 每个位面会在 1 秒内完成湮灭，向无线能源网络输出远超 <ItemLink id="gtmthings:creative_energy_hatch" /> 的功率，数量取决于结构等级。

</Column>

<Column gap="2" fullWidth={true}>

### 结构等级

宇宙之心拥有 3 个结构等级。

| 等级 | 可用配方 | 单位面功率 |
| --- | --- | --- |
| 1 | <ItemLink id="gtladditions:eternity_dark_matter_fuel_rod" /> + <ItemLink id="gtladditions:black_hole_seed" />（5% 消耗），湮灭约束器 90% 回收 1 个 | 64 个创造能源仓 |
| 2 | <ItemLink id="gtladditions:strange_annihilation_fuel_rod" /> + <ItemLink id="gtladditions:black_hole_seed" />，湮灭约束器 60% 回收 64 个 | 4096 个创造能源仓 |
| 3 | 同 2 级，黑洞种子至多 80% 概率消耗 | 最高 262,144 个创造能源仓 |

</Column>

<Column gap="2" fullWidth={true}>

### 升级

1. 打开主机 GUI 左侧的"结构升级"页，选择要看的等级。
2. 左侧会列出所需素材，达到升级所需条件后，将对应物品放入右侧的输入并点击解锁，即可消耗物品并解锁下一等级。
   * 解锁 2 级需要累计湮灭 65,536 个位面，解锁 3 级需要 262,144 个。
3. 解锁下一等级后，便可在世界中正常预览结构或使用终端搭建。搭建完成后使用激活按钮生效。
4. 湮灭位面数与解锁进度在拆卸主机后仍然保留。

</Column>

<Column gap="2" fullWidth={true}>

### 辅助流体

达到 2 级后，奇异湮灭燃料棒的配方可以额外消耗输入仓里的辅助流体，以增强稳定约束或进行湮灭增幅。每个位面固定消耗 100 mB。

| 类别 | 流体 | 效果 | 需要 |
| --- | --- | --- | --- |
| 约束稳定剂 | <FluidLink id="gtceu:exciteddtsc" /> | 约束器回收率 95% | 结构等级 2 |
| 约束稳定剂 | <FluidLink id="gtceu:exciteddtec" /> | 约束器回收率 85% | 结构等级 2 |
| 约束稳定剂 | <FluidLink id="gtceu:dimensionallytranscendentresidue" /> | 约束器回收率 70% | 结构等级 2 |
| 湮灭增幅剂 | <FluidLink id="gtceu:miracle" /> | 位面功率 ×8 | 结构等级 3 |
| 湮灭增幅剂 | <FluidLink id="gtladditions:phonon_medium" /> | 位面功率 ×4 | 结构等级 3 |
| 湮灭增幅剂 | <FluidLink id="gtladditions:proto_matter" /> | 位面功率 ×2 | 结构等级 3 |

* 效果更强的流体优先生效。
* 约束稳定和湮灭增幅可以同时生效。

</Column>

<Column gap="2" fullWidth={true}>

### 位面数增益

3 级时，累计湮灭位面数越多，同样的燃料产出越多，黑洞种子的消耗概率越低。

| 累计湮灭位面 | 燃料效率 | 黑洞种子消耗概率 |
| --- | --- | --- |
| 不足 1,048,576（2^20） | ×1 | 80% |
| 1,048,576（2^20） | ×2 | 40% |
| 2,097,152（2^21） | ×4 | 20% |
| 4,194,304（2^22） | ×8 | 10% |

* 燃料效率和湮灭增幅剂相乘，最高为原来的 64 倍。

</Column>

<Column gap="2" fullWidth={true}>

### 位面展开

* <ItemLink id="gtladditions:heart_of_the_cosmos" /> 默认一次处理一个位面。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" /> ，以同时展开更多的位面。

</Column>

</Column>
