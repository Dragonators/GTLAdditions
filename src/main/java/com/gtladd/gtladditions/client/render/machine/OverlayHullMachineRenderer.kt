package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.client.model.SpriteOverrider
import com.gregtechceu.gtceu.client.renderer.machine.IPartRenderer
import com.gregtechceu.gtceu.client.renderer.machine.MachineRenderer
import com.lowdragmc.lowdraglib.client.bakedpipeline.Quad
import com.lowdragmc.lowdraglib.client.model.ModelFactory
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelState
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.inventory.InventoryMenu
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

open class OverlayHullMachineRenderer : MachineRenderer, IPartRenderer {

    protected val overlayModel: IModelRenderer

    constructor(
        bottom: ResourceLocation,
        top: ResourceLocation,
        side: ResourceLocation,
        overlayModel: ResourceLocation
    ) : super(GTCEu.id("block/machine/hull_machine")) {
        setTextureOverride(mapOf(
            "bottom" to bottom,
            "top" to top,
            "side" to side
        ))
        this.overlayModel = IModelRenderer(overlayModel)
    }

    constructor(
        res: ResourceLocation,
        overlayModel: ResourceLocation
    ) : super(GTCEu.id("block/machine/hull_machine")) {
        setTextureOverride(mapOf(
            "bottom" to res,
            "top" to res,
            "side" to res
        ))
        this.overlayModel = IModelRenderer(overlayModel)
    }

    @OnlyIn(Dist.CLIENT)
    override fun getParticleTexture(): TextureAtlasSprite =
        Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(override["side"])

    @OnlyIn(Dist.CLIENT)
    @Suppress("DEPRECATION", "removal")
    override fun renderMachine(
        quads: MutableList<BakedQuad>,
        definition: MachineDefinition,
        machine: MetaMachine?,
        frontFacing: Direction,
        side: Direction?,
        rand: RandomSource,
        modelFacing: Direction?,
        modelState: ModelState
    ) {
        super.renderMachine(quads, definition, machine, frontFacing, side, rand, modelFacing, modelState)
        quads.addAll(
            overlayModel.getRotatedModel(frontFacing)
                .getQuads(definition.defaultBlockState(), side, rand)
                .map { Quad.from(it, reBakeOverlayQuadsOffset()).rebake() }
        )
    }

    @OnlyIn(Dist.CLIENT)
    @Suppress("removal", "DEPRECATION")
    override fun getRotatedModel(frontFacing: Direction): BakedModel =
        blockModels.computeIfAbsent(frontFacing) {
            model.bake(
                ModelFactory.getModeBaker(),
                SpriteOverrider(override),
                ModelFactory.getRotation(it),
                modelLocation
            )
        }

    open fun reBakeOverlayQuadsOffset(): Float = 0.002f
}