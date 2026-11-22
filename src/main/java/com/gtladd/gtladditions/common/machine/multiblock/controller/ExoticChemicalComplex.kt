package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.utils.GTUtil
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.api.machine.feature.TierRequirement
import com.gtladd.gtladditions.api.machine.gui.ExoticChemicalMainPage
import com.gtladd.gtladditions.api.machine.gui.StructureTierConfigurator
import com.gtladd.gtladditions.api.machine.logic.chemical.ExoticChemicalRecipeLogic
import com.gtladd.gtladditions.api.machine.logic.chemical.ExoticChemicalUnitLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.ExoticChemicalBlockLayout
import com.gtladd.gtladditions.common.machine.trait.StructureTierTrait
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.longs.LongSets
import net.minecraft.core.BlockPos
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine

class ExoticChemicalComplex(holder: IMachineBlockEntity) :
    GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine(holder),
    IStructureTierMachine,
    IDropSaveMachine,
    ISuspendableMachine {

    @field:Persisted
    @field:DescSynced
    override val structureTierTrait: StructureTierTrait = StructureTierTrait(
        this,
        ExoticChemicalBlockLayout.tiers,
        onTierSynced = { publishStructure() }
    )

    val structureTier: Int
        get() = structureTierTrait.structureTier

    private var ownerMap: Map<Long, Int> = emptyMap()
    private var maintenance: IMaintenanceMachine? = null

    // ========================================
    // CORE OVERRIDES
    // ========================================

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = ExoticChemicalRecipeLogic(this)

    override fun getRecipeLogic(): ExoticChemicalRecipeLogic = super.getRecipeLogic() as ExoticChemicalRecipeLogic

    // Unit logics are nested in the coordinator's save. Registering them as peer traits would share GT's NBT keys.
    override fun attachTraits(trait: MachineTrait) {
        if (trait !is ExoticChemicalUnitLogic) super.attachTraits(trait)
    }

    override fun onWirelessNetworkBound() = getRecipeLogic().updateTickSubscription()

    override fun `gtlcore$isSuspendAfterFinish`(): Boolean = getRecipeLogic().allSuspendAfterFinish

    override fun `gtlcore$setSuspendAfterFinish`(value: Boolean) = getRecipeLogic().setAllSuspendAfterFinish(value)

    // GTLCore adds these virtual methods to WorkableMultiblockMachine at runtime.
    fun getMaintenanceMachine(): IMaintenanceMachine? = maintenance

    fun upDate() = getRecipeLogic().refreshUnits()

    fun sortMEOutput() = getRecipeLogic().sortOutputs()

    // ========================================
    // STRUCTURE & TIERS
    // ========================================

    override fun checkPattern(): Boolean = structureTierTrait.checkPattern()

    override fun tierRequirement(tier: Int): TierRequirement? = if (tier in 2..3) TierRequirement(emptyList()) else null

    fun stockingPeers(part: IMultiPart): List<IMultiPart> {
        val owner = ownerMap[physicalPos(part).asLong()] ?: return emptyList()
        return getRecipeLogic().partsOf(owner) ?: parts.filter { ownerMap[physicalPos(it).asLong()] == owner }
    }

    private fun physicalPos(part: IMultiPart): BlockPos = (part as? MetaMachine)?.pos ?: part.self().pos

    private fun publishStructure() {
        (multiblockState.matchContext["CoilType"] as? ICoilType)?.let { coilType = it }

        val groups = Array(13) { ArrayList<IMultiPart>() }
        for (part in parts) {
            val owner = ownerMap[physicalPos(part).asLong()] ?: continue
            groups[owner].add(part)
        }

        getRecipeLogic().bind(Array(13) { groups[it].sortedBy { part -> physicalPos(part).asLong() } })
    }

    // ========================================
    // LIFECYCLE
    // ========================================

    // ExoticChemicalUnitMachine binds each unit. Calling super would subscribe the host and schedule GTLCore's extra upDate.
    override fun onStructureFormed() {
        ownerMap = ExoticChemicalBlockLayout.portOwners.entries.associate { (cell, id) ->
            ExoticChemicalBlockLayout.tiers.position(cell, this).asLong() to id
        }

        // A mirror resolving its buffer in updatePartPositions can call upDate before the new groups are bound.
        getRecipeLogic().clearUnitParts()

        isFormed = true
        val context = multiblockState.matchContext

        // getParts() reloads the old positions if called while the list and position counts differ.
        val formedParts = parts
        formedParts.clear()
        context.getOrCreate<Set<IMultiPart>>("parts", ::emptySet).filterTo(formedParts, ::shouldAddPartToController)
        formedParts.sortWith(definition.partSorter)
        formedParts.forEach { it.addedToController(this) }
        updatePartPositions()

        activeBlocks = context.getOrDefault("vaBlocks", LongSets.EMPTY_SET)
        energyContainer = getEnergyContainer()
        tier = GTUtil.getFloorTierByVoltage(maxVoltage).toInt()
        refreshTier()
        (context["CoilType"] as? ICoilType)?.let { coilType = it }
        maintenance = formedParts.firstNotNullOfOrNull { it as? IMaintenanceMachine }

        if (isFormed) structureTierTrait.onStructureFormed()
    }

    override fun onStructureInvalid() {
        // Both unbind and the parent reset read the tier.
        isFormed = false
        structureTierTrait.onStructureInvalid()
        getRecipeLogic().unbind()
        ownerMap = emptyMap()
        maintenance = null
        super.onStructureInvalid()
    }

    override fun onPartUnload() {
        maintenance = null
        getRecipeLogic().unbind()
        super.onPartUnload()
    }

    override fun onMachineRemoved() {
        structureTierTrait.onMachineRemoved()
        clearInventory(structureTierTrait.inventory)
    }

    // ========================================
    // PERSISTENCE
    // ========================================

    override fun saveBreak(): Boolean = structureTierTrait.hasDropData

    override fun savePickClone(): Boolean = false

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    // ========================================
    // GUI SYSTEM
    // ========================================

    override fun createMainPage(widget: FancyMachineUIWidget): ExoticChemicalMainPage = ExoticChemicalMainPage(this)

    override fun hasPlayerInventory(): Boolean = false

    // Rebuilt instead of calling super, which also adds GT's recipe lock button; units keep no locked recipe.
    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        configuratorPanel.attachConfigurators(
            IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.0, 1.0, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.5, 1.0, 0.5),
                { isWorkingEnabled },
                { _, pressed -> isWorkingEnabled = pressed }
            ).setTooltipsSupplier { enabled ->
                listOf((if (enabled) "behaviour.soft_hammer.enabled" else "behaviour.soft_hammer.disabled").toComponent)
            }
        )
        IRecipeCapabilityMachine.attachConfigurators(configuratorPanel, this)
        createConfigurators()?.let { configuratorPanel.attachConfigurators(it) }
        configuratorPanel.attachConfigurators(StructureTierConfigurator(this))
    }

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            ExoticChemicalComplex::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )
    }
}