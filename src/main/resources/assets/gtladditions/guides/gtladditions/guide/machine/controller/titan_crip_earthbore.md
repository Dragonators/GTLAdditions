---
navigation:
  title: Titan's Crip Earthbore
  icon: titan_crip_earthbore
  parent: controller/multiblock_controller.md
  position: 25
categories:
  - multiblock controller
item_ids:
  - gtladditions:titan_crip_earthbore
---

# Titan's Crip Earthbore

<BlockImage id = "gtladditions:titan_crip_earthbore" scale = "8"/>

<Column gap="15" fullWidth={true}>

<Column gap="2" fullWidth={true}>

* A more efficient bedrock miner that no longer needs automation tricks and can simply obtain large amounts of bedrock dust.
* The machine structure requires bedrock as part of the structure. The local implementation only checks bedrock in the structure and does not destroy it while working.
* The machine supports perfect overclocking.
* The structure can install <ItemLink id="gtladditions:thread_modifier_hatch" />.

</Column>

<Column gap="2" fullWidth={true}>

### Parallel Calculation

* Maximum parallel is determined by the machine's current voltage tier. <Color color="#FF55FF">**LuV**</Color> is the baseline tier, and each tier above it doubles the value.

<Latex math = "Maximum\ parallel = 2^{Voltage\ tier - 6}" />

</Column>

<Column gap="2" fullWidth={true}>

<Recipe id="gtladditions:tectonic_fault_generator/bedrock_dust" />

</Column>

</Column>