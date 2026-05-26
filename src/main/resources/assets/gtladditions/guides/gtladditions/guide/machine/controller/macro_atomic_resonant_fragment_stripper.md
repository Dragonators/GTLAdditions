---
navigation:
  title: Macro Atomic Resonant Fragment Stripper
  icon: macro_atomic_resonant_fragment_stripper
  parent: controller/multiblock_controller.md
  position: 46
categories:
  - multiblock controller
item_ids:
  - gtladditions:macro_atomic_resonant_fragment_stripper
---

# Macro Atomic Resonant Fragment Stripper

<BlockImage id="gtladditions:macro_atomic_resonant_fragment_stripper" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:macro_atomic_resonant_fragment_stripper" /> works around Element Copying and Star Core Stripping.
* The machine supports laser hatches and cross-recipe parallelization.
* Runtime EU multiplier is 4.

</Column>

<Column gap="2" fullWidth={true}>

### Skyblock Mode Difference

* When GTLCore skyblock mode is enabled, <ItemLink id="gtladditions:macro_atomic_resonant_fragment_stripper" /> processes both Star Core Stripping and Element Copying.
* When GTLCore skyblock mode is disabled, the machine only processes the Element Copying route.
* Therefore, under different modpack configurations, players will see different available recipe types in recipe viewers.

</Column>

<Column gap="2" fullWidth={true}>

### Star Core Stripping

* Star Core Stripping requires Starmetal coil tier or higher temperature.
* This route has 1536 base parallel, and higher coil temperature increases parallel further.
* Embedding <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> or <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> further increases Star Core Stripping parallel scale, up to 256 Astral Arrays.
* Star Core Stripping parallel is calculated as:

<Latex math = "B(T)=1536+300\times\lfloor\frac{\max(T-21600,0)}{1200}\rfloor" />

<Latex math = "P_{\text{Star Core Stripping}}=\begin{cases}B(T)\quad(count=0)\\ \operatorname{round}(B(T)\times2^{6+10\times((count-1)/184)^2})\quad(1\le count\le256)\end{cases}" />

* In the formula, T is the current coil temperature, and count is the number of embedded <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />. One <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> fills this 256-array cap.

</Column>

<Column gap="2" fullWidth={true}>

### Recipe Differences

* Star Core Stripping is the higher-tier route for normal fragment collection, with the operating voltage raised to UHV.
* Compared with normal fragment collection, Star Core Stripping usually increases product amounts or drop chances. Chance fluid outputs become stable outputs.
* World fragment items such as <ItemLink id="gtlcore:world_fragments_overworld" /> and core drops such as <ItemLink id="gtlcore:miracle_crystal" /> are not multiplied in amount.
* Some low-tier drill routes do not provide a Star Core Stripping version. The <ItemLink id="kubejs:machine_casing_grinding_head" /> route appears with an input consumption pattern better suited to high-tier machines.

</Column>

<Column gap="2" fullWidth={true}>

### Element Copying

* Element Copying requires <ItemLink id="kubejs:eternity_coil_block" /> tier and requires 256 embedded <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />. One <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> satisfies this cap.
* Embedded compressed arrays are not recovered in compressed form; breaking the host returns only ordinary <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> stacks.
* After those conditions are met, the Element Copying route has infinite parallel.
* Element Copying parallel is calculated as:

<Latex math = "P_{\text{Element Copying}}=\infty,\quad count=256" />

</Column>

</Column>