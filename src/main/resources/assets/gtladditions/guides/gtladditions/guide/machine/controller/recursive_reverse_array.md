---
navigation:
  title: Recursive Reverse Array
  icon: recursive_reverse_array
  parent: machine/controller/multiblock_controller.md
  position: 40
categories:
  - multiblock controller
item_ids:
  - gtladditions:recursive_reverse_array
  - gtladditions:suprachronal_data_module
  - gtladditions:catalytic_cascade_array
  - gtladditions:magnetorheological_convergence_core
  - gtladditions:spacetime_stasis_device
  - gtladditions:supratemporal_boosting_engine
---

# Recursive Reverse Array

<BlockImage id="recursive_reverse_array" scale="8"/>

Recursive Reverse Array is not an independent production machine. It binds to one formed <ItemLink id="gtladditions:forge_of_the_antichrist" /> and reads the state of its own formed modules to provide buffs to the bound host.

Use <ItemLink id="gtladditions:suprachronal_data_module" /> for binding:

* First right-click a formed <ItemLink id="gtladditions:forge_of_the_antichrist" /> to record the binding source.
* Then right-click a formed Recursive Reverse Array in the same dimension to establish the binding.
* A new successful binding overwrites any previous binding on both machines.
* In survival mode, the data module is consumed after successfully binding to the array. In creative mode, only the record stored in the module is cleared.
* Sneak-right-click clears the stored record.
* Cross-dimensional binding is rejected.

A recursive reversal buff package requires the bound <ItemLink id="gtladditions:forge_of_the_antichrist" /> to be formed, the array to be formed, and the connected <ItemLink id="gtladditions:supratemporal_boosting_engine" /> to be running and not overheated. It then performs one probability check for this buff package using the perfect supratemporal boost parameter. When the temperature is within 93000K-97000K, that parameter is 1.0 and the buff package always takes effect. Outside that range, the parameter is below 1.0 and the buff package takes effect by chance.

If the buff probability check fails, the output and EU buffs from Catalytic Cascade Array, the focus from Magnetorheological Convergence Core, Spacetime Stasis Device, and the Supratemporal Boosting Engine EU reduction do not take effect. The bound host's own running-time-based base output multiplier and EU reduction still run normally. The Astral Ritual entry is separate from this probability check: it strictly requires the boosting engine to be running, not overheated, and within 93000K-97000K.

## Modules

Each module must form in an array module position and connect to the array. A module interrupts its own operation while disconnected from the array.

<Column gap="20" fullWidth={true}>
<Column gap="2" fullWidth={true}>

### Catalytic Cascade Array

<BlockImage id="catalytic_cascade_array" scale="4"/>

* When the catalytic cycle succeeds, it provides a 2x output multiplier to the bound host.
* When the EU buff is active, it additionally provides a 0.15x EU multiplier.
* The output multiplier is applied after Magnetorheological Convergence Core completes focusing.
* The output item or fluid of a recipe loop container itself is not multiplied. The matching loop input increases with the multiplier.

The module cycle lasts 32 seconds:

* At second 0, it outputs a redstone signal from 1 to 15 through <ItemLink id="gtladditions:vientiane_transcription_node" /> and selects the catalyst required for this cycle.
* From second 7 until this cycle resets, it checks the LV huge fluid input hatch once per second.
* The correct catalyst is consumed at 40B/s and enables both the output buff and EU buff.
* If an incorrect fluid is input, that fluid is drained and the output buff is disabled for the rest of the cycle. The EU buff is only kept if catalyst input continues afterward.
* If no catalyst is present during a check, both the output buff and EU buff are disabled.

Signal mapping:

> 1-3: <FluidLink id="gtceu:dimensionallytranscendentcrudecatalyst" /> \
> 4-6: <FluidLink id="gtceu:dimensionallytranscendentprosaiccatalyst" /> \
> 7-9: <FluidLink id="gtceu:dimensionallytranscendentresplendentcatalyst" /> \
> 10-12: <FluidLink id="gtceu:dimensionallytranscendentexoticcatalyst" /> \
> 13-15: <FluidLink id="gtceu:dimensionallytranscendentstellarcatalyst" />

</Column>
<Column gap="2" fullWidth={true}>

### Magnetorheological Convergence Core

<BlockImage id="magnetorheological_convergence_core" scale="4"/>

* Focuses fluid outputs in the bound host that can participate in focusing.
* It currently only focuses fluid outputs. The first fluid output receives the total amount of all fluid outputs, then the output multipliers from the bound host and Catalytic Cascade Array are applied.
* Recipes with fewer than two fluid outputs are not focused.

The module has a 12-second fuel cycle. When placed, it randomly selects two item requirements and one fluid requirement:

> Items are selected from <ItemLink id="kubejs:black_body_naquadria_supersolid" />, <ItemLink id="kubejs:quantum_anomaly" />, and <ItemLink id="kubejs:hyper_stable_self_healing_adhesive" />, with each amount from 1 to 16384. \
> Fluid is selected from <FluidLink id="gtceu:exciteddtec" /> or <FluidLink id="gtceu:exciteddtsc" />, with an amount from 1 to 1638400 mB.

Activating focus requires:

* Precisely inputting the two selected item types through two ULV huge item input buses.
* Precisely inputting the selected fluid through one LV huge fluid input hatch.
* Continuously providing 2 magnetic matter blocks per second through item input.
* If amounts are too high, too low, or missing, the module GUI displays the failed item and focus does not enable.

</Column>
<Column gap="2" fullWidth={true}>

### Spacetime Stasis Device

<BlockImage id="spacetime_stasis_device" scale="4"/>

* Runs continuously at OpV voltage and consumes 70B/s <FluidLink id="gtceu:spacetime" />.
* When its own recipe is running and the common buff gate is active, the bound host does not lose running time while idle.
* It does not actively add running time and does not provide an output multiplier or EU buff.
* While the bound host is working, running time still increases normally.

</Column>
<Column gap="2" fullWidth={true}>

### Supratemporal Boosting Engine

<BlockImage id="supratemporal_boosting_engine" scale="4"/>

* Runs continuously at UXV voltage.
* It is the required module for the common buff gate.
* When a recursive reversal buff package takes effect, it provides an additional EU multiplier to the bound host.
* When <ItemLink id="gtladditions:vientiane_transcription_node" /> is installed in the structure, the node receives the redstone value matching the current temperature.

Temperature behavior:

> Initial temperature: 48000K \
> Optimal range: 93000K-97000K \
> Overheat threshold: above 105000K \
> Heating while working: +1300K/s when the engine itself is running and the array host is valid \
> Cooling while idle: -900K/s, not below 48000K \
> On load, if the engine is not overheated and its temperature is already in the optimal range, the current temperature is fixed for 5 seconds before normal heating and cooling resumes.

Temperature-control fluids are consumed at 100B/s:

> Heating: <FluidLink id="minecraft:lava" /> +2500K/s, <FluidLink id="gtceu:blaze" /> +4600K/s, <FluidLink id="gtceu:raw_star_matter_plasma" /> +14000K/s \
> Cooling: <FluidLink id="gtceu:ice" /> -1900K/s, <FluidLink id="gtceu:liquid_helium" /> -3400K/s, <FluidLink id="kubejs:gelid_cryotheum" /> -6700K/s

After overheating, the engine stops providing the common gate. Heating fluids are not used. Cooling fluids stack with the default -7125K/s overheat cooling until the temperature returns to 48000K, at which point overheat protection is cleared.

EU multiplier uses the following formulas:

> Within the optimal temperature range, the perfect supratemporal boost parameter is 1.0.

<Latex math = "P = 0.5 + 0.5 * (\frac{temperature - 48000}{45000})^{28}, \quad temperature \lt 93000K" />

<Latex math = "P = 1 - 0.85 * (\frac{temperature - 97000}{4000})^{0.42}, \quad temperature \gt 97000K" />

> The perfect supratemporal boost parameter is clamped to 0.0-1.0. The EU multiplier is:

<Latex math = "EU\ multiplier = \min(0.8, 0.05 + 0.7932 * e^{-0.8473 * P^{2.326}})" />

When temperature is outside the optimal range, the perfect supratemporal boost parameter is also the chance for recursive reversal buffs to take effect each time the bound host executes a recipe. If the chance check fails, that execution receives no recursive reversal module buffs.

</Column>
</Column>