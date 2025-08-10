package integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.integration.jade.GTElementHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import com.gtladd.gtladditions.common.machine.muiltblock.part.MESuperPatternBufferPartMachine;
import com.gtladd.gtladditions.common.machine.muiltblock.part.MESuperPatternBufferRecipeHandlerTrait;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;

import java.text.NumberFormat;

import static com.gregtechceu.gtceu.utils.FormattingUtil.DECIMAL_FORMAT_2F;

public class MESuperPatternBufferProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof MESuperPatternBufferPartMachine buffer) {

                CompoundTag serverData = blockAccessor.getServerData();

                iTooltip.add(Component.translatable("gtceu.top.proxies_bound", serverData.getInt("proxies"))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));

                ListTag itemTags = serverData.getList("items", Tag.TAG_COMPOUND);
                ListTag fluidTags = serverData.getList("fluids", Tag.TAG_COMPOUND);

                for (Tag t : itemTags) {
                    if (!(t instanceof CompoundTag itemTag)) continue;
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemTag.getString("item")));
                    var count = itemTag.getLong("real");
                    if (item != null && count > 0) {
                        var stack = new ItemStack(item);
                        iTooltip.add(iTooltip.getElementHelper().smallItem(new ItemStack(item)));
                        Component text = Component.literal(" ")
                                .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.DARK_PURPLE))
                                .append(Component.literal("× ").withStyle(ChatFormatting.WHITE))
                                .append(stack.getHoverName().copy().withStyle(ChatFormatting.GOLD));
                        iTooltip.append(text);
                    }
                }
                for (Tag t : fluidTags) {
                    if (!(t instanceof CompoundTag fluidTag)) continue;
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidTag.getString("fluid")));
                    var amount = fluidTag.getLong("real");
                    if (fluid != null && amount > 0) {
                        iTooltip.add(GTElementHelper.smallFluid(JadeFluidObject.of(fluid)));
                        Component text = Component.literal(" ")
                                .append(Component.literal(formatBuckets(amount)))
                                .withStyle(ChatFormatting.DARK_PURPLE)
                                .append(Component.literal(" ").withStyle(ChatFormatting.WHITE))
                                .append(fluid.getFluidType().getDescription().copy().withStyle(ChatFormatting.DARK_AQUA));
                        iTooltip.append(text);
                    }
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof MESuperPatternBufferPartMachine buffer) {
                compoundTag.putInt("proxies", buffer.getProxies().size());

                var merged = MESuperPatternBufferRecipeHandlerTrait.mergeInternalSlot(buffer.getInternalInventory());
                var items = merged.getLeft();
                var fluids = merged.getRight();

                ListTag itemTags = new ListTag();
                for (Item item : items.keySet()) {
                    ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
                    if (key != null) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putString("item", key.toString());
                        itemTag.putLong("real", items.getLong(item));
                        itemTags.add(itemTag);
                    }
                }
                compoundTag.put("items", itemTags);

                ListTag fluidTags = new ListTag();
                for (Fluid fluid : fluids.keySet()) {
                    ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
                    if (key != null) {
                        CompoundTag fluidTag = new CompoundTag();
                        fluidTag.putString("fluid", key.toString());
                        fluidTag.putLong("real", fluids.getLong(fluid));
                        fluidTags.add(fluidTag);
                    }
                }
                compoundTag.put("fluids", fluidTags);
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return GTCEu.id("me_super_pattern_buffer");
    }

    public static String formatNumberReadable(double number, boolean milli, NumberFormat fmt, @Nullable String unit) {
        StringBuilder sb = new StringBuilder();
        if (number < 0) {
            number = -number;
            sb.append('-');
        }

        if (milli && number >= 1e3) {
            milli = false;
            number /= 1e3;
        }

        int exp = 0;
        if (number >= 1e3) {
            exp = (int) Math.log10(number) / 3;
            if (exp > 7) exp = 7;
            if (exp > 0) number /= Math.pow(1e3, exp);
        }

        sb.append(fmt.format(number));
        if (exp > 0) sb.append("kMGTPEZ".charAt(exp - 1));
        else if (milli && number != 0) sb.append('m');

        if (unit != null) sb.append(unit);
        return sb.toString();
    }

    public static String formatBuckets(long mB) {
        return formatNumberReadable(mB, true, DECIMAL_FORMAT_2F, "B");
    }
}
