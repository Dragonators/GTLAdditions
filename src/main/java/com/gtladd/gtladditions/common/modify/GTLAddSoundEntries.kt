package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.api.sound.SoundEntry
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.Companion.REGISTRATE

object GTLAddSoundEntries {
    val FORGE_OF_THE_ANTICHRIST: SoundEntry
    val QUANTUM_OSCILLATION: SoundEntry
    val GENESIS_ENGINE: SoundEntry
    val INTER_STELLAR: SoundEntry

    fun init() {
    }

    private fun register(name: String, attenuationDistance: Int): SoundEntry {
        return REGISTRATE.sound(GTLAdditions.id(name)).attenuationDistance(attenuationDistance).build()
    }

    init {
        FORGE_OF_THE_ANTICHRIST = register("forgeofantichrist", 64)
        QUANTUM_OSCILLATION = register("quantumoscillation", 48)
        GENESIS_ENGINE = register("genesisengine", 48)
        INTER_STELLAR = register("interstellar", 96)
    }
}
