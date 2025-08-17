package com.gtladd.gtladditions.common.machine.muiltblock.part;

import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import com.gtladd.gtladditions.api.machine.trait.NotifiableMERecipeHandlerTrait;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MESuperPatternBufferRecipeHandlerTrait extends MachineTrait {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MESuperPatternBufferPartMachine.class);
    protected List<Runnable> listeners = new ArrayList<>();

    @Getter
    protected final MEItemInputHandler itemInputHandler;

    @Getter
    protected final MEFluidInputHandler fluidInputHandler;

    public MESuperPatternBufferRecipeHandlerTrait(MESuperPatternBufferPartMachine ioBuffer) {
        super(ioBuffer);
        itemInputHandler = new MEItemInputHandler(ioBuffer);
        fluidInputHandler = new MEFluidInputHandler(ioBuffer);
    }

    @Override
    public MESuperPatternBufferPartMachine getMachine() {
        return (MESuperPatternBufferPartMachine) super.getMachine();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public void onChanged() {
        listeners.forEach(Runnable::run);
    }

    public List<IMERecipeHandlerTrait<?>> getMERecipeHandlers() {
        return List.of(itemInputHandler, fluidInputHandler);
    }

    public Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandlerTrait<?>> getMERecipeHandlerMap() {
        Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandlerTrait<?>> map = new Reference2ObjectArrayMap<>();
        map.put(ItemRecipeCapability.CAP, itemInputHandler);
        map.put(FluidRecipeCapability.CAP, fluidInputHandler);
        return map;
    }

    public boolean handleItemInner(Object2LongMap<AEItemKey> left, boolean simulate, int trySlot) {
        var internalSlot = getMachine().getInternalInventory()[trySlot];
        if (internalSlot.isActive(ItemRecipeCapability.CAP)) {
            return internalSlot.handleItemInternal(left, simulate);
        } else return false;
    }

    public boolean handleFluidInner(Object2LongMap<AEFluidKey> left, boolean simulate, int trySlot) {
        var internalSlot = getMachine().getInternalInventory()[trySlot];
        if (internalSlot.isActive(FluidRecipeCapability.CAP)) {
            return internalSlot.handleFluidInternal(left, simulate);
        } else return false;
    }

    private List<Integer> getActiveSlots(MESuperPatternBufferPartMachine.InternalSlot[] slots, RecipeCapability<?> recipeCapability) {
        return IntStream.range(0, slots.length)
                .filter(i -> slots[i].isActive(recipeCapability))
                .boxed()
                .collect(Collectors.toList());
    }

    public class MEItemInputHandler extends NotifiableMERecipeHandlerTrait<Ingredient> {

        @Getter
        @Setter
        private Object2LongMap<AEItemKey> preparedMEHandleContents = new Object2LongOpenHashMap<>();

        public MEItemInputHandler(MESuperPatternBufferPartMachine machine) {
            super(machine);
        }

        public MESuperPatternBufferPartMachine getMachine() {
            return (MESuperPatternBufferPartMachine) this.machine;
        }

        @Override
        public IO getHandlerIO() {
            return IO.IN;
        }

        @Override
        public int getPriority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public RecipeCapability<Ingredient> getCapability() {
            return ItemRecipeCapability.CAP;
        }

        @Override
        public Ingredient copyContent(Object content) {
            return super.copyContent(content);
        }

        @Override
        public List<Integer> getActiveSlots(RecipeCapability<?> recipeCapability) {
            return MESuperPatternBufferRecipeHandlerTrait.this.getActiveSlots(getMachine().getInternalInventory(), recipeCapability);
        }

        @Override
        public Int2ObjectMap<List<Object>> getActiveSlotsLimitContentsMap() {
            var map = new Int2ObjectArrayMap<List<Object>>();
            var machine = getMachine();
            var shared = machine.getShareInventory().getContents();
            for (int slot : getActiveSlots(ItemRecipeCapability.CAP)) {
                var inputs = machine.getInternalInventory()[slot].getLimitItemStackInput();

                // 通过统一方法获取该槽位的电路（可能来自样板或总成配置）
                ItemStack circuitForRecipe = machine.getCircuitForRecipe(slot);
                if (!circuitForRecipe.isEmpty()) {
                    inputs.add(circuitForRecipe);
                }

                inputs.addAll(shared);
                map.put(slot, inputs);
            }
            return map;
        }

        @Override
        public Object2LongMap<ItemStack> getCustomSlotsStackMap(List<Integer> list) {
            Object2LongOpenHashMap<ItemStack> map = new Object2LongOpenHashMap<>();
            for (int i : list) {
                var slot = getMachine().getInternalInventory()[i];
                for (var it = Object2LongMaps.fastIterator(slot.getItemStackInputMap()); it.hasNext();) {
                    var entry = it.next();
                    map.addTo(entry.getKey(), entry.getLongValue());
                }
            }
            return map;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean meHandleRecipeInner(GTRecipe recipe, Object2LongMap<?> left, boolean simulate, int trySlot) {
            return handleItemInner((Object2LongMap<AEItemKey>) left, simulate, trySlot);
        }

        @Override
        public void prepareMEHandleContents(GTRecipe recipe, List<Ingredient> left, boolean simulate) {
            // 处理总成配置的电路
            getMachine().getMePatternCircuitInventory().handleRecipeInner(IO.IN, recipe, left, null, simulate);

            // 处理共享库存
            getMachine().getShareInventory().handleRecipeInner(IO.IN, recipe, left, null, simulate);

            // 从剩余需求中过滤掉电路，避免从AE网络抽取
            // 处理样板内置电路（如果总成没有配置电路）
            if (getMachine().getCircuitHandler().shouldUsePatternCircuit()) {
                setPreparedMEHandleContents(ingredientsToAEKeyMapExcludingCircuits(left));
            } else {
                setPreparedMEHandleContents(ingredientsToAEKeyMap(left));
            }
        }
    }

    public class MEFluidInputHandler extends NotifiableMERecipeHandlerTrait<FluidIngredient> {

        @Getter
        @Setter
        private Object2LongMap<AEFluidKey> preparedMEHandleContents = new Object2LongOpenHashMap<>();

        public MEFluidInputHandler(MESuperPatternBufferPartMachine machine) {
            super(machine);
        }

        public MESuperPatternBufferPartMachine getMachine() {
            return (MESuperPatternBufferPartMachine) this.machine;
        }

        @Override
        public IO getHandlerIO() {
            return IO.IN;
        }

        @Override
        public int getPriority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public RecipeCapability<FluidIngredient> getCapability() {
            return FluidRecipeCapability.CAP;
        }

        @Override
        public FluidIngredient copyContent(Object content) {
            return super.copyContent(content);
        }

        @Override
        public List<Integer> getActiveSlots(RecipeCapability<?> recipeCapability) {
            return MESuperPatternBufferRecipeHandlerTrait.this.getActiveSlots(getMachine().getInternalInventory(), recipeCapability);
        }

        @Override
        public Int2ObjectMap<List<Object>> getActiveSlotsLimitContentsMap() {
            var map = new Int2ObjectArrayMap<List<Object>>();
            var machine = getMachine();
            var shared = machine.getShareTank().getContents();
            for (int slot : getActiveSlots(FluidRecipeCapability.CAP)) {
                var inputs = machine.getInternalInventory()[slot].getLimitFluidStackInput();
                inputs.addAll(shared);
                map.put(slot, inputs);
            }
            return map;
        }

        @Override
        public Object2LongMap<?> getCustomSlotsStackMap(List<Integer> list) {
            Object2LongOpenHashMap<FluidStack> map = new Object2LongOpenHashMap<>();
            for (int i : list) {
                var slot = getMachine().getInternalInventory()[i];
                for (var it = Object2LongMaps.fastIterator(slot.getFluidStackInputMap()); it.hasNext();) {
                    var entry = it.next();
                    map.addTo(entry.getKey(), entry.getLongValue());
                }
            }
            return map;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean meHandleRecipeInner(GTRecipe recipe, Object2LongMap<?> left, boolean simulate, int trySlot) {
            return handleFluidInner((Object2LongMap<AEFluidKey>) left, simulate, trySlot);
        }

        @Override
        public void prepareMEHandleContents(GTRecipe recipe, List<FluidIngredient> left, boolean simulate) {
            getMachine().getShareTank().handleRecipeInner(IO.IN, recipe, left, null, simulate);
            setPreparedMEHandleContents(fluidIngredientsToAEKeyMap(left));
        }
    }

    // Utility Methods
    public static Pair<Object2LongOpenHashMap<Item>, Object2LongOpenHashMap<Fluid>> mergeInternalSlot(MESuperPatternBufferPartMachine.InternalSlot[] internalSlots) {
        Object2LongOpenHashMap<Item> items = new Object2LongOpenHashMap<>();
        Object2LongOpenHashMap<Fluid> fluids = new Object2LongOpenHashMap<>();
        for (var internalSlot : Arrays.stream(internalSlots).filter(MESuperPatternBufferPartMachine.InternalSlot::isActive).toList()) {
            for (var it = internalSlot.getItemInventory().object2LongEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                items.addTo(entry.getKey().getItem(), entry.getLongValue());
            }
            for (var it = internalSlot.getFluidInventory().object2LongEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                fluids.addTo(entry.getKey().getFluid(), entry.getLongValue());
            }
        }
        return new ImmutablePair<>(items, fluids);
    }

    private static Object2LongMap<AEItemKey> ingredientsToAEKeyMap(List<Ingredient> ingredients) {
        var result = new Object2LongOpenHashMap<AEItemKey>();
        for (Ingredient ingredient : ingredients) {
            ItemStack[] matchingStacks = ingredient.getItems();
            if (matchingStacks.length == 0 || matchingStacks[0].isEmpty()) continue;
            for (ItemStack stack : matchingStacks) {
                if (!stack.isEmpty()) {
                    AEItemKey aeKey = AEItemKey.of(stack);
                    result.addTo(aeKey, stack.getCount());
                }
            }
        }
        return result;
    }

    private static Object2LongMap<AEItemKey> ingredientsToAEKeyMapExcludingCircuits(List<Ingredient> ingredients) {
        var result = new Object2LongOpenHashMap<AEItemKey>();
        for (Ingredient ingredient : ingredients) {
            ItemStack[] matchingStacks = ingredient.getItems();
            if (matchingStacks.length == 0 || matchingStacks[0].isEmpty()) continue;
            for (ItemStack stack : matchingStacks) {
                if (!stack.isEmpty()) {
                    // 过滤掉电路
                    if (stack.getItem() == GTItems.INTEGRATED_CIRCUIT.asItem()) {
                        continue;
                    }
                    AEItemKey aeKey = AEItemKey.of(stack);
                    result.addTo(aeKey, stack.getCount());
                }
            }
        }
        return result;
    }

    private static Object2LongMap<AEFluidKey> fluidIngredientsToAEKeyMap(List<FluidIngredient> ingredients) {
        var result = new Object2LongOpenHashMap<AEFluidKey>();
        for (FluidIngredient ingredient : ingredients) {
            FluidStack[] matchingStacks = ingredient.getStacks();
            for (FluidStack stack : matchingStacks) {
                if (!stack.isEmpty()) {
                    AEFluidKey aeKey = AEFluidKey.of(stack.getFluid());
                    result.addTo(aeKey, stack.getAmount());
                }
            }
        }
        return result;
    }
}
