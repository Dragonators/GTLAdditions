---
navigation:
  title: Laser Hatches
  icon: gtmthings:luv_16777216a_wireless_laser_source_hatch
  parent: part/machine_part_index.md
  position: 9
categories:
  - machine part hatch
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

# Laser Hatches

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

* Adds stronger laser hatch families, including wired laser hatches and wireless laser hatches.
* Current ratings include the 16777216A series and the source 67108863A / target 67108864A series.
* Wireless laser hatches are registered from IV to MAX. Wired laser hatches provide the corresponding higher-tier versions following GTCEu laser hatch registration logic.
* These laser hatches can be used by multiblock structures that need higher laser input or output capability.

</Column>

<Column gap="2" fullWidth={true}>

> Very high-current laser hatches can greatly increase instant energy throughput. Make sure the wireless energy network or input buffer is sufficient.

~~The reason laser source hatches use 67108863A instead of 67108864A is that exceeding the long numeric limit would turn the value negative.~~

</Column>

</Column>