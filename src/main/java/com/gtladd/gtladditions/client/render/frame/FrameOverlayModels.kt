package com.gtladd.gtladditions.client.render.frame

import com.gregtechceu.gtceu.api.block.MaterialBlock
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.mixin.forge.BakedModelWrapperAccessor
import com.gtladd.gtladditions.mixin.forge.CompositeModelAccessor
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.SpriteContents
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.client.resources.model.SimpleBakedModel
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ModelEvent
import net.minecraftforge.client.model.CompositeModel
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.collections.iterator
import kotlin.math.abs

/** Only a frame overlay completely backed by a stable opaque base can omit its own depth writes. */
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object FrameOverlayModels {
    private data class Snapshot(val models: Set<BakedModel>, val blocks: Set<Block>)

    @Volatile
    private var snapshot = Snapshot(emptySet(), emptySet())

    @JvmStatic
    fun separate(state: BlockState, model: BakedModel, layer: RenderType?): Boolean =
        FrameOverlayRenderer.enabled && layer === RenderType.translucent() && eligible(state, model)

    fun candidate(state: BlockState): Boolean = state.block in snapshot.blocks

    fun eligible(state: BlockState, model: BakedModel): Boolean {
        val current = snapshot
        return state.block in current.blocks && model in current.models
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @JvmStatic
    fun onModelsBaked(event: ModelEvent.ModifyBakingResult) {
        val models = Collections.newSetFromMap(IdentityHashMap<BakedModel, Boolean>())
        val blocks = Collections.newSetFromMap(IdentityHashMap<Block, Boolean>())
        val masks = IdentityHashMap<SpriteContents, Mask>()

        for ((id, model) in event.models) {
            if (id !is ModelResourceLocation || id.variant == "inventory") continue
            val block = ForgeRegistries.BLOCKS.getValue(ResourceLocation(id.namespace, id.path)) as? MaterialBlock ?: continue
            if (block.tagPrefix != TagPrefix.frameGt || !staticModel(model)) continue
            if (covered(model, block.defaultBlockState(), masks)) {
                models.add(model)
                blocks.add(block)
            }
        }

        snapshot = Snapshot(models, blocks)
        FrameOverlayRenderer.requestReset()
        GTLAdditions.LOGGER.info("Enabled color-only overlays for {} GT frame models", models.size)
    }

    private fun staticModel(model: BakedModel, depth: Int = 0): Boolean {
        if (depth > 8) return false
        return when {
            model.javaClass == SimpleBakedModel::class.java -> true
            model is CompositeModel.Baked ->
                (model as CompositeModelAccessor).children.values.all { staticModel(it, depth + 1) }
            model is BakedModelWrapperAccessor &&
                model.javaClass.name in setOf(
                    "org.gtlcore.gtlcore.client.bloom.FrameOcclusionModels\$Model",
                    "org.gtlcore.gtlcore.client.bloom.EmissiveAliases\$Model"
                ) -> staticModel(model.originalModel, depth + 1)
            else -> false
        }
    }

    private fun covered(model: BakedModel, state: BlockState, masks: MutableMap<SpriteContents, Mask>): Boolean {
        val random = RandomSource.create(42L)
        val layers = model.getRenderTypes(state, random, ModelData.EMPTY)
        if (!layers.contains(RenderType.translucent())) return false
        val bases = layers.asList().filter { it === RenderType.cutout() || it === RenderType.cutoutMipped() }
        if (bases.isEmpty()) return false

        var found = false
        for (side in Direction.values().toList() + listOf(null)) {
            val opaque = bases.flatMap { model.getQuads(state, side, random, ModelData.EMPTY, it) }
            for (overlay in model.getQuads(state, side, random, ModelData.EMPTY, RenderType.translucent())) {
                found = true
                val front = masks.getOrPut(overlay.sprite.contents()) { mask(overlay.sprite.contents()) }
                if (opaque.none { base ->
                        val back = masks.getOrPut(base.sprite.contents()) { mask(base.sprite.contents()) }
                        back.binaryStable &&
                            back.width == front.width &&
                            back.height == front.height &&
                            sameMapping(base, overlay) &&
                            front.visible.indices.all { !front.visible[it] || back.opaque[it] }
                    }
                ) {
                    return false
                }
            }
        }

        return found
    }

    private fun sameMapping(base: BakedQuad, overlay: BakedQuad): Boolean {
        if (base.direction != overlay.direction) return false
        val a = base.vertices
        val b = overlay.vertices
        val sa = a.size / 4
        val sb = b.size / 4
        if (sa < 8 || sb < 8) return false

        for (i in 0..3) {
            if (a[i * sa + 3] ushr 24 != 255) return false
            val j = (0..3).firstOrNull { j ->
                (0..2).all { k -> abs(Float.fromBits(a[i * sa + k]) - Float.fromBits(b[j * sb + k])) < 0.00001f }
            } ?: return false

            val au = (Float.fromBits(a[i * sa + 4]) - base.sprite.u0) / (base.sprite.u1 - base.sprite.u0)
            val av = (Float.fromBits(a[i * sa + 5]) - base.sprite.v0) / (base.sprite.v1 - base.sprite.v0)
            val bu = (Float.fromBits(b[j * sb + 4]) - overlay.sprite.u0) / (overlay.sprite.u1 - overlay.sprite.u0)
            val bv = (Float.fromBits(b[j * sb + 5]) - overlay.sprite.v0) / (overlay.sprite.v1 - overlay.sprite.v0)
            if (abs(au - bu) > 0.0001f || abs(av - bv) > 0.0001f) return false
        }

        return true
    }

    private data class Mask(val width: Int, val height: Int, val visible: BooleanArray, val opaque: BooleanArray, val binaryStable: Boolean)

    private fun mask(sprite: SpriteContents): Mask {
        val width = sprite.width()
        val height = sprite.height()
        val image = sprite.originalImage
        val first = IntArray(width * height) { image.getPixelRGBA(it % width, it / width) ushr 24 }
        val visible = BooleanArray(first.size)
        val opaque = BooleanArray(first.size) { true }
        var binaryStable = true

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val index = (y % height) * width + x % width
                val alpha = image.getPixelRGBA(x, y) ushr 24
                visible[index] = visible[index] || alpha != 0
                opaque[index] = opaque[index] && alpha == 255
                if (alpha != first[index] || alpha != 0 && alpha != 255) binaryStable = false
            }
        }

        return Mask(width, height, visible, opaque, binaryStable)
    }
}