package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.utils.CommonUtils
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.common.machine.trait.MolecularAssemblerRecipesLogic
import org.gtlcore.gtlcore.mixin.gtm.api.recipe.RecipeLogicAccessor
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo

class MolecularAssemblerMultiblockMachine(holder: IMachineBlockEntity) :
    org.gtlcore.gtlcore.api.machine.multiblock.MolecularAssemblerMultiblockMachine(holder),
    IModularMachineHost<MolecularAssemblerMultiblockMachine>,
    IMachineLife {

    private var mam = 0
    private val modules: Set<IModularMachineModule<MolecularAssemblerMultiblockMachine, *>> =
        ReferenceOpenHashSet<IModularMachineModule<MolecularAssemblerMultiblockMachine, *>>()
    var isInfinityMode = false

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun createRecipeLogic(vararg args: Any?): RecipeLogic? {
        return object : MolecularAssemblerRecipesLogic(this) {
            override fun findAndHandleRecipe() {
                if (isInfinityMode) {
                    lastRecipe = null
                    val match = getMaxRecipe()
                    if (match != null) {
                        setupRecipe(match)
                    }

                    return
                } else {
                    super.findAndHandleRecipe()
                }
            }

            override fun onRecipeFinish() {
                if (!isInfinityMode) {
                    super.onRecipeFinish()
                    return
                }
                lastRecipe?.let { getMachine().maHandler?.handleRecipeOutput(it) }

                val suspendableMachine = machine as? ISuspendableMachine
                if (suspendableMachine?.`gtlcore$isSuspendAfterFinish`() == true) {
                    status = Status.SUSPEND
                    suspendableMachine.`gtlcore$setSuspendAfterFinish`(false)
                } else {
                    getMaxRecipe()?.let {
                        setupRecipe(it)
                        return
                    }
                    lastRecipe = null
                    this.status = Status.IDLE
                    (this as RecipeLogicAccessor).setIsActive(false)
                }
                this.progress = 0
                this.duration = 0
            }

            private fun getMaxRecipe(): GTRecipe? = getMachine().maHandler?.extractGTRecipe(
                Long.MIN_VALUE,
                getMachine().getTickDuration()
            )
        }
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<MolecularAssemblerMultiblockMachine, *>> = modules
    override fun getModuleScanPositions(): Array<out BlockPos> = arrayOf(
        pos.offset(0, 2, -7),
        pos.offset(0, 2, 7),
        pos.offset(-7, 2, 0),
        pos.offset(7, 2, 0)
    )
    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
        ModuleRenderInfo(
            BlockPos(0, 2, -7),
            Direction.NORTH,
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            MultiBlockMachine.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY
        )
    )

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        safeClearModules()
    }

    override fun onMachineRemoved() {
        safeClearModules()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        safeClearModules()
        scanAndConnectModules()
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        if (isFormed()) {
            textList.add(
                recipeType.registryName.toLanguageKey().toComponent
                    .setStyle(
                        Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    "gtceu.gui.machinemode.title".toComponent
                                )
                            )
                    )
            )
            if (!isWorkingEnabled) {
                textList.add("gtceu.multiblock.work_paused".toComponent)
            } else if (isActive) {
                textList.add("gtceu.multiblock.running".toComponent)
                val currentProgress = (recipeLogic.progressPercent * 100).toInt()
                textList.add("gtceu.multiblock.progress".toComponent(currentProgress))
            } else {
                textList.add("gtceu.multiblock.idling".toComponent)
            }
            if (recipeLogic.isWaiting) {
                textList.add(
                    "gtceu.multiblock.waiting".toComponent
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))
                )
            }
            if (isInfinityMode) {
                textList.add(
                    "gtceu.multiblock.parallel".toComponent(
                        CommonUtils.createLanguageRainbowComponentOnServer(
                            "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                        )
                    ).withStyle(ChatFormatting.GRAY)
                )
            } else if (maxParallel > 1) {
                textList.add(
                    "gtceu.multiblock.parallel".toComponent(
                        FormattingUtil.formatNumbers(maxParallel).literal
                            .withStyle(ChatFormatting.DARK_PURPLE)
                    ).withStyle(ChatFormatting.GRAY)
                )
            }
            textList.add(
                "gtlcore.multiblock.tick_Duration".toComponent(
                    (FormattingUtil.formatNumbers(if (isInfinityMode) 1 else tickDuration)).literal
                        .withStyle(ChatFormatting.BLUE)
                )
                    .withStyle(ChatFormatting.GRAY)
            )
            textList.add(
                "gtlcore.multiblock.contains_Patttern".toComponent(FormattingUtil.formatNumbers(patternSize).literal.withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.GRAY)
            )
            textList.add("tooltip.gtlcore.installed_module_count".toComponent(getMAM()))
        } else {
            val tooltip: Component = "gtceu.multiblock.invalid_structure.tooltip".toComponent
                .withStyle(ChatFormatting.GRAY)
            textList.add(
                "gtceu.multiblock.invalid_structure".toComponent
                    .withStyle(
                        Style.EMPTY.withColor(ChatFormatting.RED)
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))
                    )
            )
        }
        definition.additionalDisplay.accept(this, textList)
    }

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
    }
}