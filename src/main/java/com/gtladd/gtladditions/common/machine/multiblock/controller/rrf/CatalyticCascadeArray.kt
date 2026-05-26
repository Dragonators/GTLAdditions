package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.sound.SoundEntry
import com.gtladd.gtladditions.common.machine.hatch.VientianeTranscriptionNode
import com.gtladd.gtladditions.common.modify.GTLAddSoundEntries
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.machine.multiblock.part.HugeFluidHatchPartMachine
import kotlin.random.Random

class CatalyticCascadeArray(holder: IMachineBlockEntity) : RRFModuleMachine(holder) {

    @Persisted
    @DescSynced
    private var outputBoost = false

    @Persisted
    @DescSynced
    private var euBuff = false

    @Persisted
    @DescSynced
    private var failedCycle = false

    @Persisted
    private var tick: Byte = 0

    private var requestedFluid: FluidStack = FluidStack.empty()
    private var redstoneNode: VientianeTranscriptionNode? = null
    private var fluidHatch: HugeFluidHatchPartMachine? = null

    // ========================================
    // Recursive reverse buffs
    // ========================================

    fun hasOutputBoost(): Boolean = outputBoost && !failedCycle

    fun hasEuBuff(): Boolean = euBuff

    fun isReadyForRecursiveReverseEuBuff(): Boolean = super.isReadyForRecursiveReverseBuff() && hasEuBuff()

    override fun isReadyForRecursiveReverseBuff(): Boolean = super.isReadyForRecursiveReverseBuff() && hasOutputBoost()

    // ========================================
    // Working sound
    // ========================================

    override fun getWorkingSound(): SoundEntry = GTLAddSoundEntries.QUANTUM_OSCILLATION

    override fun shouldPlayWorkingSound(): Boolean = super.isReadyForRecursiveReverseBuff() && (euBuff || hasOutputBoost())

    // ========================================
    // Life cycle
    // ========================================

    override fun startupUpdate() {
        if (offsetTimer % 20 != 0L) return

        if (tick == 0.toByte()) {
            val signal = Random.nextInt(1, 16)
            redstoneNode?.setRedStoneSignal(signal)
            requestedFluid = catalystForSignal(signal)
        }

        if (tick >= CATALYST_CHECK_SECOND) {
            val stored = fluidHatch?.tank?.storages?.firstOrNull()?.fluid ?: FluidStack.empty()
            when {
                stored.isEmpty -> {
                    outputBoost = false
                    euBuff = false
                    failedCycle = true
                }
                !failedCycle && stored.fluid == requestedFluid.fluid && stored.amount >= requestedFluid.amount -> {
                    fluidHatch?.tank?.drainInternal(requestedFluid, false)
                    outputBoost = true
                    euBuff = true
                }
                else -> {
                    fluidHatch?.tank?.drainInternal(stored, false)
                    outputBoost = false
                    euBuff = true
                    failedCycle = true
                }
            }
        }

        tick++
        if (tick >= CYCLE_SECONDS) {
            failedCycle = false
            tick = 0
            requestedFluid = FluidStack.empty()
        }
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        redstoneNode = null
        fluidHatch = null
        for (part in parts) {
            when (part) {
                is VientianeTranscriptionNode -> redstoneNode = part
                is HugeFluidHatchPartMachine -> fluidHatch = part
            }
        }
    }

    override fun onStructureInvalid() {
        redstoneNode = null
        fluidHatch = null
        super.onStructureInvalid()
    }

    // ========================================
    // GUI
    // ========================================

    override fun addDisplayText(textList: MutableList<Component>) {
        super.addDisplayText(textList)
        textList.add(
            Component.translatable(
                "gtladditions.machine.catalytic_cascade_array.boost",
                Component.literal(if (hasOutputBoost()) "✓" else "x")
                    .withStyle(if (hasOutputBoost()) ChatFormatting.GREEN else ChatFormatting.RED)
            )
        )
        textList.add(Component.translatable("gtladditions.machine.recursive_reverse_array.cycle_second", tick))
    }

    // ========================================
    // Persistence
    // ========================================

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        requestedFluid.saveToTag(tag)
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        requestedFluid = FluidStack.loadFromTag(tag)
    }

    // ========================================
    // Metadata
    // ========================================

    override fun getModuleDisplayNameKey(): String = "block.gtladditions.catalytic_cascade_array"

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(CatalyticCascadeArray::class.java, RRFModuleMachine.MANAGED_FIELD_HOLDER)
        private const val CATALYST_CHECK_SECOND = 7
        private const val CYCLE_SECONDS = 32

        private fun catalystForSignal(signal: Int): FluidStack = when (signal) {
            in 1..3 -> GTLMaterials.DimensionallyTranscendentCrudeCatalyst.getFluid(40000)
            in 4..6 -> GTLMaterials.DimensionallyTranscendentProsaicCatalyst.getFluid(40000)
            in 7..9 -> GTLMaterials.DimensionallyTranscendentResplendentCatalyst.getFluid(40000)
            in 10..12 -> GTLMaterials.DimensionallyTranscendentExoticCatalyst.getFluid(40000)
            in 13..15 -> GTLMaterials.DimensionallyTranscendentStellarCatalyst.getFluid(40000)
            else -> FluidStack.empty()
        }
    }
}