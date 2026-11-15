package com.gtladd.gtladditions.mixin.gtlcore.item;

import org.gtlcore.gtlcore.common.item.UltimateTerminalBehavior;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UltimateTerminalBehavior.class, remap = false)
public abstract class UltimateTerminalBehaviorMixin {

    @Shadow
    private UltimateTerminalBehavior.AutoBuildSetting getAutoBuildSetting(ItemStack mainHandItem) {
        throw new AssertionError();
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void gtladditions$buildTiers(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || context.getLevel().isClientSide) return;
        if (!(MetaMachine.getMachine(context.getLevel(), context.getClickedPos()) instanceof IStructureTierMachine machine)) return;
        var setting = getAutoBuildSetting(player.getMainHandItem());
        if (setting.isDismantleMode()) return;
        machine.getStructureTierTrait().autoBuild(player, setting);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}