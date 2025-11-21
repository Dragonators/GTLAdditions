package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.capability.IEnergyContainer
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.common.machine.muiltblock.part.WirelessEnergyNetworkTerminalPartMachineBase
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager
import net.minecraft.core.Direction
import org.gtlcore.gtlcore.api.capability.IInt128EnergyContainer
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.datastructure.Int128

class NetworkEnergyContainer(
    machine: WirelessEnergyNetworkTerminalPartMachineBase,
    private val handlerIO: IO
) : NotifiableRecipeHandlerTrait<Long>(machine), IEnergyContainer, IInt128EnergyContainer {

    private val energyCapacity: Long = Long.MAX_VALUE
    private val inputVoltage: Long
    private val inputAmperage: Long
    private val outputVoltage: Long
    private val outputAmperage: Long

    private var updateSubs: TickableSubscription? = null
    private var lastEnergyInputPerSec = Int128.ZERO()
    private var lastEnergyOutputPerSec = Int128.ZERO()
    private var energyInputPerSec = Int128.ZERO()
    private var energyOutputPerSec = Int128.ZERO()

    init {
        when (handlerIO) {
            IO.IN -> {
                inputVoltage = Long.MAX_VALUE
                inputAmperage = 1
                outputVoltage = 0
                outputAmperage = 0
            }
            IO.OUT -> {
                inputVoltage = 0
                inputAmperage = 0
                outputVoltage = Long.MAX_VALUE
                outputAmperage = 1
            }
            IO.BOTH -> {
                inputVoltage = Long.MAX_VALUE
                inputAmperage = 1
                outputVoltage = Long.MAX_VALUE
                outputAmperage = 1
            }
            else -> {
                inputVoltage = 0
                inputAmperage = 0
                outputVoltage = 0
                outputAmperage = 0
            }
        }
    }

    override fun getMachine(): WirelessEnergyNetworkTerminalPartMachineBase {
        return super.getMachine() as WirelessEnergyNetworkTerminalPartMachineBase
    }

    override fun onMachineLoad() {
        super.onMachineLoad()
        updateSubs = machine.subscribeServerTick(updateSubs, ::updateTick)
    }

    override fun onMachineUnLoad() {
        super.onMachineUnLoad()
        updateSubs?.unsubscribe()
        updateSubs = null
    }

    private fun updateTick() {
        if (machine.offsetTimer % 20 == 0L) {
            lastEnergyOutputPerSec = energyOutputPerSec.copy()
            lastEnergyInputPerSec = energyInputPerSec.copy()
            energyOutputPerSec.set(0, 0)
            energyInputPerSec.set(0, 0)
        }
    }

    override fun changeEnergy(energyToAdd: Long): Long {
        val result = if (getMachine().uuid != null) {
            if (WirelessEnergyManager.addEUToGlobalEnergyMap(getMachine().uuid, energyToAdd, this.machine)) {
                energyToAdd
            } else {
                0
            }
        } else {
            0
        }
        addEnergyPerSec(result)
        return result
    }

    override fun handleRecipeInner(
        io: IO,
        recipe: GTRecipe,
        left: List<Long>,
        slotName: String?,
        simulate: Boolean
    ): List<Long>? {
        var sum = left.sum()
        if (io == IO.IN) {
            val canOutput = energyStored
            val actualOutput = minOf(canOutput, sum)
            if (!simulate) {
                addEnergy(-actualOutput)
            }
            sum -= canOutput
        } else if (io == IO.OUT) {
            val canInput = getMachine().uuid != null
            if (!simulate && canInput) {
                addEnergy(sum)
            }
            sum = if (canInput) 0 else sum
        }
        return if (sum <= 0) null else listOf(sum)
    }

    override fun getEnergyStored(): Long {
        return if (handlerIO == IO.IN) {
            getMachine().uuid?.let { uuid ->
                NumberUtils.getLongValue(WirelessEnergyManager.getUserEU(uuid))
            } ?: 0
        } else {
            0
        }
    }

    override fun getEnergyCapacity(): Long = energyCapacity

    override fun getInputAmperage(): Long = inputAmperage

    override fun getInputVoltage(): Long = inputVoltage

    override fun getOutputAmperage(): Long = outputVoltage

    override fun getOutputVoltage(): Long = outputVoltage

    override fun getContents(): List<Any> = listOf(energyStored)

    override fun getTotalContentAmount(): Double = energyStored.toDouble()

    override fun getCapability(): RecipeCapability<Long> = EURecipeCapability.CAP

    override fun getInt128InputPerSec(): Int128 = lastEnergyInputPerSec.copy()

    override fun getInt128OutputPerSec(): Int128 = lastEnergyOutputPerSec.copy()

    override fun addEnergyPerSec(energy: Long) {
        when {
            energy < 0 -> energyInputPerSec.add(-energy)
            energy > 0 -> energyOutputPerSec.add(energy)
        }
    }

    override fun getInt128EnergyStored(): Int128 = Int128(energyStored)

    override fun getInt128EnergyCapacity(): Int128 = Int128(energyCapacity)

    override fun getInputPerSec(): Long = lastEnergyInputPerSec.toLong()

    override fun getOutputPerSec(): Long = lastEnergyOutputPerSec.toLong()

    override fun acceptEnergyFromNetwork(side: Direction, voltage: Long, amperage: Long): Long = 0

    override fun inputsEnergy(direction: Direction): Boolean = false

    override fun outputsEnergy(side: Direction): Boolean = false

    override fun getHandlerIO(): IO?  = handlerIO
}