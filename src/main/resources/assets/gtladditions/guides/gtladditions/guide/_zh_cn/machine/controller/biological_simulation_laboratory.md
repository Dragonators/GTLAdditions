---
navigation:
  title: 生物数据模拟室
  icon: biological_simulation_laboratory
  parent: controller/multiblock_controller.md
  position: 26
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:biological_simulation_laboratory
---

# 生物数据模拟室

<BlockImage id = "gtladditions:biological_simulation_laboratory" scale = "8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 生物数据模拟室处理生物数据模拟配方，用于高效获取生物掉落物。
* 主机右下角有一个内部物品槽，只能放入 <ItemLink id="gtceu:rhenium_nanoswarm" />、<ItemLink id="gtceu:orichalcum_nanoswarm" />、<ItemLink id="gtceu:infuscolium_nanoswarm" /> 或 <ItemLink id="gtceu:nan_certificate" />。
* 结构允许使用激光仓，但只接受 <Color color="#FFFF00">**UXV**</Color> 及以上等级的激光仓。
* 结构允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />；放入 <ItemLink id="gtceu:nan_certificate" /> 解锁跨配方后，可以使用其提供的线程。
* 未放入增益物品时，机器的初始并行数为 64。

</Column>

<Column gap="2" fullWidth={true}>

### 纳米蜂群槽位加成

> 放入 <ItemLink id="gtceu:rhenium_nanoswarm" />：最大并行 2048，耗电倍率 0.9，耗时倍率 0.9。\
> 放入 <ItemLink id="gtceu:orichalcum_nanoswarm" />：最大并行 16384，耗电倍率 0.8，耗时倍率 0.6。\
> 放入 <ItemLink id="gtceu:infuscolium_nanoswarm" />：最大并行 262144，耗电倍率 0.6，耗时倍率 0.4。\
> 放入 <ItemLink id="gtceu:nan_certificate" />：最大并行 4194304，耗电倍率 0.25，耗时倍率 0.1。

</Column>

<Column gap="2" fullWidth={true}>

### 证明加成

> 放入 <ItemLink id="gtceu:nan_certificate" /> 后，机器会解锁跨配方并行处理。\
> 含 <ItemLink id="avaritia:infinity_sword" /> 的生物模拟配方也需要该物品解锁。\
> 没有该物品时，机器只会处理单个可用配方。

</Column>

</Column>