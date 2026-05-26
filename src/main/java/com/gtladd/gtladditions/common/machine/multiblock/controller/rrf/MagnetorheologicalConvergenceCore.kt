package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.sound.SoundEntry
import com.gregtechceu.gtceu.common.data.GTSoundEntries
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeBusPartMachine
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectIntPair
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.machine.multiblock.part.HugeFluidHatchPartMachine
import org.gtlcore.gtlcore.utils.Registries
import kotlin.random.Random

class MagnetorheologicalConvergenceCore(holder: IMachineBlockEntity) : RRFModuleMachine(holder) {

    @Persisted
    @DescSynced
    private var focus = false

    @Persisted
    private var failItem1: Byte = 0

    @Persisted
    private var failItem2: Byte = 0

    @Persisted
    private var failFluid: Byte = 3

    @Persisted
    private var hasMagmatter = false

    @Persisted
    private var tick = 0

    private var itemHatch1: HugeBusPartMachine? = null
    private var itemHatch2: HugeBusPartMachine? = null
    private var fluidHatch: HugeFluidHatchPartMachine? = null
    private var magmatterHatch: ItemBusPartMachine? = null
    private var requestedFluid: FluidStack? = null
    private val requestedItems = arrayOfNulls<ObjectIntPair<Item>>(2)

    // ========================================
    // Recursive reverse buff
    // ========================================

    fun hasFocus(): Boolean = focus

    override fun isReadyForRecursiveReverseBuff(): Boolean = super.isReadyForRecursiveReverseBuff() && focus

    // ========================================
    // Working sound
    // ========================================

    override fun getWorkingSound(): SoundEntry = GTSoundEntries.ELECTROLYZER

    override fun shouldPlayWorkingSound(): Boolean = isReadyForRecursiveReverseBuff()

    // ========================================
    // Life cycle
    // ========================================

    override fun startupUpdate() {
        if (tick >= 2880) tick = 0
        if (tick % 240 == 0) consumeFocusInputs()
        if (tick % 20 == 0) consumeMagmatter()
        tick++
    }

    private fun initRequests() {
        val shuffled = (0..2).shuffled().take(2).sorted()
        requestedItems[0] = ObjectIntPair.of(itemForIndex(shuffled[0]), Random.nextInt(1, 16385))
        requestedItems[1] = ObjectIntPair.of(itemForIndex(shuffled[1]), Random.nextInt(1, 16385))

        requestedFluid = if (Random.nextBoolean()) {
            GTLMaterials.ExcitedDtec.getFluid(Random.nextLong(1, 1638401))
        } else {
            GTLMaterials.ExcitedDtsc.getFluid(Random.nextLong(1, 1638401))
        }
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        itemHatch1 = null
        itemHatch2 = null
        fluidHatch = null
        magmatterHatch = null
        for (part in parts) {
            when (part) {
                is HugeBusPartMachine -> if (itemHatch1 == null) {
                    itemHatch1 = part
                } else if (itemHatch2 == null) {
                    itemHatch2 = part
                }
                is HugeFluidHatchPartMachine -> fluidHatch = part
                is ItemBusPartMachine -> magmatterHatch = part
            }
        }
    }

    override fun onStructureInvalid() {
        itemHatch1 = null
        itemHatch2 = null
        fluidHatch = null
        magmatterHatch = null
        hasMagmatter = false
        super.onStructureInvalid()
    }

    override fun onMachinePlaced(player: LivingEntity?, stack: ItemStack?) {
        initRequests()
    }

    // ========================================
    // Input consumption
    // ========================================

    private fun consumeFocusInputs() {
        failItem1 = 0
        failItem2 = 0
        failFluid = 3
        consumeItemHatch(itemHatch1)
        consumeItemHatch(itemHatch2)
        consumeTargetFluid()
        focus = failItem1 < 0 && failItem2 < 0 && failFluid < 0
    }

    private fun consumeItemHatch(hatch: HugeBusPartMachine?) {
        val storage = hatch?.inventory ?: return
        val stack = storage.getStackInSlot(0)
        if (stack.isEmpty) return

        val first = requestedItems[0]
        val second = requestedItems[1]
        if (failItem1 == 0.toByte() && first != null && stack.item == first.first()) {
            failItem1 = compareItemAmount(stack.count, first.rightInt())
        } else if (failItem2 == 0.toByte() && second != null && stack.item == second.first()) {
            failItem2 = compareItemAmount(stack.count, second.rightInt())
        }
        storage.extractItemInternal(0, stack.count, false)
    }

    private fun compareItemAmount(actual: Int, expected: Int): Byte = when {
        actual > expected -> 1
        actual < expected -> 2
        else -> -1
    }

    private fun consumeTargetFluid() {
        val requested = requestedFluid ?: return
        val stored = fluidHatch?.tank?.storages?.firstOrNull()?.fluid ?: FluidStack.empty()
        if (!stored.isEmpty) {
            if (stored.fluid == requested.fluid) {
                failFluid = when {
                    stored.amount > requested.amount -> 4
                    stored.amount < requested.amount -> 5
                    else -> -1
                }
            }
            fluidHatch?.tank?.drainInternal(requested, false)
        }
    }

    private fun consumeMagmatter() {
        val inventory = magmatterHatch?.inventory ?: run {
            hasMagmatter = false
            focus = false
            return
        }
        for (slot in 0 until inventory.size) {
            val stack = inventory.getStackInSlot(slot)
            if (stack.item == MAGMATTER_BLOCK && stack.count >= 2) {
                inventory.extractItemInternal(slot, 2, false)
                hasMagmatter = true
                return
            }
        }
        hasMagmatter = false
        focus = false
    }

    // ========================================
    // GUI
    // ========================================

    override fun addDisplayText(textList: MutableList<Component>) {
        super.addDisplayText(textList)
        if (!isFormed) return
        textList.add(
            Component.translatable(
                "gtladditions.machine.magnetorheological_convergence_core.focus",
                Component.literal(if (focus) "✓" else "x")
                    .withStyle(if (focus) ChatFormatting.GREEN else ChatFormatting.RED)
            )
        )
        textList.add(Component.translatable("gtladditions.machine.recursive_reverse_array.cycle_second", (tick % 240) / 20))
        addRequestDisplayText(textList)
        textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.6"))
        if (focus) {
            if (enabled) textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.7"))
            return
        }
        if (failItem1 > -1) textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.$failItem1", 1))
        if (failItem2 > -1) textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.$failItem2", 2))
        if (failFluid > -1) textList.add(Component.translatable("gtceu.machine.magnetorheological_convergence_core.gui.tooltip.$failFluid"))
        if (!hasMagmatter) textList.add(Component.translatable("gtladditions.machine.magnetorheological_convergence_core.missing_magmatter"))
    }

    private fun addRequestDisplayText(textList: MutableList<Component>) {
        requestedItems.forEachIndexed { index, item ->
            if (item != null) {
                textList.add(
                    Component.translatable(
                        "gtladditions.machine.magnetorheological_convergence_core.required_item",
                        index + 1,
                        ItemStack(item.first()).displayName.copy().withStyle(ChatFormatting.GOLD),
                        FormattingUtil.formatNumbers(item.rightInt())
                    )
                )
            }
        }

        requestedFluid?.takeUnless { it.isEmpty }?.let { fluid ->
            textList.add(
                Component.translatable(
                    "gtladditions.machine.magnetorheological_convergence_core.required_fluid",
                    fluid.displayName.copy().withStyle(ChatFormatting.AQUA),
                    FormattingUtil.formatNumbers(fluid.amount)
                )
            )
        }
    }

    // ========================================
    // Persistence
    // ========================================

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        requestedFluid?.saveToTag(tag)
        val list = ListTag()
        requestedItems.forEach {
            if (it != null) {
                val itemTag = CompoundTag()
                itemTag.putIntArray("itemCount", intArrayOf(Item.getId(it.first()), it.rightInt()))
                list.add(itemTag)
            }
        }
        if (list.isNotEmpty()) tag.put("RequestedItems", list)
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        requestedFluid = FluidStack.loadFromTag(tag)
        if (tag.contains("RequestedItems")) {
            val list = tag.getList("RequestedItems", 10)
            for (i in 0 until minOf(2, list.size)) {
                val itemData = list.getCompound(i).getIntArray("itemCount")
                requestedItems[i] = ObjectIntPair.of(Item.byId(itemData[0]), itemData[1])
            }
        }
    }

    // ========================================
    // Metadata
    // ========================================

    override fun getModuleDisplayNameKey(): String = "block.gtladditions.magnetorheological_convergence_core"

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(MagnetorheologicalConvergenceCore::class.java, RRFModuleMachine.MANAGED_FIELD_HOLDER)
        private val BLACK_BODY_NAQUADRIA_SUPERSOLID = Registries.getItem("kubejs:black_body_naquadria_supersolid")
        private val QUANTUM_ANOMALY = Registries.getItem("kubejs:quantum_anomaly")
        private val HYPER_STABLE_SELF_HEALING_ADHESIVE = Registries.getItem("kubejs:hyper_stable_self_healing_adhesive")
        private val MAGMATTER_BLOCK = Registries.getItem("gtceu:magmatter_block")

        private fun itemForIndex(index: Int): Item = when (index) {
            0 -> BLACK_BODY_NAQUADRIA_SUPERSOLID
            1 -> QUANTUM_ANOMALY
            2 -> HYPER_STABLE_SELF_HEALING_ADHESIVE
            else -> Items.AIR
        }
    }
}