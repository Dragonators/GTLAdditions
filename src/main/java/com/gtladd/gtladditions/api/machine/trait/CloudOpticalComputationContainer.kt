package com.gtladd.gtladditions.api.machine.trait

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableComputationContainer
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.common.machine.hatch.CloudOpticalComputationHatchMachine
import com.gtladd.gtladditions.utils.cloud.CloudNetworkManager
import org.gtlcore.gtlcore.api.machine.computation.ComputationConnections
import org.gtlcore.gtlcore.api.machine.computation.ComputationNetwork
import org.gtlcore.gtlcore.api.machine.computation.ComputationNode

class CloudOpticalComputationContainer(
    machine: MetaMachine,
    handlerIO: IO,
    transmitter: Boolean
) : NotifiableComputationContainer(machine, handlerIO, transmitter),
    ComputationNode {

    private fun getTeamId() = (machine as? CloudOpticalComputationHatchMachine)?.teamId

    override fun `gtlcore$computationLinks`(): List<IOpticalComputationProvider> =
        CloudNetworkManager.computationLinks(getTeamId())

    override fun `gtlcore$computationOnline`(): Boolean =
        ComputationConnections.loaded(machine) && getTeamId() != null

    override fun `gtlcore$requiresComputationBridge`(): Boolean = true

    override fun requestCWUt(
        cwut: Int,
        simulate: Boolean,
        seen: MutableCollection<IOpticalComputationProvider>
    ): Int = if (handlerIO == IO.IN && !isTransmitter) {
        ComputationNetwork.request(this, cwut, simulate)
    } else {
        0
    }

    override fun handleRecipeInner(
        io: IO,
        recipe: GTRecipe,
        left: List<Int>,
        slotName: String?,
        simulate: Boolean
    ): List<Int>? {
        if (io != IO.IN) return left
        return super.handleRecipeInner(io, recipe, left, slotName, simulate)
    }
}