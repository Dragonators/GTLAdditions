package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.GTCEu
import net.minecraft.resources.ResourceLocation
import java.util.function.Consumer

object RemoveRecipe {
    fun init(consumer: Consumer<ResourceLocation>) {
        consumer.accept(GTCEu.id("greenhouse/oak_sapling"))
    }
}