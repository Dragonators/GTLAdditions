---
navigation:
  title: 异星化工复合体
  icon: exotic_chemical_complex
  parent: machine/controller/multiblock_controller.md
  position: 28
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:exotic_chemical_complex
---

# 异星化工复合体

<BlockImage id="gtladditions:exotic_chemical_complex" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* 异星化工复合体处理大型化学反应釜配方，支持线圈并行、跨配方并行与分级扩展结构。
* 机器直接使用无线电网供能。
* 结构需要维护仓，并允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />，位于主机的左右两侧。
* 输入与输出仓可替换对应区域的 <ItemLink id="gtladditions:nanochip_casing" link={false} />。
* 由中央塔与 12 座外围工艺单元组成，各区域的物品、流体输入输出彼此独立。

</Column>

<Column gap="2" fullWidth={true}>

### 结构等级

异星化工复合体拥有 3 个结构等级。

| 等级 | 工艺单元 | 并行规则 | 配方倍率 |
| --- | --- | --- | --- |
| 1 | 仅中央塔运行 | 全部配方共用总并行池 | 耗电 1.0，耗时 1.0 |
| 2 | 解锁 12 座外围工艺单元 | 默认仅 <Color color="#00AAAA">**UV**</Color> 及以上配方占用总并行池 | 耗电 0.8，耗时 0.6 |
| 3 | 同 2 级 | 解锁无限并行 | 耗电 0.8，耗时 0.6 |

</Column>

<Column gap="2" fullWidth={true}>

### 升级

1. 打开主机 GUI 左侧的"结构升级"页，选择要看的等级。
2. 左侧会列出所需素材，达到升级所需条件后，将对应物品放入右侧的输入并点击解锁，即可消耗物品并解锁下一等级。
3. 解锁下一等级后，便可在世界中正常预览结构或使用终端搭建。搭建完成后使用激活按钮生效。
4. 不同等级结构的线圈也需要保持一致。
5. 已解锁的等级在拆卸主机后仍然保留。

</Column>

<Column gap="2" fullWidth={true}>

### 并行分配

* 基础线圈并行由结构内的线圈温度决定：

<Latex math = "线圈并行 = \min(2147483647, \lfloor 2^{\frac{线圈温度}{900}} \rfloor)" />

* 1 级与 2 级下，全机的总并行池由基础 256 线程、<ItemLink id="gtladditions:thread_modifier_hatch" /> 提供的额外线程与线圈并行共同决定：

<Latex math = "总并行池 = (256 + 额外线程) \times 线圈并行" />

* 1 级时，总并行池全部由中央塔使用，配方按均分模式分配并行。
* 2 级时，总并行池在中央塔与已开启“独立运行”的外围单元之间分配：
  * 默认仅 <Color color="#00AAAA">**UV**</Color> 及以上配方消耗并行数，开启“全部配方计入并行”后，该单元内所有配方合计受该并行数限制。
  * 中央塔默认使用独立单元吃剩下的并行数。
* 3 级时，中央塔与独立单元都拥有无限并行；外围单元开启“全部配方计入并行”后，改为受其手动设定的并行上限限制。
* 并行数可在输入框或两侧按钮中调整并自动生效。

</Column>

<Column gap="2" fullWidth={true}>

### 工艺单元与运行控制

* **区域隔离**：中央塔与 12 座外围工艺单元的物品、流体仓位彼此独立，中央塔只会读写中央平台上的仓位。
* **独立与归回**：2 级及以上可在主机主界面蓝图中选中外围单元（01–12）并切换“独立运行”。关闭“独立运行”时，空闲单元会立即归回并释放并行数，正在加工的单元会在本轮完成并输出产物后归回。
* **状态与产物**：选中中央或任意独立单元后，可查看当前进度条与“产物”列表。

</Column>

</Column>
