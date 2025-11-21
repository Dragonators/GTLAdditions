package com.gtladd.gtladditions.mixin.gtceu.xei;

import com.gregtechceu.gtceu.client.TooltipsHandler;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.screen.tooltip.EmiTextTooltipWrapper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = FluidEmiStack.class)
@Pseudo
public abstract class FluidEmiStackMixin extends EmiStack {

    @Shadow(remap = false)
    @Final
    private Fluid fluid;
    @Shadow(remap = false)
    @Final
    private CompoundTag nbt;

    @Shadow(remap = false)
    public List<Component> getTooltipText() {
        throw new AssertionError();
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getTooltip", at = @At("HEAD"), remap = false, cancellable = true)
    private void addFluidTooltip(CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        List<ClientTooltipComponent> list = new ObjectArrayList<>();
        List<Component> text = this.getTooltipText();
        if (!text.isEmpty()) {
            list.add(new EmiTextTooltipWrapper(this, EmiPort.ordered(text.get(0))));
        }

        list.addAll(text.stream().skip(1L).map(EmiTooltipComponents::of).toList());
        if (this.getAmount() > 1L) {
            list.add(EmiTooltipComponents.getAmount(this));
        }

        TooltipsHandler.appendFluidTooltips(FluidStack.create(this.fluid, this.getAmount(), nbt).getFluid(),
                this.getAmount(),
                component -> list.add(EmiTooltipComponents.of(component)),
                TooltipFlag.NORMAL);
        String namespace = EmiPort.getFluidRegistry().getKey(this.fluid).getNamespace();
        EmiTooltipComponents.appendModName(list, namespace);
        list.addAll(super.getTooltip());
        cir.setReturnValue(list);
    }
}
