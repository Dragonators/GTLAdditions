package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.LightningRodBlock
import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine

class WeatherControlMachine(holder: IMachineBlockEntity) : StorageMachine(holder, 1) {
    private var rodCache: List<BlockPos> = emptyList()
    private var lastScanTick = Long.MIN_VALUE

    override fun createRecipeLogic(vararg args: Any?): RecipeLogic = WeatherControlLogic(this)

    override fun filter(stack: ItemStack): Boolean = fieldTier(stack) >= 0

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        rodCache = emptyList()
        lastScanTick = Long.MIN_VALUE
    }

    override fun afterWorking() {
        // Core also invokes this callback on interruption. Mode 4 weather is committed only on completion.
        if (!isModeFour(recipeLogic.lastRecipe)) super.afterWorking()
    }

    private fun availableRods(count: Int): List<BlockPos> {
        val world = level as? ServerLevel ?: return emptyList()
        val now = offsetTimer
        val center = pos.relative(frontFacing.opposite)
        fun valid(target: BlockPos): Boolean =
            world.hasChunkAt(target) && world.getBlockState(target).block is LightningRodBlock

        rodCache = rodCache.filter(::valid)
        if (lastScanTick == Long.MIN_VALUE || now - lastScanTick >= 20L) {
            lastScanTick = now
            rodCache = BlockPos.betweenClosed(center.offset(-4, -4, -4), center.offset(4, 4, 4))
                .asSequence()
                .filter { target ->
                    val inStructure = kotlin.math.abs(target.x - center.x) <= 1 &&
                        kotlin.math.abs(target.z - center.z) <= 1 &&
                        target.y - pos.y in 0..6
                    !inStructure && valid(target)
                }
                .take(16)
                .map { it.immutable() }
                .toList()
        }

        return rodCache.take(count)
    }

    class WeatherControlLogic(private val controller: WeatherControlMachine) : RecipeLogic(controller) {
        private var lightningTicks = 0
        private var activeTier = -1
        private val lightningRecipes by lazy {
            RADON_PER_TICK.map { radon ->
                GTRecipeBuilder.ofRaw().EUt(LIGHTNING_EUT).perTick(true)
                    .inputFluids(GTMaterials.Radon.getFluid(radon)).buildRawRecipe()
            }
        }

        override fun handleTickRecipe(recipe: GTRecipe): GTRecipe.ActionResult {
            if (!isModeFour(recipe) || !controller.isFormed || !isWorkingEnabled) {
                resetLightning()
                return super.handleTickRecipe(recipe)
            }

            val tier = fieldTier(controller.machineStorageItem)
            val rods = if (tier >= 0 && controller.overclockVoltage >= LIGHTNING_EUT) {
                controller.availableRods(LIGHTNING_COUNT[tier])
            } else {
                emptyList()
            }

            if (rods.isNotEmpty()) {
                // A single tick recipe simulates both resources before debiting either.
                // It replaces the base 30 EU/t for this tick, so the total is exactly 1920.
                val result = super.handleTickRecipe(lightningRecipes[tier])
                if (result.isSuccess) {
                    if (activeTier != tier) lightningTicks = 0
                    activeTier = tier
                    lightningTicks++
                    if (lightningTicks >= LIGHTNING_INTERVAL[tier]) {
                        lightningTicks = 0
                        val world = controller.level as ServerLevel
                        rods.forEach { EntityType.LIGHTNING_BOLT.spawn(world, it.above(), MobSpawnType.TRIGGERED) }
                    }

                    return result
                }
            }

            resetLightning()
            return super.handleTickRecipe(recipe)
        }

        override fun onRecipeFinish() {
            if (isModeFour(lastRecipe) &&
                duration > 0 &&
                progress >= duration &&
                controller.isFormed &&
                isWorkingEnabled
            ) {
                (controller.level as? ServerLevel)?.setWeatherParameters(0, WEATHER_TICKS, true, true)
            }
            super.onRecipeFinish()
        }

        private fun resetLightning() {
            lightningTicks = 0
            activeTier = -1
        }
    }

    companion object {
        const val MODE_FOUR = "gtladditions_weather_mode_four"
        const val WEATHER_EUT = 30L
        const val WEATHER_TICKS = 200
        const val LIGHTNING_EUT = 1920L
        private val RADON_PER_TICK = longArrayOf(1000, 2000, 4000)
        private val LIGHTNING_COUNT = intArrayOf(4, 8, 16)
        private val LIGHTNING_INTERVAL = intArrayOf(20, 15, 10)

        private fun isModeFour(recipe: GTRecipe?): Boolean = recipe?.data?.getBoolean(MODE_FOUR) == true

        private fun fieldTier(stack: ItemStack): Int = when (stack.item) {
            GTItems.FIELD_GENERATOR_EV.asItem() -> 0
            GTItems.FIELD_GENERATOR_IV.asItem() -> 1
            GTItems.FIELD_GENERATOR_LuV.asItem() -> 2
            else -> -1
        }
    }
}