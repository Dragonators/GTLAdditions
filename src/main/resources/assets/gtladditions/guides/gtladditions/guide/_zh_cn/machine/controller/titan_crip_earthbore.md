---
navigation:
  title: 泰坦之握凿地机
  icon: titan_crip_earthbore
  parent: controller/multiblock_controller.md
  position: 25
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:titan_crip_earthbore
---

# 泰坦之握凿地机

<BlockImage id = "gtladditions:titan_crip_earthbore" scale = "8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 更高效的基岩钻机，不再需要自动化操作，可以简单获取大量的基岩粉。
* 机器结构需要基岩作为结构部分；本地实现只检查结构中的基岩，不在工作时破坏它。
* 机器支持无损超频。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。

</Column>

<Column gap="2" fullWidth={true}>

### 并行计算

* 最大并行由机器当前电压等级决定；<Color color="#FF55FF">**LuV**</Color> 为基准等级，每高一级翻倍。

<Latex math = "最大并行 = 2^{电压等级 - 6}" />

</Column>

<Column gap="2" fullWidth={true}>

<Recipe id="gtladditions:tectonic_fault_generator/bedrock_dust" />

</Column>

</Column>