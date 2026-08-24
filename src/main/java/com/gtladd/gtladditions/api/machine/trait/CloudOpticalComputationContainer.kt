package com.gtladd.gtladditions.api.machine.trait

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.machine.trait.NotifiableComputationContainer
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.common.machine.hatch.CloudOpticalComputationHatchMachine
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.canBridgeCloud

class CloudOpticalComputationContainer(
    machine: MetaMachine,
    handlerIO: IO,
    transmitter: Boolean
) : NotifiableComputationContainer(machine, handlerIO, transmitter) {

    private fun getTeamId() = (machine as? CloudOpticalComputationHatchMachine)?.teamId

    override fun handleRecipeInner(
        io: IO,
        recipe: GTRecipe,
        left: List<Int>,
        slotName: String?,
        simulate: Boolean
    ): List<Int>? {
        var sum = left.sum()
        if (io == IO.IN) {
            val teamId = getTeamId()
            val availableCWU = CloudNetworkManager.requestCWU(teamId, Int.MAX_VALUE.toLong(), true).toInt()
            if (availableCWU >= sum) {
                if (recipe.data.getBoolean("duration_is_total_cwu")) {
                    val drawn = if (simulate) {
                        availableCWU
                    } else {
                        CloudNetworkManager.requestCWU(teamId, availableCWU.toLong(), false).toInt()
                    }
                    if (!simulate) {
                        when (val targetMachine = machine) {
                            is IRecipeLogicMachine -> {
                                val recipeLogic = targetMachine.recipeLogic
                                recipeLogic.progress = recipeLogic.progress - 1 + drawn
                            }
                            is IMultiPart -> {
                                for (controller in targetMachine.controllers) {
                                    if (controller is IRecipeLogicMachine) {
                                        val recipeLogic = controller.recipeLogic
                                        recipeLogic.progress = recipeLogic.progress - 1 + drawn
                                    }
                                }
                            }
                        }
                    }
                    sum -= drawn
                } else {
                    sum -= CloudNetworkManager.requestCWU(teamId, sum.toLong(), simulate).toInt()
                }
            }
        }
        return if (sum <= 0) null else listOf(sum)
    }

    override fun requestCWUt(
        cwut: Int,
        simulate: Boolean,
        seen: MutableCollection<IOpticalComputationProvider>
    ): Int {
        if (handlerIO == IO.IN && !isTransmitter) {
            return CloudNetworkManager.requestCWU(getTeamId(), cwut.toLong(), simulate).toInt()
        }
        return 0
    }

    override fun canBridge(): Boolean {
        val targetMachine = machine
        if (targetMachine is IOpticalComputationProvider) return targetMachine.canBridgeCloud()
        if (targetMachine is IMultiPart) {
            for (controller in targetMachine.controllers) {
                if (controller is IOpticalComputationProvider) return controller.canBridgeCloud()
            }
        }
        return false
    }
}