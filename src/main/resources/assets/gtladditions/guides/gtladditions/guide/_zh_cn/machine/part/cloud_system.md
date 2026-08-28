---
navigation:
  title: 云端算力与研究数据系统
  icon: gtladditions:cloud_computation_monitor
  parent: part/machine_part_index.md
  position: 10
item_ids:
  - gtladditions:cloud_computation_monitor
  - gtladditions:cloud_computation_transmitter_hatch
  - gtladditions:cloud_computation_receiver_hatch
  - gtladditions:cloud_data_machine
  - gtladditions:cloud_data_hatch
  - gtceu:research_station
---

# 云端算力与研究数据系统

<Row>
  <BlockImage id="gtladditions:cloud_computation_monitor" scale="3" />
  <BlockImage id="gtladditions:cloud_computation_transmitter_hatch" scale="3" />
  <BlockImage id="gtladditions:cloud_computation_receiver_hatch" scale="3" />
  <BlockImage id="gtladditions:cloud_data_machine" scale="3" />
  <BlockImage id="gtladditions:cloud_data_hatch" scale="3" />
</Row>

<Column gap="12" fullWidth={true}>

### 绑定与传输范围

* 整套云端系统需要在<Color color="#55FF55">**UEV**</Color>阶段才能制作。
* 云端设备放置时会自动绑定放置者；使用数据棒右击可重新绑定，左击可解除绑定。
* 设备按 FTB Teams 队伍归组，同一队伍的不同成员可以共享系统。
* 未绑定设备不会加入云端网络。算力与研究数据都可以跨维度传输。

### 云端算力

* 云端算力供应仓等价于<ItemLink id="gtmthings:wireless_computation_transmitter_hatch" />；云端算力请求仓等价于<ItemLink id="gtmthings:wireless_computation_receiver_hatch" />。
* 供应仓必须安装在能够桥接算力的已成型多方块上。HPCA 需要安装 HPCA 桥接组件。
* <ItemLink id="gtceu:network_switch" />不能安装云端算力供应仓或请求仓。
* 云端算力监控器不是网络工作的必要条件；它用于查看供应端、请求端、最大算力和剩余算力。
* 点击监控器中的高亮按钮时，同维度目标会被高亮并自动转动视角；跨维度目标会在聊天栏给出坐标和需要命令权限的传送链接。

### 云端研究数据

* 云端研究数据存储器拥有 90 个研究数据槽位；云端研究数据请求仓等价于<ItemLink id="gtceu:wireless_data_receiver_hatch" />。
* 存储器基础耗电为 393,216 EU/t，每放入一个有效数据模块、数据球或数据棒再增加 393,216 EU/t。
* 放入<ItemLink id="gtceu:creative_data_access_hatch" />后，存储器向同队伍的所有请求仓提供全部研究数据，并将耗电固定为 393,216 EU/t。
* 拆除存储器时，普通研究数据会掉落；创造模式数据访问仓会被销毁，不会掉落。
* 供电不足时，该存储器暂停向云端提供研究数据。

</Column>
