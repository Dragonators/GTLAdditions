package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

import com.gtladd.gtladditions.utils.antichrist.ClientRingBlockHelper;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public abstract class ClientLevelChunkCacheMixin {

    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void onChunkDataReceived(int x, int z, FriendlyByteBuf buffer, CompoundTag tag,
                                     Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
                                     CallbackInfoReturnable<LevelChunk> cir) {
        LevelChunk levelChunk = cir.getReturnValue();
        if (levelChunk == null) return;

        LongSet protectedBlocks = ClientRingBlockHelper.INSTANCE.getProtectedBlocksInChunk(levelChunk.getLevel(), x, z);

        if (protectedBlocks != null) {
            for (long posLong : protectedBlocks) {
                BlockPos pos = BlockPos.of(posLong);
                levelChunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
            }
        }
    }
}
