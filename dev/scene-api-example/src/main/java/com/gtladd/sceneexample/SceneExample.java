package com.gtladd.sceneexample;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(SceneExample.MOD_ID)
public final class SceneExample {

    public static final String MOD_ID = "gtladditions_scene_example";

    public SceneExample() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ExampleClient::init);
    }
}
