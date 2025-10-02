package com.gtladd.gtladditions.common.data

import com.gregtechceu.gtceu.api.registry.GTRegistries
import com.gregtechceu.gtceu.api.sound.SoundEntry
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.REGISTRATE
import net.minecraft.resources.ResourceLocation

object GTLAddSoundEntries {
    @JvmField
    var FORGE_OF_THE_ANTICHRIST: SoundEntry

    @JvmField
    var QUANTUM_OSCILLATION: SoundEntry

    @JvmStatic
    fun init() {
    }

    private fun register(name: String, attenuationDistance: Int): SoundEntry {
        return REGISTRATE.sound(GTLAdditions.id(name)).attenuationDistance(attenuationDistance).build()
    }

    init {
        FORGE_OF_THE_ANTICHRIST = register("forgeofantichrist", 64)
        QUANTUM_OSCILLATION = register("quantumoscillation", 48)
    }
}
