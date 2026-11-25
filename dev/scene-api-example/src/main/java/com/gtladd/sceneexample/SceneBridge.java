package com.gtladd.sceneexample;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.gtladd.gtladditions.api.client.scene.SceneRendering;

/**
 * The door to everything that names the scene API; {@link ExampleClient} opens it only when GTL Additions is installed.
 */
final class SceneBridge {

    static void init(IEventBus mod) {
        mod.addListener(JavaOrbs::registerShaders);
        mod.addListener(KotlinBeacons.INSTANCE::registerShaders);
        mod.addListener(SceneBridge::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(JavaOrbs::submit);
    }

    /** Client setup runs on a worker thread, registration belongs on the render thread. */
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            JavaOrbs.register();
            KotlinBeacons.INSTANCE.register();
        });
    }

    static String status() {
        return "Scene API " + SceneRendering.API_VERSION + "\n" + JavaOrbs.status() + "\n" + KotlinBeacons.INSTANCE.status();
    }
}
