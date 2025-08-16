package integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.utils.GradientUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import com.gtladd.gtladditions.common.machine.muiltblock.part.MESuperPatternBufferProxyPartMachine;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class MESuperPatternBufferProxyProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof MESuperPatternBufferProxyPartMachine) {
                CompoundTag serverData = blockAccessor.getServerData();

                if (!serverData.getBoolean("formed")) return;
                if (!serverData.getBoolean("bound")) {
                    iTooltip.add(Component.translatable("gtceu.top.buffer_not_bound").withStyle(ChatFormatting.RED));
                    return;
                }

                int[] pos = serverData.getIntArray("pos");
                iTooltip.add(Component.translatable("gtceu.top.buffer_bound_pos", pos[0], pos[1], pos[2])
                        .withStyle(style -> style.withColor(rainbowColor(1.25f))));

                // Show buffer contents
                MESuperPatternBufferProvider.readBufferContents(iTooltip, serverData);
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof MESuperPatternBufferProxyPartMachine proxy) {
                if (!proxy.isFormed()) {
                    compoundTag.putBoolean("formed", false);
                    return;
                }
                compoundTag.putBoolean("formed", true);

                var buffer = proxy.getBuffer();
                if (buffer == null) {
                    compoundTag.putBoolean("bound", false);
                    return;
                }
                compoundTag.putBoolean("bound", true);

                var pos = buffer.getPos();
                compoundTag.putIntArray("pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });

                MESuperPatternBufferProvider.putTag(compoundTag, buffer);
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return GTCEu.id("me_super_pattern_buffer_proxy");
    }

    static TextColor rainbowColor(float speed) {
        return TextColor.fromRgb(GradientUtil.toRGB((GTValues.CLIENT_TIME & ((1 << 20) - 1)) * speed, 95f, 60f));
    }
}
