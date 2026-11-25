package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTMaterials
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.LightningRodBlock
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine
import kotlin.math.abs

class WeatherControlMachine(holder: IMachineBlockEntity) : StorageMachine(holder, 1) {
    override fun filter(stack: ItemStack): Boolean = fieldTier(stack) != null

    override fun alwaysTryModifyRecipe(): Boolean = true

    fun modifyLightningRecipe(recipe: GTRecipe): GTRecipe? {
        if (!recipe.data.getBoolean(MODE_FOUR)) return recipe
        val tier = fieldTier(machineStorageItem) ?: return null

        return recipe.copy().apply {
            data = data.copy()
            data.putInt(FIELD_TIER, tier.ordinal)

            val radon = FluidIngredient.of(GTMaterials.Radon.getFluid(FluidStorageKeys.GAS, tier.radonPerTick))
            val chance = ChanceLogic.getMaxChancedValue()
            tickInputs[FluidRecipeCapability.CAP] = listOf(Content(radon, chance, chance, 0, null, null))
            IGTRecipe.of(this).setHasTick(true)
        }
    }

    override fun beforeWorking(recipe: GTRecipe?): Boolean {
        if (recipe?.data?.getBoolean(MODE_FOUR) == true && lightningRods(level as ServerLevel).none()) {
            return false
        }

        return super.beforeWorking(recipe)
    }

    override fun onWorking(): Boolean {
        if (!super.onWorking()) return false

        val logic = recipeLogic
        val data = logic.lastRecipe?.data ?: return true

        if (data.contains(FIELD_TIER)) {
            val tier = LightningTier.entries[data.getInt(FIELD_TIER)]

            // Core increments the successful running time after this callback.
            if ((logic.totalContinuousRunningTime + 1) % tier.interval == 0L) {
                val world = level as ServerLevel
                for (rod in lightningRods(world).take(tier.rodCount)) {
                    EntityType.LIGHTNING_BOLT.spawn(world, rod.above(), MobSpawnType.TRIGGERED)
                }
            }
        }

        return true
    }

    override fun afterWorking() {
        val logic = recipeLogic
        if (logic.lastRecipe?.data?.getBoolean(MODE_FOUR) != true) {
            super.afterWorking()
        } else if (logic.duration > 0 && logic.progress >= logic.duration) {
            // An interruption calls this same hook before resetting progress.
            (level as ServerLevel).setWeatherParameters(0, WEATHER_TICKS, true, true)
        }
    }

    private fun lightningRods(world: ServerLevel): Sequence<BlockPos> {
        val center = pos.relative(frontFacing.opposite)

        return BlockPos.betweenClosed(center.offset(-4, -4, -4), center.offset(4, 4, 4))
            .asSequence()
            .filter { rod ->
                val inStructure = abs(rod.x - center.x) <= 1 &&
                    abs(rod.z - center.z) <= 1 &&
                    rod.y - center.y in 0..6
                !inStructure && world.isLoaded(rod) && world.getBlockState(rod).block is LightningRodBlock
            }
    }

    private enum class LightningTier(val radonPerTick: Long, val interval: Int, val rodCount: Int) {
        EV(400, 20, 4),
        IV(1200, 15, 8),
        LuV(2400, 10, 16)
    }

    companion object {
        const val MODE_FOUR = "gtladditions_weather_mode_four"
        const val WEATHER_TICKS = 200
        const val LIGHTNING_EUT = 1920L
        private const val FIELD_TIER = "gtladditions_weather_field_tier"

        private fun fieldTier(stack: ItemStack): LightningTier? = when (stack.item) {
            GTItems.FIELD_GENERATOR_EV.asItem() -> LightningTier.EV
            GTItems.FIELD_GENERATOR_IV.asItem() -> LightningTier.IV
            GTItems.FIELD_GENERATOR_LuV.asItem() -> LightningTier.LuV
            else -> null
        }
    }
}
