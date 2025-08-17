package com.gtladd.gtladditions.common.machine.muiltblock.part;

import org.gtlcore.gtlcore.api.machine.trait.NotifiableCircuitItemStackHandler;

import com.gregtechceu.gtceu.common.data.GTItems;

import net.minecraft.world.item.ItemStack;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 样板电路处理模块
 * 
 * 负责处理样板中的电路逻辑，包括：
 * - 电路提取和存储
 * - 样板重构（移除电路）
 * - 电路来源判断
 */
public class PatternCircuitHandler {

    private final NotifiableCircuitItemStackHandler mePatternCircuitInventory;

    public PatternCircuitHandler(NotifiableCircuitItemStackHandler mePatternCircuitInventory) {
        this.mePatternCircuitInventory = mePatternCircuitInventory;
    }

    /**
     * 全局判断是否应该使用样板中的电路
     * 
     * @return true 如果样板缓存器没有配置电路，需要使用样板中的电路
     */
    public boolean shouldUsePatternCircuit() {
        return mePatternCircuitInventory.storage.getStackInSlot(0).isEmpty();
    }

    /**
     * 处理包含电路的样板：提取电路并返回无电路的样板
     * 
     * @param originalPattern    原始样板
     * @param storedCircuit      存储电路的引用
     * @param originalPatternRef 存储原始样板的引用
     * @return 处理结果，包含无电路样板和提取的电路
     */
    public PatternProcessingResult processPatternWithCircuit(ItemStack originalPattern, Consumer<ItemStack> storedCircuit, Consumer<ItemStack> originalPatternRef) {
        if (PatternDetailsHelper.decodePattern(originalPattern, null) instanceof AEProcessingPattern processingPattern) {
            // 提取电路
            ItemStack extractedCircuit = extractCircuitFromPattern(processingPattern);
            if (extractedCircuit.isEmpty()) {
                return new PatternProcessingResult(originalPattern, ItemStack.EMPTY); // 没有电路，直接返回
            }

            // 存储原始样板和电路
            originalPatternRef.accept(originalPattern.copy());
            storedCircuit.accept(extractedCircuit);

            // 创建无电路的样板
            ItemStack patternWithoutCircuit = createPatternWithoutCircuit(processingPattern);
            return new PatternProcessingResult(patternWithoutCircuit, extractedCircuit);
        } else {
            return new PatternProcessingResult(originalPattern, ItemStack.EMPTY);
        }
    }

    /**
     * 获取用于配方的电路
     * 
     * @param storedCircuit 存储的电路
     * @return 电路ItemStack，可能为空
     */
    public ItemStack getCircuitForRecipe(ItemStack storedCircuit) {
        if (shouldUsePatternCircuit()) {
            return storedCircuit; // 直接返回缓存的电路对象
        } else {
            return mePatternCircuitInventory.storage.getStackInSlot(0); // 返回配置的电路
        }
    }

    /**
     * 从样板中提取电路
     * 
     * @param processingPattern 处理样板
     * @return 提取的电路，如果没有则返回空
     */
    private ItemStack extractCircuitFromPattern(AEProcessingPattern processingPattern) {
        for (var input : Arrays.stream(processingPattern.getSparseInputs()).filter(Objects::nonNull).toList()) {
            if (input.what() instanceof AEItemKey itemKey) {
                ItemStack itemStack = itemKey.toStack();
                if (itemStack.getItem() == GTItems.INTEGRATED_CIRCUIT.asItem()) {
                    return itemStack.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 创建一个移除了电路的新样板
     * 
     * @param pattern 原始处理样板
     * @return 无电路的样板
     */
    private ItemStack createPatternWithoutCircuit(AEProcessingPattern pattern) {
        var originalInputs = pattern.getSparseInputs();
        var originalOutputs = pattern.getSparseOutputs();
        var filteredInputs = new ObjectArrayList<GenericStack>();

        for (var input : Arrays.stream(originalInputs).filter(Objects::nonNull).toList()) {
            if (input.what() instanceof AEItemKey itemKey) {
                if (itemKey.getItem() == GTItems.INTEGRATED_CIRCUIT.asItem()) {
                    continue; // 跳过电路
                }
            }
            filteredInputs.add(input);
        }

        return PatternDetailsHelper.encodeProcessingPattern(filteredInputs.toArray(new GenericStack[0]), originalOutputs);
    }

    /**
     * 样板处理结果
     */
    public record PatternProcessingResult(@Getter ItemStack processedPattern, @Getter ItemStack extractedCircuit) {

        public boolean hasCircuit() {
            return !extractedCircuit.isEmpty();
        }
    }
}
