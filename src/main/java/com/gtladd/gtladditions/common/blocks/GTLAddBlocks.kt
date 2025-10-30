package com.gtladd.gtladditions.common.blocks

import com.gregtechceu.gtceu.api.item.tool.GTToolType
import com.gregtechceu.gtceu.common.data.GTModels
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRegistration
import com.gtladd.gtladditions.common.data.GTLAddCreativeModeTabs
import com.tterrag.registrate.providers.DataGenContext
import com.tterrag.registrate.providers.RegistrateBlockstateProvider
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.nullness.NonNullBiFunction
import com.tterrag.registrate.util.nullness.NonNullFunction
import com.tterrag.registrate.util.nullness.NonNullSupplier
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.GlassBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import org.gtlcore.gtlcore.GTLCore
import java.util.function.Supplier

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
object GTLAddBlocks {
    val QUANTUM_GLASS: BlockEntry<Block>
    val SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS: BlockEntry<Block>
    val GOD_FORGE_ENERGY_CASING: BlockEntry<Block>
    val GOD_FORGE_INNER_CASING: BlockEntry<Block>
    val GOD_FORGE_SUPPORT_CASING: BlockEntry<Block>
    val GOD_FORGE_TRIM_CASING: BlockEntry<Block>
    val SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING: BlockEntry<Block>
    val REMOTE_GRAVITON_FLOW_REGULATOR: BlockEntry<Block>
    val MEDIARY_GRAVITON_FLOW_REGULATOR: BlockEntry<Block>
    val CENTRAL_GRAVITON_FLOW_REGULATOR: BlockEntry<Block>
    val TEMPORAL_ANCHOR_FIELD_CASING: BlockEntry<Block>
    val PHONON_CONDUIT: BlockEntry<Block>

    @JvmStatic
    fun init() {}

    init {
        GTLAddRegistration.REGISTRATE.creativeModeTab(GTLAddCreativeModeTabs.GTLADD_BLOCKS)
        QUANTUM_GLASS = createGlassCasingBlock(
            "quantum_glass",
            GTLAdditions.id("block/casings/quantum_glass")
        ) { Supplier { RenderType.translucent() } }
        SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS = createGlassCasingBlock(
            "spatially_transcendent_gravitational_lens",
            GTLAdditions.id("block/casings/spatially_transcendent_gravitational_lens")
        ) { Supplier { RenderType.translucent() } }
        GOD_FORGE_ENERGY_CASING = createCasingBlock(
            "god_forge_energy_casing",
            GTLAdditions.id("block/casings/god_forge_energy_casing")
        )
        GOD_FORGE_INNER_CASING = createCasingBlock(
            "god_forge_inner_casing",
            GTLAdditions.id("block/casings/god_forge_inner_casing")
        )
        GOD_FORGE_SUPPORT_CASING = createCasingBlock(
            "god_forge_support_casing",
            GTLAdditions.id("block/casings/god_forge_support_casing")
        )
        GOD_FORGE_TRIM_CASING = createCasingBlock(
            "god_forge_trim_casing",
            GTLAdditions.id("block/casings/god_forge_trim_casing")
        )
        SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING = createCasingBlock(
            "suprachronal_magnetic_confinement_casing",
            GTLAdditions.id("block/casings/suprachronal_magnetic_confinement_casing")
        )
        REMOTE_GRAVITON_FLOW_REGULATOR = createGravitonCasingBlock(
            "remote_graviton_flow_regulator",
            GTLAdditions.id("block/casings/graviton_casing_2")
        )
        MEDIARY_GRAVITON_FLOW_REGULATOR = createGravitonCasingBlock(
            "mediary_graviton_flow_regulator",
            GTLAdditions.id("block/casings/graviton_casing_1")
        )
        CENTRAL_GRAVITON_FLOW_REGULATOR = createGravitonCasingBlock(
            "central_graviton_flow_regulator",
            GTLAdditions.id("block/casings/graviton_casing_0")
        )
        TEMPORAL_ANCHOR_FIELD_CASING = createCasingBlock(
            "temporal_anchor_field_casing",
            GTLCore.id("block/casings/sps_casing")
        )
        PHONON_CONDUIT = createCasingBlock(
            "phonon_conduit",
            GTLCore.id("block/casings/phonon_conduit")
        )
        GTLAddRegistration.REGISTRATE.creativeModeTab(GTLAddCreativeModeTabs.GTLADD_MACHINE)
    }

    fun createCasingBlock(name: String, texture: ResourceLocation?): BlockEntry<Block> {
        return createCasingBlock(
            name,
            { properties: BlockBehaviour.Properties? -> Block(properties) },
            texture,
            NonNullSupplier { Blocks.IRON_BLOCK },
            { Supplier { RenderType.cutoutMipped() } })
    }

    private fun createGlassCasingBlock(
        name: String, texture: ResourceLocation?,
        type: Supplier<Supplier<RenderType?>?>
    ): BlockEntry<Block> {
        return createCasingBlock(
            name,
            { arg: BlockBehaviour.Properties? ->
                GlassBlock(arg)
            },
            texture,
            NonNullSupplier { Blocks.GLASS },
            type
        )
    }

    fun createGravitonCasingBlock(name: String, sideTexture: ResourceLocation?): BlockEntry<Block> {
        val topBottomTexture = GTLAdditions.id("block/casings/suprachronal_magnetic_confinement_casing")
        return GTLAddRegistration.REGISTRATE.block(name) { properties: BlockBehaviour.Properties? -> Block(properties) }
            .initialProperties(NonNullSupplier { Blocks.IRON_BLOCK })
            .properties { p: BlockBehaviour.Properties? -> p!!.isValidSpawn { state: BlockState?, level: BlockGetter?, pos: BlockPos?, ent: EntityType<*>? -> false } }
            .addLayer { Supplier { RenderType.cutoutMipped() } }
            .blockstate { ctx: DataGenContext<Block, Block?>, prov: RegistrateBlockstateProvider ->
                prov.simpleBlock(ctx.entry, prov.models()
                    .cube(name, topBottomTexture, topBottomTexture, sideTexture, sideTexture, sideTexture, sideTexture)
                    .texture("particle", sideTexture))
            }
            .tag(GTToolType.WRENCH.harvestTags[0], BlockTags.MINEABLE_WITH_PICKAXE)
            .item(NonNullBiFunction { block: Block?, properties: Item.Properties? ->
                BlockItem(
                    block,
                    properties
                )
            })
            .build()
            .register()
    }

    fun createCasingBlock(
        name: String,
        blockSupplier: NonNullFunction<BlockBehaviour.Properties?, Block?>,
        texture: ResourceLocation?,
        properties: NonNullSupplier<out Block?>,
        type: Supplier<Supplier<RenderType?>?>
    ): BlockEntry<Block> {
        return GTLAddRegistration.REGISTRATE.block<Block?>(name, blockSupplier)
            .initialProperties(properties)
            .properties { p: BlockBehaviour.Properties? -> p!!.isValidSpawn { state: BlockState?, level: BlockGetter?, pos: BlockPos?, ent: EntityType<*>? -> false } }
            .addLayer(type)
            .blockstate(GTModels.cubeAllModel(name, texture))
            .tag(GTToolType.WRENCH.harvestTags[0], BlockTags.MINEABLE_WITH_PICKAXE)
            .item(NonNullBiFunction { block: Block?, properties: Item.Properties? ->
                BlockItem(
                    block,
                    properties
                )
            })
            .build()
            .register()
    }
}
