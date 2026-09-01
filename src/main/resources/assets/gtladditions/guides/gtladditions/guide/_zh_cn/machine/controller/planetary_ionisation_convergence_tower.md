---
navigation:
  title: 行星电离汇流塔
  icon: stone
  parent: controller/multiblock_controller.md
  position: 10
categories:
  - 多方块结构主机
item_ids:
  - gtladditions:planetary_ionisation_convergence_tower
---

# 行星电离汇流塔

<BlockImage id = "gtladditions:planetary_ionisation_convergence_tower" scale = "8"/>

* 只能使用泰坦钢及以上等级的线圈
* 每 3 秒为一次工作周期
* 如果内部能量缓存已满后仍然有能量进入，将会以机器为中心产生一次极大规模的爆炸
* 在工作周期开始时会产生一次瞬时极高功率 EU 脉冲（1 tick）至内部能量缓存中，然后在剩余时间内以较低功率平滑放电至内部能量缓存中
* 在脉冲结束后，内部能量缓存会通过动力仓或激光源仓向外界输出电量
* 恒星热力容器等级影响内部能量缓存量
> 基础：890,000,000,000,000 EU \
> 高级：132,000,000,000,000,000 EU \
> 终极：1,570,000,000,000,000,000 EU
* 线圈等级影响消耗的流体种类、单周期消耗量以及发电量
> 泰坦钢至精金：<FluidLink id="gtceu:rhenium" /> 73,728 mB，<FluidLink id="gtceu:ice" /> 8 KB，<ItemLink id="kubejs:space_drone_mk2" /> 2×10⁻⁴ 个 \
> 超能硅岩-塔兰至星辉：<FluidLink id="gtceu:promethium" /> 36,864 mB，<FluidLink id="gtceu:liquid_helium" /> 4 KB，<ItemLink id="kubejs:space_drone_mk4" /> 1×10⁻⁴ 个 \
> 无尽至永恒：<FluidLink id="gtceu:crystalmatrix" /> 9,216 mB，<FluidLink id="kubejs:gelid_cryotheum" /> 1 KB，<ItemLink id="kubejs:space_drone_mk6" /> 2.5×10⁻⁵ 个 \
>
> | 线圈种类 | 瞬时（A MAX） | 放电（A MAX） |
> |---|---:|---:|
> | 泰坦钢 | 4,096 | 16 |
> | 精金 | 32,768 | 128 |
> | 超能硅岩-塔兰 | 524,288 | 256 |
> | 星辉 | 4,194,304 | 2,048 |
> | 无尽 | 8,388,608 | 4,096 |
> | 海珀珍 | 67,108,864 | 32,768 |
> | 永恒 | 268,435,456 | 131,072 |
