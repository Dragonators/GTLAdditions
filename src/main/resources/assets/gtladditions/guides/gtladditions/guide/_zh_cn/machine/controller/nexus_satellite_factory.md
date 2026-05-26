---
navigation:
  title: 枢纽卫星工厂
  icon: nexus_satellite_factory_mk1
  parent: controller/multiblock_controller.md
  position: 35
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:nexus_satellite_factory_mk1
  - gtladditions:nexus_satellite_factory_mk2
  - gtladditions:nexus_satellite_factory_mk3
  - gtladditions:nexus_satellite_factory_mk4
---

# 枢纽卫星工厂

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <Color color="#FFFF00">**UXV**</Color> 阶段的 <ItemLink id="gtladditions:light_hunter_space_station" /> 系列模块。
* 枢纽卫星工厂必须安装至 <ItemLink id="gtladditions:light_hunter_space_station" /> 才能运行；未连接主站时不会执行配方。
* 主站未成型、未供能或未保持有效工作状态时，已连接模块也会停止工作。
* 单台 <ItemLink id="gtladditions:light_hunter_space_station" /> 最多连接 20 个枢纽卫星工厂模块。

</Column>

<Column gap="2" fullWidth={true}>

### 模块等级

* <ItemLink id="gtladditions:nexus_satellite_factory_mk1" />：面向车床、卷板、压缩、锻造、切割、挤压、搅拌、线材、冲压和磁化类处理。
* <ItemLink id="gtladditions:nexus_satellite_factory_mk2" />：面向碎岩、洗矿、离心、电解、筛选、研磨、脱水、热力离心和电磁选矿类处理。
* <ItemLink id="gtladditions:nexus_satellite_factory_mk3" />：面向蒸发、高压釜、提取、酿造、发酵、蒸馏、流体加热、流体固化和化学浸洗类处理。
* <ItemLink id="gtladditions:nexus_satellite_factory_mk4" />：面向装罐、电弧炉、闪电处理、组装、精密组装和电路组装类处理。

</Column>

<Column gap="2" fullWidth={true}>

### 并行与线程

* 枢纽卫星工厂支持激光供能，并允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />。
* 常规状态下，模块按自身等级、结构和已安装部件提供并行能力。
* 当主站嵌入满 64 个 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 并解锁悖论实现理论后，已连接模块会从主站获得无限并行和无限线程。1 个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 会直接填满这个上限。

</Column>

</Column>