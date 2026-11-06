---
navigation:
  title: Weather Control Modify
  parent: modify/modify_index.md
  position: 7
item_ids:
  - gtceu:weather_control
  - gtceu:ev_lightning_rod
  - gtceu:iv_lightning_rod
  - gtceu:luv_lightning_rod
---

# Weather Control Modify

<BlockImage id = "gtceu:weather_control" scale = "4" />

* If you put a field generator in the host and use Circuit 4 whilst introducing gaseous radon, you can continuously summon lightning strikes <ItemLink id="minecraft:lightning_rod" /> within a 9x9x9 area
> | Field Generator                        | Number of lightning strikes | Summoning interval | Gaseous radon consumption |
> |----------------------------------------|-----------------------------|--------------------|---------------------------|
> | <Color color="#AA00AA">**EV**</Color>  | 4                           | 1s                 | 1B/t                      |
> | <Color color="#5555FF">**IV**</Color>  | 8                           | 0.75s              | 2B/t                      |
> | <Color color="#FF55FF">**LuV**</Color> | 16                          | 0.5s               | 4B/t                      |
* The original lightning rod inside the structure has been replaced with <ItemLink id="minecraft:iron_bars" />
* The controller has one slot accepting EV, IV or LuV field generators; the structure accepts one optional fluid input hatch.
* Circuits 1–3 retain their original behavior. Circuit 4 charges 30 EU/t for a 200-tick weather cycle and starts or renews thunder when that cycle completes, without requiring a field generator or radon.
* Active lightning costs 1920 EU/t in total, including weather, and consumes radon continuously between strikes. Missing a field generator, valid rods, radon or lightning power falls back to weather alone without consuming lightning radon. Insufficient basic power or stopping prevents new operations.
