package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.utils.ClientAnimationClock
import com.gtladd.gtladditions.utils.ContinuousAnimationClock
import com.gtladd.gtladditions.utils.RenderUtils
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.config.ConfigHolder

class WorkingClockEffectsTrait(machine: GTLAddWorkableElectricMultipleRecipesMachine) : MachineTrait(machine) {

    @field:DescSynced
    @field:UpdateListener(methodName = "onAnimationStateChanged")
    private var effectAnimation = CompoundTag()

    private val serverClock = ContinuousAnimationClock()
    private val clientClock = ClientAnimationClock()

    override fun getMachine(): GTLAddWorkableElectricMultipleRecipesMachine =
        super.getMachine() as GTLAddWorkableElectricMultipleRecipesMachine

    fun isRunning(): Boolean = getMachine().isFormed && effectAnimation.getBoolean("running")

    private fun worldTime(): Double = (machine.level?.gameTime ?: 0L).toDouble()

    override fun onMachineLoad() {
        if (machine.isRemote) {
            onAnimationStateChanged(effectAnimation)
        } else {
            updateAnimation()
        }
    }

    override fun onMachineUnLoad() {
        if (machine.isRemote) {
            clientClock.clear()
            effectAnimation = CompoundTag()
        } else {
            updateAnimation(false)
        }
    }

    fun updateAnimation(working: Boolean = getMachine().recipeLogic.isWorking) {
        if (machine.isRemote) return

        val running = getMachine().isFormed && working
        val now = worldTime()
        serverClock.setRunning(running, now)

        effectAnimation = CompoundTag().apply {
            putDouble("ticks", serverClock.sample(now))
            putDouble("time", now)
            putBoolean("running", running)
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @OnlyIn(Dist.CLIENT)
    fun onAnimationStateChanged(newState: CompoundTag, oldState: CompoundTag? = null) {
        if (!machine.isRemote) return

        if (newState.isEmpty) {
            clientClock.clear()
            return
        }

        val renderTick = RenderUtils.getSmoothTickDouble(machine, 0f)
        val smooth = ConfigHolder.INSTANCE.graphics.enableSmoothAnimations

        // The server state already includes formation state. Do not depend on field sync order.
        val running = newState.getBoolean("running")

        if (clientClock.sample(renderTick, smooth) == null) {
            // Server timestamps use world time; the render clock has a different origin.
            val elapsed = newState.getDouble("ticks") +
                if (running) maxOf(0.0, worldTime() - newState.getDouble("time")) else 0.0
            clientClock.start(elapsed, renderTick, smooth, running)
        } else {
            // Preserve the local phase on subsequent notifications instead of seeking backwards
            // to an older server stop timestamp after network latency.
            clientClock.setRunning(running, renderTick, smooth)
        }
    }

    @OnlyIn(Dist.CLIENT)
    fun animationTicks(partialTicks: Float): Double = clientClock.sample(
        RenderUtils.getSmoothTickDouble(machine, partialTicks),
        ConfigHolder.INSTANCE.graphics.enableSmoothAnimations
    ) ?: 0.0

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(WorkingClockEffectsTrait::class.java)
    }
}