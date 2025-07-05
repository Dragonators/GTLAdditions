package com.gtladd.gtladditions.common.material;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.gtladd.gtladditions.GTLAdditions;

public class GTLAddMaterial {

    public static Material GALLIUM_OXIDE = (new Material.Builder(GTLAdditions.id("gallium_oxide"))).dust().color(15720677)
            .components(GTMaterials.Gallium, 2, GTMaterials.Oxygen, 3).iconSet(MaterialIconSet.DULL).buildAndRegister();
    public static Material AMMONIUM_GALIUM_SULFATE = (new Material.Builder(GTLAdditions.id("ammonium_gallium_sulfate"))).dust().color(0xFFF6E9)
            .iconSet(MaterialIconSet.DULL).buildAndRegister().setFormula("Ga(NH₄)(SO₄)₂");

    public static void init() {}
}
