package com.gtladd.gtladditions.mixin.kjs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import com.gtladd.gtladditions.common.blocks.ChargeBlockJS;
import com.gtladd.gtladditions.common.items.behavior.ChargeExplosionBehavior;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.custom.BasicBlockJS;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasicBlockJS.Builder.class)
public abstract class BasicBlockBuilderMixin extends BlockBuilder {

    public BasicBlockBuilderMixin(ResourceLocation id) {
        super(id);
    }

    @Inject(method = "createObject()Lnet/minecraft/world/level/block/Block;", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void createChargeBlock(CallbackInfoReturnable<Block> cir) {
        if (ChargeExplosionBehavior.INSTANCE.radiusForCharge(id) > 0) {
            cir.setReturnValue(new ChargeBlockJS(this));
        }
    }
}