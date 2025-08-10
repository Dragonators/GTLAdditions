package com.gtladd.gtladditions.common.machine.muiltblock.part;

import org.gtlcore.gtlcore.api.machine.trait.IMEPatternPartMachine;
import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;
import org.gtlcore.gtlcore.api.machine.trait.NotifiableCircuitItemStackHandler;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.ButtonConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancyInvConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancyTankConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEPatternViewSlotWidget;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.crafting.pattern.ProcessingPatternItem;
import appeng.helpers.patternprovider.PatternContainer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.common.machine.GTLAddMachines;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MESuperPatternBufferPartMachine extends MEBusPartMachine implements ICraftingProvider, PatternContainer, IMEPatternPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MESuperPatternBufferPartMachine.class, MEBusPartMachine.MANAGED_FIELD_HOLDER);
    protected static final int MAX_PATTERN_COUNT = 27;
    private final long[] lastNotifyTickBySlot = new long[MAX_PATTERN_COUNT];
    private final ItemStack[] lastSnapshotBySlot = new ItemStack[MAX_PATTERN_COUNT];
    private final InternalInventory internalPatternInventory = new InternalInventory() {

        @Override
        public int size() {
            return MAX_PATTERN_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return patternInventory.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            patternInventory.setStackInSlot(slotIndex, stack);
            patternInventory.onContentsChanged(slotIndex);
            onPatternChange(slotIndex);
        }
    };

    @Getter
    @Persisted
    @DescSynced // Maybe an Expansion Option in the future? a bit redundant for rn. Maybe Packdevs want to add their own
    // version.
    private final ItemStackTransfer patternInventory = new ItemStackTransfer(MAX_PATTERN_COUNT);

    @Getter
    @Persisted
    protected final NotifiableItemStackHandler shareInventory;

    @Getter
    @Persisted
    protected final NotifiableFluidTank shareTank;

    @Getter
    @Persisted
    protected final NotifiableItemStackHandler mePatternCircuitInventory;

    @Getter
    @Persisted
    protected final MESuperPatternBufferPartMachine.InternalSlot[] internalInventory = new MESuperPatternBufferPartMachine.InternalSlot[MAX_PATTERN_COUNT];

    private final BiMap<IPatternDetails, MESuperPatternBufferPartMachine.InternalSlot> detailsSlotMap = HashBiMap.create(MAX_PATTERN_COUNT);

    @DescSynced
    @Persisted
    @Setter
    private String customName = "";

    private boolean needPatternSync;

    @Persisted
    private HashSet<BlockPos> proxies = new HashSet<>();

    protected final MESuperPatternBufferRecipeHandlerTrait recipeHandler = new MESuperPatternBufferRecipeHandlerTrait(this);

    @Nullable
    protected TickableSubscription updateSubs;

    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public void setWorkingEnabled(boolean ignored) {}

    public MESuperPatternBufferPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, IO.IN, args);
        this.patternInventory.setFilter(stack -> stack.getItem() instanceof ProcessingPatternItem);
        Arrays.setAll(internalInventory, i -> new InternalSlot());
        getMainNode().addService(ICraftingProvider.class, this);
        this.mePatternCircuitInventory = new NotifiableCircuitItemStackHandler(this);
        this.shareInventory = new NotifiableItemStackHandler(this, 9, IO.IN, IO.NONE);
        this.shareTank = new NotifiableFluidTank(this, 9, 8 * FluidHelper.getBucket(), IO.IN, IO.NONE);
        Arrays.fill(lastNotifyTickBySlot, Long.MIN_VALUE);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(1, () -> {
                for (int i = 0; i < patternInventory.getSlots(); i++) {
                    var pattern = patternInventory.getStackInSlot(i);
                    var patternDetails = PatternDetailsHelper.decodePattern(pattern, getLevel());
                    if (patternDetails != null) {
                        this.detailsSlotMap.put(patternDetails, this.internalInventory[i]);
                    }
                }
            }));
        }
        this.getRecipeHandlers().forEach(handler -> handler.addChangedListener(() -> getProxies().forEach(proxy -> {
            if (handler.getCapability() == ItemRecipeCapability.CAP) {
                proxy.itemProxyHandler.notifyListeners();
            } else {
                proxy.fluidProxyHandler.notifyListeners();
            }
        })));
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        this.updateSubscription();
    }

    protected void updateSubscription() {
        if (getMainNode().isOnline()) {
            updateSubs = subscribeServerTick(updateSubs, this::update);
        } else if (updateSubs != null) {
            updateSubs.unsubscribe();
            updateSubs = null;
        }
    }

    protected void update() {
        if (needPatternSync) {
            ICraftingProvider.requestUpdate(getMainNode());
            this.needPatternSync = false;
        }
    }

    public void addProxy(MESuperPatternBufferProxyPartMachine proxy) {
        proxies.add(proxy.getPos());
    }

    public void removeProxy(MESuperPatternBufferProxyPartMachine proxy) {
        proxies.remove(proxy.getPos());
    }

    public Set<MESuperPatternBufferProxyPartMachine> getProxies() {
        Set<MESuperPatternBufferProxyPartMachine> proxies1 = new HashSet<>();
        for (var pos : proxies) {
            if (MetaMachine.getMachine(getLevel(), pos) instanceof MESuperPatternBufferProxyPartMachine p) {
                proxies1.add(p);
            }
        }
        return proxies1;
    }

    private void refundAll(ClickData clickData) {
        if (!clickData.isRemote) {
            Arrays.stream(internalInventory).filter(MESuperPatternBufferPartMachine.InternalSlot::isActive)
                    .forEach(MESuperPatternBufferPartMachine.InternalSlot::refund);
        }
    }

    private void onPatternChange(int index) {
        if (isRemote()) return;

        // remove old if applicable
        var internalInv = internalInventory[index];
        var newPattern = patternInventory.getStackInSlot(index);
        var newPatternDetails = PatternDetailsHelper.decodePattern(newPattern, getLevel());
        var oldPatternDetails = detailsSlotMap.inverse().get(internalInv);

        if (newPatternDetails != null) {
            detailsSlotMap.forcePut(newPatternDetails, internalInv);
            internalInv.hasPattern = true;
        } else {
            detailsSlotMap.inverse().remove(internalInv);
            internalInv.hasPattern = false;
        }

        if (oldPatternDetails != null && !oldPatternDetails.equals(newPatternDetails)) {
            internalInv.refund();
        }

        needPatternSync = true;
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////
    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new ButtonConfigurator(
                new GuiTextureGroup(GuiTextures.BUTTON, GuiTextures.REFUND_OVERLAY), this::refundAll)
                .setTooltips(List.of(Component.translatable("gui.gtceu.refund_all.desc"))));
        configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(mePatternCircuitInventory.storage));
        configuratorPanel.attachConfigurators(new FancyInvConfigurator(
                shareInventory.storage, Component.translatable("gui.gtceu.share_inventory.title"))
                .setTooltips(List.of(
                        Component.translatable("gui.gtceu.share_inventory.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))));
        configuratorPanel.attachConfigurators(new FancyTankConfigurator(
                shareTank.getStorages(), Component.translatable("gui.gtceu.share_tank.title"))
                .setTooltips(List.of(
                        Component.translatable("gui.gtceu.share_tank.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))));
    }

    @Override
    public Widget createUIWidget() {
        int rowSize = 9;
        int colSize = 3;
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        int index = 0;
        for (int y = 0; y < colSize; ++y) {
            for (int x = 0; x < rowSize; ++x) {
                int finalI = index;
                var slot = new AEPatternViewSlotWidget(patternInventory, index++, 8 + x * 18, 14 + y * 18)
                        .setOccupiedTexture(GuiTextures.SLOT)
                        .setItemHook(stack -> {
                            if (stack.getItem() instanceof EncodedPatternItem iep) {
                                final ItemStack out = iep.getOutput(stack);
                                return !out.isEmpty() ? out : stack;
                            }
                            return stack;
                        })
                        .setChangeListener(debounceAndFilter(finalI, () -> this.onPatternChange(finalI)))
                        .setBackground(GuiTextures.SLOT, GuiTextures.PATTERN_OVERLAY);
                group.addWidget(slot);
            }
        }
        // ME Network status
        group.addWidget(new LabelWidget(
                8,
                2,
                () -> this.isOnline ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));

        group.addWidget(new AETextInputButtonWidget(18 * rowSize + 8 - 70, 2, 70, 10)
                .setText(customName)
                .setOnConfirm(this::setCustomName)
                .setButtonTooltips(Component.translatable("gui.gtceu.rename.desc")));

        return group;
    }

    private Runnable debounceAndFilter(int slotIndex, Runnable delegate) {
        return () -> {
            long now = getGameTick();
            if (lastNotifyTickBySlot[slotIndex] == now) {
                return;
            }

            ItemStack cur = this.patternInventory.getStackInSlot(slotIndex);
            ItemStack prev = lastSnapshotBySlot[slotIndex];
            if (sameStack(prev, cur)) {
                lastNotifyTickBySlot[slotIndex] = now;
                return;
            }

            lastNotifyTickBySlot[slotIndex] = now;
            lastSnapshotBySlot[slotIndex] = cur.isEmpty() ? ItemStack.EMPTY : cur.copy();

            delegate.run();
        };
    }

    private static boolean sameStack(@Nullable ItemStack a, @Nullable ItemStack b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() ^ b.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(a, b) && a.getCount() == b.getCount();
    }

    private static long getGameTick() {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        return level != null ? level.getGameTime() : System.nanoTime();
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return detailsSlotMap.keySet().stream().filter(Objects::nonNull).toList();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !detailsSlotMap.containsKey(patternDetails) || !checkInput(inputHolder)) {
            return false;
        }

        var slot = detailsSlotMap.get(patternDetails);
        if (slot != null) {
            slot.pushPattern(patternDetails, inputHolder);
            recipeHandler.onChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    private boolean checkInput(KeyCounter[] inputHolder) {
        for (KeyCounter input : inputHolder) {
            var illegal = input.keySet().stream()
                    .map(AEKey::getType)
                    .map(AEKeyType::getId)
                    .anyMatch(id -> !id.equals(AEKeyType.items().getId()) && !id.equals(AEKeyType.fluids().getId()));
            if (illegal) return false;
        }
        return true;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public @Nullable IGrid getGrid() {
        return getMainNode().getGrid();
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return internalPatternInventory;
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        List<IMultiController> controllers = getControllers();
        // has controller
        if (!controllers.isEmpty()) {
            IMultiController controller = controllers.get(0);
            MultiblockMachineDefinition controllerDefinition = controller.self().getDefinition();
            // has customName
            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()),
                        Component.literal(customName),
                        Collections.emptyList());
            } else {
                ItemStack circuitStack = mePatternCircuitInventory.storage.getStackInSlot(0);
                int circuitConfiguration = circuitStack.isEmpty() ? -1 :
                        IntCircuitBehaviour.getCircuitConfiguration(circuitStack);

                Component groupName = circuitConfiguration != -1 ?
                        Component.translatable(controllerDefinition.getDescriptionId())
                                .append(" - " + circuitConfiguration) :
                        Component.translatable(controllerDefinition.getDescriptionId());

                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()), groupName, Collections.emptyList());
            }
        } else {
            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(GTLAddMachines.ME_SUPER_PATTERN_BUFFER.getItem()),
                        Component.literal(customName),
                        Collections.emptyList());
            } else {
                return new PatternContainerGroup(
                        AEItemKey.of(GTLAddMachines.ME_SUPER_PATTERN_BUFFER.getItem()),
                        GTLAddMachines.ME_SUPER_PATTERN_BUFFER.get().getDefinition().getItem().getDescription(),
                        Collections.emptyList());
            }
        }
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(patternInventory);
        clearInventory(shareInventory);
    }

    @Override
    public List<IMERecipeHandlerTrait<?>> getMERecipeHandlerTraits() {
        return recipeHandler.getMERecipeHandlers();
    }

    public class InternalSlot implements ITagSerializable<CompoundTag>, IContentChangeAware {

        @Getter
        @Setter
        protected Runnable onContentsChanged = () -> {
            /**/
        };
        @Getter
        @Setter
        private boolean hasPattern;
        @Getter
        private final Object2LongOpenHashMap<AEItemKey> itemInventory = new Object2LongOpenHashMap<>();
        @Getter
        private final Object2LongOpenHashMap<AEFluidKey> fluidInventory = new Object2LongOpenHashMap<>();

        public InternalSlot() {
            itemInventory.defaultReturnValue(0L);
            fluidInventory.defaultReturnValue(0L);
        }

        public boolean isActive() {
            return hasPattern && (!itemInventory.isEmpty() || !fluidInventory.isEmpty());
        }

        public boolean isActive(RecipeCapability<?> recipeCapability) {
            if (recipeCapability == ItemRecipeCapability.CAP) {
                return hasPattern && !itemInventory.isEmpty();
            } else {
                return hasPattern && !fluidInventory.isEmpty();
            }
        }

        private void addItem(AEItemKey key, long amount) {
            if (amount <= 0L) return;
            itemInventory.addTo(key, amount);
            MESuperPatternBufferPartMachine.this.recipeHandler.getItemInputHandler().notifyListeners();
        }

        private void addFluid(AEFluidKey key, long amount) {
            if (amount <= 0L) return;
            fluidInventory.addTo(key, amount);
            MESuperPatternBufferPartMachine.this.recipeHandler.getFluidInputHandler().notifyListeners();
        }

        public Object2LongMap<ItemStack> getItemStackInputMap() {
            var itemInputMap = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());
            for (Object2LongMap.Entry<AEItemKey> entry : Object2LongMaps.fastIterable(itemInventory)) {
                AEItemKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (amount <= 0) continue;

                ItemStack stack = key.toStack(1);
                itemInputMap.addTo(stack, amount);
            }
            return itemInputMap;
        }

        public Object2LongMap<FluidStack> getFluidStackInputMap() {
            var fluidInputMap = new Object2LongOpenCustomHashMap<FluidStack>(FluidStackHashStrategy.comparingAllButAmount());
            for (Object2LongMap.Entry<AEFluidKey> entry : Object2LongMaps.fastIterable(fluidInventory)) {
                AEFluidKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (amount <= 0) continue;

                FluidStack stack = FluidStack.create(key.getFluid(), 1);
                fluidInputMap.addTo(stack, amount);
            }
            return fluidInputMap;
        }

        public List<Object> getLimitItemStackInput() {
            var limitInput = new ObjectArrayList<>(itemInventory.size());
            for (Object2LongMap.Entry<AEItemKey> entry : Object2LongMaps.fastIterable(itemInventory)) {
                AEItemKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (amount <= 0) continue;
                limitInput.add(key.toStack(Ints.saturatedCast(amount)));
            }
            return limitInput;
        }

        public List<Object> getLimitFluidStackInput() {
            var limitInput = new ObjectArrayList<>(fluidInventory.size());
            for (Object2LongMap.Entry<AEFluidKey> entry : Object2LongMaps.fastIterable(fluidInventory)) {
                AEFluidKey key = entry.getKey();
                long amount = entry.getLongValue();
                if (amount <= 0) continue;
                limitInput.add(FluidStack.create(key.getFluid(), amount));
            }
            return limitInput;
        }

        public void refund() {
            var network = getMainNode().getGrid();
            if (network != null) {
                MEStorage networkInv = network.getStorageService().getInventory();
                var energy = network.getEnergyService();
                refundEntries(itemInventory, (key, amount) -> StorageHelper.poweredInsert(energy, networkInv, key, amount, actionSource));
                refundEntries(fluidInventory, (key, amount) -> StorageHelper.poweredInsert(energy, networkInv, key, amount, actionSource));
                onContentsChanged.run();
            }
        }

        private <T> void refundEntries(Object2LongOpenHashMap<T> map, java.util.function.BiFunction<T, Long, Long> inserter) {
            for (var it = map.object2LongEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                long amount = entry.getLongValue();
                if (amount <= 0) {
                    it.remove();
                    continue;
                }
                long inserted = inserter.apply(entry.getKey(), amount);
                if (inserted > 0) {
                    long left = amount - inserted;
                    if (left <= 0) {
                        it.remove();
                    } else {
                        entry.setValue(left);
                    }
                }
            }
        }

        public void pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
            patternDetails.pushInputsToExternalInventory(inputHolder, (what, amount) -> {
                if (what instanceof AEFluidKey key) {
                    addFluid(key, amount);
                }

                if (what instanceof AEItemKey key) {
                    addItem(key, amount);
                }
            });
            onContentsChanged.run();
        }

        public boolean handleItemInternal(Object2LongMap<AEItemKey> left, boolean simulate) {
            return handleInternal(left, itemInventory, simulate);
        }

        public boolean handleFluidInternal(Object2LongMap<AEFluidKey> left, boolean simulate) {
            return handleInternal(left, fluidInventory, simulate);
        }

        private <T> boolean handleInternal(Object2LongMap<T> left, Object2LongOpenHashMap<T> inventory, boolean simulate) {
            if (left.isEmpty()) return true;

            // 首先检查所有key是否都满足条件，避免对left进行不必要的修改
            for (Object2LongMap.Entry<T> entry : Object2LongMaps.fastIterable(left)) {
                var key = entry.getKey();
                long needAmount = entry.getLongValue();
                if (needAmount <= 0) continue;

                if (inventory.getLong(key) < needAmount) {
                    return false;
                }
            }

            // 如果不是模拟模式，执行对left以及Inventory实际的消耗操作
            if (!simulate) {
                for (var it = Object2LongMaps.fastIterator(left); it.hasNext();) {
                    var entry = it.next();
                    var key = entry.getKey();
                    long needAmount = entry.getLongValue();
                    if (needAmount <= 0) {
                        it.remove();
                        continue;
                    }

                    inventory.addTo(key, -needAmount);
                    it.remove();
                }
                onContentsChanged.run();
            }

            return true;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();

            ListTag itemsTag = new ListTag();
            for (var it = itemInventory.object2LongEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                var ct = entry.getKey().toTag();
                ct.putLong("real", entry.getLongValue());
                itemsTag.add(ct);
            }
            if (!itemsTag.isEmpty()) tag.put("inventory", itemsTag);

            ListTag fluidsTag = new ListTag();
            for (var entry : fluidInventory.object2LongEntrySet()) {
                var ct = entry.getKey().toTag();
                ct.putLong("real", entry.getLongValue());
                fluidsTag.add(ct);
            }
            if (!fluidsTag.isEmpty()) tag.put("fluidInventory", fluidsTag);

            tag.putBoolean("hasPattern", hasPattern);

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            ListTag items = tag.getList("inventory", Tag.TAG_COMPOUND);
            for (Tag t : items) {
                if (!(t instanceof CompoundTag ct)) continue;
                var key = AEItemKey.fromTag(ct);
                var count = ct.getLong("real");
                if (key != null && count > 0) {
                    itemInventory.put(key, count);
                }
            }

            ListTag fluids = tag.getList("fluidInventory", Tag.TAG_COMPOUND);
            for (Tag t : fluids) {
                if (!(t instanceof CompoundTag ct)) continue;
                var key = AEFluidKey.fromTag(ct);
                var amount = ct.getLong("real");
                if (key != null && amount > 0) {
                    fluidInventory.put(key, amount);
                }
            }

            hasPattern = tag.getBoolean("hasPattern");
        }
    }
}
