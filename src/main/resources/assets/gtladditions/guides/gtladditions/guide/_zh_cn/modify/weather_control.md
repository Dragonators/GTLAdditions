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

电路 1–3 沿用原版天气控制。若只需切换或维持雷雨天气，请使用 **3 号电路**。

**4 号电路专用于召雷**。
- 开工前，在控制器的单槽内放入 EV、IV 或 LuV 力场发生器。
- 结构外放置至少一根 <ItemLink id="minecraft:lightning_rod" />。避雷针须位于以控制器正后方一格为中心的 9×9×9 范围内。
- 结构内原有的避雷针已替换为 <ItemLink id="minecraft:iron_bars" />。可安装一个流体输入仓供给气态氡。

| 力场发生器 | 每次最多击中的避雷针 | 召雷间隔 | 气态氡消耗量 |
| --- | ---: | --- | ---: |
| <Color color="#AA00AA">**EV**</Color> | 4 根 | 20 tick（1 秒） | 400 mB/t |
| <Color color="#5555FF">**IV**</Color> | 8 根 | 15 tick（0.75 秒） | 1,200 mB/t |
| <Color color="#FF55FF">**LuV**</Color> | 16 根 | 10 tick（0.5 秒） | 2,400 mB/t |

正常运行一轮后，会将天气设为持续 200 tick（10 秒）的雷雨。
