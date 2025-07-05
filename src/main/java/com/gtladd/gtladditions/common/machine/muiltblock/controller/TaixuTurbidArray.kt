package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.pattern.util.IValueContainer;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.TierCasingMachine;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.pattern.util.PatternMatchContext;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TaixuTurbidArray extends TierCasingMachine implements ParallelMachine, IMachineModifyDrops {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER;
    @Persisted
    public final NotifiableItemStackHandler machineStorage;
    private ICoilType coilType;
    private int height = 0;
    private static final Map<Integer, Integer> coil = new HashMap<>();
    private static final ItemStack ENDERIUM = Registries.getItemStack("gtceu:enderium_nanoswarm", 64);
    private static final ItemStack DRACONIUM = Registries.getItemStack("gtceu:draconium_nanoswarm", 64);
    private static final ItemStack SPACETIME = Registries.getItemStack("gtceu:spacetime_nanoswarm", 64);
    private static final ItemStack ETERNITY = Registries.getItemStack("gtceu:eternity_nanoswarm", 64);

    public TaixuTurbidArray(IMachineBlockEntity holder) {
        super(holder, "SCTier");
        this.machineStorage = createMachineStorage();
        this.coilType = CoilBlock.CoilType.CUPRONICKEL;
    }

    protected NotifiableItemStackHandler createMachineStorage() {
        NotifiableItemStackHandler handler = new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH, (slots) -> new ItemStackTransfer(1) {

            public int getSlotLimit(int slot) {
                return 64;
            }
        });
        handler.setFilter(this::filter);
        return handler;
    }

    @Override
    public @NotNull Widget createUIWidget() {
        Widget widget = super.createUIWidget();
        if (widget instanceof WidgetGroup group) {
            Size size = group.getSize();
            group.addWidget((new SlotWidget(this.machineStorage.storage, 0, size.width - 30, size.height - 30, true, true))
                    .setBackground(GuiTextures.SLOT).setHoverTooltips(this.slotTooltips()));
        }
        return widget;
    }

    protected boolean filter(@NotNull ItemStack itemStack) {
        Item item = itemStack.getItem();
        return ENDERIUM.is(item) || DRACONIUM.is(item) || SPACETIME.is(item) || ETERNITY.is(item);
    }

    public void onDrops(List<ItemStack> drops) {
        this.clearInventory(this.machineStorage.storage);
    }

    private List<Component> slotTooltips() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("最多可以放入64个物品"));
        tooltip.add(Component.literal("可放入以下物品与提供对应的加成："));
        tooltip.add(Component.literal("末影纳米蜂群：0.01"));
        tooltip.add(Component.literal("龙纳米蜂群：0.05"));
        tooltip.add(Component.literal("时空纳米蜂群：0.1"));
        tooltip.add(Component.literal("永恒纳米蜂群：0.2"));
        return tooltip;
    }

    public static @Nullable GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams params, @NotNull OCResult result) {
        if (machine instanceof TaixuTurbidArray turbidArray) {
            GTRecipe recipe1 = recipe.copy();
            GTRecipeBuilder builder = new GTRecipeBuilder(Objects.requireNonNull(ResourceLocation.tryParse("uu")), GTRecipeTypes.DUMMY_RECIPES);
            recipe1.outputs.put(FluidRecipeCapability.CAP, new ArrayList<>());
            if (Math.random() * 100 <= turbidArray.successRateA() && turbidArray.getEnergyTier() >= GTValues.UIV) {
                builder.outputFluids(GTLMaterials.UuAmplifier.getFluid(turbidArray.baseOutputFluid1()));
            }
            if (Math.random() * 100 <= turbidArray.successRateB() && turbidArray.getEnergyTier() >= GTValues.OpV) {
                builder.outputFluids(GTMaterials.UUMatter.getFluid(turbidArray.baseOutputFluid2()));
            }
            if (builder.buildRawRecipe().outputs.containsKey(FluidRecipeCapability.CAP)) {
                recipe1.outputs.get(FluidRecipeCapability.CAP).addAll(builder.buildRawRecipe().outputs.get(FluidRecipeCapability.CAP));
            }
            recipe1 = GTRecipeModifiers.accurateParallel(machine, recipe1, turbidArray.getMaxParallel(), false).getFirst();
            recipe1.duration = 100;
            RecipeHelper.setInputEUt(recipe1, 524288L * GTValues.V[turbidArray.getEnergyTier()]);
            return recipe1;
        }
        return null;
    }

    public void onStructureFormed() {
        super.onStructureFormed();
        PatternMatchContext context = this.getMultiblockState().getMatchContext();
        Object type = context.get("CoilType");
        if (type instanceof ICoilType coiltype) this.coilType = coiltype;
        Object var3 = context.getOrCreate("SpeedPipeValue", IValueContainer::noop).getValue();
        if (var3 instanceof Integer integer) this.height = integer - 2;
    }

    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        return true;
    }

    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed) {
            textList.add(Component.literal("高度：" + this.height));
            textList.add(Component.literal("最大并行数：" + this.getMaxParallel()));
            if (this.getEnergyTier() >= GTValues.UIV) {
                textList.add(Component.literal("UU增幅液成功概率：" + this.successRateA() + "%"));
                textList.add(Component.literal("UU增幅液基础输出量：" + this.baseOutputFluid1() + "mb"));
                if (this.getEnergyTier() >= GTValues.OpV) {
                    textList.add(Component.literal("UU物质成功概率：" + this.successRateB() + "%"));
                    textList.add(Component.literal("UU物质基础输出量：" + this.baseOutputFluid2() + "mb"));
                }
            }
        }
    }

    private double frameA() {
        return 8.0 * (Math.pow(2, this.getCasingTier()) - 1) * Math.sqrt(GTValues.ALL_TIERS[this.getEnergyTier()] + 1);
    }

    private double frameB() {
        return 3.8 * Math.pow(1.3, coil.get(this.coilType.getCoilTemperature())) * Math.pow(this.coilType.getCoilTemperature() / 36000.0, 0.7);
    }

    private int successRateA() {
        return (int) Math.round(100 / (1 + Math.exp(-0.1 * (this.frameA() / 50 + this.frameB() / 100 + this.height / 9.0))) + this.getSlotAdd());
    }

    private int successRateB() {
        return (int) Math.round(100 * (1 - Math.exp(-0.02 * ((this.frameA() + this.frameB()) / 20.0 + Math.cbrt(this.height) * this.getEnergyTier() / 7.0))) + this.getSlotAdd());
    }

    private int baseOutputFluid1() {
        return (int) (4096 * (1 - Math.exp(-0.015 * (this.frameA() * this.height / 16.0 + this.frameB() * Math.log(this.getEnergyTier() + 2)))));
    }

    private int baseOutputFluid2() {
        return (int) (2250 * Math.tanh(Math.sqrt(this.frameA() * this.frameB()) * (this.height + this.getEnergyTier()) * 0.06 / 200.0));
    }

    @Override
    public int getMaxParallel() {
        return (int) (4096 * Math.pow(1.5, ((double) this.coilType.getCoilTemperature() / 6400)));
    }

    private int getEnergyTier() {
        return GTUtil.getFloorTierByVoltage(this.getMaxVoltage());
    }

    private double getSlotAdd() {
        Item item = this.machineStorage.storage.getStackInSlot(0).getItem();
        int amount = this.machineStorage.storage.getStackInSlot(0).getCount();
        if (ENDERIUM.is(item)) return 0.01 * amount;
        else if (DRACONIUM.is(item)) return 0.05 * amount;
        else if (SPACETIME.is(item)) return 0.1 * amount;
        else if (ETERNITY.is(item)) return 0.2 * amount;
        return 0.0;
    }

    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    static {
        MANAGED_FIELD_HOLDER = new ManagedFieldHolder(TaixuTurbidArray.class, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);
        coil.put(1800, 1);
        coil.put(2700, 2);
        coil.put(3600, 3);
        coil.put(4500, 4);
        coil.put(5400, 5);
        coil.put(7200, 6);
        coil.put(9001, 7);
        coil.put(10800, 8);
        coil.put(12600, 9);
        coil.put(14400, 10);
        coil.put(16200, 11);
        coil.put(18900, 12);
        coil.put(21600, 13);
        coil.put(36000, 14);
        coil.put(62000, 15);
        coil.put(96000, 16);
    }
}
