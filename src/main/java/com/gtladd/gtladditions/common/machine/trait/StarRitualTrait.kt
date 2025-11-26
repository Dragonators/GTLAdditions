package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gtladd.gtladditions.utils.antichrist.ClientAnimationHelper
import com.gtladd.gtladditions.client.RenderMode
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist.Companion.MAX_EFFICIENCY_SEC
import com.gtladd.gtladditions.utils.CommonUtils
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import committee.nova.mods.avaritia.init.registry.ModItems
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.povstalec.sgjourney.common.init.BlockInit

class StarRitualTrait(
    machine: ForgeOfTheAntichrist
) : MachineTrait(machine) {

    @field:Persisted
    private val collectedItems: MutableSet<Item> = ObjectOpenHashSet()

    @field:Persisted
    @field:DescSynced
    var renderMode: RenderMode = RenderMode.NORMAL
        private set

    @field:DescSynced
    @field:UpdateListener(methodName = "onCollapseStateChanged")
    var isCollapsing: Boolean = false
        private set

    @field:Persisted
    private var serverCollapseTick: Int = 0

    @field:Persisted
    private var serverRecoverTick: Int = 0

    private var collapseSubs: TickableSubscription? = null

    override fun onMachineLoad() {
        if (renderMode == RenderMode.COLLAPSING) {
            updateCollapseSubscription()
        }
    }

    override fun getMachine(): ForgeOfTheAntichrist {
        return super.getMachine() as ForgeOfTheAntichrist
    }

    fun handleStarRitualLogic() {
        when (renderMode) {
            RenderMode.NORMAL -> {
                if (getMachine().runningSecs < MAX_EFFICIENCY_SEC) return
                if (machine.offsetTimer % 10 == 0L) collectItemsInRange()
            }

            RenderMode.RAINBOW -> {
                if (machine.offsetTimer % 10 == 0L) {
                    collectItemsInRange()
                    if (hasCollectedAll()) {
                        triggerCollapse()
                    }
                }
            }

            RenderMode.RECOVERING -> {
                updateRecoveryProgress()
            }

            else -> {}
        }
    }

    // ========================================
    // Utils
    // ========================================

    private fun collectItemsInRange() {
        val level = machine.level ?: return
        if (level !is ServerLevel) return

        val offset = CommonUtils.getRotatedRenderPosition(Direction.EAST, getMachine().frontFacing, -121.5, 0.0, 0.0)
        val center = Vec3.atCenterOf(getMachine().pos).add(offset)
        val radius = 13.3 * getMachine().radiusMultiplier
        val aabb = AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius))

        val itemEntities = level.getEntitiesOfClass(ItemEntity::class.java, aabb) { entity ->
            canCollect(entity.item.item)
        }

        for (itemEntity in itemEntities) {
            if (collectItem(itemEntity.item.item)) {
                itemEntity.discard()

                if (hasCollectedAll()) {
                    level.playSound(
                        null,
                        center.x, center.y, center.z,
                        SoundEvents.END_PORTAL_SPAWN,
                        SoundSource.BLOCKS,
                        100f,
                        1.0f
                    )
                    break
                } else {
                    level.playSound(
                        null,
                        center.x, center.y, center.z,
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS,
                        60f,
                        1.0f + (collectedItems.size * 0.1f)
                    )
                }
            }
        }
    }

    private fun triggerCollapse() {
        renderMode = RenderMode.COLLAPSING
        isCollapsing = true  // Notify Client COLLAPSING
        serverCollapseTick = 0
        collectedItems.clear()

        updateCollapseSubscription()
    }

    private fun updateCollapseSubscription() {
        collapseSubs = machine.subscribeServerTick(collapseSubs) {
            if (renderMode != RenderMode.COLLAPSING) {
                collapseSubs?.unsubscribe()
                collapseSubs = null
                return@subscribeServerTick
            }

            serverCollapseTick++

            if (serverCollapseTick >= COLLAPSE_DURATION_TICKS) {
                spawnRewardItem()

                renderMode = RenderMode.RECOVERING
                isCollapsing = false  // Notify Client RECOVERING
                serverCollapseTick = 0
                serverRecoverTick = 0
                collapseSubs?.unsubscribe()
                collapseSubs = null

                return@subscribeServerTick
            }

            updateCollapseSubscription()
        }
    }

    private fun updateRecoveryProgress() {
        if (renderMode != RenderMode.RECOVERING) return

        serverRecoverTick++

        if (serverRecoverTick >= RECOVER_DURATION_TICKS) {
            serverRecoverTick = 0
            renderMode = RenderMode.NORMAL
        }
    }

    private fun spawnRewardItem() {
        val level = machine.level ?: return
        if (level !is ServerLevel) return

        val offset = CommonUtils.getRotatedRenderPosition(Direction.EAST, getMachine().frontFacing, -122.0, 0.0, 0.0)
        val center = Vec3.atLowerCornerOf(getMachine().pos).add(offset)

        val rewardStack = GTMachines.CREATIVE_ITEM.asStack()
        val itemEntity = ItemEntity(
            level,
            center.x,
            center.y,
            center.z,
            rewardStack
        )

        itemEntity.setUnlimitedLifetime()
        itemEntity.isNoGravity = true
        itemEntity.setNoPickUpDelay()
        itemEntity.deltaMovement = Vec3(0.0, 0.0, 0.0)
        itemEntity.setPos(center.x, center.y, center.z)

        level.addFreshEntity(itemEntity)
    }

    private fun collectItem(item: Item): Boolean {
        if (!canCollect(item)) return false

        collectedItems.add(item).also {
            if (it && collectedItems.size == 1)
                renderMode = RenderMode.RAINBOW
        }

        return true
    }

    private fun canCollect(item: Item): Boolean = item in REQUIRED_ITEMS && item !in collectedItems

    private fun hasCollectedAll(): Boolean = collectedItems.size == REQUIRED_ITEMS.size

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    @Suppress("unused")
    @OnlyIn(Dist.CLIENT)
    fun onCollapseStateChanged(newValue: Boolean, oldValue: Boolean) {
        ClientAnimationHelper.onCollapseStateChanged(getMachine(), newValue, oldValue)
    }

    companion object {
        const val COLLAPSE_DURATION_TICKS = 140
        const val RECOVER_DURATION_TICKS = 100
        const val CLIENT_COLLAPSE_DURATION_TICKS = COLLAPSE_DURATION_TICKS * 0.8F
        const val CLIENT_RECOVER_DURATION_TICKS = RECOVER_DURATION_TICKS * 0.9F

        private val REQUIRED_ITEMS: Set<Item> by lazy {
            setOf(
                ModItems.neutron_ring.get(),
                BlockInit.UNIVERSE_STARGATE.get().asItem(),
                ModItems.infinity_ring.get(),
                ModItems.infinity_umbrella.get()
            )
        }

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(StarRitualTrait::class.java)
    }
}