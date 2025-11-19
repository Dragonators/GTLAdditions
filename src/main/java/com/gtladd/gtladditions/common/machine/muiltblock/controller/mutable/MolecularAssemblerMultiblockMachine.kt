package com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.common.machine.trait.MolecularAssemblerRecipesLogic
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo

class MolecularAssemblerMultiblockMachine(holder: IMachineBlockEntity) :
    org.gtlcore.gtlcore.api.machine.multiblock.MolecularAssemblerMultiblockMachine(holder),
    IModularMachineHost<MolecularAssemblerMultiblockMachine>, IMachineLife {

    private var mam = 0
    private val modules: Set<IModularMachineModule<MolecularAssemblerMultiblockMachine, *>> =
        ReferenceOpenHashSet<IModularMachineModule<MolecularAssemblerMultiblockMachine, *>>()
    var isInfinityMode = false

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
                } else super.findAndHandleRecipe()
            }

            private fun getMaxRecipe(): GTRecipe? {
                return getMachine().maHandler?.extractGTRecipe(
                    Long.MIN_VALUE,
                    getMachine().getTickDuration()
                )
            }
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
    override fun getModulesForRendering(): List<ModuleRenderInfo> {
        return listOf(
            ModuleRenderInfo(
                BlockPos(0, 2, -7),
                Direction.NORTH,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY
            )
        )
    }

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
                Component.translatable(recipeType.registryName.toLanguageKey())
                    .setStyle(
                        Style.EMPTY.withColor(ChatFormatting.AQUA)
                            .withHoverEvent(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("gtceu.gui.machinemode.title")
                                )
                            )
                    )
            )
            if (!isWorkingEnabled) {
                textList.add(Component.translatable("gtceu.multiblock.work_paused"))
            } else if (isActive) {
                textList.add(Component.translatable("gtceu.multiblock.running"))
                val currentProgress = (recipeLogic.progressPercent * 100).toInt()
                textList.add(Component.translatable("gtceu.multiblock.progress", currentProgress))
            } else {
                textList.add(Component.translatable("gtceu.multiblock.idling"))
            }
            if (recipeLogic.isWaiting) {
                textList.add(
                    Component.translatable("gtceu.multiblock.waiting")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))
                )
            }
            if (isInfinityMode){
                textList.add(
                    Component.translatable(
                        "gtceu.multiblock.parallel",
                        GTLAddMachines.createRainbowComponent(
                            Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                        )
                    ).withStyle(ChatFormatting.GRAY)
                )
            } else if (maxParallel > 1) {
                textList.add(
                    Component.translatable(
                        "gtceu.multiblock.parallel",
                        Component.literal(FormattingUtil.formatNumbers(maxParallel))
                            .withStyle(ChatFormatting.DARK_PURPLE)
                    ).withStyle(ChatFormatting.GRAY)
                )
            }
            textList.add(
                Component.translatable(
                    "gtlcore.multiblock.tick_Duration",
                    Component.literal(FormattingUtil.formatNumbers(if (isInfinityMode) 1 else tickDuration))
                        .withStyle(ChatFormatting.BLUE)
                )
                    .withStyle(ChatFormatting.GRAY)
            )
            textList.add(
                Component.translatable(
                    "gtlcore.multiblock.contains_Patttern",
                    Component.literal(FormattingUtil.formatNumbers(patternSize)).withStyle(ChatFormatting.GOLD)
                )
                    .withStyle(ChatFormatting.GRAY)
            )
            textList.add(Component.translatable("tooltip.gtlcore.installed_module_count", getMAM()))
        } else {
            val tooltip: Component = Component.translatable("gtceu.multiblock.invalid_structure.tooltip")
                .withStyle(ChatFormatting.GRAY)
            textList.add(
                Component.translatable("gtceu.multiblock.invalid_structure")
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