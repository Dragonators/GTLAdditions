package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gtladd.gtladditions.client.render.machine.lighthunter.LightHunterParadoxAnimation
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class LightHunterParadoxEffectsTrait(machine: LightHunterSpaceStation) : MachineTrait(machine) {
    // -1 identifies the initial description packet. Loading an already unlocked ship is not an event.
    @field:DescSynced
    @field:UpdateListener(methodName = "onUnlockStateChanged")
    private var unlockState = -1

    override fun getMachine(): LightHunterSpaceStation = super.getMachine() as LightHunterSpaceStation

    override fun onMachineUnLoad() {
        unlockState = -1
        if (machine.isRemote) clearClientAnimation()
    }

    fun initializeState(unlocked: Boolean) {
        if (!machine.isRemote) unlockState = if (unlocked) 1 else 0
    }

    fun notifyParadoxUnlocked() {
        if (!machine.isRemote) unlockState = 1
    }

    @Suppress("unused")
    @OnlyIn(Dist.CLIENT)
    fun onUnlockStateChanged(newValue: Int, oldValue: Int) {
        LightHunterParadoxAnimation.onStateChanged(getMachine(), newValue, oldValue)
    }

    @OnlyIn(Dist.CLIENT)
    private fun clearClientAnimation() {
        LightHunterParadoxAnimation.remove(getMachine())
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(LightHunterParadoxEffectsTrait::class.java)
    }
}