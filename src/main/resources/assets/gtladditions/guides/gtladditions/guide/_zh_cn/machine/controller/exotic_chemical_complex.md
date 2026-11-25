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

* 异星化工复合体处理大型化学反应釜配方，三级结构解锁深度化学扭曲配方。
* 机器直接使用无线电网供能。
* 结构需要维护仓，并允许安装 <ItemLink id="gtladditions:thread_modifier_hatch" />，位于主机的左右两侧。
* 输入与输出仓可替换对应区域的 <ItemLink id="gtladditions:nanochip_casing" link={false} />。
* 由化合中枢与 12 座外围工艺单元组成，各区域的物品、流体输入输出彼此独立。

</Column>

<Column gap="2" fullWidth={true}>

### 结构等级

异星化工复合体拥有 3 个结构等级。

| 等级 | 工艺单元 | 并行规则 | 配方倍率 |
| --- | --- | --- | --- |
| 1 | 仅化合中枢运行 | 常规并行 | 耗电 1.0，耗时 1.0 |
| 2 | 解锁 12 座外围工艺单元 | 各有独立并行，默认仅 <Color color="#00AAAA">**UV**</Color> 及以上配方计入 | 耗电 0.8，耗时 0.6 |
| 3 | 解锁深度化学扭曲配方 | 解锁无限并行，外围单元可手动限流 | 耗电 0.8，耗时 0.6 |

</Column>

<Column gap="2" fullWidth={true}>

### 升级

1. 打开主机 GUI 侧栏的“结构升级”页，切换查看目标等级与所需素材。
2. 背包中携带对应物品后点击“解锁（从背包扣取）”，即可消耗物品并解锁下一等级。
3. 解锁下一等级后，便可在世界中正常预览结构或使用终端搭建。搭建完成后点击激活按钮生效。
4. 不同等级结构的线圈也需要保持一致。
5. 已解锁的等级与交付进度在拆卸主机后仍然保留。

</Column>

<Column gap="2" fullWidth={true}>

### 并行分配

* 基础线圈并行由结构内的线圈温度决定：

<Latex math = "线圈并行 = \min(2147483647, \lfloor 2^{\frac{线圈温度}{900}} \rfloor)" />

* 1 级与 2 级下，每个单元的满额并行由基础 256 线程、<ItemLink id="gtladditions:thread_modifier_hatch" /> 提供的额外线程与线圈并行共同决定。

<Latex math = "单元满额并行 = (256 + 额外线程) \times 线圈并行" />

* 1 级时，化合中枢在全部配方间按均分模式分配并行。
* 2 级时，化合中枢和每座外围单元各有一份满额并行。默认仅 <Color color="#00AAAA">**UV**</Color> 及以上配方消耗并行数。
  * 开启“全部配方计入并行”后，该单元内所有等级配方都受该并行数限制。
* 3 级时，所有单元都拥有无限并行。
* 并行数可在输入框或两侧按钮中调整并自动生效。

</Column>

<Column gap="2" fullWidth={true}>

### 工艺单元与运行控制

* 化合中枢与 12 座外围工艺单元间的物品、流体仓位彼此独立。
* 在蓝图中选中已解锁单元，可以查看进度与“产物”，也可以单独使用“暂停/继续”或“完成后暂停”。

</Column>

<Column gap="2" fullWidth={true}>

### 配方类型与时空扭曲

* 一级与二级限定使用大型化学反应釜配方。三级增加深度化学扭曲配方的选项。
* 安装在单元仓口上的 <ItemLink id="gtceu:me_recipe_pattern_buffer" /> 按该单元的有效配方类型生成统合样板，模式或结构等级变化后会自动刷新。
* 用 <ItemLink id="gtladditions:suprachronal_data_module" link={false} /> 绑定 <ItemLink id="gtladditions:time_space_distorter" />。三级化扭单元的物品与流体产出可获得 1.3–3.2 倍增益。
* 每轮按实际启动的并行数结算，用量固定为扭曲者基础材料用量的 0.1 倍。

</Column>

</Column>
