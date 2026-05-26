---
navigation:
  title: Door of Creation and Creative Aggregator
  parent: modify/modify_index.md
  position: 2
categories:
  - modify
item_ids:
  - gtceu:door_of_create
  - gtceu:create_aggregation
  - ae2:annihilation_plane
  - ae2:formation_plane
  - minecraft:chain_command_block
  - minecraft:repeating_command_block
---

# Door of Creation and Creative Aggregator

<Column gap="15" fullWidth={true}>

<Row>
    <BlockImage id="gtceu:door_of_create" scale="4" />

    <BlockImage id="gtceu:create_aggregation" scale="4" />
</Row>

<Column gap="2" fullWidth={true}>

* Door of Creation and Creative Aggregator now support <ItemLink id="gtladditions:thread_modifier_hatch" />.
* Maximum parallelism is 1 plus the additional thread count provided by the Thread Modifier Hatch.
* <ItemLink id="ae2:annihilation_plane" /> can collect <ItemLink id="minecraft:chain_command_block" /> and <ItemLink id="minecraft:repeating_command_block" /> produced by the Creative Aggregator.
* For high-volume item dropping with <ItemLink id="ae2:formation_plane" />, see [AE2 Automation](ae2_automation.md).

</Column>

## Door of Creation Recipes

<Row>
    <Recipe id="gtladditions:door_of_create/command_block" />

    <Recipe id="gtladditions:door_of_create/magmatter_block" />
</Row>

## Creative Aggregator Recipes

<Row>
    <Recipe id="gtladditions:create_aggregation/chain_command_block" />

    <Recipe id="gtladditions:create_aggregation/repeating_command_block" />
</Row>

</Column>