---
navigation:
  title: GTLAdditions机器设定
  parent: machine/machine_index.md
  position: 4
categories:
  - 机器
---

# GTLAdditions机器设定

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

本页说明 GTLAdditions 为跨配方、多配方或特殊并行机器固定下来的处理设定。均分模式和贪婪模式不是玩家可切换配置，而是机器注册和配方逻辑决定的固定行为。

只有配方最短耗时是部分机器 GUI 中可以调节的设定。机器实际并行还会受机器并行、线程舱、线圈、模块、能源网络或机器自身状态影响。

</Column>

<Column gap="2" fullWidth={true}>

## 配方最短耗时设定

> 部分 GTLAdditions 跨配方机器可以在 GUI 的配方最短耗时按钮中调节。\
> 默认值为 20t，最低 10t，最高 200t。\
> 它只控制最终合成配方的最短耗时，不改变机器属于均分设定、贪婪设定还是特殊设定。

</Column>

<Column gap="2" fullWidth={true}>

## 均分模式

> 均分模式下的机器会尽可能处理全部能处理的配方。\
> 分配并行时，机器会尽可能让每种可处理配方平均分配。\
> 总并行上限通常由机器并行和线程数共同决定；部分机器会按自身机制改写基础线程、基础并行或配方限制。

GTLAdditions 自有机器：

<Row>
    <BlockImage id="gtladditions:lucid_etchdreamer" scale="3" />
    <BlockImage id="gtladditions:atomic_transmutation_core" scale="3" />
    <BlockImage id="gtladditions:astral_convergence_nexus" scale="3" />
    <BlockImage id="gtladditions:nebula_reaper" scale="3" />
    <BlockImage id="gtladditions:arcane_cache_vault" scale="3" />
</Row>

<Row>
    <BlockImage id="gtladditions:biological_simulation_laboratory" scale="3" />
    <BlockImage id="gtladditions:dimensionally_transcendent_chemical_plant" scale="3" />
    <BlockImage id="gtladditions:quantum_syphon_matrix" scale="3" />
    <BlockImage id="gtladditions:fuxi_bagua_heaven_forging_furnace" scale="3" />
    <BlockImage id="gtladditions:antientropy_condensation_center" scale="3" />
</Row>

<Row>
    <BlockImage id="gtladditions:taixu_turbid_array" scale="3" />
    <BlockImage id="gtladditions:inferno_cleft_smelting_vault" scale="3" />
    <BlockImage id="gtladditions:heart_of_the_universe" scale="3" />
    <BlockImage id="gtladditions:dimension_focus_infinity_crafting_array" scale="3" />
</Row>

本 mod 改造为均分模式的外部机器：

<Row>
    <BlockImage id="gtceu:fishing_ground" scale="3" />
    <BlockImage id="gtceu:large_greenhouse" scale="3" />
</Row>

</Column>

<Column gap="2" fullWidth={true}>

## 贪婪模式

> 贪婪模式下的机器会尽可能把当前剩余并行集中投入先匹配到且能够实际输入的配方。\
> 当当前配方吃不下更多并行或输入不足时，机器才继续处理后续配方。\
> 该模式的目的不是平均铺开，而是尽可能集中处理一种或少数几种配方。

GTLAdditions 自有机器：

<Row>
    <BlockImage id="gtladditions:draconic_collapse_core" scale="3" />
    <BlockImage id="gtladditions:titan_crip_earthbore" scale="3" />
    <BlockImage id="gtladditions:skeleton_shift_rift_engine" scale="3" />
    <BlockImage id="gtladditions:apocalyptic_torsion_quantum_matrix" scale="3" />
</Row>

本 mod 改造为贪婪设定的外部机器：

<Row>
    <BlockImage id="gtceu:large_recycler" scale="3" />
    <BlockImage id="gtceu:a_mass_fabricator" scale="3" />
    <BlockImage id="gtceu:dimensionally_transcendent_mixer" scale="3" />
    <BlockImage id="gtceu:suprachronal_assembly_line" scale="3" />
    <BlockImage id="gtceu:nano_core" scale="3" />
</Row>

<Row>
    <BlockImage id="gtceu:uev_compressed_fusion_reactor" scale="3" />
    <BlockImage id="gtceu:petrochemical_plant" scale="3" />
    <BlockImage id="gtceu:pcb_factory" scale="3" />
    <BlockImage id="gtceu:advanced_integrated_ore_processor" scale="3" />
    <BlockImage id="gtceu:advanced_neutron_activator" scale="3" />
</Row>

<Row>
    <BlockImage id="gtceu:component_assembly_line" scale="3" />
    <BlockImage id="gtceu:atomic_energy_excitation_plant" scale="3" />
    <BlockImage id="gtceu:huge_incubator" scale="3" />
    <BlockImage id="gtceu:advanced_sps_crafting" scale="3" />
    <BlockImage id="gtceu:wood_distillation" scale="3" />
</Row>

<Row>
    <BlockImage id="gtceu:advanced_rare_earth_centrifugal" scale="3" />
    <BlockImage id="gtceu:gravitation_shockburst" scale="3" />
    <BlockImage id="gtceu:super_particle_collider" scale="3" />
    <BlockImage id="gtceu:matter_fabricator" scale="3" />
</Row>

GTLCore 本体多配方逻辑机器，由本 mod 补充结构或线程舱支持：

<Row>
    <BlockImage id="gtceu:cooling_tower" scale="3" />
    <BlockImage id="gtceu:mega_distillery" scale="3" />
    <BlockImage id="gtceu:holy_separator" scale="3" />
    <BlockImage id="gtceu:field_extruder_factory" scale="3" />
    <BlockImage id="gtceu:mega_canner" scale="3" />
</Row>

<Row>
    <BlockImage id="gtceu:mega_wiremill" scale="3" />
    <BlockImage id="gtceu:mega_presser" scale="3" />
    <BlockImage id="gtceu:mega_extractor" scale="3" />
    <BlockImage id="gtceu:mega_fluid_heater" scale="3" />
    <BlockImage id="gtceu:advanced_multi_smelter" scale="3" />
    <BlockImage id="gtceu:super_blast_smelter" scale="3" />
</Row>

</Column>

<Column gap="2" fullWidth={true}>

## 独立并行与特殊设定

> 这些机器不只是“均分”或“贪婪”的总并行分配。\
> 它们会按每条配方、模块主机状态、能源网络、特殊舱室或机器自身条件独立计算可运行配方。\
> 部分机器会表现为特殊的无限并行，实际运行仍受输入、输出、能源或机器状态限制。

GTLAdditions 自有机器：

<Row>
    <BlockImage id="gtladditions:space_scaling_instrument" scale="3" />
    <BlockImage id="gtladditions:forge_of_the_antichrist" scale="3" />
    <BlockImage id="gtladditions:heliofusion_exoticizer" scale="3" />
    <BlockImage id="gtladditions:helioflare_power_forge" scale="3" />
    <BlockImage id="gtladditions:heliofluix_melting_core" scale="3" />
</Row>

<Row>
    <BlockImage id="gtladditions:heliothermal_plasma_fabricator" scale="3" />
    <BlockImage id="gtladditions:heliophase_leyline_crystallizer" scale="3" />
    <BlockImage id="gtladditions:subspace_corridor_hub_industrial_array" scale="3" />
    <BlockImage id="gtladditions:nexus_satellite_factory_mk1" scale="3" />
    <BlockImage id="gtladditions:nexus_satellite_factory_mk2" scale="3" />
</Row>

<Row>
    <BlockImage id="gtladditions:nexus_satellite_factory_mk3" scale="3" />
    <BlockImage id="gtladditions:nexus_satellite_factory_mk4" scale="3" />
    <BlockImage id="gtladditions:space_infinity_integrated_ore_processor" scale="3" />
    <BlockImage id="gtladditions:macro_atomic_resonant_fragment_stripper" scale="3" />
</Row>

本 mod 改造为特殊处理的外部机器：

<Row>
    <BlockImage id="gtceu:integrated_ore_processor" scale="3" />
    <BlockImage id="gtceu:door_of_create" scale="3" />
    <BlockImage id="gtceu:create_aggregation" scale="3" />
    <BlockImage id="gtceu:molecular_assembler_matrix" scale="3" />
    <BlockImage id="gtceu:advanced_infinite_driller" scale="3" />
</Row>

说明：

* 亚空间航道枢纽模块在未解锁特殊状态时按均分处理；解锁后会按每条配方独立计算，配方并行上限按特殊并行处理。
* 伪神之锻炉及其模块按可运行配方分别计算，并会受主机输出倍率、能源网络和模块自身规则影响。
* 空间缩放仪、天基无尽集成矿石处理厂和宏原子谐振碎片剥离器按每条配方并行独立计算。
* 维度聚焦无尽合成阵列自身属于均分设定；作为分子操纵者模块时，会让分子操纵者进入特殊的无尽合成处理。
* 创造之门、创造聚合仪、集成矿石处理厂和进阶无尽钻机是本 mod 对外部机器的特殊改造，不按本页前两类固定跨配方分配来理解。

</Column>

</Column>
