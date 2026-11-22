package com.gtladd.gtladditions.api.gui.widget

import com.google.common.collect.Table
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.integration.GTRecipeWidget
import com.gtladd.gtladditions.api.machine.logic.ApocalypticMatrixRecipeLogic.Companion.chanceDivisor
import com.gtladd.gtladditions.common.machine.trait.MatrixQuantumWarpTrait
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.modular.ModularUI
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib.gui.texture.TextTexture
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.utils.TagOrCycleItemStackTransfer
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.glfw.GLFW
import java.util.Locale

class MatrixRecipeMemoryPage(private val trait: MatrixQuantumWarpTrait) : IFancyUIProvider {
    override fun createMainPage(widget: FancyMachineUIWidget): Widget = MemoryWidget(trait)

    override fun getTabIcon(): IGuiTexture = ItemStackTexture(ItemStack(Items.CLOCK))

    override fun getTitle(): Component = "gtladditions.matrix.memory.title".toComponent

    override fun getTabTooltips(): List<Component> = listOf(title)

    override fun hasPlayerInventory(): Boolean = false

    data class Page(
        val entries: List<Pair<ResourceLocation, Long>>,
        val index: Int,
        val pages: Int,
        val matched: Int,
        val total: Int
    )

    data class RecipeInfo(
        val recipe: GTRecipe?,
        val title: Component,
        val typeName: Component,
        val icon: ItemStack
    )

    companion object {
        const val PAGE_SIZE = 6
        private const val UPDATE_TIMES = 3

        fun filterRecipeIds(
            memory: Map<ResourceLocation, Long>,
            selectedType: ResourceLocation?,
            previousIds: List<ResourceLocation>,
            recipeType: (ResourceLocation) -> ResourceLocation?
        ): List<ResourceLocation> {
            val matches = if (selectedType == null) {
                memory.keys
            } else {
                memory.keys.filterTo(mutableSetOf()) { recipeType(it) == selectedType }
            }

            return if (matches.size == previousIds.size && matches.containsAll(previousIds)) {
                previousIds
            } else {
                matches.sortedBy { it.toString() }
            }
        }

        fun paginate(memory: Map<ResourceLocation, Long>, recipeIds: List<ResourceLocation>, requestedPage: Int): Page {
            val pages = maxOf(1, (recipeIds.size + PAGE_SIZE - 1) / PAGE_SIZE)
            val page = requestedPage.coerceIn(0, pages - 1)

            val start = page * PAGE_SIZE
            val entries = recipeIds.subList(start, minOf(start + PAGE_SIZE, recipeIds.size)).map { it to memory.getValue(it) }

            return Page(entries, page, pages, recipeIds.size, memory.size)
        }

        fun describeRecipe(recipe: GTRecipe?): RecipeInfo {
            if (recipe == null) {
                return RecipeInfo(
                    null,
                    "gtladditions.matrix.memory.unavailable".toComponent,
                    Component.empty(),
                    ItemStack(Items.PAPER)
                )
            }

            val type = recipe.recipeType
            val typeName = type.registryName.toLanguageKey().toComponent

            val outputs = (
                RecipeHelper.getOutputItems(recipe).filterNot { it.isEmpty }.map { it.hoverName } +
                    RecipeHelper.getOutputFluids(recipe).filterNot { it.isEmpty }.map { it.displayName }
                ).distinctBy { it.string }

            val title = when (outputs.size) {
                0 -> typeName
                1 -> outputs.first()
                else -> "gtladditions.matrix.memory.outputs".toComponent(outputs.first(), outputs.size)
            }

            return RecipeInfo(recipe, title, typeName, recipeTypeIcon(type))
        }

        private fun recipeTypeIcon(type: GTRecipeType): ItemStack =
            type.iconSupplier?.get()?.takeUnless { it.isEmpty }?.copy() ?: ItemStack(Items.PAPER)

        fun formatMemoryTime(seconds: Long): String {
            val time = seconds.coerceAtLeast(0)
            return String.format(Locale.ROOT, "%02d:%02d:%02d", time / 3600, time / 60 % 60, time % 60)
        }
    }

    private class MemoryRecipeWidget(recipe: GTRecipe) : GTRecipeWidget(recipe) {
        override fun collectStorage(
            storages: Table<IO, RecipeCapability<*>, Any>,
            contents: Table<IO, RecipeCapability<*>, List<Content>>,
            recipe: GTRecipe
        ) {
            super.collectStorage(storages, contents, recipe)

            // Embedded recipe previews use LDLib's container hover path, which cannot query null slots.
            for (transfer in storages.values()) {
                if (transfer !is TagOrCycleItemStackTransfer) continue
                transfer.stacks.forEachIndexed { index, alternatives ->
                    if (alternatives == null) transfer.setStackInSlot(index, ItemStack.EMPTY)
                }
            }
        }
    }

    private class MemoryWidget(private val trait: MatrixQuantumWarpTrait) : WidgetGroup(0, 0, 280, 190) {

        private val times = Object2LongOpenHashMap<ResourceLocation>()
        private var sentRevision = 0L

        override fun writeInitialData(buffer: FriendlyByteBuf) {
            super.writeInitialData(buffer)

            val current = trait.recipeTimes
            sentRevision = trait.recipeTimesRevision
            times.clear()

            buffer.writeVarInt(current.size)
            for ((id, seconds) in current) {
                buffer.writeResourceLocation(id)
                buffer.writeVarLong(seconds)
                times.put(id, seconds)
            }
        }

        @OnlyIn(Dist.CLIENT)
        override fun readInitialData(buffer: FriendlyByteBuf) {
            super.readInitialData(buffer)

            times.clear()
            repeat(buffer.readVarInt()) {
                times.put(buffer.readResourceLocation(), buffer.readVarLong())
            }

            if (widgets.isEmpty()) addWidget(MemoryView(trait, times))
            (widgets[0] as MemoryView).refresh()
        }

        override fun detectAndSendChanges() {
            super.detectAndSendChanges()
            if (sentRevision == trait.recipeTimesRevision) return

            sentRevision = trait.recipeTimesRevision
            val current = trait.recipeTimes
            if (times == current) return

            writeUpdateInfo(UPDATE_TIMES) { buffer ->
                for ((id, seconds) in current) {
                    if (times.getLong(id) == seconds) continue
                    buffer.writeResourceLocation(id)
                    buffer.writeVarLong(seconds)
                    times.put(id, seconds)
                }

                if (times.size != current.size) {
                    val ids = times.keys.iterator()
                    while (ids.hasNext()) {
                        val id = ids.next()
                        if (id in current) continue

                        buffer.writeResourceLocation(id)
                        buffer.writeVarLong(0)
                        ids.remove()
                    }
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
            if (id != UPDATE_TIMES) return super.readUpdateInfo(id, buffer)
            // A tab reopened at the same widget index can still receive its previous instance's queued delta.
            if (widgets.isEmpty()) return

            while (buffer.isReadable) {
                val recipe = buffer.readResourceLocation()
                val seconds = buffer.readVarLong()
                if (seconds == 0L) {
                    times.removeLong(recipe)
                } else {
                    times.put(recipe, seconds)
                }
            }

            (widgets[0] as MemoryView).refresh()
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class MemoryView(
        private val trait: MatrixQuantumWarpTrait,
        private val recipeMemoryTimes: Map<ResourceLocation, Long>
    ) : WidgetGroup(0, 0, 280, 190) {
        private var selectedType: ResourceLocation? = null
        private var recipeIds = emptyList<ResourceLocation>()
        private var page = paginate(recipeMemoryTimes, recipeIds, 0)

        private var hasRendered = false

        private val overview = WidgetGroup(0, 0, 280, 190)
        private val details = WidgetGroup(0, 0, 280, 190).apply {
            isVisible = false
            isActive = false
        }

        private val typeFilters = linkedMapOf<ResourceLocation?, ButtonWidget>()
        private val rows = WidgetGroup(4, 38, 272, 132)
        private var visibleRows = emptyList<MemoryRow>()
        private val emptyMessage = ImageWidget(4, 42, 272, 24, TextTexture("gtladditions.matrix.memory.empty".toComponent.string))
        private val countText = TextTexture("gtladditions.matrix.memory.count".toComponent(0, 0).string).setType(TextTexture.TextType.LEFT)
        private val pageText = TextTexture("gtladditions.matrix.memory.page".toComponent(1, 1).string)

        private var detailId: ResourceLocation? = null
        private var detailSeconds: Long? = null
        private var detailText: TextTexture? = null

        private val previous = ButtonWidget(4, 170, 24, 16, TextTexture("<")) { showPage(page.index - 1) }
        private val next = ButtonWidget(252, 170, 24, 16, TextTexture(">")) { showPage(page.index + 1) }

        private class MemoryRow(
            val id: ResourceLocation,
            val info: RecipeInfo,
            var seconds: Long,
            val timeText: TextTexture
        )

        init {
            setClientSideWidget()
            setBackground(GuiTextures.DISPLAY)

            addWidget(overview)
            addWidget(details)

            val filterLabel = TextTexture("gtladditions.matrix.memory.filter".toComponent.string).setType(TextTexture.TextType.LEFT)
            overview.addWidget(ImageWidget(4, 4, 38, 16, filterLabel))
            addTypeFilter(null)
            for (type in trait.getMachine().recipeTypes) {
                if (type.registryName !in typeFilters) addTypeFilter(type)
            }
            updateFilterButtons()

            overview.addWidget(ImageWidget(4, 26, 272, 10, countText))
            overview.addWidget(rows)
            overview.addWidget(emptyMessage)

            overview.addWidget(previous.apply { setHoverTooltips("gtladditions.matrix.memory.previous".toComponent) })
            overview.addWidget(next.apply { setHoverTooltips("gtladditions.matrix.memory.next".toComponent) })
            overview.addWidget(ImageWidget(32, 170, 216, 16, pageText))
        }

        private fun addTypeFilter(type: GTRecipeType?) {
            val id = type?.registryName
            val x = 46 + typeFilters.size * 24
            val label = id?.toLanguageKey()?.toComponent ?: "gtladditions.matrix.memory.all".toComponent
            val icon = type?.let { recipeTypeIcon(it) } ?: ItemStack(Items.BOOK)

            val button = ButtonWidget(x, 2, 22, 22, ColorRectTexture(0)) {
                if (selectedType != id) {
                    selectedType = id
                    updateFilterButtons()
                    refreshOverview(0)
                }
            }.setHoverBorderTexture(1, 0xFF55FFFF.toInt()).apply { setHoverTooltips(label) }

            typeFilters[id] = button
            overview.addWidget(button)
            overview.addWidget(ImageWidget(x + 3, 5, 16, 16, ItemStackTexture(icon)))
        }

        override fun setGui(gui: ModularUI?) {
            if (this.gui !== gui) {
                hasRendered = false
                clearRecipe()
            }
            super.setGui(gui)
        }

        @OnlyIn(Dist.CLIENT)
        override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks)
            hasRendered = true
        }

        @OnlyIn(Dist.CLIENT)
        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            // TabsWidget leaves its opening click unconsumed after resizing/recentering the GUI.
            // Do not let that same event click a recipe before this page has even been drawn.
            return !hasRendered || super.mouseClicked(mouseX, mouseY, button)
        }

        @OnlyIn(Dist.CLIENT)
        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            if (details.isVisible && keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeRecipe()
                return true
            }

            return super.keyPressed(keyCode, scanCode, modifiers)
        }

        fun refresh() {
            if (details.isVisible) {
                updateDetailTime()
            } else {
                refreshOverview(page.index)
            }
        }

        private fun resolveRecipe(id: ResourceLocation): GTRecipe? =
            trait.getMachine().level?.recipeManager?.byKey(id)?.orElse(null) as? GTRecipe

        private fun refreshOverview(index: Int) {
            recipeIds = filterRecipeIds(recipeMemoryTimes, selectedType, recipeIds) { resolveRecipe(it)?.recipeType?.registryName }
            showPage(index)
        }

        private fun memoryDetail(seconds: Long): Component = "gtladditions.matrix.memory.time".toComponent(
            formatMemoryTime(seconds),
            String.format(Locale.ROOT, "%.3f", chanceDivisor(seconds))
        )

        private fun updateFilterButtons() {
            typeFilters.forEach { (type, button) ->
                val selected = type == selectedType
                button.setBackground(
                    ColorRectTexture(if (selected) 0x4455FFFF else 0x44000000),
                    ColorBorderTexture(1, if (selected) 0xFF55FFFF.toInt() else 0xFF666666.toInt())
                )
            }
        }

        private fun showPage(index: Int) {
            val updated = paginate(recipeMemoryTimes, recipeIds, index)

            if (updated.matched != page.matched || updated.total != page.total) {
                countText.updateText("gtladditions.matrix.memory.count".toComponent(updated.matched, updated.total).string)
            }

            if (updated.index != page.index || updated.pages != page.pages) {
                pageText.updateText("gtladditions.matrix.memory.page".toComponent(updated.index + 1, updated.pages).string)
            }

            page = updated
            previous.isActive = page.index > 0
            next.isActive = page.index + 1 < page.pages
            emptyMessage.isVisible = page.entries.isEmpty()

            val recipes = page.entries.map { resolveRecipe(it.first) }
            val rebuild = visibleRows.size != page.entries.size ||
                page.entries.indices.any {
                    visibleRows[it].id != page.entries[it].first || visibleRows[it].info.recipe !== recipes[it]
                }

            if (rebuild) {
                rows.clearAllWidgets()
                visibleRows = page.entries.mapIndexed { index, (id, seconds) ->
                    createRow(index, id, seconds, describeRecipe(recipes[index]))
                }
            } else {
                visibleRows.forEachIndexed { index, row ->
                    val seconds = page.entries[index].second
                    if (row.seconds != seconds) {
                        row.timeText.updateText(memoryDetail(seconds).string)
                        row.seconds = seconds
                    }
                }
            }
        }

        private fun createRow(index: Int, id: ResourceLocation, seconds: Long, info: RecipeInfo): MemoryRow {
            val timeText = TextTexture(memoryDetail(seconds).string).setType(TextTexture.TextType.LEFT).setColor(0xDDDDDD)

            rows.addWidget(
                ButtonWidget(0, index * 22, 272, 21, ColorRectTexture(0)) {
                    showRecipe(id, info)
                }.setHoverTexture(ColorRectTexture(0x2255FFFF)).apply {
                    isActive = info.recipe != null
                    if (info.recipe != null) setHoverTooltips("gtladditions.matrix.memory.open".toComponent)
                }
            )

            rows.addWidget(ImageWidget(0, index * 22 + 1, 18, 18, ItemStackTexture(info.icon)))
            rows.addWidget(
                ImageWidget(
                    22,
                    index * 22,
                    250,
                    10,
                    TextTexture(info.title.string).setWidth(250).setType(TextTexture.TextType.LEFT_ROLL).setColor(0x55FFFF)
                )
            )
            rows.addWidget(ImageWidget(22, index * 22 + 10, 250, 10, timeText))

            return MemoryRow(id, info, seconds, timeText)
        }

        private fun updateDetailTime() {
            val id = detailId ?: return
            val seconds = recipeMemoryTimes[id] ?: 0L

            if (detailSeconds != seconds) {
                detailText?.updateText(memoryDetail(seconds).string)
                detailSeconds = seconds
            }
        }

        @OnlyIn(Dist.CLIENT)
        private fun showRecipe(id: ResourceLocation, info: RecipeInfo) {
            val recipe = info.recipe ?: return
            clearRecipe()

            details.addWidget(ImageWidget(6, 5, 18, 18, ItemStackTexture(info.icon)).apply { setHoverTooltips(info.typeName) })
            details.addWidget(
                ImageWidget(28, 5, 220, 12, TextTexture(info.title.string).setWidth(220).setType(TextTexture.TextType.LEFT_ROLL))
            )

            details.addWidget(
                ButtonWidget(254, 4, 20, 16, TextTexture("×")) { closeRecipe() }
                    .apply { setHoverTooltips("gtladditions.matrix.memory.close".toComponent) }
            )

            val contents = DraggableScrollableWidgetGroup(4, 26, 272, 140)
                .setDraggable(false).setScrollable(true).setYScrollBarWidth(4).setXScrollBarHeight(4)
            val recipeWidget = MemoryRecipeWidget(recipe)
            recipeWidget.setSelfPosition(maxOf(4, (268 - recipeWidget.size.width) / 2), 2)
            contents.addWidget(recipeWidget)
            details.addWidget(contents)

            val timeText = TextTexture("").setWidth(268).setType(TextTexture.TextType.LEFT)
            details.addWidget(ImageWidget(6, 173, 268, 10, timeText))
            detailId = id
            detailText = timeText
            updateDetailTime()

            overview.isVisible = false
            overview.isActive = false
            details.isVisible = true
            details.isActive = true
        }

        private fun closeRecipe() {
            clearRecipe()
            refresh()
        }

        private fun clearRecipe() {
            // Discard details synchronously, including when the page is detached before another UI tick.
            details.isVisible = false
            details.isActive = false
            details.clearAllWidgets()

            detailId = null
            detailSeconds = null
            detailText = null

            overview.isVisible = true
            overview.isActive = true
        }
    }
}