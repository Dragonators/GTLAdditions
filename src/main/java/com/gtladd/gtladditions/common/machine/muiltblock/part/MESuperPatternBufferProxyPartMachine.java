package com.gtladd.gtladditions.common.machine.muiltblock.part;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.BlockHitResult;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MESuperPatternBufferProxyPartMachine extends TieredIOPartMachine implements IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MESuperPatternBufferProxyPartMachine.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    protected MESuperPatternBufferProxyRecipeHandler<Ingredient> itemProxyHandler;

    @Getter
    protected MESuperPatternBufferProxyRecipeHandler<FluidIngredient> fluidProxyHandler;

    @Persisted
    @Getter
    @DescSynced
    private BlockPos bufferPos;

    public MESuperPatternBufferProxyPartMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.LuV, IO.BOTH);
        this.itemProxyHandler = new MESuperPatternBufferProxyRecipeHandler<>(this, IO.IN, ItemRecipeCapability.CAP);
        this.fluidProxyHandler = new MESuperPatternBufferProxyRecipeHandler<>(this, IO.IN, FluidRecipeCapability.CAP);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel level) {
            level.getServer().tell(new TickTask(0, () -> this.setBuffer(bufferPos)));
        }
    }

    public boolean setBuffer(@Nullable BlockPos pos) {
        var level = getLevel();
        if (pos == null || level == null) return false;
        if (MetaMachine.getMachine(getLevel(), pos) instanceof MESuperPatternBufferPartMachine machine) {
            this.bufferPos = pos;

            List<NotifiableRecipeHandlerTrait<Ingredient>> itemHandlers = new ArrayList<>();
            List<NotifiableRecipeHandlerTrait<FluidIngredient>> fluidHandlers = new ArrayList<>();
            for (var handler : machine.getRecipeHandlers()) {
                if (handler.isProxy()) continue;

                if (handler.getCapability() == ItemRecipeCapability.CAP) {
                    itemHandlers.add((NotifiableRecipeHandlerTrait<Ingredient>) handler);
                } else {
                    fluidHandlers.add((NotifiableRecipeHandlerTrait<FluidIngredient>) handler);
                }
            }
            itemProxyHandler.setHandlers(itemHandlers);
            fluidProxyHandler.setHandlers(fluidHandlers);

            machine.addProxy(this);

            return true;
        } else {
            return false;
        }
    }

    @Nullable
    private MESuperPatternBufferPartMachine getBuffer() {
        var level = getLevel();
        if (level == null || bufferPos == null) return null;
        if (MetaMachine.getMachine(level, bufferPos) instanceof MESuperPatternBufferPartMachine buffer) {
            return buffer;
        } else {
            this.bufferPos = null;
            return null;
        }
    }

    @Override
    public MetaMachine self() {
        var buffer = getBuffer();
        return buffer != null ? buffer.self() : super.self();
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        var buffer = getBuffer();
        return buffer != null;
    }

    @Override
    public @Nullable ModularUI createUI(Player entityPlayer) {
        GTCEu.LOGGER.warn("'createUI' of the Crafting Buffer Proxy was incorrectly called!");
        return null;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onMachineRemoved() {
        var level = getLevel();
        if (level == null || bufferPos == null) return;
        if (MetaMachine.getMachine(getLevel(), this.bufferPos) instanceof MESuperPatternBufferPartMachine machine) {
            machine.removeProxy(this);
        }
    }
}
