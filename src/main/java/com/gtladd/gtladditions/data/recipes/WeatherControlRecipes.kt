package com.gtladd.gtladditions.data.recipes

import com.gtladd.gtladditions.common.machine.multiblock.controller.WeatherControlMachine
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.WEATHER_CONTROL_RECIPES
import java.util.function.Consumer

object WeatherControlRecipes {
    fun init(provider: Consumer<FinishedRecipe?>) {
        WEATHER_CONTROL_RECIPES.recipeBuilder("weather_control_4")
            .circuitMeta(4)
            .EUt(WeatherControlMachine.LIGHTNING_EUT)
            .duration(WeatherControlMachine.WEATHER_TICKS)
            .addData(WeatherControlMachine.MODE_FOUR, true)
            .save(provider)
    }
}