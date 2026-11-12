package com.gtladd.gtladditions.utils.recipe

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey
import net.minecraft.world.level.material.Fluid

/** `ChemicalHelper.getMaterial(Fluid)` needs bound fluid tags, which recipe generation on first load does not have yet. */
object TempChemicalHelper {
    private val materials: Map<Fluid, Material> by lazy {
        buildMap {
            GTCEuAPI.materialManager.registeredMaterials.forEach { material ->
                if (material.hasProperty(PropertyKey.FLUID)) {
                    val fluidProperty = material.getProperty(PropertyKey.FLUID)
                    FluidStorageKey.allKeys().forEach { key ->
                        fluidProperty.get(key)?.let { put(it, material) }
                    }
                }
            }
        }
    }

    fun getMaterialFromFluid(fluid: Fluid): Material? = materials[fluid]
}