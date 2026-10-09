package com.gtladd.gtladditions.mixin.buildinggadgets2;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.direwolf20.buildinggadgets2.common.blockentities.RenderBlockBE;
import com.direwolf20.buildinggadgets2.common.events.ServerBuildList;
import com.direwolf20.buildinggadgets2.common.events.ServerTickHandler;
import com.gtladd.gtladditions.integration.buildinggadgets2.BuildingGadgetsMachineSupport;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerTickHandler.class)
public class ServerTickHandlerMixin {

    @Inject(method = "build",
            at = @At(value = "INVOKE",
                     target = "Lcom/direwolf20/buildinggadgets2/common/blockentities/RenderBlockBE;setRenderData(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;B)V"),
            remap = false,
            require = 1)
    private static void gtladditions$build(ServerBuildList serverBuildList, Player player, CallbackInfo ci,
                                           @Local(name = "be") RenderBlockBE be,
                                           @Local(name = "blockState") BlockState blockState) {
        CompoundTag data = BuildingGadgetsMachineSupport.newMachineData(blockState, player, serverBuildList.teData != null);
        if (data != null) be.setBlockEntityData(data);
    }

    @Inject(method = "exchange",
            at = @At(value = "INVOKE",
                     target = "Lcom/direwolf20/buildinggadgets2/common/blockentities/RenderBlockBE;setRenderData(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;B)V"),
            remap = false,
            require = 2,
            expect = 2)
    private static void gtladditions$exchange(ServerBuildList list, Player player, CallbackInfo ci,
                                              @Local(name = "be") RenderBlockBE be,
                                              @Local(name = "blockState") BlockState state) {
        CompoundTag data = BuildingGadgetsMachineSupport.newMachineData(state, player, list.teData != null);
        if (data != null) be.setBlockEntityData(data);
    }

    @Inject(method = "cut",
            at = @At(value = "INVOKE",
                     target = "Lcom/direwolf20/buildinggadgets2/common/worlddata/BG2Data;addToTEMap(Ljava/util/UUID;Ljava/util/ArrayList;)V",
                     shift = At.Shift.AFTER),
            remap = false,
            require = 1)
    private static void gtladditions$stopCutMachine(ServerBuildList list, Player player, CallbackInfo ci,
                                                    @Local(name = "blockPos") BlockPos pos,
                                                    @Local(name = "level") Level level,
                                                    @Local(name = "doRemove") boolean doRemove) {
        if (doRemove && level instanceof ServerLevel serverLevel) {
            BuildingGadgetsMachineSupport.stopCutMachine(serverLevel, pos);
        }
    }
}