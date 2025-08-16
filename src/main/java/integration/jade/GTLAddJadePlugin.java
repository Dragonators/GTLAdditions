package integration.jade;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import integration.jade.provider.MESuperPatternBufferProvider;
import integration.jade.provider.MESuperPatternBufferProxyProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GTLAddJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new MESuperPatternBufferProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new MESuperPatternBufferProxyProvider(), BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new MESuperPatternBufferProvider(), Block.class);
        registration.registerBlockComponent(new MESuperPatternBufferProxyProvider(), Block.class);
    }
}
