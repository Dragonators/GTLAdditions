package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.common.machine.multiblock.controller.SubspaceCorridorNexusIndustrialArray
import com.gtladd.gtladditions.utils.SubspaceNexusAnimationClock
import com.gtladd.gtladditions.utils.SubspaceNexusTimeline
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.fml.DistExecutor
import org.gtlcore.gtlcore.config.ConfigHolder

class NexusEffectsTrait(machine: SubspaceCorridorNexusIndustrialArray) : MachineTrait(machine) {
    @field:Persisted
    @field:DescSynced
    @field:UpdateListener(methodName = "onAnimationStateChanged")
    private var effectAnimation = CompoundTag()

    private val clientClock = SubspaceNexusAnimationClock()
    private var effectSubscription: TickableSubscription? = null
    private var pendingSince = -1L

    override fun getMachine(): SubspaceCorridorNexusIndustrialArray = super.getMachine() as SubspaceCorridorNexusIndustrialArray

    override fun onMachineLoad() {
        if (machine.isRemote) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                Runnable { com.gtladd.gtladditions.client.render.machine.nexus.NexusClientTracker.track(getMachine()) }
            }
            onAnimationStateChanged(effectAnimation, null)
        } else if (getMachine().isFormed) {
            wake()
        }
    }

    override fun onMachineUnLoad() {
        if (machine.isRemote) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                Runnable { com.gtladd.gtladditions.client.render.machine.nexus.NexusClientTracker.untrack(getMachine()) }
            }
            clientClock.clear()
        }
    }

    fun wake() {
        if (!machine.isRemote) effectSubscription = machine.subscribeServerTick(effectSubscription, ::tickEffects)
    }

    fun onParadoxUnlocked() {
        val now = machine.level?.gameTime ?: return
        if (effectAnimation.isEmpty) effectAnimation = initialAnimation(now, false)
        val eventId = effectAnimation.getInt("eventId") + 1

        if (isWorking()) {
            changeState("unlock", now, now, eventId)
        } else {
            effectAnimation = effectAnimation.copy().apply {
                putLong("eventTick", -1)
                putInt("eventId", eventId)
            }
        }

        wake()
    }

    private fun isWorking(): Boolean = getMachine().isFormed && getMachine().recipeLogic.isWorking

    private fun visualWorking(): Boolean = SubspaceNexusTimeline.isWorkingState(effectAnimation.getString("state"))

    private fun eclipseCompleted(now: Long): Boolean {
        val state = effectAnimation.getString("state")
        return SubspaceNexusTimeline.isParadoxState(state) || (state == "unlock" && now - effectAnimation.getLong("since") >= ECLIPSE_TICKS)
    }

    private fun initialAnimation(now: Long, completed: Boolean): CompoundTag {
        val working = isWorking()
        return CompoundTag().apply {
            putString(
                "state",
                if (completed) {
                    if (working) "paradox" else "idleP"
                } else {
                    if (working) "normal" else "idle"
                }
            )
            putLong("since", now)
            putDouble("chartAt", now / 20.0)
            putLong("eventTick", 0)
            putInt("eventId", 0)
        }
    }

    private fun changeState(
        state: String,
        now: Long,
        eventTick: Long = effectAnimation.getLong("eventTick"),
        eventId: Int = effectAnimation.getInt("eventId")
    ) {
        effectAnimation = SubspaceNexusTimeline.transition(effectAnimation, state, now.toDouble(), eventTick.toDouble()).apply {
            putLong("eventTick", eventTick)
            putInt("eventId", eventId)
        }
    }

    private fun changeWorkingState(working: Boolean, now: Long) {
        val previous = effectAnimation.getString("state")
        val age = (now - effectAnimation.getLong("since")) / 20.0
        val completed = eclipseCompleted(now)
        var eventTick = effectAnimation.getLong("eventTick")
        var eventId = effectAnimation.getInt("eventId")

        val next = if (working) {
            when {
                getMachine().unlockParadoxical() && !completed -> {
                    eventTick = now - 40
                    eventId++
                    "startU"
                }
                completed && ((previous == "stopP" && age < 13) || (previous == "refail" && age < 8)) -> "resumeP"
                completed -> "startP"
                else -> "startN"
            }
        } else {
            when {
                (previous == "unlock" || previous == "startU") && !completed -> {
                    eventTick = -1
                    "abort"
                }
                completed && (previous == "startP" || previous == "resumeP") -> "refail"
                completed -> "stopP"
                else -> "stopN"
            }
        }

        changeState(next, now, eventTick, eventId)
    }

    private fun tickEffects() {
        val now = machine.level?.gameTime ?: return
        val working = isWorking()
        // An unlocked machine without animation state has no event left to replay.
        if (effectAnimation.isEmpty) effectAnimation = initialAnimation(now, getMachine().unlockParadoxical())

        if (working == visualWorking()) {
            pendingSince = -1
        } else {
            if (pendingSince < 0) pendingSince = now
            if (now - pendingSince >= WORKING_DEBOUNCE_TICKS) {
                changeWorkingState(working, now)
                pendingSince = -1
            }
        }

        val state = effectAnimation.getString("state")
        val next = SubspaceNexusTimeline.completedState(state)
        if (next != null && (now - effectAnimation.getLong("since")) / 20.0 >= SubspaceNexusTimeline.duration(state)) {
            changeState(next, now)
            if (state == "startU") effectAnimation = SubspaceNexusTimeline.joinShow(effectAnimation, effectAnimation.getDouble("eventTick"))
        }

        if (working == visualWorking() && SubspaceNexusTimeline.completedState(effectAnimation.getString("state")) == null) {
            effectSubscription?.unsubscribe()
            effectSubscription = null
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @OnlyIn(Dist.CLIENT)
    fun onAnimationStateChanged(newState: CompoundTag, oldState: CompoundTag?) {
        if (!machine.isRemote) return
        val level = machine.level ?: return
        clientClock.synchronize(
            newState,
            level.gameTime.toDouble(),
            RenderUtils.getSmoothTickDouble(machine, 0f),
            ConfigHolder.INSTANCE.graphics.enableSmoothAnimations
        )
    }

    @OnlyIn(Dist.CLIENT)
    fun animationFrame(partialTicks: Float): SubspaceNexusAnimationClock.Frame? = clientClock.sample(
        RenderUtils.getSmoothTickDouble(machine, partialTicks),
        ConfigHolder.INSTANCE.graphics.enableSmoothAnimations
    )

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private const val ECLIPSE_TICKS = 320L
        private const val WORKING_DEBOUNCE_TICKS = 20L
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(NexusEffectsTrait::class.java)
    }
}
