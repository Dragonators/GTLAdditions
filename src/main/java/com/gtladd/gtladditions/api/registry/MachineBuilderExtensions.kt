package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder
import com.gtladd.gtladditions.client.render.machine.OverlayHullMachineRenderer
import net.minecraft.resources.ResourceLocation

object MachineBuilderExtensions {
    fun <D: MachineDefinition> MachineBuilder<D>.overlayHullRenderer(
        bottom: ResourceLocation,
        top: ResourceLocation,
        side: ResourceLocation,
        overlayModel: ResourceLocation
    ): MachineBuilder<D> =
        this.renderer { OverlayHullMachineRenderer(bottom, top, side, overlayModel) }

    fun <D: MachineDefinition> MachineBuilder<D>.overlayHullRenderer(
        res: ResourceLocation,
        overlayModel: ResourceLocation
    ): MachineBuilder<D> =
        this.renderer { OverlayHullMachineRenderer(res, overlayModel) }
}
