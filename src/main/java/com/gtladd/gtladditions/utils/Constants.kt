package com.gtladd.gtladditions.utils

import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.resources.ResourceLocation
import java.math.BigDecimal
import java.math.BigInteger

object Constants {
    val INT_MAX_BIG: BigInteger = BigInteger.valueOf(Int.MAX_VALUE.toLong())
    val LONG_MAX_BIG: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
    val LONG_MAX_DECIMAL: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE)
    val ORBIT_OBJECTS: List<ResourceLocation> = listOf(
        GTLAdditions.id("obj/planets/the_nether"),
        GTLAdditions.id("obj/planets/overworld"),
        GTLAdditions.id("obj/planets/the_end"),
        GTLAdditions.id("obj/planets/ceres"),
        GTLAdditions.id("obj/planets/enceladus"),
        GTLAdditions.id("obj/planets/ganymede"),
        GTLAdditions.id("obj/planets/io"),
        GTLAdditions.id("obj/planets/mars"),
        GTLAdditions.id("obj/planets/mercury"),
        GTLAdditions.id("obj/planets/moon"),
        GTLAdditions.id("obj/planets/pluto"),
        GTLAdditions.id("obj/planets/titan"),
        GTLAdditions.id("obj/planets/venus")
    )
}