package com.gtladd.gtladditions.utils

import appeng.api.crafting.PatternDetailsHelper
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.GenericStack
import appeng.crafting.pattern.AEProcessingPattern
import com.gregtechceu.gtceu.client.util.TooltipHelper
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour
import com.gregtechceu.gtceu.utils.FormattingUtil
import committee.nova.mods.avaritia.init.registry.ModItems
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.povstalec.sgjourney.StargateJourney
import net.povstalec.sgjourney.common.init.BlockInit
import org.gtlcore.gtlcore.utils.TextUtil
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object CommonUtils {

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

    @JvmStatic
    fun createRainbowComponent(string: String): Component {
        return Component.literal(TextUtil.full_color(string))
            .withStyle { style: Style? -> style!!.withColor(TooltipHelper.RAINBOW.current) }
    }

    fun createLanguageRainbowComponent(component: MutableComponent): Component {
        return component.withStyle { style: Style -> style.withColor(TooltipHelper.RAINBOW.current) }
    }

    fun createObfuscatedRainbowComponent(text: String): Component {
        val component = Component.empty()

        text.forEachIndexed { index, char ->
            component.append(
                Component.literal(char.toString())
                    .withStyle(ChatFormatting.OBFUSCATED)
                    .withStyle { style: Style? -> style!!.withColor(TooltipHelper.RAINBOW.current)}
            )
        }

        return component
    }

    fun createObfuscatedDeleteComponent(text: String): Component {
        val rainbowColors = arrayOf(
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE
        )
        val component = Component.empty()

        text.forEachIndexed { index, char ->
            component.append(
                Component.literal(char.toString())
                    .withStyle(ChatFormatting.OBFUSCATED)
                    .withStyle(ChatFormatting.STRIKETHROUGH)
                    .withStyle(rainbowColors[index % 7])
            )
        }

        return component
    }

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

    // ===================================================
    // StarGate Journey
    // ===================================================

    const val VALID_TAG = "gtladditions:stargate_transfer"

    private val FORBIDDEN_DIMENSIONS by lazy {
        mapOf(
            ResourceLocation(StargateJourney.MODID, "lantea") to ItemStack(BlockInit.UNIVERSE_STARGATE.get().asItem()),
            ResourceLocation(StargateJourney.MODID, "abydos") to ItemStack(ModItems.neutron_ring.get()),
            ResourceLocation(StargateJourney.MODID, "chulak") to ItemStack(ModItems.infinity_umbrella.get()),
            ResourceLocation(StargateJourney.MODID, "cavum_tenebrae") to ItemStack(ModItems.infinity_ring.get())
        )
    }

    @JvmStatic
    fun selectDisplayItem(dimensionLocation: ResourceLocation): ItemStack? = FORBIDDEN_DIMENSIONS[dimensionLocation]

    @JvmStatic
    fun isTargetDimension(dimensionLocation: ResourceLocation): Boolean = FORBIDDEN_DIMENSIONS.containsKey(dimensionLocation)

    // ===================================================
    // Pos Offset
    // ===================================================

    /**
     * Calculates a rotated render position with full control over base and target facing.
     * Transforms an offset from one facing orientation to another.
     * @param baseFacing The facing direction when the offset was recorded/defined
     * @param targetFacing The current facing direction of the machine
     * @param offsetX The X offset in the base facing coordinate system
     * @param offsetY The Y offset (not affected by rotation)
     * @param offsetZ The Z offset in the base facing coordinate system
     * @return Rotated Vec3 position relative to block center (0.5, 0.5, 0.5)
     */
    fun getRotatedRenderPosition(
        baseFacing: Direction,
        targetFacing: Direction,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double
    ): Vec3 {
        require(baseFacing.axis == Direction.Axis.Y || targetFacing.axis != Direction.Axis.Y) {
            "Facing must be horizontal (NORTH, SOUTH, EAST, WEST)"
        }

        val y = 0.5 + offsetY

        if (baseFacing == targetFacing) {
            return Vec3(0.5 + offsetX, y, 0.5 + offsetZ)
        }

        val baseIndex = getHorizontalIndex(baseFacing)
        val targetIndex = getHorizontalIndex(targetFacing)
        val rotationSteps = (targetIndex - baseIndex + 4) % 4

        return when (rotationSteps) {
            0 -> {
                Vec3(0.5 + offsetX, y, 0.5 + offsetZ)
            }
            1 -> {
                val x = 0.5 - offsetZ
                val z = 0.5 + offsetX
                Vec3(x, y, z)
            }
            2 -> {
                val x = 0.5 - offsetX
                val z = 0.5 - offsetZ
                Vec3(x, y, z)
            }
            3 -> {
                val x = 0.5 + offsetZ
                val z = 0.5 - offsetX
                Vec3(x, y, z)
            }
            else -> Vec3(0.5 + offsetX, y, 0.5 + offsetZ)
        }
    }

    private fun getHorizontalIndex(facing: Direction): Int {
        return when (facing) {
            Direction.EAST -> 0
            Direction.SOUTH -> 1
            Direction.WEST -> 2
            Direction.NORTH -> 3
            else -> 0
        }
    }
}