package com.gtladd.gtladditions.api.Registry;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.resources.ResourceKey;

import com.gtladd.gtladditions.GTLAdditions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class GTLAddRegistration extends GTRegistrate {

    public static final GTLAddRegistration REGISTRATE = new GTLAddRegistration();

    private GTLAddRegistration() {
        super(GTLAdditions.MOD_ID);
    }

    public @NotNull GTLAddMultiBlockMachineBuilder multiblock(@NotNull String name, @NotNull Function<IMachineBlockEntity, ? extends MultiblockControllerMachine> metaMachine) {
        return GTLAddMultiBlockMachineBuilder.createMulti(name, metaMachine, MetaMachineBlock::new, MetaMachineItem::new, MetaMachineBlockEntity::createBlockEntity);
    }

    static {
        REGISTRATE.defaultCreativeTab((ResourceKey) null);
    }
}
