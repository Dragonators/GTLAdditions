---
navigation:
  title: 嬗变总线
  icon: me_block_conservation
  parent: part/machine_part_index.md
  position: 10
categories:
  - 机器仓室
item_ids:
  - gtladditions:me_block_conservation
---

# 嬗变总线

<BlockImage id="gtladditions:me_block_conservation" scale="4" />

* 作为 <ItemLink id="gtladditions:subatomic_transmutatioon_core" /> 的方块转换输入与输出总线
* 内部槽位需要放入 AE 物品存储磁盘；转换后的输出会先写入该磁盘
* 当可以写入 AE 网络时，嬗变总线会把内部磁盘中的输出返还到连接的 AE 网络
* 在嬗变总线 GUI 中配置需要请求的输入方块与数量。它会像 ME 输入总线一样从 AE 网络中拉取对应物品
* 内部存储槽接受 AE 物品存储磁盘和无尽物品存储磁盘