---
navigation:
  title: Cloud Computation and Research Data
  icon: gtladditions:cloud_computation_monitor
  parent: part/machine_part_index.md
  position: 10
item_ids:
  - gtladditions:cloud_computation_monitor
  - gtladditions:cloud_computation_transmitter_hatch
  - gtladditions:cloud_computation_receiver_hatch
  - gtladditions:cloud_data_machine
  - gtladditions:cloud_data_hatch
---

# Cloud Computation and Research Data

<Row>
  <BlockImage id="gtladditions:cloud_computation_monitor" scale="3" />
  <BlockImage id="gtladditions:cloud_computation_transmitter_hatch" scale="3" />
  <BlockImage id="gtladditions:cloud_computation_receiver_hatch" scale="3" />
  <BlockImage id="gtladditions:cloud_data_machine" scale="3" />
  <BlockImage id="gtladditions:cloud_data_hatch" scale="3" />
</Row>

<Column gap="12" fullWidth={true}>

### Binding and range

* Cloud devices bind to their placer automatically. Right-click them with a data stick to rebind them, or left-click to unbind them.
* Devices are grouped by FTB Teams team, so members of the same team can share one cloud system.
* Unbound devices do not join the network. Both computation and research data work across dimensions.

### Cloud computation

* The cloud computation transmitter acts as a computation source hatch; the receiver acts as a computation target hatch.
* A transmitter must be installed on a formed multiblock that can bridge computation. An HPCA requires an HPCA bridge component.
* The monitor is optional for network operation. It displays providers, requesters, maximum computation, and remaining computation.
* The highlight button outlines same-dimension targets and turns the player's view toward them. Cross-dimension targets are shown as coordinates with a teleport link that requires command permission.

### Cloud research data

* The cloud research data storage machine has 90 research-data slots. The cloud research data hatch acts as an optical data receiver.
* The storage machine consumes a base 393,216 EU/t plus 393,216 EU/t for each valid data module, data orb, or data stick.
* A Creative Data Access Hatch makes every research entry available to all same-team receivers and fixes consumption at 393,216 EU/t.
* Normal research items drop when the storage machine is removed. The Creative Data Access Hatch is destroyed instead of dropping.
* A storage machine with insufficient power stops serving research data.

</Column>
