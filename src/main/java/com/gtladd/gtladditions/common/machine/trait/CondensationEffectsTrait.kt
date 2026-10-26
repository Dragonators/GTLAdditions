package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gtladd.gtladditions.common.machine.multiblock.controller.AntientropyCondensationCenter
import com.gtladd.gtladditions.utils.ContinuousAnimationClock
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.config.ConfigHolder
import org.gtlcore.gtlcore.utils.RenderUtil

class CondensationEffectsTrait(machine: AntientropyCondensationCenter) : MachineTrait(machine) {

    @field:DescSynced
    @field:UpdateListener(methodName = "onAnimationStateChanged")
    private var effectAnimation = CompoundTag()

    private val serverClock = ContinuousAnimationClock()
    private var clientClock: ContinuousAnimationClock? = null
    private var lastSampledTick = Double.NEGATIVE_INFINITY
    private var clientSmoothAnimations: Boolean? = null

    override fun getMachine(): AntientropyCondensationCenter = super.getMachine() as AntientropyCondensationCenter

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
            clientClock = null
            lastSampledTick = Double.NEGATIVE_INFINITY
            clientSmoothAnimations = null
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
            clientClock = null
            lastSampledTick = Double.NEGATIVE_INFINITY
            clientSmoothAnimations = null
            return
        }

        // A state change must not rewind a fractional frame already displayed.
        val now = clientTime(0f)

        // Formation is already part of the server's running flag, regardless of field sync order.
        val running = newState.getBoolean("running")
        val clock = clientClock

        if (clock == null) {
            // Server timestamps use world time; the render clock has a different origin.
            val elapsed = newState.getDouble("ticks") +
                if (running) maxOf(0.0, worldTime() - newState.getDouble("time")) else 0.0

            clientClock = ContinuousAnimationClock().also {
                it.restore(elapsed, now, running)
            }
        } else {
            // Stop/resume at receipt time, preserving the phase already displayed to the player.
            clock.setRunning(running, now)
        }
    }

    @OnlyIn(Dist.CLIENT)
    fun animationTicks(partialTicks: Float): Double {
        val clock = clientClock ?: return 0.0
        return clock.sample(clientTime(partialTicks))
    }

    @OnlyIn(Dist.CLIENT)
    private fun clientTime(partialTicks: Float): Double {
        val smooth = ConfigHolder.INSTANCE.enableSmoothAnimations
        val now = RenderUtil.getSmoothTick(machine, partialTicks).toDouble()
        if (clientSmoothAnimations != null && clientSmoothAnimations != smooth) {
            // Both sources advance in ticks, but their origins are unrelated.
            clientClock?.rebase(lastSampledTick, now)
            lastSampledTick = now
        }
        clientSmoothAnimations = smooth
        lastSampledTick = maxOf(lastSampledTick, now)
        return lastSampledTick
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(CondensationEffectsTrait::class.java)
    }
}