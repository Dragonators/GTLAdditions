package com.gtladd.gtladditions.common.machine.muiltblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.muiltblock.controller.LightHunterSpaceStation
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.LightHunterSpaceStationPosHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule

class LightHunterSpaceStationModuleBase(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder),
    IModularMachineModule<LightHunterSpaceStation, LightHunterSpaceStationModuleBase>,
    IMachineLife {
    @field:Persisted
    private var hostPosition: BlockPos? = null
    private var host: LightHunterSpaceStation? = null

    override fun getHostPosition(): BlockPos? = hostPosition
    override fun setHostPosition(pos: BlockPos?) { hostPosition = pos }
    override fun getHost(): LightHunterSpaceStation? = host
    override fun setHost(host: LightHunterSpaceStation?) { this.host = host }
    override fun getHostType(): Class<LightHunterSpaceStation> = LightHunterSpaceStation::class.java
    override fun getHostScanPositions(): Array<BlockPos> = LightHunterSpaceStationPosHelper.calculatePossibleHostPositions(pos)

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return LightHunterSpaceStationModuleBaseLogic(this)
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!this.isFormed) return
        textList.add(
            Component.translatable(
                if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed"
            )
        )
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        if (isConnectedToHost && host!!.unlockParadoxical()) {
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.parallel",
                    createRainbowComponent(
                        Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
            textList.add(
                Component.translatable(
                    "gtladditions.multiblock.threads",
                    createRainbowComponent(
                        Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
        } else super.addParallelDisplay(textList)
    }

    // ===============================================
    // LightHunterSpaceStation connection
    // ===============================================

    override fun onConnected(host: LightHunterSpaceStation) {
        recipeLogic.updateTickSubscription()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        if (!findAndConnectToHost()) {
            removeFromHost(this.host)
        }
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        removeFromHost(this.host)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(this.host)
    }

    override fun onMachineRemoved() {
        removeFromHost(this.host)
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            LightHunterSpaceStationModuleBase::class.java,
            GTLAddWorkableElectricMultipleRecipesMachine.Companion.MANAGED_FIELD_HOLDER
        )

        val BEFORE_WORKING = Predicate { machine: IRecipeLogicMachine ->
            (machine as LightHunterSpaceStationModuleBase).host?.let { it -> return@let it.isActive } ?: false
        }

        open class LightHunterSpaceStationModuleBaseLogic(
            parallel: LightHunterSpaceStationModuleBase
        ) : GTLAddMultipleRecipesLogic(parallel, BEFORE_WORKING){

            override fun getMachine(): LightHunterSpaceStationModuleBase = machine as LightHunterSpaceStationModuleBase

            override fun calculateParallels(): ParallelData? {
                if (!getMachine().host!!.unlockParadoxical()) {
                    return super.calculateParallels()
                }

                val recipes = lookupRecipeIterator()
                if (recipes.isEmpty()) return null

                val recipeList = ObjectArrayList<GTRecipe>(recipes.size)
                val parallelsList = LongArrayList(recipes.size)

                for (recipe in recipes) {
                    val parallel = getMaxParallel(recipe, Long.MAX_VALUE)
                    if (parallel > 0) {
                        recipeList.add(recipe)
                        parallelsList.add(parallel)
                    }
                }

                return if (recipeList.isEmpty()) null
                else ParallelData(recipeList, parallelsList.toLongArray())
            }
        }
    }
}