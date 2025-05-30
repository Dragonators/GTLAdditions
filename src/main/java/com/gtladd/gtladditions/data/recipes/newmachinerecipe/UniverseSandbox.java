package com.gtladd.gtladditions.data.recipes.newmachinerecipe;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static org.gtlcore.gtlcore.common.data.GTLMaterials.*;

public class UniverseSandbox {

    public UniverseSandbox() {}

    private static final Material[] INPUTS = {
            Tetrahedrite, Goethite, YellowLimonite, Hematite, Malachite, Soapstone, Talc, GlauconiteSand, Pentlandite, Grossular, Spessartine,
            Pyrolusite, Tantalite, Magnetite, Olivine, Almandine, Pyrope, Sapphire, GreenSapphire, Stibnite, Uraninite, Bastnasite, Molybdenum,
            Neodymium, Monazite, Redstone, Ruby, Chalcopyrite, Zeolite, Cassiterite, Realgar, Cinnabar, Saltpeter, Diatomite, Electrotine,
            Alunite, Rubidium, Beryllium, Emerald, Pyrite, VanadiumMagnetite, Lazurite, Sodalite, Lapis, Calcite, Wulfenite, Calorite, Galena,
            Molybdenite, Powellite, Kyanite, Mica, Bauxite, Pollucite, Quartzite, CertusQuartz, Zircon, Barite, GarnetRed, GarnetYellow, Amethyst,
            Opal, AlienAlgae, BlueTopaz, BasalticMineralSand, GraniticMineralSand, FullersEarth, Gypsum, RockSalt, Salt, Lepidolite, Spodumene,
            Chalcocite, Bornite, Cinnabar, NetherQuartz, Apatite, TricalciumPhosphate, Pyrochlore, Sphalerite, CassiteriteSand, GarnetSand, Asbestos,
            Oilsands, InfusedGold, Ilmenite,
    };

    public static void init(Consumer<FinishedRecipe> provider) {
        GTRecipeBuilder recipeBuilder = GTLAddRecipesTypes.UNIVERSE_SANDBOX.recipeBuilder(GTLAdditions.id("universe_sandbox"));
    }
}
