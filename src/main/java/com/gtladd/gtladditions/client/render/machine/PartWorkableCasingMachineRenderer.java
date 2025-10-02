package com.gtladd.gtladditions.client.render.machine;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.client.model.SpriteOverrider;
import com.gregtechceu.gtceu.client.renderer.machine.IControllerRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.tterrag.registrate.util.entry.BlockEntry;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PartWorkableCasingMachineRenderer extends WorkableCasingMachineRenderer implements IControllerRenderer {

    protected final BlockEntry<Block> partEntry;
    protected final ResourceLocation partCasing;
    protected final Map<Direction, BakedModel> partCasingModels = new EnumMap<>(Direction.class);

    public PartWorkableCasingMachineRenderer(ResourceLocation baseCasing, ResourceLocation workableModel, BlockEntry<Block> partEntry, ResourceLocation partCasing) {
        super(baseCasing, workableModel);
        this.partEntry = partEntry;
        this.partCasing = partCasing;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderPartModel(List<BakedQuad> quads, IMultiController machine, IMultiPart part, Direction frontFacing,
                                @Nullable Direction side, RandomSource rand, Direction modelFacing, ModelState modelState) {
        quads.addAll(getPartCasingModel(frontFacing).getQuads(partEntry.get().defaultBlockState(), side, rand));
    }

    @OnlyIn(Dist.CLIENT)
    protected BakedModel getPartCasingModel(Direction frontFacing) {
        return partCasingModels.computeIfAbsent(frontFacing, facing -> getModel().bake(
                ModelFactory.getModeBaker(),
                new SpriteOverrider(Map.of("all", partCasing)),
                ModelFactory.getRotation(facing),
                modelLocation));
    }
}
