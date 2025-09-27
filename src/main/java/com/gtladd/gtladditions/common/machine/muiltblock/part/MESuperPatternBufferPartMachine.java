package com.gtladd.gtladditions.common.machine.muiltblock.part;

import org.gtlcore.gtlcore.api.gui.MEPatternCatalystUIManager;
import org.gtlcore.gtlcore.common.machine.multiblock.part.PaginationUIManager;
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget;
import com.gregtechceu.gtceu.utils.ResearchManager;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.NotNull;

public class MESuperPatternBufferPartMachine extends MEPatternBufferPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MESuperPatternBufferPartMachine.class, MEPatternBufferPartMachine.MANAGED_FIELD_HOLDER);

    @Override
    @NotNull
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected final PaginationUIManager paginationUIManager;

    public MESuperPatternBufferPartMachine(IMachineBlockEntity holder) {
        this(holder, 9, 6, 3);
    }

    public MESuperPatternBufferPartMachine(IMachineBlockEntity holder, int patternsPerRow, int rowsPerPage, int maxPages) {
        super(holder, patternsPerRow * rowsPerPage * maxPages, IO.BOTH);
        final int uiWidth = Math.max(patternsPerRow * 18 + 16, 106);
        final int uiHeight = rowsPerPage * 18 + 28;
        paginationUIManager = new PaginationUIManager(
                patternsPerRow, rowsPerPage, maxPages,
                uiWidth, uiHeight,
                this::onPatternChange,
                i -> cacheRecipe[i],
                getPatternInventory());
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

                if (stack.getOrCreateTag().contains("pos", Tag.TAG_INT_ARRAY)) {
                    stack.getOrCreateTag().remove("pos");
                }

                // Store this pattern buffer's position in the data stick
                stack.getOrCreateTag().putIntArray("superPos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
                player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"));
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    // ========================================
    // GUI SYSTEM
    // ========================================

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

        final var catalystUIManager = new MEPatternCatalystUIManager(group.getSizeWidth() + 4, catalystItems, catalystFluids);
        group.waitToAdded(catalystUIManager);

        // Create pagination UI using the manager
        group.addWidget(paginationUIManager.createPaginationUI(catalystUIManager::toggleFor));

        return group;
    }
}
