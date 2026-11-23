package com.gtladd.gtladditions.integration.jade.provider

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.gui.ExoticChemicalDisplay
import com.gtladd.gtladditions.api.machine.gui.ExoticChemicalOutput
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex
import com.gtladd.gtladditions.utils.CommonUtils
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec2
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.fluids.FluidStack
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.TextUtil
import snownee.jade.JadeClient
import snownee.jade.api.Accessor
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.config.IPluginConfig
import snownee.jade.api.config.IWailaConfig
import snownee.jade.api.fluid.JadeFluidObject
import snownee.jade.api.ui.BoxStyle
import snownee.jade.api.ui.IElement
import snownee.jade.api.ui.IElementHelper
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.min

object ExoticChemicalProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    private const val PER_PAGE = 4
    private const val NAME_WIDTH = 80
    private const val ELLIPSIS = "…"

    private val box = BoxStyle().apply {
        borderColor = 0xFF555555.toInt()
        borderWidth = 1f
    }

    // Same order as the unlocked states in ExoticChemicalUnitState.
    private enum class Look(
        val key: String,
        val text: Int,
        val bar: Int
    ) {
        RUN("run", 0x55FF55, 0xFF4CB717.toInt()),
        WAIT("wait", 0xFFAA00, 0xFFD48A15.toInt()),
        PAUSE("pause", 0xC3B1F2, 0xFF7A5EC2.toInt()),
        IDLE("idle", 0xAAAAAA, 0xFF555555.toInt())
    }

    private class Row(
        val id: Int,
        val look: Look,
        val distortRecipes: Boolean,
        val progress: Int,
        val duration: Int,
        val outputCount: Int,
        val outputs: ListTag
    )

    // ========================================
    // JADE PROVIDER
    // ========================================

    override fun getUid(): ResourceLocation = GTLAdditions.id("exotic_chemical_complex")

    override fun appendServerData(tag: CompoundTag, accessor: BlockAccessor) {
        val host = (accessor.blockEntity as? IMachineBlockEntity)?.metaMachine as? ExoticChemicalComplex ?: return
        ExoticChemicalDisplay.writeJade(tag, host.getRecipeLogic())
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val t = accessor.serverData
        if (!t.contains("tier", Tag.TAG_INT.toInt())) {
            ExoticChemicalJadePages.compact()
            return
        }

        val tier = t.getInt("tier")
        tooltip.add(colored(ExoticChemicalDisplay.label("tier.$tier"), 0xFFFFFF))
        if (t.contains("eut")) energy(tooltip, BigInteger(t.getByteArray("eut")))

        // The server sends only units with a current run (duration > 0), so like an ordinary GT machine an idle unit shows nothing.
        val units = t.getList("units", Tag.TAG_COMPOUND.toInt()).map { entry ->
            val r = entry as CompoundTag
            Row(
                r.getInt("id"),
                Look.entries[r.getByte("state").toInt()],
                r.getBoolean("distortRecipes"),
                r.getInt("progress"),
                r.getInt("duration"),
                r.getInt("outputCount"),
                r.getList("outputs", Tag.TAG_COMPOUND.toInt())
            )
        }

        if (accessor.showDetails()) {
            if (units.isEmpty()) {
                ExoticChemicalJadePages.compact()
                return
            }

            val pages = (units.size + PER_PAGE - 1) / PER_PAGE
            val host = (accessor.blockEntity as? IMachineBlockEntity)?.metaMachine as? ExoticChemicalComplex
            val page = ExoticChemicalJadePages.show(host, pages)
            units.drop(page * PER_PAGE).take(PER_PAGE).forEach { unit(tooltip, it, true) }
            if (pages > 1) {
                tooltip.add(
                    colored(ExoticChemicalDisplay.label("jade_page", page + 1, pages), 0x9A9A9A),
                    ExoticChemicalJadePages.PAGE_TAG
                )
            }
        } else {
            ExoticChemicalJadePages.compact()

            val nexus = units.firstOrNull { it.id == 0 }
            nexus?.let { unit(tooltip, it, false) }

            val locals = units.filter { it.id > 0 }
            val shown = locals.sortedBy {
                when (it.look) {
                    Look.WAIT, Look.PAUSE -> 0
                    Look.RUN -> 1
                    else -> 2
                }
            }.take(if (nexus != null) PER_PAGE - 1 else PER_PAGE)
            shown.forEach { unit(tooltip, it, false) }

            val remaining = locals.size - shown.size
            if (remaining > 0) {
                tooltip.add(colored(ExoticChemicalDisplay.label("jade_more", remaining), 0x9A9A9A))
            }
        }
    }

    // ========================================
    // TOOLTIP RENDERING
    // ========================================

    // Same layout as the wireless energy line of RecipeLogicProviderMixin; a positive value is consumption.
    private fun energy(tooltip: ITooltip, eut: BigInteger) {
        val abs = eut.abs()
        val longEu = NumberUtils.getLongValue(abs)
        val tier = if (longEu == Long.MAX_VALUE) GTValues.MAX_TRUE else NumberUtils.getFakeVoltageTier(longEu)
        val amps = FormattingUtil.DECIMAL_FORMAT_2F.format(
            BigDecimal(abs).divide(BigDecimal.valueOf(GTValues.VEX[tier]), 3, RoundingMode.DOWN).toDouble()
        )

        val text = Component.literal(CommonUtils.formatDouble(abs.toDouble())).withStyle(ChatFormatting.RED)
            .append(
                Component.literal(" EU/t").withStyle(ChatFormatting.RESET)
                    .append(
                        Component.literal(" (").withStyle(ChatFormatting.GREEN)
                            .append(
                                Component.translatable("gtceu.top.electricity", amps, NewGTValues.VNF[tier])
                                    .withStyle(TextUtil.`GTL_CORE$VC`[min(tier, 14)])
                            )
                            .append(Component.literal(")").withStyle(ChatFormatting.GREEN))
                    )
            )
        val key = if (eut.signum() > 0) "gtceu.top.energy_consumption" else "gtceu.top.energy_production"
        tooltip.add(Component.translatable(key).append(" ").append(text))
    }

    private fun unit(tooltip: ITooltip, r: Row, expanded: Boolean) {
        val name = ExoticChemicalDisplay.label("unit.${r.id}")
        val heading = colored(Component.literal(r.id.toString().padStart(2, '0') + " ").append(name).append("  "), 0xE8E8E8)
            .append(colored(ExoticChemicalDisplay.label("state." + r.look.key), r.look.text))

        if (r.distortRecipes) {
            heading.append("  ").append(ExoticChemicalDisplay.recipeMode(true).withStyle(ChatFormatting.GOLD))
        }

        tooltip.add(heading)

        val helper = tooltip.elementHelper
        if (!expanded && r.outputs.isNotEmpty()) {
            val first = output(r.outputs.getCompound(0))
            tooltip.append(helper.spacer(8, 0))
            icon(helper, first).forEach { tooltip.append(it.align(IElement.Align.RIGHT)) }
            tooltip.append(
                helper.text(Component.literal(" ").append(colored(shortName(first, r.outputCount > 1), 0xCCCCCC)))
                    .align(IElement.Align.RIGHT)
            )
        }

        val style = helper.progressStyle().color(r.look.bar).textColor(-1)
        val ratio = r.progress.coerceIn(0, r.duration).toFloat() / r.duration
        tooltip.add(helper.progress(ratio, ExoticChemicalDisplay.progressLabel(r.progress, r.duration), style, box, true))

        if (!expanded) return

        for (tag in r.outputs) {
            val o = output(tag as CompoundTag)
            val icons = icon(helper, o)
            tooltip.add(icons.first())
            icons.drop(1).forEach { tooltip.append(it) }

            val amount = FormattingUtil.formatNumbers(o.amount) + if (o.item.isEmpty) " mB " else "× "
            tooltip.append(helper.text(Component.literal(" $amount").append(name(o)).withStyle(ChatFormatting.WHITE)))
        }
    }

    /**
     * Draws an 8 px fluid icon inside a 10 px cell to align with `smallItem`,
     * since `smallItem` draws an 8 px item in a 10 px cell while `smallFluid` fills all 10 px.
     */
    private fun icon(helper: IElementHelper, o: ExoticChemicalOutput): List<IElement> = if (o.item.isEmpty) {
        listOf(
            helper.spacer(1, 0),
            helper.fluid(JadeFluidObject.of(o.fluid.fluid, o.amount, o.fluid.tag)).size(Vec2(8f, 8f)),
            helper.spacer(1, 0)
        )
    } else {
        listOf(helper.smallItem(o.item))
    }

    // ========================================
    // FORMATTING HELPERS
    // ========================================

    private fun colored(text: Component, rgb: Int): MutableComponent = text.copy().withStyle { it.withColor(rgb) }

    private fun output(tag: CompoundTag): ExoticChemicalOutput = if (tag.getString("kind") == "fluid") {
        ExoticChemicalOutput(
            ItemStack.EMPTY,
            FluidStack.loadFluidStackFromNBT(tag.getCompound("fluid")),
            tag.getLong("amount")
        )
    } else {
        ExoticChemicalOutput(ItemStack.of(tag.getCompound("item")), FluidStack.EMPTY, tag.getLong("amount"))
    }

    private fun name(o: ExoticChemicalOutput): Component = if (o.item.isEmpty) o.fluid.displayName else o.item.hoverName

    /**
     * Truncates long output names in the compact tooltip so they do not widen the whole panel.
     * The Shift view keeps names whole; one ellipsis marks both a cut name and further outputs.
     */
    private fun shortName(o: ExoticChemicalOutput, more: Boolean): Component {
        val font = Minecraft.getInstance().font
        val raw = name(o).string
        if (font.width(raw) > NAME_WIDTH) {
            return Component.literal(font.plainSubstrByWidth(raw, NAME_WIDTH - font.width(ELLIPSIS)) + ELLIPSIS)
        }
        return Component.literal(if (more) raw + ELLIPSIS else raw)
    }
}

object ExoticChemicalJadeCleanupProvider : IServerDataProvider<BlockAccessor> {

    private val WORKABLE_KEY = GTCEu.id("workable_provider").toString()
    private val RECIPE_LOGIC_KEY = GTCEu.id("recipe_logic_provider").toString()

    override fun getUid(): ResourceLocation = GTLAdditions.id("exotic_chemical_complex_cleanup")

    override fun getDefaultPriority(): Int = 100

    override fun appendServerData(tag: CompoundTag, accessor: BlockAccessor) {
        if ((accessor.blockEntity as? IMachineBlockEntity)?.metaMachine !is ExoticChemicalComplex) return
        tag.remove(WORKABLE_KEY)
        tag.remove(RECIPE_LOGIC_KEY)
    }
}

internal object ExoticChemicalJadePages {

    val PAGE_TAG: ResourceLocation = GTLAdditions.id("exotic_chemical_page")

    private var target: WeakReference<ExoticChemicalComplex>? = null
    private var drawnTooltip: ITooltip? = null
    private var page = 0
    private var pages = 1

    fun registerClient(registration: IWailaClientRegistration) {
        registration.addAfterRenderCallback { tooltip, _, _, accessor -> onAfterRender(tooltip, accessor) }
        MinecraftForge.EVENT_BUS.addListener(
            EventPriority.NORMAL,
            false,
            TickEvent.RenderTickEvent::class.java,
            ::onRenderTick
        )
        MinecraftForge.EVENT_BUS.addListener(
            EventPriority.HIGH,
            false,
            InputEvent.MouseScrollingEvent::class.java,
            ::scroll
        )
    }

    fun show(host: ExoticChemicalComplex?, count: Int): Int {
        if (host == null || target?.get() !== host) {
            target = host?.let { WeakReference(it) }
            page = 0
        }

        pages = count
        page = page.coerceIn(0, pages - 1)
        return page
    }

    fun compact() {
        pages = 1
    }

    private fun onRenderTick(event: TickEvent.RenderTickEvent) {
        if (event.phase == TickEvent.Phase.START) {
            drawnTooltip = null
        }
    }

    private fun onAfterRender(tooltip: ITooltip, accessor: Accessor<*>?) {
        if (pages <= 1) return
        val machine = ((accessor as? BlockAccessor)?.blockEntity as? IMachineBlockEntity)?.metaMachine ?: return
        if (machine === target?.get()) {
            drawnTooltip = tooltip
        }
    }

    private fun scroll(event: InputEvent.MouseScrollingEvent) {
        val tooltip = drawnTooltip ?: return

        val mc = Minecraft.getInstance()
        val holdingShift = Screen.hasShiftDown() || mc.player?.isShiftKeyDown == true
        if (!holdingShift) return

        val world = mc.level ?: return
        val general = IWailaConfig.get().general
        if (mc.options.hideGui || !general.shouldDisplayTooltip()) return
        if (general.displayMode == IWailaConfig.DisplayMode.HOLD_KEY && !JadeClient.showOverlay.isDown) return
        if (mc.options.renderDebug && general.shouldHideFromDebug()) return
        if (tooltip.get(PAGE_TAG).isEmpty()) return

        val host = target?.get() ?: return
        val hit = mc.hitResult as? BlockHitResult ?: return
        if ((world.getBlockEntity(hit.blockPos) as? IMachineBlockEntity)?.metaMachine !== host) return

        page = Math.floorMod(page + if (event.scrollDelta < 0) 1 else -1, pages)
        event.isCanceled = true
    }
}