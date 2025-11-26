package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.server.MinecraftServer;
import net.povstalec.sgjourney.common.config.CommonStargateConfig;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.events.custom.SGJourneyEvents;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StargateNetwork.class)
public abstract class StargateNetworkMixin {

    @Shadow(remap = false)
    public final boolean addConnection(StargateConnection connection) {
        throw new AssertionError();
    }

    /**
     * @author Dragons
     * @reason Remove energy requirement
     */
    @Overwrite(remap = false)
    public final StargateInfo.Feedback createConnection(MinecraftServer server, Stargate dialingStargate, Stargate dialedStargate, Address.Type addressType, boolean doKawoosh) {
        StargateConnection.Type connectionType = StargateConnection.getType(server, dialingStargate, dialedStargate);

        if (SGJourneyEvents.onStargateConnect(server, dialingStargate, dialedStargate, connectionType, addressType, doKawoosh))
            return StargateInfo.Feedback.NONE;

        if (!dialedStargate.isValid(server))
            return dialingStargate.resetStargate(server, StargateInfo.Feedback.COULD_NOT_REACH_TARGET_STARGATE, true);

        if (!CommonStargateConfig.allow_interstellar_8_chevron_addresses.get() &&
                addressType == Address.Type.ADDRESS_8_CHEVRON &&
                connectionType == StargateConnection.Type.INTERSTELLAR)
            return dialingStargate.resetStargate(server, StargateInfo.Feedback.INVALID_8_CHEVRON_ADDRESS, true);

        if (!CommonStargateConfig.allow_system_wide_connections.get() && connectionType == StargateConnection.Type.SYSTEM_WIDE)
            return dialingStargate.resetStargate(server, StargateInfo.Feedback.INVALID_SYSTEM_WIDE_CONNECTION, true);

        if (dialingStargate.equals(dialedStargate))
            return dialingStargate.resetStargate(server, StargateInfo.Feedback.SELF_DIAL, true);

        if (dialedStargate.isConnected(server))
            return dialingStargate.resetStargate(server, StargateInfo.Feedback.ALREADY_CONNECTED, true);
        else if (dialedStargate.isObstructed(server))
            return dialingStargate.resetStargate(server, StargateInfo.Feedback.TARGET_OBSTRUCTED, true);

        StargateConnection connection = StargateConnection.create(server, connectionType, dialingStargate, dialedStargate, doKawoosh);
        if (connection != null) {
            addConnection(connection);

            return switch (connectionType) {
                case SYSTEM_WIDE -> StargateInfo.Feedback.CONNECTION_ESTABLISHED_SYSTEM_WIDE;
                case INTERSTELLAR -> StargateInfo.Feedback.CONNECTION_ESTABLISHED_INTERSTELLAR;
                default -> StargateInfo.Feedback.CONNECTION_ESTABLISHED_INTERGALACTIC;
            };
        }

        return StargateInfo.Feedback.COULD_NOT_REACH_TARGET_STARGATE;
    }
}
