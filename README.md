# GTLAdditions

[简体中文](README.zh.md)

> [!TIP]
> GTLAdditions is an extension mod for the GregTech Leisure environment. It adds late-game multiblocks, machine parts, recipes, materials, renderers, GuideME documentation, and external-mod integrations around GTCEu and GTLCore.
>
> For exact recipes and structure previews, use JEI and the in-game GTLAdditions Guide.

## Introduction

GTLAdditions is developed for Minecraft Forge 1.20.1 and the GregTech Leisure ecosystem. While preserving the original hardcore technology framework, it expands GTCEu/GTLCore late-game progression with many innovative designs and balance optimizations, giving players a smoother automation experience and a more difficult final goal.

## Requirements

| Requirement | Version / Scope |
| --- | --- |
| GTLCore (`gtlcore`) | `>= 1.2.3.0` |
| Stargate Journey (`sgjourney`) | `>= 0.6.44` |
| Oculus (`oculus`) | `>= 1.7.0`, client side |

## Install

1. Download `GregTech Leisure` [here](https://pan.quark.cn/s/d13f899cdab5#/list/share) or [here](https://drive.google.com/drive/folders/1Ga_w-TmDKNru0me1kAM_gXyedz_Ne4-x), and install it with your Minecraft launcher.
2. Delete the bundled `GTLCore`, then install the newest GTLCore from [here](https://github.com/AaAdoniSsS/GTLCore).
3. Add GTLAdditions to the `mods` folder.
4. Use JEI and the in-game GTLAdditions Guide for recipes, structures, and feature details.

## Features

### Multiblock Machines

GTLAdditions registers the following multiblock controllers in this checkout.

| Machine | Recipe / System | Usage |
| --- | --- | --- |
| Nexus Satellite Factory MK-I | Lathe, Bender, Compressor, Forge Hammer, Cutter, Extruder, Mixer, Wiremill, Forming Press, Polarizer | Light Hunter module for broad processing recipes |
| Nexus Satellite Factory MK-II | Rock Breaker, Ore Washer, Centrifuge, Electrolyzer, Sifter, Macerator, Dehydrator, Thermal Centrifuge, Electromagnetic Separator | Light Hunter module for ore and material processing |
| Nexus Satellite Factory MK-III | Evaporation, Autoclave, Extractor, Brewing, Fermenting, Distillery, Distillation, Fluid Heater, Fluid Solidification, Chemical Bath | Light Hunter module for fluid and chemical support recipes |
| Nexus Satellite Factory MK-IV | Canner, Arc Furnace, Lightning Processor, Assembler, Precision Assembler, Circuit Assembler | Light Hunter module for assembly and high-value processing |
| Lucid Etchdreamer | Photon Matrix Etch | Advanced etching route for engraving-style production |
| Atomic Transmutation Core | EM Resonance Conversion Field | Legacy/discontinued controller kept for compatibility |
| Subatomic Transmutatioon Core | Transmutation Block Conversion | Block transmutation controller using the Transmutation Bus Hatch |
| Astral Convergence Nexus | Space Assembler Module | Space elevator assembler module replacement |
| Nebula Reaper | Space Miner Module, Space Drilling Module | Space elevator mining and drilling module replacement |
| Arcanic Astrograph | Cosmos Simulation, Compressed Astral Array | Eye of Harmony style endgame simulation and astral-array compression |
| Arcane Cache Vault | Packer | Multi-recipe packer-style processing |
| Space Scaling Instrument | Packer | The final form of packers |
| Draconic Collapse Core | Aggregation Device | Higher-parallel aggregation processing with advanced input support |
| Titan's Crip Earthbore | Tectonic Fault Generator | High-parallel advanced drill that does not require bedrock maintenance |
| Biological Simulation Laboratory | Biological Simulation | Entity and world-data based biological resource simulation |
| Advanced Chemical Plant | Large Chemical Reactor | Multi-recipe advanced chemical processing |
| Quantum Syphon Matrix | Voidflux Reaction | Voidflux and air-series production |
| Fuxi Bagua Heaven-forging Furnace | Stellar Lgnition, Chaotic Alchemy, Molecular Deconstruction, Ultimate Material Forge | Stellar Lgnition directly converts some fluids into plasma; Chaotic Alchemy combines alloy smelting and vacuum freezing into one-step reactions; Molecular Deconstruction directly converts powders that could not originally be extracted into fluids |
| Antientropy Condensation Center | Antientropy Condensation | Cooling-tower style antientropy processing without the old helium requirement |
| Taixu Turbid Array | Chaos Weave | Scrap Box, UU Amplifier, and UU Matter |
| Planetary Ionisation Convergence Tower | Generator / laser output system | UHV-and-above power generation alternative |
| Inferno Cleft Smelting Vault | Pyrolyse Oven, Cracker | Multi-recipe pyrolysis and cracking replacement |
| Skeleton Shift Rift Engine | Decay Hastener | High-tier decay-hastening machine |
| Time Space Distorter | Time Space Distortion | Boost module for the Apocalyptic Torsion Quantum Matrix |
| Apocalyptic Torsion Quantum Matrix | QFT, Distort, Neutron Compressor | Endgame replacement for QFT and Chemical Distorter processing |
| Forge of the Antichrist | Dimensionally Transcendent Plasma Forge, Stellar Forge, Ultimate Material Forge | Modular endgame forge with Helio module support |
| Recursive Reverse Array | Recursive Reverse Array system | Binds modules and Forge of the Antichrist state for endgame buffs |
| Heart of the Universe | Genesis Engine | Endgame generation machine with wireless energy output support |
| Dimension Focus Infinity Crafting Array | Nightmare Crafting | Infinity Catalyst crafting automation and Molecular Assembler boost module |
| Light Hunter Space Station | Interstellar | Main controller for factory modules |
| Space Infinity Integrated Ore Processor | Space Ore Processor | Endgame version of integrated ore processing |
| Macro Atomic Resonant Fragment Stripper | Element Copying, optional Star Core Stripper | Endgame fragment stripping and element-copying machine |

### Recursive Reverse Array Modules

These are registered as multiblock modules for the Recursive Reverse Array system.

| Module | Recipe / System | Usage |
| --- | --- | --- |
| Catalytic Cascade Array | Recursive Reverse Array module | Multiplies massive outputs by decoding redstone signals |
| Magnetorheological Convergence Core | Recursive Reverse Array module | Provides fluid focus behavior for Recursive Reverse Array buffs |
| Spacetime Stasis Device | Spacetime Stasis | Provides an anchored state for Forge of the Antichrist |
| Supratemporal Boosting Engine | Supratemporal Boosting | Gate device for all modules |

### Forge of the Antichrist Modules

These are registered Helio modules for Forge of the Antichrist.

| Module | Recipe / System | Usage |
| --- | --- | --- |
| Heliofusion Exoticizer | Matter Exotic | Simplifies multiple loops and production-line recipes |
| Helioflare Power Forge | Furnace, Blast, Alloy Smelter, Alloy Blast | Smelting and alloying module |
| Heliofluix Melting Core | Chaotic Alchemy, Molecular Deconstruction | Alchemy and decomposition module |
| Heliothermal Plasma Fabricator | Stellar Lgnition, Fusion, Super Particle Collider | Plasma, fusion, and particle-collider module |
| Heliophase Leyline Crystallizer | Leyline Crystallize | Optimizes leyline aggregation and command-block automation |

### Multiblock Machine Parts

| Part | Usage |
| --- | --- |
| Laser Hatches | Laser hatch family with higher power, used by advanced machines |
| Huge Output Dual Hatch | Output version of the Huge Input Dual Hatch |
| Huge Steam Input Hatch | Raises steam multiblock recipe voltage support to HV and fixes duration to 1 tick |
| Super Input Dual Hatch | Higher-capacity Huge Input Dual Hatch |
| Infinity Input Dual Hatch | Ultimate input bus with infinite capacity, optimized for high-performance AE2-related parts |
| ME Super Pattern Buffer | Custom-capacity ME pattern buffer with Forge pattern mode support |
| ME Super Pattern Buffer Proxy | Proxy part for ME Super Pattern Buffer setups |
| Transmutation Bus Hatch | Block transmutation bus for transmutation controllers |
| Spectral Analysis Hatch | Boost module usable by integrated ore processing |
| Vientiane Transcription Node | Redstone signal output part for supported module machines |
| Super Parallel Hatch | Endgame parallel control hatch |
| Wireless Energy Network Input Terminal | Unlimited energy hatch / laser target hatch directly connected to the wireless energy network |
| Wireless Energy Network Output Terminal | Unlimited dynamo hatch / laser source hatch directly connected to the wireless energy network |
| Thread Modifier Hatch | Thread boost module, and parallel boost module for some machines |

### Other Modified Content

- `Wireless Optical Data Hatch` and `Wireless Optical Computation Hatch` can be shared by multiblock structures.
- The `Configurable Automatic Maintenance Hatch` series can be shared by multiblock structures, and items can be placed inside to enhance hatch effects.

### Recipe

- Many simplified recipes have been added to help optimize large-scale late-game production lines. Please learn to use **`JEI`** to find recipes. If a recipe is added by `GTLAdditions`, JEI will show an annotation for it. Check JEI often, and you may find new GTLAdditions recipes.
- The newly added SoC production line provides efficient recipes for late-game circuit board production.
- The modification pages in the in-game GTLAdditions Guide also introduce some unique late-game process recipes.

For further information, please consult the in-game GTLAdditions Guide.


## Build / Development

```bash
# Build the mod
./gradlew build

# Run in a development client
./gradlew runClient

# Run in a development server
./gradlew runServer

# Generate data
./gradlew runData

# Check formatting
./gradlew spotlessCheck

# Apply formatting
./gradlew spotlessApply
```

Use `gradle` instead of `./gradlew` only if the Gradle wrapper is unavailable in your checkout.
