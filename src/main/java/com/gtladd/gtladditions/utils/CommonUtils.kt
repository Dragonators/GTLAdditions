package com.gtladd.gtladditions.utils

import appeng.api.crafting.PatternDetailsHelper
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.GenericStack
import appeng.crafting.pattern.AEProcessingPattern
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.common.record.ParallelData
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object CommonUtils {

    // ===================================================
    // Recipe Calculation
    // ===================================================

    @JvmStatic
    fun getParallelData(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData? {
        if (recipeList.isEmpty()) return null
        if (remaining <= 0 || remainingWants.isEmpty()) return ParallelData(recipeList, parallels)

        return if (remainingWants.size <= 64)
            getParallelDataBitmap(remaining, parallels, remainingWants, remainingIndices, recipeList)
        else
            getParallelDataIndexArray(remaining, parallels, remainingWants, remainingIndices, recipeList)
    }

    private fun getParallelDataBitmap(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData {
        val count = remainingWants.size
        var activeBits = (1L shl count) - 1
        var activeCount = count

        var remaining = remaining
        while (remaining > 0 && activeCount > 0) {
            val perRecipe = remaining / activeCount
            if (perRecipe <= 0L) break

            var distributed = 0L
            var newActiveBits = 0L
            var newActiveCount = 0

            var bits = activeBits
            while (bits != 0L) {
                val i = bits.countTrailingZeroBits()
                bits = bits and (bits - 1)

                val idx = remainingIndices.getInt(i)
                val want = remainingWants.getLong(i)
                val give = min(want, perRecipe)
                parallels[idx] += give
                distributed += give
                remainingWants.set(i, want - give)

                if (want - give > 0) {
                    newActiveBits = newActiveBits or (1L shl i)
                    newActiveCount++
                }
            }

            activeBits = newActiveBits
            activeCount = newActiveCount
            remaining -= distributed
        }

        return ParallelData(recipeList, parallels)
    }

    private fun getParallelDataIndexArray(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData {
        var activeCount = remainingWants.size
        var remaining = remaining

        while (remaining > 0 && activeCount > 0) {
            val perRecipe = remaining / activeCount
            if (perRecipe <= 0L) break

            var distributed = 0L
            var writePos = 0

            for (readPos in 0 until activeCount) {
                val idx = remainingIndices.getInt(readPos)
                val want = remainingWants.getLong(readPos)
                val give = min(want, perRecipe)
                parallels[idx] += give
                distributed += give

                val newWant = want - give
                if (newWant > 0) {
                    remainingWants.set(writePos, newWant)
                    remainingIndices.set(writePos, idx)
                    writePos++
                }
            }

            activeCount = writePos
            remaining -= distributed
        }

        return ParallelData(recipeList, parallels)
    }

    fun copyFixRecipe(origin: GTRecipe, modifier: ContentModifier, fixMultiplier: Int) =
        GTRecipe(
            origin.recipeType,
            origin.id,
            copyFixContents(origin.inputs, modifier, fixMultiplier),
            copyFixContents(origin.outputs, modifier, fixMultiplier),
            copyFixContents(origin.tickInputs, modifier, fixMultiplier),
            copyFixContents(origin.tickOutputs, modifier, fixMultiplier),
            Reference2ReferenceArrayMap(origin.inputChanceLogics),
            Reference2ReferenceArrayMap(origin.outputChanceLogics),
            Reference2ReferenceArrayMap(origin.tickInputChanceLogics),
            Reference2ReferenceArrayMap(origin.tickOutputChanceLogics),
            ObjectArrayList(origin.conditions),
            ObjectArrayList(origin.ingredientActions),
            origin.data,
            origin.duration,
            origin.isFuel
        )

    fun copyFixContents(
        contents: Map<RecipeCapability<*>, List<Content>>,
        modifier: ContentModifier,
        fixMultiplier: Int
    ): Map<RecipeCapability<*>, List<Content>> =
        Reference2ReferenceArrayMap<RecipeCapability<*>, List<Content>>().apply {
            contents.forEach { (cap, contentList) ->
                if (contentList.isNotEmpty()) {
                    put(cap, ObjectArrayList(contentList.map { content ->
                        copyFixBoost(content, cap, modifier, fixMultiplier)
                    }))
                }
            }
        }

    fun copyFixBoost(
        content: Content,
        capability: RecipeCapability<*>,
        modifier: ContentModifier,
        fixMultiplier: Int
    ): Content {
        val newContent = if (content.chance != 0) {
            capability.copyContent(content.content, modifier)
        } else {
            capability.copyContent(content.content)
        }

        return Content(
            newContent,
            content.chance,
            content.maxChance,
            content.tierChanceBoost / fixMultiplier,
            content.slotName,
            content.uiName
        )
    }

    // ===================================================
    // Format
    // ===================================================

    private val EXTENDED_UNITS = arrayOf(
        "",  // 10^0
        "K",  // 10^3 - Kilo
        "M",  // 10^6 - Mega
        "G",  // 10^9 - Giga
        "T",  // 10^12 - Tera
        "P",  // 10^15 - Peta
        "E",  // 10^18 - Exa
        "Z",  // 10^21 - Zetta
        "Y",  // 10^24 - Yotta
        "R",  // 10^27 - Ronna
        "Q",  // 10^30 - Quetta
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
        "e303", "e306", "e309" // Covers up to 10^309+
    )

    private val DECIMAL2_FORMAT = DecimalFormat("0.00")
    private val DECIMAL_FORMAT = DecimalFormat("#.##")
    private val SCIENTIFIC_FORMAT = DecimalFormat("0.########E0")
    private val SCIENTIFIC_FIXED_FORMAT = DecimalFormat("0.00000000E0")
    private val SCIENTIFIC2_FORMAT = DecimalFormat("0.##E0")
    private val LONG_DECIMAL: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE)
    private val LONG_INTEGER: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)
    private val LOG_1000 = log10(1000.0)

    @JvmStatic
    fun format2Double(number: Double): String {
        val unitIndex = min(EXTENDED_UNITS.size - 1, max(0, (log10(number) / LOG_1000).toInt()))
        val scaledValue = number / 1000.0.pow(unitIndex)
        return DECIMAL2_FORMAT.format(scaledValue) + EXTENDED_UNITS[unitIndex]
    }

    @JvmStatic
    fun formatDouble(number: Double): String {
        val unitIndex = min(EXTENDED_UNITS.size - 1, max(0, (log10(number) / LOG_1000).toInt()))
        val scaledValue = number / 1000.0.pow(unitIndex)
        return DECIMAL_FORMAT.format(scaledValue) + EXTENDED_UNITS[unitIndex]
    }

    @JvmStatic
    fun formatSignBigInteger(value: BigInteger): String {
        val absValue = value.abs()
        val sign = if (value.signum() >= 0) "+" else "-"
        return if (absValue <= LONG_INTEGER)
            sign + FormattingUtil.formatNumbers(absValue.toLong())
        else
            sign + SCIENTIFIC_FORMAT.format(value).lowercase().replace("e", "e+")
    }

    @JvmStatic
    fun formatBigIntegerFixed(value: BigInteger): String =
        value.abs().let { absValue ->
            if (absValue <= LONG_INTEGER)
                FormattingUtil.formatNumbers(absValue.toLong())
            else
                SCIENTIFIC_FIXED_FORMAT.format(value).lowercase().replace("e", "e+")
        }

    @JvmStatic
    fun formatFixedBigDecimal(value: BigDecimal): String =
        if (value <= LONG_DECIMAL)
            FormattingUtil.formatNumbers(value)
        else
            SCIENTIFIC_FIXED_FORMAT.format(value).lowercase().replace("e", "e+")

    @JvmStatic
    fun format2BigDecimal(value: BigDecimal): String =
        if (value <= LONG_DECIMAL)
            FormattingUtil.formatNumbers(value)
        else
            SCIENTIFIC2_FORMAT.format(value).lowercase().replace("e", "e+")

    // ===================================================
    // Pattern Helper
    // ===================================================

    fun createPatternWithCircuit(
        originalPatternStack: ItemStack,
        circuitConfig: Int,
        replaceExisting: Boolean,
        level: Level?
    ): ItemStack {
        val pattern = PatternDetailsHelper.decodePattern(originalPatternStack, level) as? AEProcessingPattern
            ?: return ItemStack.EMPTY

        val originalInputs = pattern.sparseInputs
        val originalOutputs = pattern.sparseOutputs

        val filteredInputs = ObjectArrayList<GenericStack>()
        var hasCircuit = false

        for (input in originalInputs.filterNotNull()) {
            val isCircuit = (input.what() as? AEItemKey)?.item == GTItems.INTEGRATED_CIRCUIT.asItem()

            if (isCircuit) {
                hasCircuit = true
            } else {
                filteredInputs.add(input)
            }
        }

        return when {
            circuitConfig == 0 && !hasCircuit -> originalPatternStack
            circuitConfig != 0 && hasCircuit && !replaceExisting -> originalPatternStack
            circuitConfig != 0 -> {
                filteredInputs.add(0, GenericStack.fromItemStack(IntCircuitBehaviour.stack(circuitConfig)))
                PatternDetailsHelper.encodeProcessingPattern(filteredInputs.toTypedArray(), originalOutputs)
            }
            else -> PatternDetailsHelper.encodeProcessingPattern(filteredInputs.toTypedArray(), originalOutputs)
        }
    }
}
