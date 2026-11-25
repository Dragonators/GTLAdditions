package com.gtladd.gtladditions.common.machine.multiblock.controller

import appeng.client.render.effects.ParticleTypes
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IExplosionMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.api.sound.AutoReleasedSound
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTSoundEntries
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.translatable
import com.gtladd.gtladditions.utils.recipe.MachineUtil.input
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine
import org.gtlcore.gtlcore.integration.monitor.IMonitorRecipeType
import org.gtlcore.gtlcore.integration.voxelregion.GtlRegionExplosionFacade
import org.gtlcore.gtlcore.utils.Registries
import java.util.UUID
import kotlin.enums.enumEntries
import kotlin.math.min

class PlanetaryIonisationConvergenceTower(holder: IMachineBlockEntity) :
    StorageMachine(holder, 64),
    IExplosionMachine,
    IMonitorRecipeType {

    @field:Persisted
    private var storageEUt = 0L

    @field:Persisted
    private var cycleAmount = 0

    @field:Persisted
    private var startCycle = false

    @field:Persisted
    private var fired = false

    private var coilEnergy: CoilToEnergy? = null
    private var stellarTier = 0
    private var maxStorageEUt = 0L
    private var particlePos: BlockPos? = null

    init {
        isWorkingEnabled = false
    }

    override fun filter(itemStack: ItemStack): Boolean {
        val requiredDrone = when (coilEnergy?.workTier) {
            1 -> SPACE_DRONE_MK2
            2 -> SPACE_DRONE_MK4
            3 -> SPACE_DRONE_MK6
            else -> null
        }
        return requiredDrone != null && itemStack.`is`(requiredDrone)
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        val context = multiblockState.matchContext
        val coilType = context["CoilType"] as? ICoilType ?: return
        stellarTier = context["SCTier"] as? Int ?: 0
        coilEnergy = findCoil(coilType.material)
        maxStorageEUt = when (stellarTier) {
            1 -> 0x3297345d9a000
            2 -> 0x1d4f54cf65a0000
            3 -> 0x15c9c2ae895d0000
            else -> 0L
        }
        particlePos = pos.offset(
            when (frontFacing) {
                Direction.WEST -> 7
                Direction.EAST -> -7
                else -> 0
            },
            20,
            when (frontFacing) {
                Direction.SOUTH -> -7
                Direction.NORTH -> 7
                else -> 0
            }
        )
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        coilEnergy = null
        stellarTier = 0
        cycleAmount = 0
        maxStorageEUt = 0
        particlePos = null
    }

    override fun onPartUnload() {
        super.onPartUnload()
        coilEnergy = null
        stellarTier = 0
        cycleAmount = 0
        maxStorageEUt = 0
        particlePos = null
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        configuratorPanel.attachConfigurators(
            IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.0, 1.0, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.5, 1.0, 0.5),
                { isWorkingEnabled },
                { _, pressed -> isWorkingEnabled = pressed }
            ).setTooltipsSupplier { pressed ->
                listOf(
                    (if (pressed) "behaviour.soft_hammer.enabled" else "behaviour.soft_hammer.disabled").toComponent
                )
            }
        )
    }

    override fun monitorRecipeTypeName(): Component = "monitor.gtladditions.recipe_type.ionisation_power".toComponent

    override fun addDisplayText(textList: MutableList<Component>) {
        MultiblockDisplayText.builder(textList, isFormed)
            .setWorkingStatus(recipeLogic.isWorkingEnabled, recipeLogic.isActive)
            .addEnergyUsageLine(energyContainer)
            .addEnergyTierLine(tier)
            .addWorkingStatusLine()
            .addProgressLine(recipeLogic.progressPercent)
            .addComponent(
                Component.translatable("gui.gtladditions.planetary_ionisation_convergence_tower_1", (coilEnergy?.material?.unlocalizedName ?: "").translatable),
                Component.translatable("gui.gtladditions.planetary_ionisation_convergence_tower_2", this.stellarTier),
                Component.translatable("gui.gtladditions.planetary_ionisation_convergence_tower_3", FormattingUtil.formatNumbers(this.storageEUt), "EU")
            )
            .addRecipeStatus(recipeLogic as IRecipeStatus)
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = PICTRecipeLogic(this)

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    override fun doExplosion(pos: BlockPos, explosionPower: Float) {
        if (fired) return

        val currentLevel = level as? ServerLevel ?: return
        val radius = explosionPower.toInt()
        if (radius <= 0) return

        fired = true
        storageEUt = 0L
        startCycle = false
        isWorkingEnabled = false

        currentLevel.removeBlock(this.pos, false)
        GtlRegionExplosionFacade.submitSphere(currentLevel, MACHINE_ACTOR, pos, radius)
    }

    private fun addStorageEUt(start: Boolean): Long {
        if (fired) return 0L

        val addEUt = if (start) coilEnergy?.instantPower ?: 0 else coilEnergy?.dischargePower ?: 0
        if (storageEUt > maxStorageEUt || addEUt > maxStorageEUt - storageEUt) {
            doExplosion(pos, 80f)
            return 0L
        }

        storageEUt += addEUt
        return addEUt
    }

    private fun renderParticles() {
        val targetPos = particlePos ?: return
        val serverLevel = level as? ServerLevel ?: return
        serverLevel.sendParticles(
            ParticleTypes.LIGHTNING,
            targetPos.x.toDouble(),
            targetPos.y.toDouble(),
            targetPos.z.toDouble(),
            200,
            4.0,
            4.0,
            4.0,
            0.01
        )
    }

    enum class CoilToEnergy(
        val material: Material,
        val instantPower: Long,
        val dischargePower: Long,
        val workTier: Int
    ) {
        TITAN_STEEL(TitanSteel, 0x7fffffff000, 0x7fffffff0, 1),
        ADAMANTINE(Adamantine, 0x3fffffff8000, 0x3fffffff80, 1),
        NAQUADRIATIC_TARANIUM(NaquadriaticTaranium, 0x3fffffff80000, 0x7fffffff00, 2),
        STAR_METAL(Starmetal, 0x1ffffffffc0000, 0x3fffffff800, 2),
        INFINITY(Infinity, 0x3fffffff800000, 0x7fffffff000, 3),
        HYPOGEN(Hypogen, 0x1ffffffffc00000, 0x3fffffff8000, 3),
        ETERNITY(Eternity, 0x7fffffff0000000, 0xfffffffe0000, 3)
    }

    companion object {
        private val MACHINE_ACTOR = UUID(0L, 0L)
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            PlanetaryIonisationConvergenceTower::class.java,
            StorageMachine.MANAGED_FIELD_HOLDER
        )

        private const val DURATION = 60

        private val SPACE_DRONE_MK2 = Registries.getItem("kubejs:space_drone_mk2")
        private val SPACE_DRONE_MK4 = Registries.getItem("kubejs:space_drone_mk4")
        private val SPACE_DRONE_MK6 = Registries.getItem("kubejs:space_drone_mk6")

        private val RHENIUM = Rhenium.getFluid(73_728)
        private val ICE = Ice.getFluid(8_000_000)
        private val PROMETHIUM = Promethium.getFluid(36_864)
        private val HELIUM = Helium.getFluid(FluidStorageKeys.LIQUID, 4_000_000)
        private val CRYSTALMATRIX = Crystalmatrix.getFluid(9_216)
        private val CRYOTHEUM = FluidStack.create(Registries.getFluid("kubejs:gelid_cryotheum"), 1_000_000)

        private fun findCoil(material: Material?): CoilToEnergy? =
            enumEntries<CoilToEnergy>().find { it.material == material }

        class PICTRecipeLogic(val pictMachine: PlanetaryIonisationConvergenceTower) :
            RecipeLogic(pictMachine),
            IRecipeStatus {

            override fun loadCustomPersistedData(tag: CompoundTag) {
                super.loadCustomPersistedData(tag)
                val restored = lastRecipe
                // The recipe codec supplies Map.of() when a saved cycle had no power output.
                if (restored != null && restored.tickOutputs.isEmpty()) lastRecipe = restored.copy()
            }

            override fun setStatus(status: Status) {
                if (status == Status.SUSPEND && !pictMachine.startCycle) lastRecipe = null
                super.setStatus(status)
            }

            override fun serverTick() {
                if (pictMachine.fired) {
                    lastRecipe = null
                    status = Status.IDLE
                    progress = 0
                    subscription?.unsubscribe()
                    subscription = null
                    return
                }

                if (!isSuspend) {
                    if (progress < DURATION) {
                        handleRecipeWorking()
                    }
                    if (progress >= DURATION) {
                        pictMachine.startCycle = false
                        progress = 0
                    }
                } else if (subscription != null) {
                    subscription.unsubscribe()
                    subscription = null
                }
            }

            override fun handleRecipeWorking() {
                workingStatus = null
                pictMachine.renderParticles()
                if (progress == 0) {
                    pictMachine.startCycle = when (pictMachine.coilEnergy?.workTier) {
                        1 -> startCycle(RHENIUM, ICE, SPACE_DRONE_MK2, 10_000)
                        2 -> startCycle(PROMETHIUM, HELIUM, SPACE_DRONE_MK4, 20_000)
                        3 -> startCycle(CRYSTALMATRIX, CRYOTHEUM, SPACE_DRONE_MK6, 100_000)
                        else -> false
                    }
                }

                if (pictMachine.startCycle) {
                    val generatedEUt = pictMachine.addStorageEUt(progress == 0)
                    if (pictMachine.fired) {
                        lastRecipe = null
                        status = Status.IDLE
                        progress = 0
                        return
                    }

                    val energyContainer = pictMachine.energyContainer
                    val actualEUt = energyContainer.addEnergy(min(energyContainer.energyCanBeInserted, generatedEUt))
                    pictMachine.storageEUt -= actualEUt

                    lastRecipe?.let { cycle ->
                        if (actualEUt > 0) {
                            val output = cycle.tickOutputs.computeIfAbsent(EURecipeCapability.CAP) {
                                val chance = ChanceLogic.getMaxChancedValue()
                                listOf(Content(actualEUt, chance, chance, 0, null, null))
                            }
                            output[0].content = actualEUt
                        } else {
                            cycle.tickOutputs.remove(EURecipeCapability.CAP)
                        }
                    }

                    this.status = Status.WORKING
                    ++this.progress
                } else {
                    lastRecipe = null
                    this.setWaiting(null)
                }

                if (this.status == Status.WAITING) {
                    this.doDamping()
                }
            }

            @OnlyIn(Dist.CLIENT)
            override fun updateSound() {
                if (isWorking && pictMachine.shouldWorkingPlaySound()) {
                    val sound = GTSoundEntries.ARC
                    if (workingSound is AutoReleasedSound) {
                        val soundEntry = workingSound as AutoReleasedSound
                        if (soundEntry.soundEntry == sound && !soundEntry.isStopped) {
                            return
                        }
                        soundEntry.release()
                        workingSound = null
                    }
                    if (sound != null) {
                        workingSound = sound.playAutoReleasedSound(
                            {
                                pictMachine.shouldWorkingPlaySound() &&
                                    isWorking &&
                                    !pictMachine.isInValid &&
                                    pictMachine.level!!.isLoaded(pictMachine.pos) &&
                                    getMachine(
                                        pictMachine.level!!,
                                        pictMachine.pos
                                    ) == pictMachine
                            },
                            pictMachine.pos,
                            true,
                            0,
                            1f,
                            1f
                        )
                    }
                } else if (workingSound is AutoReleasedSound) {
                    (workingSound as AutoReleasedSound).release()
                    workingSound = null
                }
            }

            private fun startCycle(first: FluidStack, second: FluidStack, drone: Item, droneInterval: Int): Boolean {
                val nextCycle = pictMachine.cycleAmount + 1
                val needsDrone = pictMachine.cycleAmount == 0 || nextCycle % droneInterval == 0

                if (needsDrone && !pictMachine.machineStorageItem.`is`(drone)) {
                    workingStatus = RecipeResult.fail(
                        "gui.gtladditions.planetary_ionisation_convergence_tower_4".toComponent
                            .append(drone.descriptionId.toComponent)
                    )
                    return false
                }
                if (!input(pictMachine).fluid(first).fluid(second).execute()) return false

                val cycle = GTRecipeBuilder.ofRaw()
                    .duration(DURATION)
                    .inputFluids(FluidIngredient.of(first), FluidIngredient.of(second))
                if (needsDrone) {
                    val consumed = pictMachine.machineStorage.extractItemInternal(0, 1, false)
                    if (!consumed.isEmpty) cycle.inputItems(consumed)
                }

                pictMachine.cycleAmount = nextCycle
                lastRecipe = cycle.buildRawRecipe()
                duration = DURATION

                return true
            }
        }
    }
}
