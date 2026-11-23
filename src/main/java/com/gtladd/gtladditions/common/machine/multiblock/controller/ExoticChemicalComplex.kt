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
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.data.recipe.CustomTags
import com.gregtechceu.gtceu.utils.GTUtil
import com.gtladd.gtladditions.api.machine.ITimeSpaceDistorterSource
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.api.machine.feature.TierCost
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
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.utils.Registries.getItem

class ExoticChemicalComplex(holder: IMachineBlockEntity) :
    GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine(holder),
    IStructureTierMachine,
    IDropSaveMachine,
    ISuspendableMachine,
    ITimeSpaceDistorterSource {

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

    @field:Persisted
    override var timeSpaceDistorterPos: BlockPos? = null

    override var timeSpaceDistorter: TimeSpaceDistorter? = null

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

    override fun tierRequirement(tier: Int): TierRequirement? = REQUIREMENTS.getOrNull(tier - 2)

    fun stockingPeers(part: IMultiPart): List<IMultiPart> {
        val owner = unitOwner(part) ?: return emptyList()
        return getRecipeLogic().partsOf(owner) ?: parts.filter { unitOwner(it) == owner }
    }

    fun getUnitRecipeType(part: IMultiPart): GTRecipeType? {
        if (!isRecipeLogicAvailable) return null

        val id = unitOwner(part) ?: return null
        val logic = getRecipeLogic()
        if (!logic.isUnitUnlocked(id)) return null

        return logic.units[id].recipeType
    }

    private fun unitOwner(part: IMultiPart): Int? = ownerMap[physicalPos(part).asLong()]

    private fun physicalPos(part: IMultiPart): BlockPos = (part as? MetaMachine)?.pos ?: part.self().pos

    private fun publishStructure() {
        (multiblockState.matchContext["CoilType"] as? ICoilType)?.let { coilType = it }

        val groups = Array(13) { ArrayList<IMultiPart>() }
        for (part in parts) {
            val owner = unitOwner(part) ?: continue
            groups[owner].add(part)
        }

        getRecipeLogic().bind(Array(13) { groups[it].sortedBy { part -> physicalPos(part).asLong() } })
    }

    // ========================================
    // WIRELESS BINDING
    // ========================================

    override val bindingType: ResourceLocation = BINDING_TYPE

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

        if (isFormed) {
            structureTierTrait.onStructureFormed()
            reconnectTarget()
        }
    }

    override fun onStructureInvalid() {
        // Both unbind and the parent reset read the tier.
        isFormed = false
        structureTierTrait.onStructureInvalid()
        getRecipeLogic().unbind()
        ownerMap = emptyMap()
        maintenance = null

        super.onStructureInvalid()
        detachTarget()
    }

    override fun onUnload() {
        super.onUnload()
        detachTarget()
    }

    override fun onPartUnload() {
        maintenance = null
        getRecipeLogic().unbind()
        super.onPartUnload()
    }

    override fun onMachineRemoved() {
        structureTierTrait.onMachineRemoved()
        clearInventory(structureTierTrait.inventory)
        unbindTarget()
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
        val BINDING_TYPE = ResourceLocation("gtladditions", "exotic_chemical_complex")
        const val TSD_COST_COEFFICIENT = 0.1

        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            ExoticChemicalComplex::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )

        private fun cost(id: String, amount: Long) = TierCost({ Ingredient.of(getItem(id)) }, amount)

        private fun cost(tag: TagKey<Item>, amount: Long) = TierCost({ Ingredient.of(tag) }, amount)

        private val REQUIREMENTS = listOf(
            TierRequirement(
                listOf(
                    cost("gtceu:large_chemical_plant", 64),
                    cost("gtceu:dimensionally_transcendent_mixer", 16),
                    cost("gtladditions:me_super_pattern_buffer", 12),
                    cost("gtladditions:super_input_dual_hatch", 12),
                    cost("gtceu:uxv_parallel_hatch", 8),
                    cost("gtlcore:insanely_max_battery", 32),
                    cost(CustomTags.OpV_CIRCUITS, 64),
                    cost("gtceu:uxv_electric_pump", 32),
                    cost("gtceu:uxv_fluid_regulator", 32),
                    cost("gtceu:uxv_field_generator", 16),
                    cost("kubejs:quantumchromodynamic_protective_plating", 32),
                    cost("gtceu:draconium_nanoswarm", 32),
                    cost("kubejs:awakened_core", 12),
                    cost("kubejs:recursively_folded_negative_space", 12)
                )
            ),
            TierRequirement(
                listOf(
                    cost("gtceu:chemical_distort", 256),
                    cost("gtceu:qft", 64),
                    cost("gtladditions:arcanic_astrograph", 4),
                    cost("gtladditions:thread_modifier_hatch", 12),
                    cost("gtladditions:super_parallel_hatch", 12),
                    cost("gtladditions:infinity_input_dual_hatch", 12),
                    cost("gtlcore:max_electric_pump", 64),
                    cost("gtlcore:max_fluid_regulator", 64),
                    cost("gtlcore:max_field_generator", 32),
                    cost("kubejs:create_ultimate_battery", 16),
                    cost(CustomTags.MAX_CIRCUITS, 64),
                    cost("gtladditions:primary_soc", 128),
                    cost("gtladditions:astral_array", 32),
                    cost("gtladditions:spatially_transcendent_gravitational_lens", 12),
                    cost("gtceu:transcendentmetal_nanoswarm", 64)
                )
            )
        )
    }
}