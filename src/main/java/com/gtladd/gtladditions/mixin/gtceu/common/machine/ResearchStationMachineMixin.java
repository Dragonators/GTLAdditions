package com.gtladd.gtladditions.mixin.gtceu.common.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.gtladd.gtladditions.utils.CloudTeamUtil;
import com.hepdd.gtmthings.api.capability.IBindable;
import com.hepdd.gtmthings.utils.TeamUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mixin(ResearchStationMachine.class)
public abstract class ResearchStationMachineMixin extends WorkableElectricMultiblockMachine implements IDataStickInteractable, IBindable {

    @Unique
    private static final String GTLADDITIONS$TEAM_ID_KEY = "teamId";

    @Unique
    @Nullable
    private UUID gtladditions$teamId;

    protected ResearchStationMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    @Nullable
    public UUID getUUID() {
        return gtladditions$teamId;
    }

    @Override
    public void setUUID(@Nullable UUID uuid) {
        if (Objects.equals(gtladditions$teamId, uuid)) return;
        gtladditions$teamId = uuid;
        markDirty();
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        gtladditions$teamId = tag.hasUUID(GTLADDITIONS$TEAM_ID_KEY) ? tag.getUUID(GTLADDITIONS$TEAM_ID_KEY) : null;
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        if (gtladditions$teamId == null) {
            tag.remove(GTLADDITIONS$TEAM_ID_KEY);
        } else {
            tag.putUUID(GTLADDITIONS$TEAM_ID_KEY, gtladditions$teamId);
        }
    }

    @Override
    public InteractionResult onDataStickRightClick(Player player, ItemStack stack) {
        return CloudTeamUtil.INSTANCE.bindFromDataStick(this, player);
    }

    @Override
    public boolean onDataStickLeftClick(Player player, ItemStack stack) {
        return CloudTeamUtil.INSTANCE.unbindFromDataStick(this, player);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(GTItems.TOOL_DATA_STICK.asItem())) {
            return onDataStickRightClick(player, stack);
        }
        return super.onUse(state, level, pos, player, hand, hit);
    }

    @Override
    public boolean onLeftClick(Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(GTItems.TOOL_DATA_STICK.asItem())) {
            return onDataStickLeftClick(player, stack);
        }
        return super.onLeftClick(player, level, hand, pos, direction);
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        if (isFormed()) {
            if (gtladditions$teamId != null && getLevel() != null && TeamUtil.hasOwner(getLevel(), gtladditions$teamId)) {
                textList.add(Component.translatable(
                        "gui.gtladditions.cloud.bind_success",
                        TeamUtil.GetName(getLevel(), gtladditions$teamId)));
            } else {
                textList.add(Component.translatable("gui.gtladditions.cloud.not_bound"));
            }
        }
        super.addDisplayText(textList);
    }
}