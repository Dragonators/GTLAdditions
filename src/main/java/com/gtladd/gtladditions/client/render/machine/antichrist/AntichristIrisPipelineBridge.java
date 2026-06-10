package com.gtladd.gtladditions.client.render.machine.antichrist;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface AntichristIrisPipelineBridge {

    void beginAntichristFallbackTarget();

    void endAntichristFallbackTarget();
}