package com.gtladd.gtladditions.utils;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.gtladd.gtladditions.common.record.ParallelData;
import com.gtladd.gtladditions.common.record.RecipeData;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class CommonUtils {

    // ===================================================
    // Recipe Calculation
    // ===================================================

    public static @Nullable ParallelData getParallelData(int length, long remaining, long[] parallels, ObjectArrayFIFOQueue<RecipeData> queue, ObjectArrayList<GTRecipe> recipeList) {
        if (recipeList.isEmpty()) return null;

        var remainingWants = new long[length];
        var activeIndices = new IntArrayList(queue.size());
        while (!queue.isEmpty()) {
            var data = queue.dequeue();
            remainingWants[data.index] = data.remainingWant;
            activeIndices.add(data.index);
        }

        while (remaining > 0 && !activeIndices.isEmpty()) {
            long perRecipe = remaining / activeIndices.size();
            if (perRecipe == 0) break;

            long distributed = 0;
            for (var it = activeIndices.iterator(); it.hasNext();) {
                int idx = it.nextInt();
                long give = Math.min(remainingWants[idx], perRecipe);
                parallels[idx] += give;
                distributed += give;
                remainingWants[idx] -= give;
                if (remainingWants[idx] == 0) {
                    it.remove();
                }
            }
            remaining -= distributed;
        }

        return new ParallelData(recipeList, parallels);
    }

    public static GTRecipe copyFixRecipe(GTRecipe origin, @NotNull ContentModifier modifier, int fixMultiplier) {
        return new GTRecipe(origin.recipeType, origin.id,
                copyFixContents(origin.inputs, modifier, fixMultiplier), copyFixContents(origin.outputs, modifier, fixMultiplier),
                copyFixContents(origin.tickInputs, modifier, fixMultiplier), copyFixContents(origin.tickOutputs, modifier, fixMultiplier),
                new Reference2ReferenceArrayMap<>(origin.inputChanceLogics), new Reference2ReferenceArrayMap<>(origin.outputChanceLogics),
                new Reference2ReferenceArrayMap<>(origin.tickInputChanceLogics), new Reference2ReferenceArrayMap<>(origin.tickOutputChanceLogics),
                new ObjectArrayList<>(origin.conditions),
                new ObjectArrayList<>(origin.ingredientActions), origin.data, origin.duration, origin.isFuel);
    }

    public static Map<RecipeCapability<?>, List<Content>> copyFixContents(Map<RecipeCapability<?>, List<Content>> contents,
                                                                          @NotNull ContentModifier modifier, int fixMultiplier) {
        Map<RecipeCapability<?>, List<Content>> copyContents = new Reference2ReferenceArrayMap<>();
        for (var entry : contents.entrySet()) {
            var contentList = entry.getValue();
            var cap = entry.getKey();
            if (contentList != null && !contentList.isEmpty()) {
                List<Content> contentsCopy = new ObjectArrayList<>();
                for (Content content : contentList) {
                    contentsCopy.add(copyFixBoost(content, cap, modifier, fixMultiplier));
                }
                copyContents.put(entry.getKey(), contentsCopy);
            }
        }
        return copyContents;
    }

    public static Content copyFixBoost(Content content, RecipeCapability<?> capability, @NotNull ContentModifier modifier, int fixMultiplier) {
        final var result = content.chance != 0 ? new Content(capability.copyContent(content.content, modifier), content.chance, content.maxChance, content.tierChanceBoost, content.slotName, content.uiName) : new Content(capability.copyContent(content.content), content.chance, content.maxChance, content.tierChanceBoost, content.slotName, content.uiName);
        result.tierChanceBoost /= fixMultiplier;
        return result;
    }

    // ===================================================
    // Format
    // ===================================================

    private static final String[] EXTENDED_UNITS = new String[] {
            "",    // 10^0
            "K",   // 10^3 - Kilo
            "M",   // 10^6 - Mega
            "G",   // 10^9 - Giga
            "T",   // 10^12 - Tera
            "P",   // 10^15 - Peta
            "E",   // 10^18 - Exa
            "Z",   // 10^21 - Zetta
            "Y",   // 10^24 - Yotta
            "R",   // 10^27 - Ronna
            "Q",   // 10^30 - Quetta
            // Beyond standard SI prefixes, use scientific notation style
            "e33", "e36", "e39", "e42", "e45", "e48", "e51", "e54", "e57", "e60",
            "e63", "e66", "e69", "e72", "e75", "e78", "e81", "e84", "e87", "e90",
            "e93", "e96", "e99", "e102", "e105", "e108", "e111", "e114", "e117", "e120",
            "e123", "e126", "e129", "e132", "e135", "e138", "e141", "e144", "e147", "e150",
            "e153", "e156", "e159", "e162", "e165", "e168", "e171", "e174", "e177", "e180",
            "e183", "e186", "e189", "e192", "e195", "e198", "e201", "e204", "e207", "e210",
            "e213", "e216", "e219", "e222", "e225", "e228", "e231", "e234", "e237", "e240",
            "e243", "e246", "e249", "e252", "e255", "e258", "e261", "e264", "e267", "e270",
            "e273", "e276", "e279", "e282", "e285", "e288", "e291", "e294", "e297", "e300",
            "e303", "e306", "e309"  // Covers up to 10^309+
    };

    private static final DecimalFormat DECIMAL2_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final DecimalFormat SCIENTIFIC_FORMAT = new DecimalFormat("0.########E0");
    private static final DecimalFormat SCIENTIFIC_FIXED_FORMAT = new DecimalFormat("0.00000000E0");
    private static final DecimalFormat SCIENTIFIC2_FORMAT = new DecimalFormat("0.##E0");
    private static final BigDecimal LONG_DECIMAL = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigInteger LONG_INTEGER = BigInteger.valueOf(Long.MAX_VALUE);
    private static final double LOG_1000 = Math.log10(1000.0); // 3.0

    public static String format2Double(double number) {
        // Clamp unit index to valid range [0, EXTENDED_UNITS.length - 1]
        // Handles edge cases: number < 1, Infinity, NaN
        int unitIndex = Math.min(EXTENDED_UNITS.length - 1,
                Math.max(0, (int) (Math.log10(number) / LOG_1000)));

        double scaledValue = number / Math.pow(1000.0, unitIndex);

        return DECIMAL2_FORMAT.format(scaledValue) + EXTENDED_UNITS[unitIndex];
    }

    public static String formatDouble(double number) {
        int unitIndex = Math.min(EXTENDED_UNITS.length - 1,
                Math.max(0, (int) (Math.log10(number) / LOG_1000)));

        double scaledValue = number / Math.pow(1000.0, unitIndex);

        return DECIMAL_FORMAT.format(scaledValue) + EXTENDED_UNITS[unitIndex];
    }

    public static String formatSignBigInteger(BigInteger value) {
        BigInteger absValue = value.abs();
        String sign = value.signum() >= 0 ? "+" : "-";

        return absValue.compareTo(LONG_INTEGER) <= 0 ? sign + FormattingUtil.formatNumbers(absValue.longValue()) : sign + SCIENTIFIC_FORMAT.format(value).toLowerCase().replace("e", "e+");
    }

    public static String formatBigIntegerFixed(BigInteger value) {
        BigInteger absValue = value.abs();
        return absValue.compareTo(LONG_INTEGER) <= 0 ? FormattingUtil.formatNumbers(absValue.longValue()) : SCIENTIFIC_FIXED_FORMAT.format(value).toLowerCase().replace("e", "e+");
    }

    public static String formatFixedBigDecimal(BigDecimal value) {
        return value.compareTo(LONG_DECIMAL) <= 0 ? FormattingUtil.formatNumbers(value) : SCIENTIFIC_FIXED_FORMAT.format(value).toLowerCase().replace("e", "e+");
    }

    public static String format2BigDecimal(BigDecimal value) {
        return value.compareTo(LONG_DECIMAL) <= 0 ? FormattingUtil.formatNumbers(value) : SCIENTIFIC2_FORMAT.format(value).toLowerCase().replace("e", "e+");
    }

    // ===================================================
    // Pattern Helper
    // ===================================================

    public static ItemStack createPatternWithCircuit(ItemStack originalPatternStack, int circuitConfig, boolean replaceExisting, Level level) {
        if (!(PatternDetailsHelper.decodePattern(originalPatternStack, level) instanceof AEProcessingPattern pattern))
            return ItemStack.EMPTY;

        GenericStack[] originalInputs = pattern.getSparseInputs();
        GenericStack[] originalOutputs = pattern.getSparseOutputs();

        var filteredInputs = new ObjectArrayList<GenericStack>();
        boolean hasCircuit = false;

        for (var input : originalInputs) {
            if (input == null) continue;

            boolean isCircuit = input.what() instanceof AEItemKey itemKey &&
                    itemKey.getItem() == GTItems.INTEGRATED_CIRCUIT.asItem();

            if (isCircuit) {
                hasCircuit = true;
                continue;
            }

            filteredInputs.add(input);
        }

        if (circuitConfig == 0) {
            if (!hasCircuit) return originalPatternStack;
        } else {
            if (hasCircuit && !replaceExisting) return originalPatternStack;
            filteredInputs.add(0, GenericStack.fromItemStack(IntCircuitBehaviour.stack(circuitConfig)));
        }

        return PatternDetailsHelper.encodeProcessingPattern(
                filteredInputs.toArray(GenericStack[]::new),
                originalOutputs);
    }
}
