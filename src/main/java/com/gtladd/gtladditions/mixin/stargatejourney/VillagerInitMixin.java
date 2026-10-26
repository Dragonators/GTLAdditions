package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.povstalec.sgjourney.common.init.VillagerInit;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;
import java.util.function.Supplier;

@Mixin(VillagerInit.class)
public abstract class VillagerInitMixin {

    // Keep the POI/profession IDs valid for vanilla tags and saved villagers, but assign no workstation blocks.
    @WrapOperation(method = "<clinit>",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/registries/RegistryObject;"),
                   remap = false,
                   require = 2,
                   allow = 2)
    private static RegistryObject<?> gtladditions$disableArcheologistWorkstation(DeferredRegister<?> register, String name, Supplier<?> supplier,
                                                                                 Operation<RegistryObject<?>> original) {
        Supplier<?> replacement = name.equals("archeologist_poi") ? () -> new PoiType(Set.of(), 1, 1) : supplier;
        return original.call(register, name, replacement);
    }
}