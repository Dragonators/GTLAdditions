package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.gui.widget.MatrixRecipeMemoryPage
import com.gtladd.gtladditions.common.machine.multiblock.controller.ApocalypticWarpQuantumMatrix
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.ThreadMultiplierStrategy
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import kotlin.math.pow

class MatrixQuantumWarpTrait(machine: ApocalypticWarpQuantumMatrix) : MachineTrait(machine) {

    @field:Persisted
    var runningSecs = 0L
        private set

    @field:Persisted
    var enableDimensionFold = false
        private set

    private val recipeRunningSecs = Object2LongOpenHashMap<ResourceLocation>()
    private var clockSubscription: TickableSubscription? = null

    val recipeTimes: Map<ResourceLocation, Long>
        get() = recipeRunningSecs

    internal var recipeTimesRevision = 0L
        private set

    // ========================================
    // Utils
    // ========================================

    fun recipeSecs(id: ResourceLocation): Long = recipeRunningSecs.getLong(id)

    fun hasInfiniteParallel(): Boolean =
        hasInfiniteParallel(true) && getMachine().resolveTSD()?.isReady() == true

    fun hasInfiniteParallel(connected: Boolean): Boolean =
        runningSecs >= MACHINE_SECONDS && getMachine().astralArrayCount >= MAX_ARRAYS && connected

    fun finiteThreads(): Long {
        val omegaMultiplier = ThreadMultiplierStrategy.getAdditionalMultiplier(getMachine().definition)
        val base = 1024L + 64L * omegaMultiplier * getMachine().astralArrayCount

        if (runningSecs >= MACHINE_SECONDS) return base * 16

        return (base * 16.0.pow(runningSecs.toDouble() / MACHINE_SECONDS)).toLong()
    }

    fun auxiliaryCoefficient(): Double {
        // At t >= T, 0.4 converts to an exact decimal for settlement.
        if (runningSecs >= MACHINE_SECONDS) return 0.4

        return 0.8 * 2.0.pow(-runningSecs.toDouble() / MACHINE_SECONDS)
    }

    // ========================================
    // Life Cycle
    // ========================================

    override fun onMachineLoad() {
        (machine.level as? ServerLevel)?.server?.tell(TickTask(0, ::updateRunningSecSubscription))
    }

    fun onWorking() {
        if (this.runningSecs == 0L) {
            this.runningSecs = 1
            this.updateRunningSecSubscription()
        }
    }

    private fun updateRunningSecSubscription() {
        if (this.runningSecs > 0 || !recipeRunningSecs.isEmpty()) {
            this.clockSubscription = machine.subscribeServerTick(this.clockSubscription, ::updateRunningClock)
        } else if (this.clockSubscription != null) {
            this.clockSubscription!!.unsubscribe()
            this.clockSubscription = null
        }
    }

    private fun updateRunningClock() {
        if (machine.offsetTimer % 20 != 0L) return

        val logic = getMachine().recipeLogic
        val working = logic.isWorking
        val activeRecipes = if (working) {
            logic.lastRecipe?.data?.getCompound("quantumWarpRecipes")?.allKeys
                ?.mapNotNullTo(mutableSetOf()) { key ->
                    ResourceLocation.tryParse(key)?.also { recipeRunningSecs.putIfAbsent(it, 0L) }
                } ?: emptySet()
        } else {
            emptySet()
        }

        runningSecs = (runningSecs + if (working) 1L else -16L).coerceAtLeast(0)

        val iterator = Object2LongMaps.fastIterator(recipeRunningSecs)
        // Every entry advances, decays or expires on this pass.
        if (iterator.hasNext()) recipeTimesRevision++

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val seconds = (entry.longValue + if (entry.key in activeRecipes) 1L else -4L).coerceAtLeast(0)
            if (seconds == 0L) {
                iterator.remove()
            } else {
                entry.setValue(seconds)
            }
        }

        this.updateRunningSecSubscription()
    }

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        tag.put(
            "quantumWarpRecipeSeconds",
            CompoundTag().apply {
                for (entry in Object2LongMaps.fastIterable(recipeRunningSecs)) {
                    putLong(entry.key.toString(), entry.longValue)
                }
            }
        )
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        recipeTimesRevision++
        recipeRunningSecs.clear()

        val timers = tag.getCompound("quantumWarpRecipeSeconds")
        timers.allKeys.forEach { key ->
            val id = ResourceLocation.tryParse(key) ?: return@forEach
            val seconds = timers.getLong(key)
            if (seconds > 0) recipeRunningSecs.put(id, seconds)
        }
    }

    // ========================================
    // GUI
    // ========================================

    fun addParallelDisplay(textList: MutableList<Component?>) {
        val p = getMachine().maxParallel
        if (p > 1 && !hasInfiniteParallel()) {
            textList.add("gtceu.multiblock.parallel".toComponent(p))
        }
    }

    fun addDisplayText(textList: MutableList<Component?>) {
        val distorter = getMachine().resolveTSD()?.takeIf { it.isReady() }
        val infinite = hasInfiniteParallel(distorter != null)

        textList.add(
            "gtladditions.multiblock.forge_of_the_antichrist.running_sec".toComponent(
                createRainbowComponent(FormattingUtil.DECIMAL_FORMAT_2F.format(runningSecs / 3600.0))
            )
        )
        textList.add(
            "tooltip.gtladditions.astral_array_count".toComponent(
                "${getMachine().astralArrayCount} / $MAX_ARRAYS".literal.withStyle(ChatFormatting.GOLD)
            )
        )
        textList.add(
            "gtladditions.multiblock.threads".toComponent(
                if (infinite) {
                    createLanguageRainbowComponentOnServer(
                        "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                    )
                } else {
                    FormattingUtil.formatNumbers(finiteThreads()).literal.withStyle(ChatFormatting.GOLD)
                }
            ).withStyle(ChatFormatting.GRAY)
        )

        if (distorter != null) {
            val folded = infinite && enableDimensionFold
            val output = distorter.outputMultiplier * (if (folded) 2 else 1)
            val auxiliary = auxiliaryCoefficient() * (if (folded) 6 else 1)

            textList.add(
                "gtladditions.matrix.output_multiplier".toComponent(
                    output.toString().literal.withStyle(ChatFormatting.GOLD)
                )
            )
            textList.add(
                "gtladditions.matrix.auxiliary_multiplier".toComponent(
                    "%.2f".format(auxiliary).literal.withStyle(ChatFormatting.GOLD)
                )
            )
        }
    }

    fun attachSideTabs(tabs: TabsWidget) {
        tabs.attachSubTab(MatrixRecipeMemoryPage(this))
    }

    fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        configuratorPanel.attachConfigurators(
            IFancyConfiguratorButton.Toggle(
                GuiTextureGroup(GuiTextures.BUTTON, ResourceTexture("gtladditions:textures/gui/button/dimension_fold_off.png")),
                GuiTextureGroup(GuiTextures.BUTTON, ResourceTexture("gtladditions:textures/gui/button/dimension_fold_on.png")),
                { enableDimensionFold },
                { clickData, pressed -> if (!clickData.isRemote) enableDimensionFold = pressed }
            ).setTooltipsSupplier { listOf("gtladditions.matrix.fold_toggle".toComponent) }
        )
    }

    override fun getMachine(): ApocalypticWarpQuantumMatrix = super.getMachine() as ApocalypticWarpQuantumMatrix

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        // 4 h.
        const val MACHINE_SECONDS = 14400L
        const val MAX_ARRAYS = 256
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(MatrixQuantumWarpTrait::class.java)
    }
}
