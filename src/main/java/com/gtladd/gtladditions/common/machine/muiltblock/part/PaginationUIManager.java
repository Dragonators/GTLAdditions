package com.gtladd.gtladditions.common.machine.muiltblock.part;

import org.gtlcore.gtlcore.integration.ae2.widget.AEPatternViewExtendSlotWidget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import appeng.crafting.pattern.EncodedPatternItem;
import lombok.Getter;

import java.util.function.Function;
import java.util.function.IntConsumer;

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

    private final ItemStackTransfer patternInventory;
    private final IntConsumer onPatternChange;
    private IntConsumer onMiddleClicked;

    @DescSynced
    private int currentPageIndex;

    private WidgetGroup paginationUI;

    // 回调函数
    private final Function<Integer, Boolean> isCached;

    public PaginationUIManager(int patternsPerRow, int rowsPerPage, int maxPages,
                               int uiWidth, int uiHeight,
                               IntConsumer onPatternChange,
                               Function<Integer, Boolean> isCached,
                               ItemStackTransfer patternInventory) {
        this.patternsPerRow = patternsPerRow;
        this.rowsPerPage = rowsPerPage;
        this.maxPages = maxPages;
        this.maxPatternCount = patternsPerRow * rowsPerPage * maxPages;
        this.uiWidth = uiWidth;
        this.uiHeight = uiHeight;

        this.onPatternChange = onPatternChange;
        this.isCached = isCached;
        this.patternInventory = patternInventory;
    }

    /**
     * 重建完整的翻页UI
     */
    public WidgetGroup createPaginationUI(IntConsumer onMiddleClicked) {
        this.onMiddleClicked = onMiddleClicked;

        var basePage = new WidgetGroup(0, 0, uiWidth, uiHeight);
        // 默认重建当前页面的slot组
        this.paginationUI = rebuildPatternSlots(currentPageIndex);
        basePage.addWidget(this.paginationUI);
        // 重建翻页控制按钮
        createPageControls(basePage);

        return basePage;
    }

    /**
     * 重建所有页面的pattern slots
     */
    protected WidgetGroup rebuildPatternSlots(int pageIndex) {
        final int startY = 16;
        final int patternAreaHeight = rowsPerPage * 18;
        final int startSlot = pageIndex * (rowsPerPage * patternsPerRow);
        final int endSlot = Math.min(startSlot + (rowsPerPage * patternsPerRow), maxPatternCount);

        var pageGroup = new WidgetGroup(0, startY, uiWidth, patternAreaHeight);
        recreatePatternSlots(pageGroup, startSlot, endSlot);
        return pageGroup;
    }

    private void recreatePatternSlots(WidgetGroup pageGroup, int startSlot, int endSlot) {
        for (int i = startSlot; i < endSlot; i++) {
            int finalI = i;
            int slotInPage = i - startSlot;
            int row = slotInPage / patternsPerRow;
            int col = slotInPage % patternsPerRow;

            int x = uiWidth == 106 ? (106 - patternsPerRow * 18) / 2 + col * 18 : 8 + col * 18;
            int y = row * 18;

            var slot = new AEPatternViewExtendSlotWidget(patternInventory, i, x, y)
                    .setOnMiddleClick(() -> onMiddleClicked.accept(finalI))
                    .setOnPatternSlotChanged(() -> onPatternChange.accept(finalI))
                    .setOccupiedTexture(GuiTextures.SLOT)
                    .setOccupiedTexture(GuiTextures.SLOT)
                    .setItemHook(stack -> {
                        if (!stack.isEmpty() && stack.getItem() instanceof EncodedPatternItem iep) {
                            final ItemStack out = iep.getOutput(stack);
                            if (!out.isEmpty()) return out;
                        }
                        return stack;
                    })
                    .setOnAddedTooltips((s, l) -> {
                        if (isCached.apply(finalI)) l.add(Component.translatable("gtceu.machine.pattern.recipe.cache"));
                    })
                    .setBackground(GuiTextures.SLOT, GuiTextures.PATTERN_OVERLAY);

            pageGroup.addWidget(slot);
        }
    }

    /**
     * 重建翻页控制按钮
     */
    protected void createPageControls(WidgetGroup parentGroup) {
        int startY = 16;
        int patternAreaHeight = rowsPerPage * 18;
        int pageControlY = startY + patternAreaHeight + 4;

        // 上一页按钮
        parentGroup.addWidget(new ButtonWidget(8, pageControlY, 30, 12,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("<<")),
                clickData -> {
                    if (currentPageIndex > 0) {
                        currentPageIndex--;
                        refreshPage(currentPageIndex);
                    }
                }));

        // 页面指示器（居中）
        int pageIndicatorWidth = 12; // 估计页面文本宽度
        int pageIndicatorX = (uiWidth - pageIndicatorWidth) / 2; // 居中文本块
        parentGroup.addWidget(new LabelWidget(pageIndicatorX, pageControlY + 2,
                () -> (currentPageIndex + 1) + " / " + maxPages));

        // 下一页按钮
        parentGroup.addWidget(new ButtonWidget(uiWidth - 38, pageControlY, 30, 12,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture(">>")),
                clickData -> {
                    if (currentPageIndex < maxPages - 1) {
                        currentPageIndex++;
                        refreshPage(currentPageIndex);
                    }
                }));
    }

    public void refreshPage(int pageIndex) {
        this.paginationUI.clearAllWidgets();

        final int startSlot = pageIndex * (rowsPerPage * patternsPerRow);
        final int endSlot = Math.min(startSlot + (rowsPerPage * patternsPerRow), maxPatternCount);

        recreatePatternSlots(this.paginationUI, startSlot, endSlot);
    }
}
