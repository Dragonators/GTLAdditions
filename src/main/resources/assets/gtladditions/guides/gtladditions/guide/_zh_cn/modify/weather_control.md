---
navigation:
  title: 天气控制器的相关修改
  parent: modify/modify_index.md
  position: 7
item_ids:
  - gtceu:weather_control
  - gtceu:ev_lightning_rod
  - gtceu:iv_lightning_rod
  - gtceu:luv_lightning_rod
---

# 天气控制器的相关修改

<BlockImage id = "gtceu:weather_control" scale = "4" />

* 在主机放入力场发生器并使用4号电路的同时输入气态氡后可在9x9x9的范围内持续召唤闪电击中<ItemLink id="minecraft:lightning_rod" />
> | 力场发生器                                  | 闪电数量 | 召唤间隔  | 气态氡消耗量 |
> |----------------------------------------|------|-------|--------|
> | <Color color="#AA00AA">**EV**</Color>  | 4    | 1s    | 1B/t   |
> | <Color color="#5555FF">**IV**</Color>  | 8    | 0.75s | 2B/t   |
> | <Color color="#FF55FF">**LuV**</Color> | 16   | 0.5s  | 4B/t   |
* 结构内原有的避雷针被替换为<ItemLink id="minecraft:iron_bars" />
* 控制器为单槽，仅接受 EV、IV、LuV 力场发生器；结构可增加一个流体输入仓。
* 电路 1–3 保持原版。电路 4 的纯雷雨按 30 EU/t、200 tick 周期计费，完成周期后切换或续期雷雨，无需力场或氡。
* 有效召雷期间总耗电为 1920 EU/t（含天气部分），间隔期间也持续耗氡。缺力场、有效避雷针、氡或召雷电力时，退回纯天气且不耗召雷氡；基础供电不足或停机时停止新操作。
