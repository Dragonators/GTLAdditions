package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(AbstractDHDEntity.class)
public abstract class AbstractDHDEntityMixin extends EnergyBlockEntity {

    @Shadow(remap = false)
    protected AbstractStargateEntity stargate;

    @Shadow(remap = false)
    protected abstract SoundEvent getEnterSound();

    @Shadow(remap = false)
    protected abstract SoundEvent getPressSound();

    @Shadow(remap = false)
    public void sendMessageToNearbyPlayers(Component message, int distance) {
        throw new AssertionError();
    }

    public AbstractDHDEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean canGenerateEnergy) {
        super(type, pos, state, canGenerateEnergy);
    }

    /**
     * @author Dragons
     * @reason No Energy cost
     */
    @Overwrite(remap = false)
    public void engageChevron(int symbol) {
        if (this.stargate != null) {
            if (symbol == 0)
                Objects.requireNonNull(level).playSound(null, this.getBlockPos(), getEnterSound(), SoundSource.BLOCKS, 0.5F, 1F);
            else
                Objects.requireNonNull(level).playSound(null, this.getBlockPos(), getPressSound(), SoundSource.BLOCKS, 0.25F, 1F);

            stargate.dhdEngageSymbol(symbol);
        } else
            sendMessageToNearbyPlayers(Component.translatable("message.sgjourney.dhd.error.not_connected_to_stargate").withStyle(ChatFormatting.DARK_RED), 5);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        var tempCap = super.getCapability(cap, side);
        if (tempCap.isPresent() && tempCap.resolve().isPresent()) {
            if (cap == ForgeCapabilities.ENERGY)
                return LazyOptional.empty();
        }
        return tempCap;
    }

    @Override
    protected void outputEnergy(Direction outputDirection) {}

    /**
     * @author Dragons
     * @reason Disable
     */
    @Overwrite(remap = false)
    private void tryStoreEnergy(ItemStack energyStack) {}

    /**
     * @author Dragons
     * @reason Disable
     */
    @Overwrite(remap = false)
    private void tryPowerStargate(ItemStack energyStack) {}

    /**
     * @author Dragons
     * @reason Disable
     */
    @Overwrite(remap = false)
    public static void tick(Level level, BlockPos pos, BlockState state, AbstractDHDEntity dhd) {
        if (level.isClientSide())
            return;

        dhd.updateClient();
    }
}
