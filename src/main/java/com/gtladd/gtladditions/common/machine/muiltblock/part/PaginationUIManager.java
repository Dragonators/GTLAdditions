package com.gtladd.gtladditions.common.machine.muiltblock.part;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEPatternViewSlotWidget;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;

import net.minecraft.world.item.ItemStack;

import appeng.crafting.pattern.EncodedPatternItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 管理翻页UI的创建和状态
 */
public class PaginationUIManager {

    // Getters for configuration
    // UI配置
    @Getter
    private final int uiWidth;
    @Getter
    private final int uiHeight;
    @Getter
    private final int patternsPerRow;
    @Getter
    private final int rowsPerPage;
    @Getter
    private final int maxPages;
    @Getter
    private final int maxPatternCount;

    // UI组件
    @Getter
    private final List<WidgetGroup> pageGroups = new ObjectArrayList<>();

    // 回调函数
    private final IntSupplier currentPageSupplier;
    private final IntConsumer currentPageSetter;
    private final Function<Integer, Runnable> changeListenerFactory;

    public PaginationUIManager(int patternsPerRow, int rowsPerPage, int maxPages,
                               IntSupplier currentPageSupplier, IntConsumer currentPageSetter,
                               Function<Integer, Runnable> changeListenerFactory) {
        this.patternsPerRow = patternsPerRow;
        this.rowsPerPage = rowsPerPage;
        this.maxPages = maxPages;
        this.maxPatternCount = patternsPerRow * rowsPerPage * maxPages;
        this.uiWidth = Math.max(patternsPerRow * 18 + 16, 106);
        this.uiHeight = rowsPerPage * 18 + 28;

        this.currentPageSupplier = currentPageSupplier;
        this.currentPageSetter = currentPageSetter;
        this.changeListenerFactory = changeListenerFactory;
    }

    /**
     * 创建完整的翻页UI
     */
    public Widget createPaginationUI(ItemStackTransfer patternInventory) {
        var group = new WidgetGroup(0, 0, uiWidth, uiHeight);
        pageGroups.clear();

        // 创建所有页面的slot组
        createPatternSlots(group, patternInventory);

        // 创建翻页控制按钮
        createPageControls(group);

        return group;
    }

    /**
     * 创建所有页面的pattern slots
     */
    protected void createPatternSlots(WidgetGroup parentGroup, ItemStackTransfer patternInventory) {
        int startY = 16;
        int patternAreaHeight = rowsPerPage * 18;

        for (int page = 0; page < maxPages; page++) {
            var pageGroup = new WidgetGroup(0, startY, uiWidth, patternAreaHeight);

            int startSlot = page * (rowsPerPage * patternsPerRow);
            int endSlot = Math.min(startSlot + (rowsPerPage * patternsPerRow), maxPatternCount);

            for (int i = startSlot; i < endSlot; i++) {
                int finalI = i;
                int slotInPage = i - startSlot;
                int row = slotInPage / patternsPerRow;
                int col = slotInPage % patternsPerRow;

                int x = uiWidth == 106 ? (106 - patternsPerRow * 18) / 2 + col * 18 : 8 + col * 18;
                int y = row * 18;

                var slot = new AEPatternViewSlotWidget(patternInventory, i, x, y) {

                    @Override
                    public boolean canPutStack(ItemStack stack) {
                        // 只有当前页面的slot允许输入物品
                        int slotPage = finalI / (rowsPerPage * patternsPerRow);
                        return slotPage == currentPageSupplier.getAsInt() && super.canPutStack(stack);
                    }

                    @Override
                    public boolean isEnabled() {
                        // 只有当前页面的slot被启用
                        int slotPage = finalI / (rowsPerPage * patternsPerRow);
                        return slotPage == currentPageSupplier.getAsInt() && super.isEnabled();
                    }
                }
                        .setOccupiedTexture(GuiTextures.SLOT)
                        .setItemHook(stack -> {
                            if (stack.getItem() instanceof EncodedPatternItem iep) {
                                final ItemStack out = iep.getOutput(stack);
                                return !out.isEmpty() ? out : stack;
                            }
                            return stack;
                        })
                        .setChangeListener(changeListenerFactory.apply(finalI))
                        .setBackground(GuiTextures.SLOT, GuiTextures.PATTERN_OVERLAY);

                pageGroup.addWidget(slot);
            }

            // 设置页面可见性
            int currentPage = currentPageSupplier.getAsInt();
            pageGroup.setVisible(page == currentPage);
            pageGroup.setActive(page == currentPage);

            pageGroups.add(pageGroup);
            parentGroup.addWidget(pageGroup);
        }
    }

    /**
     * 创建翻页控制按钮
     */
    protected void createPageControls(WidgetGroup parentGroup) {
        int startY = 16;
        int patternAreaHeight = rowsPerPage * 18;
        int pageControlY = startY + patternAreaHeight + 4;

        // 上一页按钮
        parentGroup.addWidget(new ButtonWidget(8, pageControlY, 30, 12,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("<<")),
                clickData -> {
                    int currentPage = currentPageSupplier.getAsInt();
                    if (currentPage > 0) {
                        currentPageSetter.accept(currentPage - 1);
                        refreshPageVisibility();
                    }
                }));

        // 页面指示器（居中）
        int pageIndicatorWidth = 12; // 估计页面文本宽度
        int pageIndicatorX = (uiWidth - pageIndicatorWidth) / 2; // 居中文本块
        parentGroup.addWidget(new LabelWidget(pageIndicatorX, pageControlY + 2,
                () -> (currentPageSupplier.getAsInt() + 1) + " / " + maxPages));

        // 下一页按钮
        parentGroup.addWidget(new ButtonWidget(uiWidth - 38, pageControlY, 30, 12,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture(">>")),
                clickData -> {
                    int currentPage = currentPageSupplier.getAsInt();
                    if (currentPage < maxPages - 1) {
                        currentPageSetter.accept(currentPage + 1);
                        refreshPageVisibility();
                    }
                }));
    }

    /**
     * 刷新页面可见性
     */
    public void refreshPageVisibility() {
        int currentPage = currentPageSupplier.getAsInt();
        for (int i = 0; i < pageGroups.size(); i++) {
            WidgetGroup pageGroup = pageGroups.get(i);
            boolean shouldBeVisible = (i == currentPage);
            pageGroup.setVisible(shouldBeVisible);
            pageGroup.setActive(shouldBeVisible);
        }
    }
}
