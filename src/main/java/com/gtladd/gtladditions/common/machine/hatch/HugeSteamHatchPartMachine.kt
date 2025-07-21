package com.gtladd.gtladditions.common.machine.hatch

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.UITemplate
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine
import com.lowdragmc.lowdraglib.gui.modular.ModularUI
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.TankWidget
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import net.minecraft.MethodsReturnNonnullByDefault
import net.minecraft.world.entity.player.Player
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class HugeSteamHatchPartMachine(holder: IMachineBlockEntity) :
    FluidHatchPartMachine(holder, 0, IO.IN, 0, 1) {
    override fun createTank(initialCapacity: Long, slots: Int, vararg args: Any?): NotifiableFluidTank {
        return NotifiableFluidTank(this, slots, Int.Companion.MAX_VALUE.toLong(), io)
            .setFilter { fluidStack : FluidStack? -> fluidStack !!.fluid.`is`(GTMaterials.Steam.fluidTag) }
    }

    override fun createUI(entityPlayer: Player): ModularUI {
        return ModularUI(176, 166, this, entityPlayer)
            .background(GuiTextures.BACKGROUND_STEAM.get(false))
            .widget(ImageWidget(7, 16, 81, 55, GuiTextures.DISPLAY_STEAM.get(false)))
            .widget(LabelWidget(11, 20, "gtceu.gui.fluid_amount"))
            .widget(LabelWidget(11, 30) { tank.getFluidInTank(0).amount.toString() + "" }
                .setTextColor(-1).setDropShadow(true))
            .widget(LabelWidget(6, 6, blockState.block.descriptionId))
            .widget(TankWidget(tank.storages[0], 90, 35, true, true)
                .setBackground(GuiTextures.FLUID_SLOT))
            .widget(UITemplate.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT_STEAM.get(false), 7, 84, true))
    }
}
