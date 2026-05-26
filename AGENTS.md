# AGENTS.md

This file provides guidance to Codex when working with this repository.

## Project Overview

GTLAdditions is a Minecraft Forge 1.20.1 mod for the GregTech Leisure environment. It extends GTCEu/GTLCore with new multiblock machines, hatches, materials, recipes, renderers, network sync, and external-mod integrations. The project uses Kotlin for most mod logic and data generation, with Java mainly used for mixins.

## Non-Negotiable Rule

**Do not guess the maintainer's intent. If a request, requirement, target file, behavior, or acceptance criterion is unclear, ask for clarification before making assumptions or changing code.**

This rule takes priority over implementation momentum. If the repository has multiple plausible ownership locations or behavior paths, inspect the code first; if ambiguity remains, stop and ask before editing.

## Search Rule

- When searching for files, symbols, or text in this repository, prefer `rg` / `rg --files` first. Only fall back to slower tools when `rg` is unavailable or clearly unsuitable for the task.
- The local upstream comparison checkout is `H:\GTLAdditions_Origin`. Use it when migrating upstream code, assets, GuideME files, localization, or recipes; prefer `rg` / `rg --files` against that path before assuming an upstream file location.

## Recent Migration Notes

- 上游路径固定为 `H:\GTLAdditions_Origin`，迁移时优先用 `rg` 对照上下游文件。
- 机器部件，注册在 `GTLAddMachines.kt`，配方应放入 `PartMachine.kt`，不要塞进多方块主机配方文件。
- GuideME 的 `<ItemLink>` 同命名空间物品需要有可索引页面；外部命名空间物品没有页面时可以退化显示 tooltip。
- GuideME 中块间距不要依赖单独的 `<br/>`；需要实际显示间距时用 `<Column gap=... fullWidth={true}>`。
- MDX 布尔属性必须写成 `{true}` / `{false}`，不要写 `"true"` 或裸 `true`。
- GuideME 中英文同步时，以中文页为当前标准，再修英文页；避免英文页保留旧逻辑描述。
- 多方块主机阶段颜色应按上游迁移，例如 `MAX` 使用 `#FF0000`。

## Build Commands

```bash
# Build the mod
./gradlew build

# Run in development environment
./gradlew runClient

# Run server in development environment
./gradlew runServer

# Generate data: recipes, models, blockstates, loot tables
./gradlew runData

# Apply formatting
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Clean build artifacts
./gradlew clean
```

Use `gradle` instead of `./gradlew` only if the Gradle wrapper is unavailable.

## Initialization Flow

- Main Forge mod entry: `src/main/java/com/gtladd/gtladditions/GTLAdditions.kt`.
  - Initializes creative tabs, config, networking, Forge event bus registration, and Registrate listeners.
  - Registers GTCEu `GTRecipeType` event to call `GTLAddRecipesTypes.init()`.
  - Registers GTCEu `MachineDefinition` event to call `GTLAddMachines.init()`.
  - Handles material registry creation and material registration events.
- GTCEu addon entry: `src/main/java/com/gtladd/gtladditions/GTLAdditionsGTAddon.kt`.
  - Provides `GTLAddRegistration.REGISTRATE`.
  - Calls `GTLAddItems.init()` and `GTLAddBlocks.init()` in `initializeAddon()`.
  - Calls all data recipe generator entry points in `addRecipes(...)`.
  - Calls `GTLAddSoundEntries.init()` and `GTLAddElements.init()`.
- Registrate wrapper: `src/main/java/com/gtladd/gtladditions/api/registry/GTLAddRegistration.kt`.
  - Owns the shared `REGISTRATE`.
  - Overrides `multiblock(...)` to use `GTLAddMultiBlockMachineBuilder`.

## Responsibility Map

### Machine Controllers

- New multiblock controller implementation classes live under `src/main/java/com/gtladd/gtladditions/common/machine/multiblock/controller/`.
- Module controllers live under `common/machine/multiblock/controller/module/`.
- Mutable/replacement controllers for existing machines live under `common/machine/multiblock/controller/mutable/`.
- Shared base machine implementations live under:
  - `api/machine/multiblock/` for GTLAdd multiple-recipe multiblock bases.
  - `api/machine/wireless/` for wireless variants.
  - `api/machine/mutable/` for mutable/thread-modified machine bases.
- Upstream now uses the corrected directory name `multiblock`. When migrating upstream machine code, prefer `multiblock`; if legacy `muiltblock` paths still exist locally, inspect the actual registration and usage path before moving or editing files.

### Machine Structures

- New multiblock structure patterns live under `src/main/java/com/gtladd/gtladditions/common/machine/multiblock/structure/`.
- Subspace Corridor Hub specific structure slices live in the shared `common/machine/multiblock/structure/LargeStructure*.kt` files.
- Structure overrides or replacements for upstream GTLCore machines live under `common/modify/multiblockMachine/`.
- When one shared structure file grows too large, continue with the next sequential shared file such as `MultiBlockStructureF.kt` instead of creating a machine-specific standalone structure file.
- Do not put structure definitions in controller classes unless the surrounding machine registration already does so.

### Machine Controller Registration

- New GTLAdd multiblock definitions are registered in `src/main/java/com/gtladd/gtladditions/common/machine/multiblock/MultiBlockMachine.kt` with `REGISTRATE.multiblock(...)`.
- `MultiBlockMachine.kt` is the definition/registration hub: recipe types, recipe modifiers, appearance blocks, patterns, renderers, and controller constructors are wired there.
- `src/main/java/com/gtladd/gtladditions/common/machine/GTLAddMachines.kt` calls `MultiBlockMachine.init()` from `GTLAddMachines.init()`.
- Existing GTLCore/GTCEu machine modifications are registered in:
  - `common/modify/MultiBlockModify.kt` for direct pattern/supplier/working-behavior modifications.
  - `common/modify/MutableMultiBlockModify.kt` for thread modifier support, mutable suppliers, and extra tooltips.

### Hatches And Machine Parts

- Main hatch classes live under `src/main/java/com/gtladd/gtladditions/common/machine/hatch/`.
  - Current examples include `HugeSteamHatchPartMachine`, `SuperDualHatchPartMachine`, and `InfinityDualHatchPartMachine`.
- Multiblock part classes live under `common/machine/multiblock/part/`.
  - Current examples include ME super pattern buffer, wireless energy network terminals, super parallel hatch, and thread modifier hatch.
- Hatch and part registration lives in `src/main/java/com/gtladd/gtladditions/common/machine/GTLAddMachines.kt` with `REGISTRATE.machine(...)`.
- `GTLAddMachines.kt` also registers or delegates special GTCEu/GTMThings hatch families such as laser hatches, wireless laser hatches, and huge output dual hatches.
- New part abilities belong in `src/main/java/com/gtladd/gtladditions/api/machine/GTLAddPartAbility.kt`.

### Materials, Items, And Blocks

- Material elements: `src/main/java/com/gtladd/gtladditions/common/material/GTLAddElements.kt`.
- New materials and new fluid-bearing material definitions belong in `common/material/GTLAddMaterial.kt`.
- Changes to existing GT/GTCEu/GTLCore materials, including added flags and `PropertyKey` assignments such as fluid, wire, ingot, or pipe properties, belong in `common/material/MaterialAdd.kt`.
- Do not set properties on already-existing materials inside `GTLAddMaterial.kt`; that file is for defining GTLAdd-owned materials, not mutating upstream ones.
- Item registration: `src/main/java/com/gtladd/gtladditions/common/items/GTLAddItems.kt`.
- Item behavior classes: `common/items/behavior/`.
- Block registration: `src/main/java/com/gtladd/gtladditions/common/blocks/GTLAddBlocks.kt`.
- If an upstream material/block migration also needs explicit hand-registered blocks or items beyond the material system's generated outputs, wire those through `GTLAddBlocks.kt` and/or `GTLAddItems.kt` instead of hiding that registration inside material files.
- Generated block/item resources are under `src/generated/resources/`; hand-authored assets are under `src/main/resources/assets/`.

### Machine Renderers

- Machine renderer implementations live in `src/main/java/com/gtladd/gtladditions/client/render/machine/`.
- Shared render types live in `src/main/java/com/gtladd/gtladditions/client/GTLAddRenderTypes.kt`.
- Renderer hookup for multiblocks is usually in `MultiBlockMachine.kt` via `.renderer { ... }`, `.hasTESR(true)`, `.workableCasingRenderer(...)`, or related builder extensions.
- Renderer helper extensions/builders live under `api/registry/`, especially `MachineBuilderExtensions.kt` and `GTLAddMultiBlockMachineBuilder.kt`.

### Recipe Types And Recipe Generation

- Custom GT recipe type registration lives in `src/main/java/com/gtladd/gtladditions/common/recipe/GTLAddRecipesTypes.kt`.
  - Define new `GTRecipeType` constants here.
  - `GTLAddRecipesTypes.init()` currently calls `RecipesModify.init()`.
- Generated recipe providers live under `src/main/java/com/gtladd/gtladditions/data/recipes/`.
  - New-machine recipes are grouped under `data/recipes/newmachinerecipe/`.
  - Process chains live under `data/recipes/process/`.
  - Cross-cutting recipe groups such as AE2, assembler, assembly line, EBF, QFT, StarGate, part machines, and misc recipes live directly under `data/recipes/`.
- Recipe provider entry points must be added to `GTLAdditionsGTAddon.addRecipes(...)`; adding a file under `data/recipes/` alone is not enough.
- Runtime recipe-type modifications and recipe-copy hooks live in `src/main/java/com/gtladd/gtladditions/common/modify/RecipesModify.kt`.
- Static JSON recipes under `src/main/resources/data/gtladditions/recipes/` are hand-authored resources, not Kotlin data-generation entry points.

### Recipe Logic And Machine Logic

- Multiple-recipe and mutable recipe logic lives in `src/main/java/com/gtladd/gtladditions/api/machine/logic/`.
  - `GTLAddMultipleRecipesLogic.kt` is the core multiple-recipe logic.
  - `GTLAddMultipleWirelessRecipesLogic.kt` and `GTLAddMultipleTypeWirelessRecipesLogic.kt` handle wireless variants.
  - `MutableRecipesLogic.kt` and `AddMutableRecipesLogic.kt` handle mutable machine behavior.
  - `IWirelessRecipeLogic.kt` is the wireless recipe-logic interface.
- Machine capability/behavior interfaces live under `api/machine/`.
  - Examples: `IGTLAddMultiRecipeMachine`, `IThreadModifierMachine`, `IWirelessElectricMultiblockMachine`, `IWirelessThreadModifierParallelMachine`, `IAstralArrayInteractionMachine`.
- Recipe builder/model extensions live under `src/main/java/com/gtladd/gtladditions/api/recipe/`.
  - Examples: `WirelessGTRecipe`, `WirelessGTRecipeBuilder`, `IWirelessGTRecipe`, `ChanceParallelLogic`.
- Runtime helper traits and machine-side state live under:
  - `common/machine/trait/` for concrete traits/containers.
  - `api/machine/trait/` for trait interfaces.
  - `api/machine/feature/` for feature interfaces such as thread modifier parts.

### Events

- General event registration starts in `GTLAdditions.kt`.
- Dedicated Forge event subscribers live in `src/main/java/com/gtladd/gtladditions/events/`.
  - `ServerLifecycleHandler.kt` clears server-side machine tracking on level unload/server stop.
  - `PlayerDimensionChangeHandler.kt` handles dimension restrictions and machine sync on login/dimension change.
  - `MECapabilityHandler.kt` attaches AE2 storage capability to the infinity input dual hatch.
  - `TooltipHandler.kt` handles client-side tooltip changes.
- Mixin-side event changes live under `src/main/java/com/gtladd/gtladditions/mixin/` and are listed in `src/main/resources/gtladditions.mixin.json`.

### External Mod Integrations

- AE2 integration code lives in `src/main/java/com/gtladd/gtladditions/integration/ae2/`.
- Jade plugin registration lives in `integration/jade/GTLAddJadePlugin.kt`; providers live in `integration/jade/provider/`.
- JEI/EMI integration lives in `integration/xei/`.
  - `GTJEIPlugin.kt` is annotated with `@JeiPlugin`.
  - `GTEMIPlugin.kt` is annotated with `@EmiEntrypoint`.
  - `LongFluidStack.kt` supports XEI display data.
- External compatibility patches are mostly mixins grouped by target mod under `mixin/ae2/`, `mixin/gtceu/`, `mixin/gtlcore/`, `mixin/gtmtings/`, `mixin/stargatejourney/`, `mixin/ldlib/`, and `mixin/mc/`.
- Mod dependencies and load constraints are declared in `src/main/resources/META-INF/mods.toml`.

### Networking

- Network channel and packet registration live in `src/main/java/com/gtladd/gtladditions/network/GTLAddNetworking.kt`.
- Packet classes live in the same package.
  - `SyncDimensionMachinesPacket.kt` syncs full dimension machine data.
  - `MachineDeltaPacket.kt` syncs add/remove deltas.
- `GTLAddNetworking.init()` is called from `GTLAdditions.kt` during mod construction.

### Utilities

- General utility classes live in `src/main/java/com/gtladd/gtladditions/utils/`.
- Forge of the Antichrist / ring-block / dimension-machine sync helpers live under `utils/antichrist/`.
- Rendering helpers belong in `RenderUtils.kt` or client renderer classes, not in common machine registration unless the existing renderer API requires it.
- Recipe math and transfer helpers belong in `RecipeCalculationHelper.kt`, `TransferHelper.kt`, `TempChemicalHelper.kt`, or a narrowly named utility in `utils/`.
- Utilities are not automatically registered; if a helper requires lifecycle hooks, wire it from the relevant entry point: `GTLAdditions.kt`, `GTLAdditionsGTAddon.kt`, `GTLAddMachines.kt`, an event subscriber, or networking.

## File Organization Rules

- Do not infer new ownership boundaries from naming alone. Verify the existing registration/usage path before placing or moving code.
- If the requested behavior can reasonably belong to more than one file or lifecycle entry point, ask for clarification before changing code.
- Prefer existing package boundaries over creating new top-level packages.
- Use Kotlin for new machine definitions, data generation, registration objects, and helper logic unless adjacent code is Java.
- Use Java mainly for mixins when the surrounding mixin package is Java.
- Keep generated resources in `src/generated/resources/`; keep hand-authored textures, models, lang, sounds, and mixin config in `src/main/resources/`.
- When adding a new machine, update all required layers: controller class, structure, recipe type if needed, machine registration, recipes, localization/assets/renderers if needed.
- When adding a new recipe generator file, add its `init(provider)` call to `GTLAdditionsGTAddon.addRecipes(...)`.

## Localization

- GTLAdditions localization files live in `src/main/resources/assets/gtladditions/lang/en_us.json` and `zh_cn.json`.
- This repository also overrides or supplements assets/lang for other namespaces such as `gtceu`, `gtlcore`, `gtmtings`, `avaritia`, and `sgjourney`. Keep namespace ownership clear when editing localization.
