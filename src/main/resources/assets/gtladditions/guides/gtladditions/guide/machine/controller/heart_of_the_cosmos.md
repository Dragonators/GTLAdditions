---
navigation:
  title: Heart of the Universe
  icon: heart_of_the_cosmos
  parent: machine/controller/multiblock_controller.md
  position: 44
categories:
  - multiblock controller
item_ids:
  - gtladditions:heart_of_the_cosmos
  - gtladditions:strange_annihilation_fuel_rod
  - gtladditions:eternity_dark_matter_fuel_rod
---

# Heart of the Universe

<BlockImage id="gtladditions:heart_of_the_cosmos" scale="8"/>

<Column gap="20" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* The final generator. It burns fuel rods to annihilate planes into energy in the wireless energy network.
* The structure requires <ItemLink id="gtladditions:wireless_energy_network_output_terminal" />.
* Each plane is annihilated within 1 second and outputs power far beyond a <ItemLink id="gtmthings:creative_energy_hatch" /> into the wireless energy network; how much depends on the structure tier.

</Column>

<Column gap="2" fullWidth={true}>

### Structure Tiers

The Heart of the Universe has 3 structure tiers.

| Tier | Available recipe | Power per plane |
| --- | --- | --- |
| 1 | <ItemLink id="gtladditions:eternity_dark_matter_fuel_rod" /> + <ItemLink id="gtladditions:black_hole_seed" link={false} /> (consumed 5% of the time); 1 annihilation constrainer back 90% of the time | 64 creative energy hatches |
| 2 | <ItemLink id="gtladditions:strange_annihilation_fuel_rod" /> + <ItemLink id="gtladditions:black_hole_seed" link={false} />; 64 annihilation constrainers back 60% of the time | 4096 creative energy hatches |
| 3 | As tier 2; black hole seeds consumed at most 80% of the time | up to 262,144 creative energy hatches |

</Column>

<Column gap="2" fullWidth={true}>

### Upgrading

1. Open the "Structure Upgrade" sub-tab in the controller GUI sidebar and switch to view the target tier and required materials.
2. With the required items in your inventory, click "Unlock (From Inventory)" to deduct them directly from your inventory (partial payments across multiple clicks are supported); once all items are paid and the upgrade conditions are met, the next tier is unlocked.
   * Unlocking tier 2 needs 65,536 planes annihilated in total, tier 3 needs 262,144.
3. Once the next tier is unlocked, its structure can be previewed in the world as usual or built with the terminal. After building it, click Activate to make it take effect.
4. The annihilated plane count and the unlock progress are kept when the controller is dismantled.

</Column>

<Column gap="2" fullWidth={true}>

### Auxiliary Fluids

From tier 2, the strange annihilation fuel rod recipe can draw extra auxiliary fluids from the input hatches to stabilize the constraint or amplify the annihilation. Each plane always uses 100 mB.

| Kind | Fluid | Effect | Needs |
| --- | --- | --- | --- |
| Constraint Stabilizer | <FluidLink id="gtceu:exciteddtsc" /> | constrainer recovery 95% | structure tier 2 |
| Constraint Stabilizer | <FluidLink id="gtceu:exciteddtec" /> | constrainer recovery 85% | structure tier 2 |
| Constraint Stabilizer | <FluidLink id="gtceu:dimensionallytranscendentresidue" /> | constrainer recovery 70% | structure tier 2 |
| Annihilation Amplifier | <FluidLink id="gtceu:miracle" /> | plane power ×8 | structure tier 3 |
| Annihilation Amplifier | <FluidLink id="gtladditions:phonon_medium" /> | plane power ×4 | structure tier 3 |
| Annihilation Amplifier | <FluidLink id="gtladditions:proto_matter" /> | plane power ×2 | structure tier 3 |

* The stronger fluid takes effect first.
* Constraint stabilizing and annihilation amplifying can work at the same time.

</Column>

<Column gap="2" fullWidth={true}>

### Plane Count Bonus

At tier 3, the more planes have been annihilated, the more the same fuel produces and the lower the chance that a black hole seed is consumed.

| Planes annihilated | Fuel efficiency | Black hole seed consumed |
| --- | --- | --- |
| below 1,048,576 (2^20) | ×1 | 80% of the time |
| 1,048,576 (2^20) | ×2 | 40% |
| 2,097,152 (2^21) | ×4 | 20% |
| 4,194,304 (2^22) | ×8 | 10% |

* Fuel efficiency multiplies with the annihilation amplifier, up to 64 times the original.

</Column>

<Column gap="2" fullWidth={true}>

### Plane Unfolding

* <ItemLink id="gtladditions:heart_of_the_cosmos" /> processes one plane at a time by default.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" /> to unfold more planes at the same time.

</Column>

</Column>
