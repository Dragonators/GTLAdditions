package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.phys.AABB;
import net.povstalec.sgjourney.common.blocks.dhd.AbstractDHDBlock;
import net.povstalec.sgjourney.common.structures.SGJourneyStructure;
import net.povstalec.sgjourney.common.structures.StargateStructure;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Optional;

import static com.gtladd.gtladditions.utils.CommonUtils.isTargetDimension;
import static com.gtladd.gtladditions.utils.CommonUtils.selectDisplayItem;

@SuppressWarnings("all")
@Mixin(StargateStructure.class)
public abstract class StargateStructureMixin extends SGJourneyStructure {

    public StargateStructureMixin(StructureSettings config, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int size, HeightProvider startHeight, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter, Optional<Boolean> commonStargates) {
        super(config, startPool, startJigsawName, size, startHeight, projectStartToHeightmap, maxDistanceFromCenter, commonStargates);
    }

    /**
     * @author Dragons
     * @reason Ban Dimensions
     */
    @Override
    protected boolean extraSpawningChecks(GenerationContext context) {
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                context.chunkPos().getMinBlockX() >> 2,
                0,
                context.chunkPos().getMinBlockZ() >> 2,
                context.randomState().sampler());

        if (gTLAdditions$isDimensionBiome(biome, context)) return false;
        return true;
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource randomSource,
                           BoundingBox boundingBox, ChunkPos chunkPos, PiecesContainer piecesContainer) {
        super.afterPlace(level, structureManager, chunkGenerator, randomSource, boundingBox, chunkPos, piecesContainer);

        ResourceLocation dimensionLocation = level.getLevel().dimension().location();
        if (isTargetDimension(dimensionLocation)) {
            gTLAdditions$createDHDItemFrames(level, boundingBox, randomSource);
        }
    }

    @Unique
    private void gTLAdditions$createDHDItemFrames(WorldGenLevel level, BoundingBox boundingBox, RandomSource randomSource) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = boundingBox.minX(); x <= boundingBox.maxX(); x++) {
            for (int y = boundingBox.minY(); y <= boundingBox.maxY(); y++) {
                for (int z = boundingBox.minZ(); z <= boundingBox.maxZ(); z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.getBlock() instanceof AbstractDHDBlock) {
                        gTLAdditions$createItemFrameInFrontOfDHD(level, pos.immutable(), state, randomSource);
                    }
                }
            }
        }
    }

    @Unique
    private void gTLAdditions$createItemFrameInFrontOfDHD(WorldGenLevel level, BlockPos dhdPos, BlockState dhdState, RandomSource randomSource) {
        ItemStack displayItem = gTLAdditions$selectDisplayItemForDimension(level, randomSource);

        Direction dhdFacing = dhdState.getValue(AbstractDHDBlock.FACING);
        BlockPos itemFramePos = dhdPos.relative(dhdFacing);

        if (!level.getBlockState(itemFramePos).isAir()) {
            if (displayItem != null && !displayItem.isEmpty()) {
                level.setBlock(itemFramePos, Blocks.AIR.defaultBlockState(), 3);
                level.addFreshEntity(gTLAdditions$getItemEntity(level, itemFramePos, displayItem));
            }
            return;
        }

        AABB checkArea = new AABB(itemFramePos);
        List<ItemFrame> existingFrames = level.getEntitiesOfClass(ItemFrame.class, checkArea);
        if (!existingFrames.isEmpty()) {
            if (displayItem != null && !displayItem.isEmpty()) {
                level.addFreshEntity(gTLAdditions$getItemEntity(level, itemFramePos, displayItem));
            }
            return;
        }

        ItemFrame itemFrame = new ItemFrame(level.getLevel(), itemFramePos, Direction.UP);

        if (displayItem != null && !displayItem.isEmpty()) {
            itemFrame.setItem(displayItem, false);
            level.addFreshEntity(itemFrame);
        }
    }

    @Unique
    private static @NotNull ItemEntity gTLAdditions$getItemEntity(WorldGenLevel level, BlockPos itemFramePos, ItemStack displayItem) {
        double x = itemFramePos.getX() + 0.5;
        double y = itemFramePos.getY() + 0.5;
        double z = itemFramePos.getZ() + 0.5;

        ItemEntity itemEntity = new ItemEntity(level.getLevel(), x, y, z, displayItem);

        itemEntity.setUnlimitedLifetime();
        itemEntity.setInvulnerable(true);

        itemEntity.setDeltaMovement(0, 0, 0);
        itemEntity.setNoGravity(true);
        return itemEntity;
    }

    @Unique
    private ItemStack gTLAdditions$selectDisplayItemForDimension(WorldGenLevel level, RandomSource randomSource) {
        ResourceLocation dimensionLocation = level.getLevel().dimension().location();
        return selectDisplayItem(dimensionLocation);
    }

    @Unique
    private boolean gTLAdditions$isDimensionBiome(Holder<Biome> biome, GenerationContext context) {
        if (biome.is(BiomeTags.IS_OVERWORLD)) return true;

        if (biome.is(BiomeTags.IS_NETHER)) return true;

        if (biome.is(BiomeTags.IS_END)) return true;

        return false;
    }
}
