package com.gtladd.gtladditions.common.machine.multiblock.structure

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object RingStructure {
    val RINGS: Array<Array<Array<String>>> by lazy(LazyThreadSafetyMode.NONE) {
        StructureResourceLoader.loadRings("rings/forge_of_the_antichrist_rings.bin", "forge_of_the_antichrist_rings")
    }

    val FIRST_RING: Array<Array<String>>
        get() = RINGS[0]

    val SECOND_RING: Array<Array<String>>
        get() = RINGS[1]

    val THRID_RING: Array<Array<String>>
        get() = RINGS[2]
}