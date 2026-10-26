package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gtladd.gtladditions.common.machine.multiblock.controller.ApocalypticWarpQuantumMatrix
import com.gtladd.gtladditions.utils.ContinuousAnimationClock
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.config.ConfigHolder
import org.gtlcore.gtlcore.utils.RenderUtil

class MatrixEffectsTrait(machine: ApocalypticWarpQuantumMatrix) : MachineTrait(machine) {

    @field:DescSynced
    @field:UpdateListener(methodName = "onAnimationStateChanged")
    private var effectAnimation = CompoundTag()

    private val serverEffectClock = ContinuousAnimationClock()
    private var clientEffectClock: ContinuousAnimationClock? = null
    private var lastSampledTick = Double.NEGATIVE_INFINITY
    private var clientSmoothAnimations: Boolean? = null

    override fun getMachine(): ApocalypticWarpQuantumMatrix = super.getMachine() as ApocalypticWarpQuantumMatrix

    fun isRunning(): Boolean = getMachine().isFormed && effectAnimation.getBoolean("running")

    private fun effectWorldTime(): Double = (machine.level?.gameTime ?: 0L).toDouble()

    override fun onMachineLoad() {
        if (machine.isRemote) {
            onAnimationStateChanged(effectAnimation)
        } else {
            updateAnimation()
        }
    }

    override fun onMachineUnLoad() {
        if (machine.isRemote) {
            clientEffectClock = null
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
        val now = effectWorldTime()
        serverEffectClock.setRunning(running, now)

        effectAnimation = CompoundTag().apply {
            putDouble("ticks", serverEffectClock.sample(now))
            putDouble("time", now)
            putBoolean("running", running)
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @OnlyIn(Dist.CLIENT)
    fun onAnimationStateChanged(newState: CompoundTag, oldState: CompoundTag? = null) {
        if (!machine.isRemote) return

        if (newState.isEmpty) {
            clientEffectClock = null
            lastSampledTick = Double.NEGATIVE_INFINITY
            clientSmoothAnimations = null
            return
        }

        // A state change must not rewind a fractional frame already displayed.
        val now = clientTime(0f)

        // The server state already includes formation state. Do not depend on field sync order.
        val running = newState.getBoolean("running")
        val clock = clientEffectClock

        if (clock == null) {
            // Server timestamps use world time; the render clock has a different origin.
            val elapsed = newState.getDouble("ticks") +
                if (running) maxOf(0.0, effectWorldTime() - newState.getDouble("time")) else 0.0

            clientEffectClock = ContinuousAnimationClock().also {
                it.restore(elapsed, now, running)
            }
        } else {
            // Preserve the local phase on subsequent notifications instead of seeking backwards
            // to an older server stop timestamp after network latency.
            clock.setRunning(running, now)
        }
    }

    @OnlyIn(Dist.CLIENT)
    fun animationTicks(partialTicks: Float): Double {
        val clock = clientEffectClock ?: return 0.0
        return clock.sample(clientTime(partialTicks))
    }

    @OnlyIn(Dist.CLIENT)
    private fun clientTime(partialTicks: Float): Double {
        val smooth = ConfigHolder.INSTANCE.graphics.enableSmoothAnimations
        val now = RenderUtil.getSmoothTick(machine, partialTicks).toDouble()
        if (clientSmoothAnimations != null && clientSmoothAnimations != smooth) {
            // Both sources advance in ticks, but their origins are unrelated.
            clientEffectClock?.rebase(lastSampledTick, now)
            lastSampledTick = now
        }
        clientSmoothAnimations = smooth
        lastSampledTick = maxOf(lastSampledTick, now)
        return lastSampledTick
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(MatrixEffectsTrait::class.java)
    }
}