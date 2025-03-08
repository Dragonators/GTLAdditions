package com.gtladd.gtladditions.data.Recipes.Process;

import com.gtladd.gtladditions.data.Recipes.Process.Soc.circuitassembler;
import com.gtladd.gtladditions.data.Recipes.Process.Soc.cutter;
import com.gtladd.gtladditions.data.Recipes.Process.Soc.electricblastfurnace;
import com.gtladd.gtladditions.data.Recipes.Process.Soc.engravingarray;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

public class process {
    public process() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        electricblastfurnace.init(provider);
        cutter.init(provider);
        engravingarray.init(provider);
        circuitassembler.init(provider);
    }
}
