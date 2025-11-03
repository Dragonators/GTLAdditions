package com.gtladd.gtladditions.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey
import net.minecraft.world.level.material.Fluid
import java.util.concurrent.TimeUnit

object TempChemicalHelper {
    @Volatile
    private var needBuild = true
    private val tempCache: Cache<Fluid, Material> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build()

    fun getMaterialFromFluid(fluid: Fluid): Material? {
        if (needBuild) {
            synchronized(this) {
                if (needBuild) {
                    GTCEuAPI.materialManager.registeredMaterials.forEach { material ->
                        if (material.hasProperty(PropertyKey.FLUID)) {
                            val fluidProperty = material.getProperty(PropertyKey.FLUID)
                            FluidStorageKey.allKeys().forEach { key ->
                                fluidProperty.get(key)?.let { f ->
                                    tempCache.put(f, material)
                                }
                            }
                        }
                    }
                }
            }
        }
        return tempCache.getIfPresent(fluid)
    }
}