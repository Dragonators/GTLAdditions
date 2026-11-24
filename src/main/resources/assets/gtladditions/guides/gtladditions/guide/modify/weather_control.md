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

Circuits 1–3 retain their original weather controls. Use **Circuit 3** if you only want to set or maintain a thunderstorm.

**Circuit 4 is dedicated to summoning lightning.**
- Before starting, insert an EV, IV, or LuV field generator into the controller's single slot.
- Place at least one <ItemLink id="minecraft:lightning_rod" /> outside the structure. Rods must be within the 9×9×9 area centered one block behind the controller.
- The original lightning rods inside the structure have been replaced with <ItemLink id="minecraft:iron_bars" />. A fluid input hatch can be installed to supply gaseous radon.

| Field generator | Maximum rods struck at once | Strike interval | Gaseous radon used |
| --- | ---: | --- | ---: |
| <Color color="#AA00AA">**EV**</Color> | 4 | 20 ticks (1 s) | 400 mB/t |
| <Color color="#5555FF">**IV**</Color> | 8 | 15 ticks (0.75 s) | 1,200 mB/t |
| <Color color="#FF55FF">**LuV**</Color> | 16 | 10 ticks (0.5 s) | 2,400 mB/t |

Completing a cycle normally sets the weather to a 200-tick (10 s) thunderstorm.
