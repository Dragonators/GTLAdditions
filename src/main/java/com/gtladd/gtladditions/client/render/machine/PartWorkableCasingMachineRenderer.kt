package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.client.model.SpriteOverrider
import com.gregtechceu.gtceu.client.renderer.machine.IControllerRenderer
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.lowdragmc.lowdraglib.client.model.ModelFactory
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelState
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*

open class PartWorkableCasingMachineRenderer(
    baseCasing: ResourceLocation,
    workableModel: ResourceLocation,
    protected val partEntry: BlockEntry<Block>,
    protected val partCasing: ResourceLocation
) : WorkableCasingMachineRenderer(baseCasing, workableModel), IControllerRenderer {

    protected val partCasingModels: MutableMap<Direction, BakedModel> = EnumMap(Direction::class.java)

    @Suppress("DEPRECATION")
    @OnlyIn(Dist.CLIENT)
    override fun renderPartModel(
        quads: MutableList<BakedQuad>,
        machine: IMultiController,
        part: IMultiPart,
        frontFacing: Direction,
        side: Direction?,
        rand: RandomSource,
        modelFacing: Direction?,
        modelState: ModelState?
    ) {
        quads.addAll(getPartCasingModel(frontFacing).getQuads(partEntry.get().defaultBlockState(), side, rand))
    }

    @OnlyIn(Dist.CLIENT)
    protected fun getPartCasingModel(frontFacing: Direction): BakedModel =
        partCasingModels.computeIfAbsent(frontFacing) {
            model.bake(
                ModelFactory.getModeBaker(),
                SpriteOverrider(mapOf("all" to partCasing)),
                ModelFactory.getRotation(it),
                modelLocation
            )!!
        }
}