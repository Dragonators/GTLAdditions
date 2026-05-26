---
navigation:
  title: 激光仓
  icon: gtmthings:luv_16777216a_wireless_laser_source_hatch
  parent: part/machine_part_index.md
  position: 9
categories:
  - 机器仓室
item_ids:
  - gtmthings:luv_16777216a_wireless_laser_source_hatch
  - gtmthings:zpm_16777216a_wireless_laser_target_hatch
  - gtmthings:uv_67108863a_wireless_laser_source_hatch
  - gtmthings:uhv_67108864a_wireless_laser_target_hatch
  - gtceu:uev_16777216a_laser_source_hatch
  - gtceu:uiv_16777216a_laser_target_hatch
  - gtceu:uxv_67108863a_laser_source_hatch
  - gtceu:opv_67108864a_laser_target_hatch
---

# 激光仓

<Column gap="15" fullWidth={true}>

<Row>
    <BlockImage id="gtmthings:luv_16777216a_wireless_laser_source_hatch" scale="4" />
    <BlockImage id="gtmthings:zpm_16777216a_wireless_laser_target_hatch" scale="4" />
    <BlockImage id="gtmthings:uv_67108863a_wireless_laser_source_hatch" scale="4" />
    <BlockImage id="gtmthings:uhv_67108864a_wireless_laser_target_hatch" scale="4" />
</Row>

<Row>
    <BlockImage id="gtceu:uev_16777216a_laser_source_hatch" scale="4" />
    <BlockImage id="gtceu:uiv_16777216a_laser_target_hatch" scale="4" />
    <BlockImage id="gtceu:uxv_67108863a_laser_source_hatch" scale="4" />
    <BlockImage id="gtceu:opv_67108864a_laser_target_hatch" scale="4" />
</Row>

<Column gap="2" fullWidth={true}>

* 添加了功率更大的激光仓家族，包含有线激光仓与无线激光仓。
* 电流规格包含 16777216A 系列，以及源仓 67108863A / 靶仓 67108864A 系列。
* 无线激光仓注册范围覆盖 IV 至 MAX；有线激光仓按 GTCEu 激光仓注册逻辑提供对应高阶版本。
* 这些激光仓可用于需要更高激光输入或输出能力的多方块结构。

</Column>

<Column gap="2" fullWidth={true}>

> 使用超大电流激光仓会显著提高机器瞬时能量吞吐，请确认无线电网或能量输入缓存足够。

~~关于为什么激光源仓不是 67108864A 而是 67108863A，是因为超过了 long 数值类型上限会变成负数。~~

</Column>

</Column>