package com.gtladd.gtladditions.common.material

import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gtladd.gtladditions.GTLAdditions

object GTLAddMaterial {
    @JvmField
    var GALLIUM_OXIDE: Material? = (Material.Builder(GTLAdditions.id("gallium_oxide"))).dust().color(15720677)
        .components(GTMaterials.Gallium, 2, GTMaterials.Oxygen, 3).iconSet(MaterialIconSet.DULL).buildAndRegister()
    @JvmField
    var AMMONIUM_GALIUM_SULFATE: Material? =
        (Material.Builder(GTLAdditions.id("ammonium_gallium_sulfate"))).dust().color(0xFFF6E9)
            .iconSet(MaterialIconSet.DULL).buildAndRegister().setFormula("Ga(NH₄)(SO₄)₂")

    @JvmStatic
    fun init() {}
}
