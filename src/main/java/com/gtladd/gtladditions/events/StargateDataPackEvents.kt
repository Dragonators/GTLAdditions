package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.minecraftforge.event.AddPackFindersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.resource.PathPackResources

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object StargateDataPackEvents {
    private const val PACK_ID = "gtladditions:stargate_tag_isolation"

    @JvmStatic
    @SubscribeEvent
    fun addDataPack(event: AddPackFindersEvent) {
        if (event.packType != PackType.SERVER_DATA) return
        val root = ModList.get().getModFileById(GTLAdditions.MOD_ID).file
            .findResource("resourcepacks", "stargate_tag_isolation")
        event.addRepositorySource { packs ->
            val resources = Pack.ResourcesSupplier { id -> PathPackResources(id, true, root) }
            val info = requireNotNull(Pack.readPackInfo(PACK_ID, resources))
            // Forge's ordinary mod resource order need not follow mods.toml dependency ordering.
            packs.accept(
                Pack.create(
                    PACK_ID, Component.literal("GTLAdditions Stargate compatibility"), true,
                    resources, info, PackType.SERVER_DATA, Pack.Position.TOP, true, PackSource.BUILT_IN
                )
            )
        }
    }
}