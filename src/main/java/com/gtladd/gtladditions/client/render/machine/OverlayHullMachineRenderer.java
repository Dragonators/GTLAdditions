package com.gtladd.gtladditions.client.render.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.client.model.SpriteOverrider;
import com.gregtechceu.gtceu.client.renderer.machine.IPartRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.MachineRenderer;

import com.lowdragmc.lowdraglib.client.bakedpipeline.Quad;
import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class OverlayHullMachineRenderer extends MachineRenderer implements IPartRenderer {

    protected IModelRenderer overlayModel;

    public OverlayHullMachineRenderer(ResourceLocation bottom, ResourceLocation top, ResourceLocation side, ResourceLocation overlayModel) {
        super(GTCEu.id("block/machine/hull_machine"));
        setTextureOverride(Map.of(
                "bottom", bottom,
                "top", top,
                "side", side));
        this.overlayModel = new IModelRenderer(overlayModel);
    }

    public OverlayHullMachineRenderer(ResourceLocation res, ResourceLocation overlayModel) {
        super(GTCEu.id("block/machine/hull_machine"));
        setTextureOverride(Map.of(
                "bottom", res,
                "top", res,
                "side", res));
        this.overlayModel = new IModelRenderer(overlayModel);
    }

    @NotNull
    @Override
    @OnlyIn(Dist.CLIENT)
    public TextureAtlasSprite getParticleTexture() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(override.get("side"));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings({ "removal", "deprecation" })
    public void renderMachine(List<BakedQuad> quads, MachineDefinition definition, @Nullable MetaMachine machine,
                              Direction frontFacing, @Nullable Direction side, RandomSource rand, Direction modelFacing,
                              ModelState modelState) {
        super.renderMachine(quads, definition, machine, frontFacing, side, rand, modelFacing, modelState);
        // expand the overlay quads ever so slightly to combat z-fighting.
        // noinspection removal
        quads.addAll(overlayModel.getRotatedModel(frontFacing)
                .getQuads(definition.defaultBlockState(), side, rand)
                .stream()
                .map(quad -> Quad.from(quad, this.reBakeOverlayQuadsOffset()).rebake())
                .toList());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings({ "removal" })
    public BakedModel getRotatedModel(Direction frontFacing) {
        // noinspection removal
        return blockModels.computeIfAbsent(frontFacing, facing -> getModel().bake(
                ModelFactory.getModeBaker(),
                new SpriteOverrider(override),
                ModelFactory.getRotation(facing),
                modelLocation));
    }

    public float reBakeOverlayQuadsOffset() {
        return 0.002f;
    }
}
