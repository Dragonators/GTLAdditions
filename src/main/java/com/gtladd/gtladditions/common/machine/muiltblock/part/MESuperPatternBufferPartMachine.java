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
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;
import com.gregtechceu.gtceu.utils.ResearchManager;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.crafting.pattern.ProcessingPatternItem;
import appeng.helpers.patternprovider.PatternContainer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.common.machine.GTLAddMachines;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MESuperPatternBufferPartMachine extends MEBusPartMachine
                                             implements ICraftingProvider, PatternContainer, IMEPatternPartMachine, IInteractedMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MESuperPatternBufferPartMachine.class, MEBusPartMachine.MANAGED_FIELD_HOLDER);

    private final boolean[] hasPatternArray;
    private final long[] lastNotifyTickBySlot;
    private final ItemStack[] lastSnapshotBySlot;

    private final InternalInventory internalPatternInventory = new InternalInventory() {

        @Override
        public int size() {
            return paginationUIManager.getMaxPatternCount();
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
    @DescSynced
    private final ItemStackTransfer patternInventory;

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
    protected final InternalSlot[] internalInventory;

    private final BiMap<IPatternDetails, Integer> patternSlotMap;

    private boolean needPatternSync;

    @Persisted
    private HashSet<BlockPos> proxies = new HashSet<>();

    @DescSynced
    @Persisted
    @Setter
    private String customName = "";

    @Persisted
    @Getter
    private final PendingRefundData pendingRefundData;

    /** Recipe handler trait for ME Pattern Buffer */
    protected final MESuperPatternBufferRecipeHandlerTrait recipeHandler = new MESuperPatternBufferRecipeHandlerTrait(this);

    /** Pattern circuit handler for managing circuit logic */
    @Getter
    protected final PatternCircuitHandler circuitHandler;

    @DescSynced
    @Persisted
    private int currentPage = 0;
    protected final PaginationUIManager paginationUIManager;

    public MESuperPatternBufferPartMachine(IMachineBlockEntity holder, Object... args) {
        this(holder, 9, 6, 3, args);
    }

    public MESuperPatternBufferPartMachine(IMachineBlockEntity holder, int patternsPerRow, int rowsPerPage, int maxPages, Object... args) {
        super(holder, IO.IN, args);

        // Initialize pagination UI manager
        this.paginationUIManager = new PaginationUIManager(
                patternsPerRow, rowsPerPage, maxPages,
                () -> currentPage,                          // currentPageSupplier
                page -> currentPage = page,                 // currentPageSetter
                slotIndex -> debounceAndFilter(slotIndex, () -> this.onPatternChange(slotIndex)) // changeListenerFactory
        );

        // Initialize arrays with calculated size
        int maxPatternCount = paginationUIManager.getMaxPatternCount();
        this.hasPatternArray = new boolean[maxPatternCount];
        this.lastNotifyTickBySlot = new long[maxPatternCount];
        this.lastSnapshotBySlot = new ItemStack[maxPatternCount];
        this.internalInventory = new InternalSlot[maxPatternCount];
        this.patternSlotMap = HashBiMap.create(maxPatternCount);

        // Initialize inventories
        this.patternInventory = new ItemStackTransfer(maxPatternCount);
        this.patternInventory.setFilter(stack -> stack.getItem() instanceof ProcessingPatternItem);
        Arrays.setAll(internalInventory, InternalSlot::new);
        getMainNode().addService(ICraftingProvider.class, this);
        Arrays.fill(lastNotifyTickBySlot, Long.MIN_VALUE);

        this.mePatternCircuitInventory = new NotifiableCircuitItemStackHandler(this);
        this.shareInventory = new NotifiableItemStackHandler(this, 9, IO.IN, IO.NONE);
        this.shareTank = new NotifiableFluidTank(this, 9, 8 * FluidHelper.getBucket(), IO.IN, IO.NONE);

        this.pendingRefundData = new PendingRefundData();
        this.circuitHandler = new PatternCircuitHandler((NotifiableCircuitItemStackHandler) mePatternCircuitInventory);
    }

    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public void setWorkingEnabled(boolean ignored) {}

    // ========================================
    // LIFECYCLE & NETWORK MANAGEMENT
    // ========================================

    @Nullable
    protected TickableSubscription updateSubs;

    @Override
    public void onLoad() {
        super.onLoad();
        this.getMERecipeHandlerTraits().forEach(handler -> handler.addChangedListener(() -> getProxies().forEach(proxy -> {
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

    private void onPatternChange(int index) {
        if (isRemote()) return;

        var internalInv = internalInventory[index];
        var newPattern = patternInventory.getStackInSlot(index);
        var newPatternDetailsWithOutCircuit = getRealPattern(index, newPattern);
        var oldPatternDetails = patternSlotMap.inverse().get(index);

        // Update pattern mapping and tracking
        if (newPatternDetailsWithOutCircuit != null) {
            patternSlotMap.forcePut(newPatternDetailsWithOutCircuit, index);
            hasPatternArray[index] = true;
        } else {
            patternSlotMap.inverse().remove(index);
            hasPatternArray[index] = false;
        }

        // Refund old pattern contents if pattern changed
        // remove old pattern cache
        if (oldPatternDetails != null && !oldPatternDetails.equals(newPatternDetailsWithOutCircuit)) {
            internalInv.storedCircuit = ItemStack.EMPTY;
            internalInv.cacheManager.clearAllCaches();
            refundSlot(internalInv);
            pendingRefundData.processPendingRefunds();
        }

        needPatternSync = true;
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(patternInventory);
        clearInventory(shareInventory);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ========================================
    // PROXY MANAGEMENT SYSTEM
    // ========================================

    public void addProxy(MESuperPatternBufferProxyPartMachine proxy) {
        proxies.add(proxy.getPos());
    }

    public void removeProxy(MESuperPatternBufferProxyPartMachine proxy) {
        proxies.remove(proxy.getPos());
    }

    public Set<MESuperPatternBufferProxyPartMachine> getProxies() {
        Set<MESuperPatternBufferProxyPartMachine> activatedProxies = new HashSet<>();
        for (var pos : proxies) {
            if (MetaMachine.getMachine(Objects.requireNonNull(getLevel()), pos) instanceof MESuperPatternBufferProxyPartMachine proxy) {
                activatedProxies.add(proxy);
            }
        }
        return activatedProxies;
    }

    // ========================================
    // REFUND SYSTEM
    // ========================================

    private void refundAll(ClickData clickData) {
        if (!clickData.isRemote) {
            // Move all slot contents to pending refund
            Arrays.stream(internalInventory)
                    .filter(InternalSlot::isActive)
                    .forEach(this::refundSlot);

            // Immediately try to process pending refunds
            pendingRefundData.processPendingRefunds();
        }
    }

    public void refundSlot(InternalSlot slot) {
        // Move all item contents to pending refund
        for (var it = slot.itemInventory.object2LongEntrySet().fastIterator(); it.hasNext();) {
            var entry = it.next();
            long amount = entry.getLongValue();
            if (amount > 0) {
                pendingRefundData.addItem(entry.getKey(), amount);
                it.remove();
            }
        }

        // Move all fluid contents to pending refund
        for (var it = slot.fluidInventory.object2LongEntrySet().fastIterator(); it.hasNext();) {
            var entry = it.next();
            long amount = entry.getLongValue();
            if (amount > 0) {
                pendingRefundData.addFluid(entry.getKey(), amount);
                it.remove();
            }
        }
    }

    // ========================================
    // DATASTICK INTERACTION
    // ========================================

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResult.PASS;

        if (stack.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!world.isClientSide) {
                // Check if it's research data - if so, pass to avoid conflicts
                Pair<GTRecipeType, String> researchData = ResearchManager.readResearchId(stack);
                if (researchData != null) {
                    return InteractionResult.PASS;
                }

                // Store this pattern buffer's position in the data stick
                stack.getOrCreateTag().putIntArray("pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    // ========================================
    // GUI SYSTEM
    // ========================================

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        // Refund all button
        configuratorPanel.attachConfigurators(new ButtonConfigurator(
                new GuiTextureGroup(GuiTextures.BUTTON, GuiTextures.REFUND_OVERLAY), this::refundAll)
                .setTooltips(List.of(Component.translatable("gui.gtceu.refund_all.desc"))));

        // Circuit configurator
        configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(mePatternCircuitInventory.storage));

        // Share inventory configurator
        configuratorPanel.attachConfigurators(new FancyInvConfigurator(
                shareInventory.storage, Component.translatable("gui.gtceu.share_inventory.title"))
                .setTooltips(List.of(
                        Component.translatable("gui.gtceu.share_inventory.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))));

        // Share tank configurator
        configuratorPanel.attachConfigurators(new FancyTankConfigurator(
                shareTank.getStorages(), Component.translatable("gui.gtceu.share_tank.title"))
                .setTooltips(List.of(
                        Component.translatable("gui.gtceu.share_tank.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))));
    }

    @Override
    public @NotNull Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, paginationUIManager.getUiWidth(), paginationUIManager.getUiHeight());

        // ME Network status indicator
        group.addWidget(new LabelWidget(8, 2,
                () -> this.isOnline ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));

        // Custom name input widget
        group.addWidget(new AETextInputButtonWidget(paginationUIManager.getUiWidth() - 78, 2, 70, 10)
                .setText(customName)
                .setOnConfirm(this::setCustomName)
                .setButtonTooltips(Component.translatable("gui.gtceu.rename.desc")));

        // Create pagination UI using the manager
        Widget paginationWidget = paginationUIManager.createPaginationUI(patternInventory);
        group.addWidget(paginationWidget);

        return group;
    }

    // ========================================
    // PERFORMANCE OPTIMIZATION UTILITIES
    // ========================================

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

    // ========================================
    // CIRCUIT HANDLING
    // ========================================

    private IPatternDetails getRealPattern(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            var internalSlot = internalInventory[slot];
            return circuitHandler.processPatternWithCircuit(
                    stack.copy(),
                    circuit -> internalSlot.storedCircuit = circuit,
                    getLevel());
        }
        return null;
    }

    /**
     * 获取用于配方的电路
     * 
     * @param slotIndex 槽位索引
     * @return 电路ItemStack，可能为空
     */
    public ItemStack getCircuitForRecipe(int slotIndex) {
        return circuitHandler.getCircuitForRecipe(internalInventory[slotIndex].getStoredCircuit());
    }

    // ========================================
    // AE2 CRAFTING
    // ========================================

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patternSlotMap.keySet().stream().filter(Objects::nonNull).toList();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!getMainNode().isActive() || !patternSlotMap.containsKey(patternDetails) || !checkInput(inputHolder)) {
            return false;
        }

        var slotIndex = patternSlotMap.get(patternDetails);
        if (slotIndex != null && slotIndex >= 0) {
            internalInventory[slotIndex].pushPattern(patternDetails, inputHolder);
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

    // ========================================
    // PATTERN CONTAINER IMPLEMENTATION
    // ========================================

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

        // Handle multiblock controller grouping
        if (!controllers.isEmpty()) {
            IMultiController controller = controllers.get(0);
            MultiblockMachineDefinition controllerDefinition = controller.self().getDefinition();

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

    // ========================================
    // IMEPatternPartMachine
    // ========================================

    @Override
    public List<IMERecipeHandlerTrait<?>> getMERecipeHandlerTraits() {
        return recipeHandler.getMERecipeHandlers();
    }

    public Reference2ObjectMap<RecipeCapability<?>, IMERecipeHandlerTrait<?>> getMERecipeHandlerMap() {
        return recipeHandler.getMERecipeHandlerMap();
    }

    /**
     * Internal Slot: Pattern-specific inventory management
     * 
     * Each slot represents a single pattern's ingredient storage with separate item and fluid inventories.
     * Provides optimized handling for recipe matching
     * and ingredient consumption with efficient serialization.
     * 
     * Features:
     * - Separate inventories per pattern
     * - Efficient recipe matching with early exit conditions
     */
    public class InternalSlot implements ITagSerializable<CompoundTag>, IContentChangeAware {

        @Getter
        @Setter
        protected Runnable onContentsChanged = () -> {};

        @Getter
        private final Object2LongOpenHashMap<AEItemKey> itemInventory = new Object2LongOpenHashMap<>();

        @Getter
        private final Object2LongOpenHashMap<AEFluidKey> fluidInventory = new Object2LongOpenHashMap<>();

        @Getter
        private final int slotIndex;

        @Getter
        private ItemStack storedCircuit = ItemStack.EMPTY;

        @Persisted
        @Getter
        private final SlotCacheManager cacheManager = new SlotCacheManager();

        public InternalSlot(int slotIndex) {
            this.slotIndex = slotIndex;
            itemInventory.defaultReturnValue(0L);
            fluidInventory.defaultReturnValue(0L);
        }

        public boolean isActive() {
            return hasPatternArray[slotIndex] && (!itemInventory.isEmpty() || !fluidInventory.isEmpty());
        }

        public boolean isActive(RecipeCapability<?> recipeCapability) {
            if (recipeCapability == ItemRecipeCapability.CAP) {
                return hasPatternArray[slotIndex] && (!itemInventory.isEmpty() || !storedCircuit.isEmpty());
            } else {
                return hasPatternArray[slotIndex] && !fluidInventory.isEmpty();
            }
        }

        private void add(AEKey what, long amount) {
            if (amount <= 0L) return;
            if (what instanceof AEItemKey itemKey) {
                itemInventory.addTo(itemKey, amount);
            } else if (what instanceof AEFluidKey fluidKey) {
                fluidInventory.addTo(fluidKey, amount);
            }
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
            var fluidInputMap = new Object2LongOpenCustomHashMap<>(FluidStackHashStrategy.comparingAllButAmount());
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

        public void pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
            patternDetails.pushInputsToExternalInventory(inputHolder, this::add);
            recipeHandler.getFluidInputHandler().notifyListeners();
            recipeHandler.getItemInputHandler().notifyListeners();
            onContentsChanged.run();
        }

        public boolean handleItemInternal(Object2LongMap<Ingredient> left, boolean simulate) {
            if (left.isEmpty()) return true;

            for (Object2LongMap.Entry<Ingredient> entry : Object2LongMaps.fastIterable(left)) {
                var ingredient = entry.getKey();
                long needAmount = entry.getLongValue();
                if (needAmount <= 0) continue;

                AEItemKey bestMatch = cacheManager.getBestItemMatch(ingredient, itemInventory, needAmount);
                if (bestMatch == null) {
                    return false;
                }
            }

            if (!simulate) {
                for (var it = Object2LongMaps.fastIterator(left); it.hasNext();) {
                    var entry = it.next();
                    var ingredient = entry.getKey();
                    long needAmount = entry.getLongValue();
                    if (needAmount <= 0) {
                        it.remove();
                        continue;
                    }

                    AEItemKey bestMatch = cacheManager.getBestItemMatch(ingredient, itemInventory, needAmount);
                    if (bestMatch != null) {
                        itemInventory.addTo(bestMatch, -needAmount);
                        it.remove();
                    }
                }
                onContentsChanged.run();
            }

            return true;
        }

        public boolean handleFluidInternal(Object2LongMap<FluidIngredient> left, boolean simulate) {
            if (left.isEmpty()) return true;

            for (Object2LongMap.Entry<FluidIngredient> entry : Object2LongMaps.fastIterable(left)) {
                var ingredient = entry.getKey();
                long needAmount = entry.getLongValue();
                if (needAmount <= 0) continue;

                AEFluidKey bestMatch = cacheManager.getBestFluidMatch(ingredient, fluidInventory, needAmount);
                if (bestMatch == null) {
                    return false;
                }
            }

            if (!simulate) {
                for (var it = Object2LongMaps.fastIterator(left); it.hasNext();) {
                    var entry = it.next();
                    var ingredient = entry.getKey();
                    long needAmount = entry.getLongValue();
                    if (needAmount <= 0) {
                        it.remove();
                        continue;
                    }

                    AEFluidKey bestMatch = cacheManager.getBestFluidMatch(ingredient, fluidInventory, needAmount);
                    if (bestMatch != null) {
                        fluidInventory.addTo(bestMatch, -needAmount);
                        it.remove();
                    }
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

            if (!storedCircuit.isEmpty()) {
                tag.put("storedCircuit", storedCircuit.save(new CompoundTag()));
            }

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            itemInventory.clear();
            fluidInventory.clear();

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

            if (tag.contains("storedCircuit")) {
                this.storedCircuit = ItemStack.of(tag.getCompound("storedCircuit"));
            } else {
                this.storedCircuit = ItemStack.EMPTY;
            }
        }
    }

    public class PendingRefundData implements ITagSerializable<CompoundTag>, IContentChangeAware {

        @Getter
        @Setter
        protected Runnable onContentsChanged = () -> {};

        @Getter
        private final Object2LongOpenHashMap<AEItemKey> pendingRefundItems = new Object2LongOpenHashMap<>();

        @Getter
        private final Object2LongOpenHashMap<AEFluidKey> pendingRefundFluids = new Object2LongOpenHashMap<>();

        public PendingRefundData() {
            pendingRefundItems.defaultReturnValue(0L);
            pendingRefundFluids.defaultReturnValue(0L);
        }

        public void addItem(AEItemKey key, long amount) {
            if (amount > 0) {
                pendingRefundItems.addTo(key, amount);
            }
        }

        public void addFluid(AEFluidKey key, long amount) {
            if (amount > 0) {
                pendingRefundFluids.addTo(key, amount);
            }
        }

        public void processPendingRefunds() {
            var network = getMainNode().getGrid();
            if (network != null) {
                MEStorage networkInv = network.getStorageService().getInventory();
                var energy = network.getEnergyService();

                // Process pending item and fluid refunds
                processRefundMap(pendingRefundItems, energy, networkInv);
                processRefundMap(pendingRefundFluids, energy, networkInv);
            }
        }

        private <T extends AEKey> void processRefundMap(Object2LongOpenHashMap<T> refundMap,
                                                        IEnergyService energy, MEStorage networkInv) {
            for (var it = refundMap.object2LongEntrySet().fastIterator(); it.hasNext();) {
                var entry = it.next();
                long amount = entry.getLongValue();
                if (amount <= 0) {
                    it.remove();
                    continue;
                }

                long inserted = StorageHelper.poweredInsert(energy, networkInv, entry.getKey(), amount, actionSource);
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

        private <T extends AEKey> void serializeRefundMap(Object2LongOpenHashMap<T> refundMap,
                                                          String tagName, CompoundTag parentTag) {
            ListTag listTag = new ListTag();
            for (var entry : refundMap.object2LongEntrySet()) {
                var aeKey = entry.getKey();
                long amount = entry.getLongValue();
                if (amount > 0) {
                    var keyTag = aeKey.toTag();
                    keyTag.putLong("pendingAmount", amount);
                    listTag.add(keyTag);
                }
            }
            if (!listTag.isEmpty()) {
                parentTag.put(tagName, listTag);
            }
        }

        private <T extends AEKey> void deserializeRefundMap(Object2LongOpenHashMap<T> refundMap,
                                                            String tagName, CompoundTag parentTag,
                                                            java.util.function.Function<CompoundTag, T> keyParser) {
            refundMap.clear();
            ListTag listTag = parentTag.getList(tagName, Tag.TAG_COMPOUND);
            for (Tag t : listTag) {
                if (!(t instanceof CompoundTag keyTag)) continue;
                var aeKey = keyParser.apply(keyTag);
                long amount = keyTag.getLong("pendingAmount");
                if (aeKey != null && amount > 0) {
                    refundMap.put(aeKey, amount);
                }
            }
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();

            serializeRefundMap(pendingRefundItems, "pendingRefundItems", tag);
            serializeRefundMap(pendingRefundFluids, "pendingRefundFluids", tag);

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            deserializeRefundMap(pendingRefundItems, "pendingRefundItems", tag, AEItemKey::fromTag);
            deserializeRefundMap(pendingRefundFluids, "pendingRefundFluids", tag, AEFluidKey::fromTag);
        }
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);

        ListTag patternMapTag = new ListTag();
        for (var entry : patternSlotMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("pattern", entry.getKey().getDefinition().toTag());
            entryTag.putInt("slot", entry.getValue());
            patternMapTag.add(entryTag);
        }
        tag.put("patternSlotMap", patternMapTag);
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);

        if (tag.contains("patternSlotMap")) {
            patternSlotMap.clear();
            ListTag patternMapTag = tag.getList("patternSlotMap", Tag.TAG_COMPOUND);
            for (int i = 0; i < patternMapTag.size(); i++) {
                CompoundTag entryTag = patternMapTag.getCompound(i);
                var pattern = PatternDetailsHelper.decodePattern(AEItemKey.fromTag(entryTag.getCompound("pattern")), getLevel());
                int slot = entryTag.getInt("slot");
                if (pattern != null) {
                    patternSlotMap.put(pattern, slot);
                }
            }
        }
    }
}
