package com.gtladd.sceneexample;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Client wiring. Nothing in this class, the commands, the meshes or the direct path names a type of the scene API or of
 * Kotlin, so all of it loads without GTL Additions; {@link SceneBridge} is touched only when the mod is installed.
 */
final class ExampleClient {

    static final boolean SCENE = ModList.get().isLoaded("gtladditions");

    /** The orbs go through the scene scheduler; otherwise the example draws them itself. */
    static boolean sceneOrbs = SCENE;

    static void init() {
        IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
        mod.addListener(DirectOrbs::registerShaders);
        MinecraftForge.EVENT_BUS.addListener(DirectOrbs::render);
        MinecraftForge.EVENT_BUS.addListener(ExampleCommands::register);
        if (SCENE) {
            SceneBridge.init(mod);
        } else {
            MinecraftForge.EVENT_BUS.addListener(ExampleClient::onLevelUnload);
        }
    }

    /** With GTL Additions the provider's onLevelChanged does this. */
    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) Placements.clear();
    }
}
