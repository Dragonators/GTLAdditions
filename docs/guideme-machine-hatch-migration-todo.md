# GuideME Machine and Hatch Migration Todo

本文档用于规划已有机器与舱室的 GuideME 文档迁移。当前阶段只规划迁移流程与待办清单，不开始正式迁移正文。

## Scope

- Local repository: `H:\GTLAdditions`
- Upstream comparison checkout: `H:\GTLAdditions_Origin`
- Local zh-CN machine docs: `src/main/resources/assets/gtladditions/guides/gtladditions/guide/_zh_cn/machine/`
- Local English machine docs: `src/main/resources/assets/gtladditions/guides/gtladditions/guide/machine/`
- Upstream zh-CN/English machine docs use the same relative paths under `H:\GTLAdditions_Origin`.

## Migration Rules

- 先迁移所有中文文档，再以本地中文文档和上游英文用语为基准迁移英文文档。
- 不全量复制上游 GuideME。只迁移本 mod 当前已有机器和舱室对应的页面。
- 上游有页面、本地也有机器或舱室时，优先迁移上游中文页，再按本地实现修正。
- 本地已有页面时，先做中文页差异检查，不直接覆盖已有内容。
- 本地已有机器或舱室但上游没有页面时，标记为“本地补写”，正文必须从本地实现、tooltip、lang、配方和结构约束推导；如行为不明确，迁移前先问。
- GuideME 正文面向玩家可观察行为，不暴露底层能力名、字段名、类名或标签名等具体实现细节；例如 `superPos` 只应表述为“闪存记录了超级样板总成绑定信息”。
- 上游有页面但本地没有明显对应机器或舱室时，不要直接判定迁移或跳过；必须先查本地注册、控制器、结构、配方、lang 和历史迁移文档，判断是否存在改名、拆分、合并或本地替代实现。
- 代码检查后仍确认本地没有对应实现时，不纳入本轮迁移，除非后续正式迁移了该机器或舱室。
- GuideME MDX 规则继续遵守：
  - `<ItemLink>` 同命名空间物品需要可索引页面；外部命名空间缺页面时可退化为 tooltip。
  - 不用单独 `<br/>` 制造块间距；需要间距时用 `<Column gap=... fullWidth={true}>`。
  - 布尔属性写 `{true}` / `{false}`。

## Layout Rules

- 页面正文需要分隔多个说明块、机制块、步骤块、配方块或模块块时，统一使用 `<Column>` 做间隔排版。
- 外层大段落间距建议使用 `<Column gap="15" fullWidth={true}>` 或 `<Column gap="20" fullWidth={true}>`。
- 同一机制内部的短说明、列表和引用块建议使用 `<Column gap="2" fullWidth={true}>`。
- 不使用单独 `<br/>`、连续空行或行尾反斜杠来模拟块间距；只有在 GuideME 渲染确实需要同一引用块内换行时，才保留行尾反斜杠。
- 一个页面内多台模块机器或多个效果频段并列说明时，优先用外层 `<Column gap="20" fullWidth={true}>` 包住全部模块，再用内层 `<Column gap="2" fullWidth={true}>` 包住单个模块内容。
- `<Column>` 布尔属性必须写成 `fullWidth={true}`，不要写 `fullWidth="true"` 或裸 `fullWidth=true`。

推荐结构：

```mdx
<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

### 模块或机制标题

* 第一条说明。
* 第二条说明。

</Column>

<Column gap="2" fullWidth={true}>

### 下一个模块或机制标题

> 需要突出显示的运行条件或消耗说明。

</Column>

</Column>
```

## Migration Flow

- [x] Phase 0: 冻结清单
  - [x] 用 `rg --files` 重新确认本地和上游 GuideME 页面列表。
  - [x] 用 `rg` 对照 `GTLAddMachines.kt` 和 `MultiBlockMachine.kt`，确认每个页面确实对应当前注册机器或舱室。
  - [x] 对“上游有页面、本地列表中没有”的条目，先查本地代码是否存在改名、拆分、合并或替代实现，再决定迁移、改写、并入其他页或暂不迁移。
  - [x] 对本地已有页面先标记为“校验/修订”，不要直接覆盖。

- [x] Phase 1: 中文索引与基础页
  - [x] 更新 `_zh_cn/machine/machine_index.md`，补回“机制”区域：`add_machine_config.md`、`multi_type.md`。
  - [x] 更新 `_zh_cn/machine/part/machine_part_index.md`，补齐已有舱室页面链接。
  - [x] 迁移 `_zh_cn/machine/add_machine_config.md`。
  - [x] 迁移 `_zh_cn/machine/multi_type.md`。该页应只描述 ForgeOfTheAntichrist 和 ApocalypticTorsionQuantumMatrix 等机器侧跨配方模式并行能力，不写具体实现差异。

- [x] Phase 2: 中文舱室页
  - [x] 先迁移上游已有且本地已注册的舱室页。
  - [x] 再补写本地新增舱室页。
  - [x] 所有 `<ItemLink id="gtladditions:...">` 指向本命名空间机器或舱室时，确认目标页是否已存在；没有页面则记录补页或改写为不依赖索引的表述。

- [x] Phase 3: 中文多方块主机页
  - [x] 先迁移上游已有且本地已注册的旧机器/已迁移机器页面。
  - [x] 再校验本地已有页面与当前实现是否一致。
  - [x] 最后补写本地独有机器页面。
  - [x] 对行为复杂机器，每页迁移前先确认代码来源：控制器、结构、配方、tooltip/lang、右键交互、运行状态文本。

- [x] Phase 4: 英文页
  - [x] 以完成后的中文页为结构和事实基准。
  - [x] 对上游已有英文页，沿用上游术语，但修正为本地实现事实。
  - [x] 对本地补写页，先从中文页翻译，再统一术语。

- [x] Phase 5: Verification
  - [x] 检查每个页面 front matter：`navigation.title`、`parent`、`position`、`categories`。
  - [x] 检查索引页能覆盖新增页面。
  - [x] `rg "<ItemLink id=\"gtladditions:"` 检查本命名空间链接是否有页面或明确可接受。
  - [x] 每完成一轮迁移后，重新检查此前没有使用 `<ItemLink>` 的本地机器/舱室名称；如果目标页已经在本轮新增或补齐，应改回 `<ItemLink id="gtladditions:...">`。
  - [x] 对仍不能使用 `<ItemLink>` 的目标，记录原因：页面未迁移、无本地实现、外部命名空间、或需要维护者确认是否建页。
  - [x] `rg "fullWidth=\"true\"|fullWidth=true|<br ?/?>"` 检查 MDX 规则，必要间隔改用 `<Column gap="..." fullWidth={true}>`。
  - [x] 如正文涉及配方或结构，运行 `./gradlew runData` 后复核配方 id；若只改文档，可先不跑完整构建。本轮仅同步 GuideME 文档，未改配方或结构，未运行 `runData`。

## Hatch And Part Pages

| Status | ID / Page | Local registration | Upstream page | Action |
| --- | --- | --- | --- | --- |
| 完成中文校验 | `spectral_analysis_hatch` | `GTLAddMachines.ORE_PROCESSOR_HATCH` | 有 | 已对照本地矿处路径、频道效果、完美调频；保留本地天基无尽集成矿石处理厂说明。 |
| 完成中文校验 | `me_block_conservation` | `GTLAddMachines.ME_BLOCK_CONVERSATION` | 有 | 已对照当前嬗变总线路径和创造数据访问仓行为；未回退到上游旧 Create Door 描述。 |
| 完成中文校验 | `vientiane_transcription_node` | `GTLAddMachines.VIENTIANE_TRANSCRIPTION_NODE` | 有 | 已对照递归反演阵列模块与超时空助推引擎，改写未建模块页链接并修正红石输出方向。 |
| 完成中文迁移 | `huge_steam_hatch_part_machine.md` / `huge_steam_input_hatch` | `GTLAddMachines.HUGE_STEAM_HATCH` | 有 | 已迁移中文页；已确认本地巨型蒸汽输入仓容量、蒸汽过滤和配方来源。 |
| 完成中文迁移 | `super_input_dual_hatch` | `GTLAddMachines.SUPER_INPUT_DUAL_HATCH` | 有 | 已迁移中文页；已确认本地 `SuperDualHatchPartMachine` 物品/流体容量和返回/转移说明。 |
| 完成中文迁移 | `huge_output_dual_hatch` | `GTLAddMachines.HUGE_OUTPUT_DUAL_HATCH` | 有 | 已迁移中文页；已确认分级输出总成注册为 LV-OpV，并补充能力说明。 |
| 完成中文迁移 | `laser_hatch` | laser / wireless laser hatch families | 有 | 已迁移为家族页；已按本地注册写入有线与无线、16777216A 与 67108864A/67108863A 系列说明。 |
| 完成中文补写 | `infinity_input_dual_hatch` | `GTLAddMachines.INFINITY_INPUT_DUAL_HATCH` | 无 | 已独立建页，覆盖本地类、ME 网络优化、转移行为、聚合存储和 UXV 后期制作阶段。 |
| 完成中文补写 | `me_super_pattern_buffer` | `GTLAddMachines.ME_SUPER_PATTERN_BUFFER` | 无 | 已与 proxy 合并为 `me_super_pattern_buffer.md` 家族页，覆盖 ME 网络功能、配置项、闪存绑定和电路嵌入。 |
| 完成中文补写 | `me_super_pattern_buffer_proxy` | `GTLAddMachines.ME_SUPER_PATTERN_BUFFER_PROXY` | 无 | 已并入 `me_super_pattern_buffer.md`，通过 `item_ids` 覆盖镜像。 |
| 完成中文补写 | `super_parallel_hatch` | `GTLAddMachines.SUPER_PARALLEL_HATCH` | 无 | 已独立建页，覆盖本地并行仓实现、可配置范围和共享语义。 |
| 完成中文补写 | `thread_modifier_hatch` | `GTLAddMachines.THREAD_MODIFIER_HATCH` | 无 | 已独立建页，覆盖线程修改器接口、星规矩阵输入和线程公式。 |
| 完成中文补写 | `wireless_energy_network_input_terminal` | `GTLAddMachines.Wireless_Energy_Network_INPUT_Terminal` | 无 | 已与输出终端合并为 `wireless_energy_network_terminal.md` 家族页，覆盖绑定和输入行为。 |
| 完成中文补写 | `wireless_energy_network_output_terminal` | `GTLAddMachines.Wireless_Energy_Network_OUTPUT_Terminal` | 无 | 已并入 `wireless_energy_network_terminal.md`，通过 `item_ids` 覆盖输出终端。 |

## Multiblock Controller Pages

### 上游已有页面，本地已有机器，待迁移中文页

| Status | ID / Page | Local registration | Upstream page | Action |
| --- | --- | --- | --- | --- |
| 完成中文迁移 | `antientropy_condensation_center` | `ANTIENTROPY_CONDENSATION_CENTER` | 有 | 已对照本地实现；当前无 `create_ultimate_battery` 右键加成，按凛冰粉消耗和配方派生事实改写。 |
| 完成中文迁移 | `arcane_cache_vault` | `ARCANE_CACHE_VAULT` | 有 | 已按本地打包机、线圈并行、激光、跨配方和线程修改器事实迁移；保留与空间缩放仪的语义区分。 |
| 完成中文迁移 | `arcanic_astrograph` | `ARCANIC_ASTROGRAPH` | 有 | 已按本地宇宙模拟、星规矩阵并行公式和鸿蒙之眼消耗规则改写。 |
| 完成中文迁移 | `atomic_transmutation_core` | `ATOMIC_TRANSMUTATION_CORE` | 有 | 已确认本地该页应标注废弃；方块嬗变路线指向 `subatomic_transmutatioon_core` 与 `me_block_conservation`。 |
| 完成中文迁移 | `biological_simulation_laboratory` | `BIOLOGICAL_SIMULATION_LABORATORY` | 有 | 已按本地纳米蜂群槽位、不再是菜鸟的证明门控、UXV+ 激光限制和寰宇支配之剑配方条件迁移。 |
| 完成中文迁移 | `dimensionally_transcendent_chemical_plant` | `DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT` | 有 | 已按本地大型化学反应釜配方、UV 阈值分配、线圈/激光/线程修改器事实迁移。 |
| 完成中文迁移 | `draconic_collapse_core` | `DRACONIC_COLLAPSE_CORE` | 有 | 已确认本地无 `harmonizing_core` 槽位或输出替换；按 UEV 后每级 x8 并行和输入侧结构能力迁移。 |
| 完成中文迁移 | `fuxi_bagua_heaven_forging_furnace` | `FUXI_BAGUA_HEAVEN_FORGING_FURNACE` | 有 | 已按本地四配方类型、并行控制仓、线圈显示、激光和线程修改器迁移。 |
| 完成中文迁移 | `inferno_cleft_smelting_vault` | `INFERNO_CLEFT_SMELTING_VAULT` | 有 | 已按本地热解/裂化双配方类型、线圈并行、UV 消声仓和跨配方能力迁移。 |
| 完成中文迁移 | `lucid_etchdreamer` | `LUCID_ETCHDREAMER` | 有 | 已按本地光子晶阵蚀刻配方、线圈并行、激光、跨配方和线程修改器迁移。 |
| 完成中文迁移 | `quantum_syphon_matrix` | `QUANTUM_SYPHON_MATRIX` | 有 | 已确认本地没有 Barnarda 分支；按主世界/下界/末地空气与液态空气路线迁移。 |
| 完成中文迁移 | `skeleton_shift_rift_engine` | `SKELETON_SHIFT_RIFT_ENGINE` | 有 | 已按本地衰变加速配方、线圈温度并行、恒星热力容器耗时缩减和完美超频迁移。 |
| 完成中文迁移 | `time_space_distorter` | `TIME_SPACE_DISTORTER` | 有 | 已按本地绑定增益、数据模块绑定、1 号电路空转、因果扭曲倍率与分级资源消耗迁移。 |
| 完成中文迁移 | `taixu_turbid_array` | `TAIXU_TURBID_ARRAY` | 有 | 已按本地公式、槽位加成、星规矩阵跨配方、输出解锁、固定耗能/耗时迁移。 |
| 完成中文迁移 | `titan_crip_earthbore` | `TITAN_CRIP_EARTHBORE` | 有 | 已按本地地脉断层发生器配方、基岩结构检查、LuV 后每级 x2 并行和完美超频迁移。 |

### 本地已有页面，需校验或保留

| Status | ID / Page | Local registration | Upstream page | Action |
| --- | --- | --- | --- | --- |
| 完成中文校验 | `planetary_ionisation_convergence_tower` | `PLANETARY_IONISATION_CONVERGENCE_TOWER` | 有 | 已确认中文页包含本地行星电离汇流塔工作周期、能量缓存、输出与线圈等级消耗/发电行为。 |
| 完成中文校验 | `recursive_reverse_array` | `RECURSIVE_REVERSE_ARRAY` | 无直接页；上游为 `recursive_reverse_forge` | 已保留本地 BUFF-array 设计；催化迭升阵列、磁流聚敛核心、时空静滞装置、超时空助推引擎均并入本页并由 `item_ids` 提供索引。 |
| 完成中文校验 | `space_infinity_integrated_ore_processor` | `SPACE_INFINITY_INTEGRATED_ORE_PROCESSOR` | 无 | 已按本地无限并行/无限线程、恒星能火箭燃料消耗和谱解析仓完美调频行为校验。 |
| 完成中文校验 | `space_scaling_instrument` | `SPACE_SCALING_INSTRUMENT` | 无 | 已确认中文页按本地空间缩放仪定位保留：打包机之终、无限并行/无限线程、超级热容线圈要求。 |
| 完成中文校验 | `subatomic_transmutatioon_core` | `SUBATOMIC_TRANSMUTATIOON_CORE` | 无；上游近似 `atomic_transmutation_core` | 已保留本地拼写和注册路径；页面按本地方块转换、转换卡、星阵和配方展示校验。 |
| 完成中文同步 | `multiblock_controller.md` | index/support page | 有 | 已同步等级颜色，`MAX` 使用 `#FF0000`。 |

### 本地已有机器，上游无页面，需本地补写

| Status | ID | Local registration | Action |
| --- | --- | --- | --- |
| 完成中文补写 | `apocalyptic_torsion_quantum_matrix` | `APOCALYPTIC_TORSION_QUANTUM_MATRIX` | 已按本地跨配方、无线能源、概率输入/输出和线程/并行行为补写。 |
| 完成中文补写 | `astral_convergence_nexus` | `ASTRAL_CONVERGENCE_NEXUS` | 已按空间电梯模块、空间组装配方、动力模块等级并行和线程修改器行为补写。 |
| 已并入递归反演阵列页 | `catalytic_cascade_array` | `CATALYTIC_CASCADE_ARRAY` | 不独立建页；由 `recursive_reverse_array.md` 的 `item_ids` 提供 `<ItemLink>` 索引目标，并在模块章节中说明。 |
| 完成中文补写 | `dimension_focus_infinity_crafting_array` | `DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY` | 已按梦魇合成、独立并行和作为分子操纵者模块时的无尽合成模式补写。 |
| 完成中文补写 | `forge_of_the_antichrist` | `FORGE_OF_THE_ANTICHRIST` | 已独立建页，并说明连续运行效率、无线能源、跨配方、Helio 模块和递归反演阵列关系。 |
| 完成中文补写 | `heart_of_the_universe` | `HEART_OF_THE_UNIVERSE` | 已按创世引擎、无线能源网络输出和平面展开行为补写。 |
| 完成中文补写 | `helioflare_power_forge` | `HELIOFLARE_POWER_FORGE` | 已合并至 `helio_forge_modules.md` 系列页，说明运行主机要求、无线能源、无限并行/线程、合金高炉产出倍率共享和 EU 减免。 |
| 完成中文补写 | `heliofluix_melting_core` | `HELIOFLUIX_MELTING_CORE` | 已合并至 `helio_forge_modules.md` 系列页，说明混沌炼金/分子解构、混沌炼金产出倍率共享和 EU 减免。 |
| 完成中文补写 | `heliofusion_exoticizer` | `HELIOFUSION_EXOTICIZER` | 已合并至 `helio_forge_modules.md` 系列页，说明物质异化、单配方锁定、产出倍率共享和 EU 减免。 |
| 完成中文补写 | `heliophase_leyline_crystallizer` | `HELIOPHASE_LEYLINE_CRYSTALLIZER` | 已合并至 `helio_forge_modules.md` 系列页，说明龙脉结晶、主机最大效率门槛和 EU 减免。 |
| 完成中文补写 | `heliothermal_plasma_fabricator` | `HELIOTHERMAL_PLASMA_FABRICATOR` | 已合并至 `helio_forge_modules.md` 系列页，说明星焰跃迁/聚变/粒子对撞机、非星焰跃迁产出倍率共享和 EU 减免。 |
| 完成中文补写 | `light_hunter_space_station` | `LIGHT_HUNTER_SPACE_STATION` | 已按主站、枢纽卫星工厂模块、星规矩阵和悖论实现理论补写，并将相关物品名改为 `<ItemLink>`。 |
| 完成中文补写 | `macro_atomic_resonant_fragment_stripper` | `MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER` | 已按元素复制、星核剥离、线圈/星规矩阵要求和 GTLCore 空岛模式差异补写。 |
| 已并入递归反演阵列页 | `magnetorheological_convergence_core` | `MAGNETORHEOLOGICAL_CONVERGENCE_CORE` | 不独立建页；由 `recursive_reverse_array.md` 的 `item_ids` 提供 `<ItemLink>` 索引目标，并在模块章节中说明。 |
| 完成中文补写 | `nebula_reaper` | `NEBULA_REAPER` | 已按空间电梯模块、空间采矿/钻探配方、动力模块等级并行和线程修改器行为补写。 |
| 完成中文补写 | `nexus_satellite_factory_mk1` | `NEXUS_SATELLITE_FACTORY_MKI` | 已合并至 `nexus_satellite_factory.md` 系列页，说明与猎光号同属 UXV 阶段、主站连接要求、配方类别和悖论实现理论联动。 |
| 完成中文补写 | `nexus_satellite_factory_mk2` | `NEXUS_SATELLITE_FACTORY_MKII` | 同上。 |
| 完成中文补写 | `nexus_satellite_factory_mk3` | `NEXUS_SATELLITE_FACTORY_MKIII` | 同上。 |
| 完成中文补写 | `nexus_satellite_factory_mk4` | `NEXUS_SATELLITE_FACTORY_MKIV` | 同上。 |
| 已并入递归反演阵列页 | `spacetime_stasis_device` | `SPACETIME_STASIS_DEVICE` | 不独立建页；由 `recursive_reverse_array.md` 的 `item_ids` 提供 `<ItemLink>` 索引目标，并在模块章节中说明。 |
| 已并入递归反演阵列页 | `supratemporal_boosting_engine` | `SUPRATEMPORAL_BOOSTING_ENGINE` | 不独立建页；由 `recursive_reverse_array.md` 的 `item_ids` 提供 `<ItemLink>` 索引目标，并在模块章节中说明。 |

## Upstream Pages Not In This Round

这些上游页面初步没有对应当前本地注册机器，或者本地采用了不同设计。本轮不直接迁移正文，仅作为后续机器迁移时的参考。

处理规则：

- [ ] 每个条目在最终跳过前，都要先用 `rg` 检查本地注册、控制器、结构、配方、lang、GuideME 现有页和 `docs/` 迁移记录。
- [ ] 如果发现本地只是改名、拆分、合并或转为替代设计，不能复制上游页面；应把上游内容作为参考，改写到本地实际页面或记录为本地补写任务。
- [ ] 如果本地没有任何对应实现，再保持“不在本轮迁移”状态。
- [ ] 如果本地实现是否对应上游页面仍不明确，先问维护者，不要为了补齐目录而迁移。

- `floating_light_deep_space_industrial_vessel`
- `recursive_reverse_forge`
- `advanced_space_elevator_module`
- `floating_light_deep_space_industrial_vessel_module_1` 至 `module_5`
- `hyperdimensional_energy_concentrator`
- `reverse_time_boosting_engine`
- `fractal_manipulator`
- `super_factory_mk1` 至 `super_factory_mk4`

## Suggested Work Batches

- [x] Batch A: 索引和基础页
  - [x] `machine_index.md`
  - [x] `machine_part_index.md`
  - [x] `add_machine_config.md`
  - [x] `multi_type.md`
  - [x] `multiblock_controller.md`

- [x] Batch B: 舱室页
  - [x] `huge_steam_hatch_part_machine.md`
  - [x] `super_input_dual_hatch.md`
  - [x] `huge_output_dual_hatch.md`
  - [x] `laser_hatch.md`
  - [x] 校验 `spectral_analysis_hatch.md`
  - [x] 校验 `me_block_conservation.md`
  - [x] 校验 `vientiane_transcription_node.md`
  - [x] 确认本地补写舱室是否独立建页
  - [x] Batch B 完成后重新扫描本地 `gtladditions:` 机器/舱室名称，能链接的改为 `<ItemLink>`。

- [x] Batch C: 上游已有旧机器页
  - [x] `lucid_etchdreamer.md`
  - [x] `atomic_transmutation_core.md`
  - [x] `arcanic_astrograph.md`
  - [x] `arcane_cache_vault.md`
  - [x] `draconic_collapse_core.md`
  - [x] `titan_crip_earthbore.md`
  - [x] `biological_simulation_laboratory.md`
  - [x] `dimensionally_transcendent_chemical_plant.md`
  - [x] `quantum_syphon_matrix.md`
  - [x] `fuxi_bagua_heaven_forging_furnace.md`
  - [x] `antientropy_condensation_center.md`
  - [x] `taixu_turbid_array.md`
  - [x] `inferno_cleft_smelting_vault.md`
  - [x] `skeleton_shift_rift_engine.md`
  - [x] Batch C 完成后重新扫描旧机器互相引用，能链接的改为 `<ItemLink>`。

- [x] Batch D: 本地已迁移/本地设计页校验
  - [x] `planetary_ionisation_convergence_tower.md`
  - [x] `recursive_reverse_array.md`
  - [x] `space_infinity_integrated_ore_processor.md`
  - [x] `space_scaling_instrument.md`
  - [x] `subatomic_transmutatioon_core.md`
  - [x] Batch D 完成后重新检查这些页面中的普通机器名是否可替换为 `<ItemLink>`。

- [x] Batch E: 本地独有机器补写
  - [x] 先确认是否按系列合并页面：Helio 系列、Recursive Reverse Array 模块、无线电网终端。
  - [x] 对每台机器建立“控制器/结构/配方/tooltip/lang/右键交互”证据表。
  - [x] 补写本轮目录缺失页面：`nebula_reaper.md`、`light_hunter_space_station.md`、`astral_convergence_nexus.md`、`apocalyptic_torsion_quantum_matrix.md`、`forge_of_the_antichrist.md`、`heart_of_the_universe.md`、`dimension_focus_infinity_crafting_array.md`、`macro_atomic_resonant_fragment_stripper.md`、`helio_forge_modules.md`。
  - [x] 证据不足或多种写法都合理时，先问再写。
  - [x] Batch E 完成后对全体 GuideME 机器/舱室页做一次 `<ItemLink>` 回填检查。

- [x] Batch F: 英文同步
  - [x] 按 Batch A-E 的中文最终稿逐页同步英文。
  - [x] 上游英文术语只作为术语来源，不作为事实来源。
  - [x] 英文页完成后再次检查中英文目录一致性。
  - [x] 英文页完成后复查英文普通机器/舱室名称是否应同步改为 `<ItemLink>`。