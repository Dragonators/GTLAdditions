package com.gtladd.gtladditions.mixin.gtmtings;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.gtladd.gtladditions.api.machine.gui.WirelessEnergyMonitorPage;
import com.gtladd.gtladditions.integration.buildinggadgets2.BuildingGadgetsWirelessEnergySupport;
import com.hepdd.gtmthings.common.block.machine.electric.WirelessEnergyMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(WirelessEnergyMonitor.class)
public abstract class WirelessEnergyMonitorMixin extends MetaMachine implements IFancyUIMachine, IInteractedMachine {

    @Shadow(remap = false)
    private UUID userid;

    public WirelessEnergyMonitorMixin(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public boolean onLeftClick(Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction) {
        if (hand != InteractionHand.MAIN_HAND ||
                !(player.getMainHandItem().getItem() instanceof BaseGadget)) {
            return false;
        }
        BuildingGadgetsWirelessEnergySupport.toggleBinding(player, player.getMainHandItem());
        return true;
    }

    /**
     * @author Dragons
     * @reason Replace the text list with the monitor page
     */
    @Overwrite(remap = false)
    public Widget createUIWidget() {
        return new WirelessEnergyMonitorPage(this, userid);
    }

    @Override
    public boolean hasPlayerInventory() {
        return false;
    }
}
