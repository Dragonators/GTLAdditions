package com.gtladd.gtladditions.common.material

import com.gregtechceu.gtceu.api.data.chemical.Element
import com.gregtechceu.gtceu.common.data.GTElements

object GTLAddElements {

    val CREON: Element? = GTElements.createAndRegister(1000, 1000, -1, null, "creon", "⸎", false)

    @JvmStatic
    fun init() {}
}