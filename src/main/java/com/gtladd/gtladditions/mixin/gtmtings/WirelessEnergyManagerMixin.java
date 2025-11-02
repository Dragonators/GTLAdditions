package com.gtladd.gtladditions.mixin.gtmtings;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.hepdd.gtmthings.api.misc.GlobalVariableStorage;
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import com.hepdd.gtmthings.data.WirelessEnergySavaedData;
import it.unimi.dsi.fastutil.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.math.BigInteger;
import java.util.UUID;

import static com.gtladd.gtladditions.utils.WirelessEnergyManagerData.BIG_MACHINE_DATA;
import static com.hepdd.gtmthings.utils.TeamUtil.getTeamUUID;

@Mixin(WirelessEnergyManager.class)
public abstract class WirelessEnergyManagerMixin {

    /**
     * @author Dragons
     * @reason Fix MachineData
     */
    @Overwrite(remap = false)
    public static boolean addEUToGlobalEnergyMap(UUID user_uuid, BigInteger EU, MetaMachine machine) {
        try {
            WirelessEnergySavaedData.INSTANCE.setDirty(true);
        } catch (Exception exception) {
            System.out.println("COULD NOT MARK GLOBAL ENERGY AS DIRTY IN ADD EU");
            exception.printStackTrace();
        }

        UUID teamUUID = getTeamUUID(user_uuid);
        BIG_MACHINE_DATA.put(machine, Pair.of(user_uuid, EU));

        BigInteger totalEU = GlobalVariableStorage.GlobalEnergy.getOrDefault(teamUUID, BigInteger.ZERO);
        if (totalEU.signum() < 0) {
            totalEU = BigInteger.ZERO;
            GlobalVariableStorage.GlobalEnergy.put(getTeamUUID(user_uuid), totalEU);
        }

        totalEU = totalEU.add(EU);

        if (totalEU.signum() >= 0) {
            GlobalVariableStorage.GlobalEnergy.put(teamUUID, totalEU);
            return true;
        }

        return false;
    }
}
