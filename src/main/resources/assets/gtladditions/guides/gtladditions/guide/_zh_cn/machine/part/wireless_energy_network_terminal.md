---
navigation:
  title: 无线电网终端
  icon: wireless_energy_network_input_terminal
  parent: part/machine_part_index.md
  position: 20
categories:
  - 机器仓室
item_ids:
  - gtladditions:wireless_energy_network_input_terminal
  - gtladditions:wireless_energy_network_output_terminal
---

# 无线电网终端

<Column gap="15" fullWidth={true}>

<Row>
    <BlockImage id="gtladditions:wireless_energy_network_input_terminal" scale="6" />
    <BlockImage id="gtladditions:wireless_energy_network_output_terminal" scale="6" />
</Row>

<Column gap="2" fullWidth={true}>

* 无线电网终端分为 <ItemLink id="gtladditions:wireless_energy_network_input_terminal" /> 和 <ItemLink id="gtladditions:wireless_energy_network_output_terminal" />。
* 放置时会自动绑定放置者；手持 <ItemLink id="gtceu:data_stick" /> 右键可重新绑定到当前玩家，手持 <ItemLink id="gtceu:data_stick" /> 左键可解绑。
* 两种终端都使用无线电网中对应 UUID 的能量账户作为电量来源。

</Column>

<Column gap="2" fullWidth={true}>

## 输入终端

* <ItemLink id="gtladditions:wireless_energy_network_input_terminal" /> 提供输入能源仓与激光靶仓能力。
* 普通能源输入上限为 Long.MAX 级别。
* 在跨配方模式下，它可以让兼容无线机器读取无线电网中的可用能量。

</Column>

<Column gap="2" fullWidth={true}>

## 输出终端

* <ItemLink id="gtladditions:wireless_energy_network_output_terminal" /> 提供动力仓与激光源仓能力。
* 普通能源输出上限为 Long.MAX 级别。
* 它可以把兼容机器输出的能量写入绑定玩家的无线电网；安装在 <ItemLink id="gtladditions:heart_of_the_universe" /> 上时，能够输出无限大的功率。

</Column>

</Column>