package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gregtechceu.gtceu.client.renderer.GTRenderTypes
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.common.data.CircularMotionParams
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.Minecraft
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.IdentityHashMap

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object LightHunterOrbitRenderer {
    private val entries = IdentityHashMap<BlockEntity, OrbitBuffers>()

    // BufferBuilder owns native memory; reuse one staging allocation across machines and reloads.
    private val builder by lazy { BufferBuilder(4096) }

    fun releaseBuffers() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall { releaseBuffers() }
            return
        }

        entries.values.forEach(OrbitBuffers::close)
        entries.clear()
    }

    @SubscribeEvent
    @JvmStatic
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || entries.isEmpty()) return
        val level = Minecraft.getInstance().level
        val iterator = entries.entries.iterator()

        while (iterator.hasNext()) {
            val (blockEntity, buffers) = iterator.next()
            if (blockEntity.isRemoved || blockEntity.level !== level) {
                buffers.close()
                iterator.remove()
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) releaseBuffers()
    }

    class OrbitBuffers(val orbits: List<CircularMotionParams>) : AutoCloseable {
        val outerRadius = orbits.maxOf { it.radius } + LightHunterOrbitGeometry.TUBE_RADIUS
        private val buffers = arrayOfNulls<VertexBuffer>(3)

        fun buffer(level: Int): VertexBuffer = buffers[level] ?: run {
            val type = GTRenderTypes.getLightRing()
            builder.begin(type.mode(), type.format())
            LightHunterOrbitGeometry.write(builder, orbits, level)
            RenderUtils.uploadStaticBuffer(builder).also { buffers[level] = it }
        }

        override fun close() {
            for (index in buffers.indices) {
                buffers[index]?.close()
                buffers[index] = null
            }
        }
    }
}

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object LightHunterOrbitReloadListener {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(ResourceManagerReloadListener { LightHunterOrbitRenderer.releaseBuffers() })
    }
}