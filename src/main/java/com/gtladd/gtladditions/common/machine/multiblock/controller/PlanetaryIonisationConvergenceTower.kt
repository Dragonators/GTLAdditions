package com.gtladd.gtladditions.common.machine.multiblock.controller

import appeng.client.render.effects.ParticleTypes
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IExplosionMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.sound.AutoReleasedSound
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTSoundEntries
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.translatable
import com.gtladd.gtladditions.utils.MachineUtil.inputFluid
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.api.machine.trait.ICheckPatternMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine
import org.gtlcore.gtlcore.utils.Registries
import kotlin.enums.enumEntries
import kotlin.math.min

class PlanetaryIonisationConvergenceTower(holder: IMachineBlockEntity) :
    StorageMachine(holder, 64),
    IExplosionMachine {

    @Persisted
    private var storageEUt = 0L

    @Persisted
    private var cycleAmount = 0

    @Persisted
    private var startCycle = false

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
            1 -> 0x3138cb601000
            2 -> 0xc587e7c983000
            3 -> 0x101925daa3740000
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
        ICheckPatternMachine.attachConfigurators(configuratorPanel, this)
    }

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
                Component.translatable("gui.gtladditions.planetary_ionisation_convergence_tower_3", FormattingUtil.formatNumbers(this.storageEUt))
            )
            .addRecipeStatus(recipeLogic as IRecipeStatus)
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = PICTRecipeLogic(this)

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    override fun doExplosion(pos: BlockPos, explosionPower: Float) {
        level?.let {
            it.removeBlock(this.pos, false)
            it.explode(
                null,
                pos.x + 0.5,
                pos.y + 0.5,
                pos.z + 0.5,
                explosionPower,
                Level.ExplosionInteraction.BLOCK
            )
        }
    }

    private fun addStorageEUt(start: Boolean): Long {
        val addEUt = if (start) coilEnergy?.instantPower ?: 0 else coilEnergy?.dischargePower ?: 0
        storageEUt += addEUt
        if (storageEUt > maxStorageEUt) {
            doExplosion(pos, 500f)
        }
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
        TITAN_STEEL(TitanSteel, 0x7fffffff00, 0x7fffffff, 1),
        ADAMANTINE(Adamantine, 0x7fffffff000, 0x7fffffff0, 1),
        NAQUADRIATIC_TARANIUM(NaquadriaticTaranium, 0x3fffffff8000, 0x7fffffff0, 2),
        STAR_METAL(Starmetal, 0x3fffffff80000, 0x7fffffff00, 2),
        INFINITY(Infinity, 0x7fffffff00000, 0xfffffffe00, 3),
        HYPOGEN(Hypogen, 0x7fffffff000000, 0xfffffffe000, 3),
        ETERNITY(Eternity, 0x7fffffff0000000, 0xfffffffe0000, 3)
    }

    companion object {
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

            init {
                duration = DURATION
            }

            override fun serverTick() {
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
                    val droneResult = DroneResult(false, "")
                    val nextCycle = pictMachine.cycleAmount + 1
                    pictMachine.coilEnergy?.let {
                        when (it.workTier) {
                            1 -> {
                                when {
                                    (nextCycle % 10000 == 0 || pictMachine.cycleAmount == 0) && !pictMachine.machineStorageItem.`is`(SPACE_DRONE_MK2) -> {
                                        pictMachine.startCycle = false
                                        droneResult.isDrone = false
                                        droneResult.tier = SPACE_DRONE_MK2.descriptionId
                                    }
                                    else -> {
                                        pictMachine.startCycle =
                                            inputFluid(pictMachine, RHENIUM) &&
                                            inputFluid(pictMachine, ICE)
                                        if (pictMachine.startCycle) {
                                            if (nextCycle % 10000 == 0 || pictMachine.cycleAmount == 0) {
                                                pictMachine.machineStorage.extractItemInternal(0, 1, false)
                                                droneResult.isDrone
                                                droneResult.tier = SPACE_DRONE_MK2.descriptionId
                                            }
                                            pictMachine.cycleAmount = nextCycle
                                        }
                                    }
                                }
                            }
                            2 -> {
                                when {
                                    (nextCycle % 20000 == 0 || pictMachine.cycleAmount == 0) && !pictMachine.machineStorageItem.`is`(SPACE_DRONE_MK4) -> {
                                        pictMachine.startCycle = false
                                        droneResult.isDrone = false
                                        droneResult.tier = SPACE_DRONE_MK4.descriptionId
                                    }
                                    else -> {
                                        pictMachine.startCycle =
                                            inputFluid(pictMachine, PROMETHIUM) &&
                                            inputFluid(pictMachine, HELIUM)
                                        if (pictMachine.startCycle) {
                                            if (nextCycle % 20000 == 0 || pictMachine.cycleAmount == 0) {
                                                pictMachine.machineStorage.extractItemInternal(0, 1, false)
                                                droneResult.isDrone
                                                droneResult.tier = SPACE_DRONE_MK4.descriptionId
                                            }
                                            pictMachine.cycleAmount = nextCycle
                                        }
                                    }
                                }
                            }
                            3 -> {
                                when {
                                    (nextCycle % 100000 == 0 || pictMachine.cycleAmount == 0) && !pictMachine.machineStorageItem.`is`(SPACE_DRONE_MK6) -> {
                                        pictMachine.startCycle = false
                                        droneResult.isDrone = false
                                        droneResult.tier = SPACE_DRONE_MK6.descriptionId
                                    }
                                    else -> {
                                        pictMachine.startCycle =
                                            inputFluid(pictMachine, CRYSTALMATRIX) &&
                                            inputFluid(pictMachine, CRYOTHEUM)
                                        if (pictMachine.startCycle) {
                                            if (nextCycle % 100000 == 0 || pictMachine.cycleAmount == 0) {
                                                pictMachine.machineStorage.extractItemInternal(0, 1, false)
                                                droneResult.isDrone
                                                droneResult.tier = SPACE_DRONE_MK6.descriptionId
                                            }
                                            pictMachine.cycleAmount = nextCycle
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!droneResult.isDrone && !droneResult.tier.isEmpty()) {
                        workingStatus = RecipeResult.fail("gui.gtladditions.planetary_ionisation_convergence_tower_4".toComponent.append(droneResult.tier.toComponent))
                    }
                }
                if (pictMachine.startCycle) {
                    val actualEUt = min(
                        pictMachine.energyContainer.energyCanBeInserted,
                        pictMachine.addStorageEUt(progress == 0)
                    )
                    pictMachine.energyContainer.addEnergy(actualEUt)
                    pictMachine.storageEUt -= actualEUt
                    this.status = Status.WORKING
                    ++this.progress
                } else {
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

            private data class DroneResult(var isDrone: Boolean, var tier: String)
        }
    }
}