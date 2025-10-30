package com.gtladd.gtladditions.common.material

import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags
import com.gregtechceu.gtceu.api.data.chemical.material.properties.*
import com.gregtechceu.gtceu.api.fluids.FluidBuilder
import com.gregtechceu.gtceu.api.fluids.FluidState
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.common.data.GTMaterials
import org.gtlcore.gtlcore.common.data.GTLMaterials

object MaterialAdd {
    init {
        GTLMaterials.TranscendentMetal.addFlags(MaterialFlags.GENERATE_FRAME, MaterialFlags.GENERATE_DENSE)
        GTLMaterials.AttunedTengam.addFlags(MaterialFlags.GENERATE_DENSE, MaterialFlags.GENERATE_RING)
        GTLMaterials.AstralTitanium.addFlags(MaterialFlags.GENERATE_FRAME)
        GTLMaterials.Hypogen.addFlags(
            MaterialFlags.GENERATE_BOLT_SCREW,
            MaterialFlags.GENERATE_FRAME,
            MaterialFlags.GENERATE_DENSE,
            MaterialFlags.GENERATE_LONG_ROD
        )
        GTLMaterials.MetastableOganesson.addFlags(MaterialFlags.GENERATE_DENSE)
        GTLMaterials.Infinity.addFlags(MaterialFlags.GENERATE_DENSE)
        GTLMaterials.BlackDwarfMatter.addFlags(MaterialFlags.GENERATE_DENSE)
        GTLMaterials.DraconiumAwakened.addFlags(MaterialFlags.GENERATE_LONG_ROD)
        GTLMaterials.Legendarium.addFlags(MaterialFlags.GENERATE_LONG_ROD)
        GTLMaterials.TitanPrecisionSteel.addFlags(MaterialFlags.GENERATE_FRAME)
        GTLMaterials.CosmicNeutronium.addFlags(MaterialFlags.GENERATE_DENSE)
        GTLMaterials.AbyssalAlloy.addFlags(MaterialFlags.GENERATE_FRAME)
        GTLMaterials.Periodicium.addFlags(MaterialFlags.GENERATE_ROD)

        GTLMaterials.Cosmic.setProperty(PropertyKey.FLUID, FluidProperty(FluidStorageKeys.LIQUID, FluidBuilder()))
        GTLMaterials.Eternity.setProperty(
            PropertyKey.WIRE,
            WireProperties(Int.MAX_VALUE, 4194304, 0, true)
        )
        GTLMaterials.MetastableOganesson.setProperty(PropertyKey.INGOT, IngotProperty())
        GTMaterials.Plutonium241.getProperty(PropertyKey.FLUID)
            .enqueueRegistration(FluidStorageKeys.PLASMA, (FluidBuilder()).state(FluidState.PLASMA))
        GTLMaterials.TranscendentMetal.setProperty(
            PropertyKey.FLUID_PIPE,
            FluidPipeProperties(Int.MAX_VALUE, Int.MAX_VALUE.toLong(), true, true, true, true)
        )
    }

    @JvmStatic
    fun init() {
    }
}