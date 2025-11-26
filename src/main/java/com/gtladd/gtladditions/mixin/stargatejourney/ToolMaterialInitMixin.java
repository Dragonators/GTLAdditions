package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;
import net.povstalec.sgjourney.common.init.ToolMaterialInit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ToolMaterialInit.class)
public abstract class ToolMaterialInitMixin {

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/registries/RegistryObject;get()Ljava/lang/Object;", remap = false))
    private static Object replaceArmorMaterial(RegistryObject<?> instance) {
        try {
            return instance.get();
        } catch (Exception ignore) {
            return Items.IRON_INGOT;
        }
    }
}
