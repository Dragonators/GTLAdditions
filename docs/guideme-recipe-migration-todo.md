# GuideME Recipe Migration Todo

本文档用于规划 GTLAdditions 配方相关 GuideME 文档迁移。当前阶段只规划迁移流程与核验清单，不开始正式迁移正文。

## Scope

- Local repository: `H:\GTLAdditions`
- Upstream comparison checkout: `H:\GTLAdditions_Origin`
- Local zh-CN recipe docs: `src/main/resources/assets/gtladditions/guides/gtladditions/guide/_zh_cn/recipe/`
- Local English recipe docs: `src/main/resources/assets/gtladditions/guides/gtladditions/guide/recipe/`
- Local zh-CN modify docs: `src/main/resources/assets/gtladditions/guides/gtladditions/guide/_zh_cn/modify/`
- Local English modify docs: `src/main/resources/assets/gtladditions/guides/gtladditions/guide/modify/`
- Upstream zh-CN/English recipe docs use the same relative paths under `H:\GTLAdditions_Origin`.
- Local recipe data entry point: `src/main/java/com/gtladd/gtladditions/GTLAdditionsGTAddon.kt`.
- Local recipe providers: `src/main/java/com/gtladd/gtladditions/data/recipes/`.
- Local GuideME recipe renderer mapping: `src/main/java/com/gtladd/gtladditions/data/guide/RecipeTypeContributions.kt`.

## Current Snapshot

- 本地 `recipe/`、`_zh_cn/recipe/`、`modify/` 与 `_zh_cn/modify/` 目录已经建立，并完成中英文同步。
- 已落地的 recipe GuideME 页面包括：`recipe_index.md`、`ae2_recipe.md`、`compressed_astral_array_recipe.md`、`assembler_recipe.md`、`distort_recipe.md`、`extended_fluid_recipe.md`、`integratedted_ore_processor_recipe.md`、`leyline_crystallize_recipe.md`、`matter_exotic_recipe.md`、`nightmare_crafting_recipe.md`、`others_recipe.md`、`qft_recipe.md`、`soc_process.md`。
- 已落地的 modify GuideME 页面包括：`modify_index.md`、`eye_of_harmony.md`、`create_door_aggregator.md`、`computation_data_hatch.md`、`auto_configuration_maintenance_hatch.md`、`active_transformer.md`、`stargate_journey.md`、`cross_recipe_parallel_modifications.md`、`molecular_assembler_matrix.md`、`wireless_energy_monitor.md`、`ae2_automation.md`。
- 本地 `GTLAdditionsGTAddon.addRecipes(...)` 已接入 `AE2`、`Assembler`、`Distort`、`ElectricBlastFurnace`、`IntegratedOreProcessor`、`LeylineCrystallize`、`NightmareCrafting`、`Qft`、`SocProcess`、`Misc` 与物质异化等当前 GuideME 页面所需 provider。
- 本地 `RecipeTypeContributions.kt` 已补齐当前 recipe GuideME 页面所需的渲染映射；`runData` 复核仍受本地 AE2 / JEI 依赖版本约束阻塞。

## Migration Rules

- 规则与机器/舱室 GuideME 迁移一致：先迁移所有中文文档，再以本地中文文档和上游英文用语为基准迁移英文文档。
- 不全量复制上游 GuideME。只迁移本地代码中确实存在、能生成或能由当前运行时提供的配方相关页面。
- 魔改页面同样只记录玩家可观察行为；不要暴露 mixin、class、trait、recipe logic、字段名、能力名等具体实现细节。
- 上游有页面但本地没有明显对应 provider、recipe type 或 recipe id 时，不要直接迁移或跳过；必须先查本地 recipe provider、`GTLAdditionsGTAddon.addRecipes(...)`、`RecipeTypeContributions.kt`、`src/main/resources/data/gtladditions/recipes/`、`src/generated/resources/` 和相关迁移文档。
- 代码检查后仍确认本地没有对应配方时，不纳入本轮正文迁移，除非后续正式迁移了对应配方或改写为“不含 Recipe 展示”的说明页。
- 每个 `<Recipe id="gtladditions:...">` 必须逐条确认：
  - [ ] 本地源码中存在对应 `recipeBuilder(...)` 或手写 JSON。
  - [ ] `GTLAdditionsGTAddon.addRecipes(...)` 会调用对应 provider。
  - [ ] `runData` 后生成的 recipe id 与页面中的 id 完全一致。
  - [ ] 该 recipe type 已在 `RecipeTypeContributions.kt` 中映射，或 GuideME 已能通过其他机制显示。
- 不确定 recipe id、条件配方、配置门控或数据生成输出时，先问，不要凭上游页面补正文。
- 页面正文需要分隔多个说明块、配方块、产线阶段或注意事项时，统一使用 `<Column>` 做间隔排版：
  - 外层大段落建议 `<Column gap="15" fullWidth={true}>` 或 `<Column gap="20" fullWidth={true}>`。
  - 同一产线阶段内部建议 `<Column gap="2" fullWidth={true}>`。
  - 不使用单独 `<br/>`、连续空行或行尾反斜杠模拟块间距；只有 GuideME 渲染确实需要同一引用块内换行时，才保留行尾反斜杠。
- `<ItemLink>` / `<FluidLink>` 规则与机器迁移一致：
  - 本命名空间 `gtladditions:` 物品、流体、机器若已有或本轮新增可索引页面，优先使用链接。
  - 每轮迁移完成后，重新检查此前没有使用链接的本地物品/流体/机器名；目标页或目标资源补齐后应回填为 `<ItemLink>` / `<FluidLink>`。
  - 对仍不能使用链接的目标，记录原因：页面未迁移、资源未注册、外部命名空间、或需要维护者确认是否建页。

## Migration Flow

- [x] Phase 0: 冻结清单
  - [x] 用 `rg --files` 重新确认本地和上游 recipe GuideME 页面列表。
  - [x] 用 `rg` 对照 `GTLAdditionsGTAddon.kt`、`data/recipes/`、`GTLAddRecipesTypes.kt` 和 `RecipeTypeContributions.kt`。
  - [x] 对上游每个 `<Recipe id="...">` 建立“页面 -> recipe id -> 本地 provider -> recipe type -> renderer mapping -> runData 输出”的证据表。
  - [x] 对“上游有页面、本地没有同名 provider 或 recipe id”的条目，先查是否存在改名、拆分、合并、配置门控或本地替代 recipe。

- [x] Phase 1: 中文索引与目录
  - [x] 新建或迁移 `_zh_cn/recipe/recipe_index.md`。
  - [x] 只链接已确认本地可迁移的 recipe 页面。
  - [x] 对暂缓页面不要放进索引，或在索引迁移后作为未启用项记录在本 TODO 中。

- [x] Phase 2: 中文 `<Recipe>` 展示页
  - [x] 优先迁移本地 recipe id 已确认存在、且 `RecipeTypeContributions.kt` 已支持显示的页面。
  - [x] 对 recipe id 存在但 renderer mapping 缺失的页面，先记录需要补映射；不直接迁移会渲染失败的 `<Recipe>`。
  - [x] 对 recipe id 与上游不一致的页面，先改为本地 id 或暂缓。

- [x] Phase 3: 中文说明型产线页
  - [x] 迁移 `soc_process.md` 前，核对本地 `SocProcess.kt` 中所有阶段、物品、流体、机器名是否仍一致。
  - [x] 使用 `<Column>` 分隔“单晶硅 / 晶圆 / 处理后晶圆 / SoC 晶圆 / SoC 芯片”等阶段。
  - [x] 对本地新增或不同的 SoC 产线 recipe，优先按本地事实改写中文页。

- [x] Phase 4: 英文页
  - [x] 以完成后的中文页为事实和结构基准。
  - [x] 上游英文只作为术语参考，不作为事实来源。
  - [x] 英文页完成后复查英文普通物品/流体/机器名称是否应同步改为 `<ItemLink>` / `<FluidLink>`。

- [x] Phase 5: Verification
  - [x] 检查每个页面 front matter：`navigation.title`、`parent`、`position`、`categories`。
  - [x] `rg "<Recipe id=\""` 检查所有 recipe id，确认源码或生成资源中存在。
  - [x] `rg "<ItemLink id=\"gtladditions:|<FluidLink id=\"gtladditions:"` 检查本命名空间链接是否可用或记录原因。
  - [x] `rg "fullWidth=\"true\"|fullWidth=true|<br ?/?>"` 检查 MDX 规则，必要间隔改用 `<Column gap="..." fullWidth={true}>`。
  - [x] 已尝试运行 `./gradlew runData` 复核生成资源路径；当前阻塞于本地依赖版本检查，未能完成该步。
  - [x] 已运行 `./gradlew --rerun-tasks processResources` 与 `./gradlew --rerun-tasks compileKotlin compileJava`；当前未运行完整 `spotlessApply build`。

## Recipe Pages

| Status | Page | Upstream recipe ids / content | Local code evidence | Action |
| --- | --- | --- | --- | --- |
| 完成 Batch A | `recipe_index.md` | 上游索引链接 8 个 recipe 页和 SoC 产线 | 本地 recipe 目录已新建 | 中文/英文索引已建立；当前只链接已确认可显示的装配机配方页。 |
| 完成 Batch D | `compressed_astral_array_recipe.md` | `compressed_astral_array/compressed_astral_array` | `Misc.kt` 存在对应 `COMPRESSED_ASTRAL_ARRAY.recipeBuilder(...)`；`ArcanicAstrograph` 与 `RecipeTypeContributions.kt` 已接入对应配方类型 | 中英文页已迁移，并从 recipe index 链接。 |
| 完成 Batch B | `ae2_recipe.md` | `matter_fabricator/singularity_1`、`matter_fabricator/singularity_2` | `AE2.kt` 存在对应 `MATTER_FABRICATOR_RECIPES.recipeBuilder(...)`；`addRecipes` 已调用 `AE2.init(provider)`；已补 renderer mapping | 中英文页已迁移。 |
| 完成 Batch A | `assembler_recipe.md` | `assembler/naquadria_charge_more`、`assembler/leptonic_charge`、`assembler/quantum_chromodynamic_charge` | `Assembler.kt` 存在对应 id；`RecipeTypeContributions.kt` 已映射 `ASSEMBLER_RECIPES`；`addRecipes` 已调用 `Assembler.init(provider)` | 中英文页已迁移。未改 recipe provider；后续统一 runData 时复核生成路径。 |
| 完成 Batch B | `distort_recipe.md` | `distort/rare_earth_dust_monazite`、`rhenium_dust`、`composite_1/2/3`、`biology_process`、`bedrock_gas`、`trinium_compound`、`agar_dust`、`tcetieseaweedextract` | `Distort.kt` 存在对应 `DISTORT_RECIPES.recipeBuilder(...)`；`addRecipes` 已调用 `Distort.init(provider)`；已补 renderer mapping | 中英文页已迁移。 |
| 完成 Batch B | `qft_recipe.md` | `qft/resonating_gem`、`gamma_rays_photoresist`、`radox_easy`、`super_mutated_living_solder` | `Qft.kt` 存在对应 `QFT_RECIPES.recipeBuilder(...)`；`addRecipes` 已调用 `Qft.init(provider)`；已补 renderer mapping | 中英文页已迁移。 |
| 完成 Batch C | `integratedted_ore_processor_recipe.md` | 说明集成矿处 24 号、8 号、9 号电路配方，无 `<Recipe>` 标签 | 本地 `IntegratedOreProcessor.kt` 存在 `jasper_ore_processed`、`purified_*_ore_8/9` 生成逻辑；本地机器行为已迁移并有谱解析仓互动 | 中英文说明页已按本地事实改写；保留上游文件名拼写只为路径兼容。 |
| 完成 Batch D | `extended_fluid_recipe.md` | `alloy_blast_smelter/ruridit`、`alloy_blast_smelter/ruridit_gas`，说明 `molten_ruridit` / `liquid_ruridit` | 已补本地 `RuriditExtend.kt` 与 `GTLAdditionsGTAddon.addRecipes(...)` 入口；已补 `ALLOY_BLAST_RECIPES`、`CHAOTIC_ALCHEMY`、`MOLECULAR_DECONSTRUCTION` GuideME 映射 | 中英文页已展示钌铱合金高炉、分子解构、混沌炼金配方，并移除迁移过程说明。 |
| 完成 Batch D | `matter_exotic_recipe.md` | 本地 `HeliofusionExoticizer.kt` 中 `MATTER_EXOTIC` 五条配方 | `HeliofusionExoticizer.init(provider)` 已接入 `addRecipes`；已补 `MATTER_EXOTIC` GuideME 映射 | 新增中英文物质异化配方页，并从 recipe index 链接。 |
| 完成 Batch D | `nightmare_crafting_recipe.md` | `NightmareCrafting.kt` 五条配方与 `StarGate.kt` 四条经典星门配方 | `NightmareCrafting.init(provider)` 与 `StarGate.init(provider)` 已接入 `addRecipes`；已补 `NIGHTMARE_CRAFTING` GuideME 映射 | 新增中英文梦魇合成配方页，并从 recipe index 链接。 |
| 完成 Batch D | `leyline_crystallize_recipe.md` | `LeylineCrystallize.kt` 四条配方 | `LeylineCrystallize.init(provider)` 已接入 `addRecipes`；已补 `LEYLINE_CRYSTALLIZE` GuideME 映射 | 新增中英文龙脉结晶配方页，并从 recipe index 链接。 |
| 完成 Batch D | `others_recipe.md` | `electric_blast_furnace/magnesium_chloride_dust`、`decay_hastener/titanium50`、`greenhouse/apple`、`apple_fertiliser`、`incubator/cake`、`bee_spawn_egg`、`honey_bottle`、`mixer/warped_ender_pearl` | `ElectricBlastFurnace.kt` 有 `magnesium_chloride_dust`；`Misc.kt` 当前是 `tiranium50` 而非上游 `titanium50`；本地检索未确认其余同名 id | 已迁移已确认的 `magnesium_chloride_dust` 与本地 `tiranium50`；页面正文已移除上游对照和 id 说明。 |
| 完成 Batch C | `soc_process.md` | SoC 产线说明，含 `gallium_oxide_dust`、`periodicium_wafer` 等链接 | `SocProcess.kt` 已接入 `addRecipes`，包含 EBF、Cutter、EngravingArray、CircuitAssembler 和额外 generatedRecipe | 中英文说明页已按本地阶段和本地物品事实重写；使用 `<Column>` 分隔阶段。 |

## Modify Pages

| Status | Page | Upstream content | Local code evidence | Action |
| --- | --- | --- | --- | --- |
| 完成 Batch F | `modify_index.md` | 上游索引链接 5 个魔改页 | 本地 `modify` 目录此前不存在 | 中英文索引已建立；根索引已补机器、配方、魔改入口。 |
| 完成 Batch F | `eye_of_harmony.md` | 鸿蒙之眼与奥术星图氢氦存储说明 | `HarmonyMachineMixin` 修改氢、氦存储与显示；`MultiBlockMachine.kt` 注册本地奥术星图 | 中英文页已按本地显示信息改写，避免实现细节。 |
| 完成 Batch F | `create_door_aggregator.md` | 创造之门、创造聚合仪转换说明和配方 | `MultiBlockModify.kt` 替换机器行为；`DoorOfCreate.kt` / `CreateAggregation.kt` 支持线程修改仓；`Misc.kt` 生成 4 条本地配方 | 中英文页已改写为本地线程修改仓与本地配方；已补 `DOOR_OF_CREATE_RECIPES` / `CREATE_AGGREGATION_RECIPES` 渲染映射。 |
| 完成 Batch F | `computation_data_hatch.md` | 无线算力与数据仓可共享 | `WirelessOpticalComputationHatchMachineMixin` 和 `WirelessOpticalDataHatchMachineMixin` 使对应仓室可共享 | 中英文页已迁移为玩家可见描述。 |
| 完成 Batch F | `auto_configuration_maintenance_hatch.md` | 可配置自动维护仓可共享并支持主机扩展范围 | `AutoConfigurationMaintenanceHatchPartMachineMixin` 支持共享、主机槽与 3 组范围；语言键已存在 | 中英文页已迁移，使用 `<Column>` 排版。 |
| 完成 Batch F | `active_transformer.md` | 有源变压器结构限制调整 | `MultiBlockModify.kt` 指向本地 `WorkableMultiBlock.ACTIVE_TRANSFORMER`；结构不设置额外最小数量 | 中英文页已迁移为结构行为描述。 |
| 完成 Batch F | `stargate_journey.md` | 星门之旅结构与奖励说明 | `AbstractStargateEntityMixin`、`StargateConnectionMixin` 与相关资源说明了本地星门奖励与跨维度限制 | 中英文页已按本地行为迁移。 |
| 完成 Batch F | `cross_recipe_parallel_modifications.md` | 既有多方块的跨配方并行改造说明 | `MutableMultiBlockModify.kt`、`MultiBlockModify.kt` 与线程修改仓相关页面提供了本地证据 | 中英文页已按本地可观察行为迁移。 |
| 完成 Batch F | `molecular_assembler_matrix.md` | 分子操纵者与无尽合成模块联动说明 | `MolecularAssemblerMultiblockMachine.kt` 与对应 GuideME 机器页提供了本地联动事实 | 中英文页已按本地行为迁移。 |
| 完成 Batch F | `wireless_energy_monitor.md` | 无线能源显示与监控格式化说明 | `WirelessEnergyMonitorMixin` 提供本地大数值显示与监控行为 | 中英文页已按本地显示行为迁移。 |
| 完成 Batch F | `ae2_automation.md` | AE2 成型面板与回收相关自动化说明 | `ItemPlacementStrategyMixin` 等本地实现提供了成型与回收行为证据 | 中英文页已迁移，并从 `create_door_aggregator.md` 与 modify index 链接。 |

## Recipe Type Mapping Tasks

在迁移含 `<Recipe>` 的页面前，先确认以下 recipe type 是否需要加入 `RecipeTypeContributions.kt`：

- [x] `MATTER_FABRICATOR_RECIPES`：用于 `ae2_recipe.md`。
- [x] `DISTORT_RECIPES`：用于 `distort_recipe.md`。
- [x] `QFT_RECIPES`：用于 `qft_recipe.md`。
- [x] `ALLOY_BLAST_RECIPES`：用于 `extended_fluid_recipe.md` 钌铱合金高炉配方。
- [x] `CHAOTIC_ALCHEMY`：用于 `extended_fluid_recipe.md` 钌铱合金混沌炼金配方。
- [x] `MOLECULAR_DECONSTRUCTION`：用于 `extended_fluid_recipe.md` 钌铱合金分子解构配方。
- [x] `MATTER_EXOTIC`：用于 `matter_exotic_recipe.md`。
- [x] `NIGHTMARE_CRAFTING`：用于 `nightmare_crafting_recipe.md`。
- [x] `LEYLINE_CRYSTALLIZE`：用于 `leyline_crystallize_recipe.md`。
- [x] `COMPRESSED_ASTRAL_ARRAY`：用于 `compressed_astral_array_recipe.md`。
- [x] `DECAY_HASTENER_RECIPES`：用于 `others_recipe.md` 中本地实际 id `tiranium50`。
- [x] `DOOR_OF_CREATE_RECIPES`：用于 `modify/create_door_aggregator.md`。
- [x] `CREATE_AGGREGATION_RECIPES`：用于 `modify/create_door_aggregator.md`。
- [ ] `GREENHOUSE_RECIPES` / `INCUBATOR_RECIPES`：仅在确认本地有对应页面展示需求后再加入。
- [ ] `INTEGRATED_ORE_PROCESSOR`：当前上游页面没有 `<Recipe>` 标签；如改为 recipe 展示页再考虑加入。

## Suggested Work Batches

- [x] Batch A: 索引和最小可显示页面
  - [x] 新建 `_zh_cn/recipe/recipe_index.md`。
  - [x] 迁移 `_zh_cn/recipe/assembler_recipe.md`。
  - [x] 迁移英文 `recipe_index.md` 和 `assembler_recipe.md`。
  - [x] Batch A 后重新检查 `<ItemLink>` / `<FluidLink>` 是否可回填。

- [x] Batch B: 已有本地 recipe id、缺 renderer mapping 的页面
  - [x] 补或确认 `MATTER_FABRICATOR_RECIPES` 映射。
  - [x] 迁移 `ae2_recipe.md` 中英文。
  - [x] 补或确认 `DISTORT_RECIPES` 映射。
  - [x] 迁移 `distort_recipe.md` 中英文。
  - [x] 补或确认 `QFT_RECIPES` 映射。
  - [x] 迁移 `qft_recipe.md` 中英文。
  - [x] 已尝试运行 `runData` 复核 recipe id；当前阻塞于本地依赖版本检查：`gtlcore` 要求 AE2 `[15.4.10,)`、JEI `[15.20.0.129,)`，当前运行环境分别为 AE2 `15.2.1`、JEI `15.19.5.99`；Kotlin 编译阶段已通过。

- [x] Batch C: 说明型页面
  - [x] 迁移并本地化改写 `integratedted_ore_processor_recipe.md`。
  - [x] 迁移并本地化改写 `soc_process.md`。
  - [x] 使用 `<Column>` 重排长段落和阶段说明。
  - [x] Batch C 后重新检查本地 `gtladditions:` 物品、流体和机器链接。

- [x] Batch D: 风险页面
  - [x] 对 `extended_fluid_recipe.md` 做 runData / 源码证据核查，确认是否有 `ruridit` / `ruridit_gas` recipe。
  - [x] 对 `others_recipe.md` 逐条核验 recipe id，尤其 `titanium50` vs `tiranium50`。
  - [x] 只迁移存在且可显示的 `<Recipe>`；不存在的条目留在 TODO 或改写说明。

- [x] Batch E: 全量复核
  - [x] 中英文目录一致性。
  - [x] `<Recipe>` id 与源码 / 已生成资源一致性已复核；`runData` 外部阻塞已单独记录。
  - [x] `<ItemLink>` / `<FluidLink>` 回填。
  - [x] `<Column>` 排版规范。
  - [x] 已运行 `processResources` 与 Kotlin / Java 编译门禁；完整 `spotlessApply build` 暂未执行。

- [x] Batch F: 魔改 GuideME 页面
  - [x] 用 `rg --files` 对照上游和本地 `modify` 页面列表。
  - [x] 逐页核对本地代码证据，不直接复制上游旧玩法说明。
  - [x] 新建 `_zh_cn/modify/` 与 `modify/` 中英文页面。
  - [x] 为创造之门与创造聚合仪的本地 `<Recipe>` 展示补 renderer mapping。
  - [x] 更新根索引入口。

## Special Recipe Candidates

已扫 `GTLAddRecipesTypes.kt` 与 `data/recipes/newmachinerecipe/`。物质异化、梦魇合成与龙脉结晶已经补页；后续较适合继续补 GuideME 的特殊配方候选：

- `BIOLOGICAL_SIMULATION`：覆盖特殊生物产物与模拟实验室配方，适合按产物分组展示。
- `VOIDFLUX_REACTION`：与巴纳德空气和虚空反应相关，适合放在对应机器或配方页。
- `PHOTON_MATRIX_ETCH`：配方数量较多且含自动镜像到其他蚀刻设备的内容，适合单独规划后迁移。