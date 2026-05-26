---
navigation:
  title: 宏原子谐振碎片剥离器
  icon: macro_atomic_resonant_fragment_stripper
  parent: controller/multiblock_controller.md
  position: 46
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:macro_atomic_resonant_fragment_stripper
---

# 宏原子谐振碎片剥离器

<BlockImage id="gtladditions:macro_atomic_resonant_fragment_stripper" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* <ItemLink id="gtladditions:macro_atomic_resonant_fragment_stripper" /> 围绕元素复制和星核剥离工作。
* 机器支持激光仓与跨配方并行。
* 运行时耗电倍率为 4。

</Column>

<Column gap="2" fullWidth={true}>

### 空岛模式差异

* 当 GTLCore 空岛模式开启时，<ItemLink id="gtladditions:macro_atomic_resonant_fragment_stripper" /> 同时处理星核剥离和元素复制。
* 当 GTLCore 空岛模式关闭时，机器只处理元素复制路线。
* 因此，不同整合包配置下，玩家在配方查看器中看到的可用配方类型会不同。

</Column>

<Column gap="2" fullWidth={true}>

### 星核剥离

* 星核剥离需要达到星辉金属线圈等级或更高温度。
* 该路线有 1536 基础并行，线圈温度继续提高后会增加并行。
* 嵌入 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 或 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 可以进一步提高星核剥离的并行规模，最多计入 256 个星阵。
* 星核剥离的并行计算公式为：

<Latex math = "B(T)=1536+300\times\lfloor\frac{\max(T-21600,0)}{1200}\rfloor" />

<Latex math = "P_{\text{星核剥离}}=\begin{cases}B(T)\quad(count=0)\\ \operatorname{round}(B(T)\times2^{6+10\times((count-1)/184)^2})\quad(1\le count\le256)\end{cases}" />

* 公式中的 T 是当前线圈温度，count 是已嵌入的 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} /> 数量。1 个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 会直接填满 256 个上限。

</Column>

<Column gap="2" fullWidth={true}>

### 配方差异

* 星核剥离是普通碎片采集的高阶路线，运行电压提升到 UHV。
* 与普通碎片采集相比，星核剥离通常会提高产物数量或掉落概率；概率流体产出会变为稳定产出。
* 各类 <ItemLink id="gtlcore:world_fragments_overworld" /> 等世界碎片本体和 <ItemLink id="gtlcore:miracle_crystal" /> 这类核心掉落不会被数量放大。
* 部分低阶钻头路线不会提供星核剥离版本；<ItemLink id="kubejs:machine_casing_grinding_head" /> 路线会以更适合高阶机器的输入消耗方式呈现。

</Column>

<Column gap="2" fullWidth={true}>

### 元素复制

* 元素复制需要 <ItemLink id="kubejs:eternity_coil_block" /> 等级，并要求嵌入 256 个 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />。1 个 <ItemLink id="gtladditions:compressed_astral_array" link={false} linkColor={true} /> 可满足该上限。
* 已嵌入的压缩星阵不会以压缩态回收；打掉主机时只会返还普通 <ItemLink id="gtladditions:astral_array" link={false} linkColor={true} />。
* 满足条件后，元素复制路线拥有无限并行。
* 元素复制的并行计算公式为：

<Latex math = "P_{\text{元素复制}}=\infty,\quad count=256" />

</Column>

</Column>