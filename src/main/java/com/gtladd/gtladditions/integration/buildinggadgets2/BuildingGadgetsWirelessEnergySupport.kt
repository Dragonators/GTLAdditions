package com.gtladd.gtladditions.integration.buildinggadgets2

import com.direwolf20.buildinggadgets2.common.items.BaseGadget
import com.gregtechceu.gtceu.api.capability.compat.FeCompat
import com.gtladd.gtladditions.utils.CommonUtils
import com.hepdd.gtmthings.api.misc.GlobalVariableStorage
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager
import com.hepdd.gtmthings.utils.TeamUtil
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.math.BigInteger
import java.util.UUID

object BuildingGadgetsWirelessEnergySupport {

    private val ledger = WirelessGadgetEnergyLedger(
        { owner -> GlobalVariableStorage.GlobalEnergy.getOrDefault(TeamUtil.getTeamUUID(owner), BigInteger.ZERO) },
        { owner, balance -> WirelessEnergyManager.setUserEU(owner, balance) }
    )

    fun owner(stack: ItemStack): UUID? {
        if (stack.item !is BaseGadget) return null
        val tag = stack.tag ?: return null
        return if (tag.hasUUID("gtladditions:wireless_energy_owner")) tag.getUUID("gtladditions:wireless_energy_owner") else null
    }

    @JvmStatic
    fun isBound(stack: ItemStack): Boolean = owner(stack) != null

    fun ownerName(stack: ItemStack): String = stack.tag?.getString("gtladditions:wireless_energy_owner_name")
        ?.takeIf { it.isNotBlank() } ?: owner(stack)?.toString().orEmpty()

    @JvmStatic
    fun toggleBinding(player: Player, stack: ItemStack) {
        if (player !is ServerPlayer || stack.item !is BaseGadget) return
        val tag = stack.orCreateTag
        if (owner(stack) == player.uuid) {
            tag.remove("gtladditions:wireless_energy_owner")
            tag.remove("gtladditions:wireless_energy_owner_name")
            player.displayClientMessage(Component.translatable("gtladditions.gadget.wireless.unbound"), true)
        } else {
            tag.putUUID("gtladditions:wireless_energy_owner", player.uuid)
            tag.putString("gtladditions:wireless_energy_owner_name", player.name.string)
            player.displayClientMessage(
                Component.translatable("gtladditions.gadget.wireless.bound", player.name),
                true
            )
        }
        player.inventory.setChanged()
    }

    @JvmStatic
    fun tryPayBlock(stack: ItemStack): Boolean {
        val owner = owner(stack) ?: return false
        return ledger.tryDebit(owner, blockCost(stack))
    }

    @JvmStatic
    fun canAffordCut(stack: ItemStack, positions: Long): Boolean {
        val owner = owner(stack) ?: return false
        return positions >= 0 && ledger.canAfford(owner, blockCost(stack).multiply(BigInteger.valueOf(positions)))
    }

    @JvmStatic
    fun cutFailureMessage(stack: ItemStack, positions: Long): Component {
        val owner = owner(stack)
        val balance = if (owner != null) ledger.balance(owner) else BigInteger.ZERO
        val required = blockCost(stack).multiply(BigInteger.valueOf(positions))
        return Component.translatable(
            "gtladditions.gadget.wireless.insufficient",
            CommonUtils.formatBigIntegerFixed(required),
            CommonUtils.formatBigIntegerFixed(balance)
        )
    }

    private fun blockCost(stack: ItemStack): BigInteger =
        WirelessGadgetEnergyLedger.euCost((stack.item as BaseGadget).energyCost, FeCompat.ratio(false))
}

/** The caller serializes payments on the server thread; reads and failed payments never change the account. */
internal class WirelessGadgetEnergyLedger(
    private val readBalance: (UUID) -> BigInteger,
    private val writeBalance: (UUID, BigInteger) -> Unit
) {
    fun balance(owner: UUID): BigInteger = readBalance(owner).max(BigInteger.ZERO)

    fun canAfford(owner: UUID, cost: BigInteger): Boolean =
        cost.signum() >= 0 && balance(owner) >= cost

    fun tryDebit(owner: UUID, cost: BigInteger): Boolean {
        if (cost.signum() < 0) return false
        val available = balance(owner)
        if (available < cost) return false
        if (cost.signum() > 0) writeBalance(owner, available.subtract(cost))
        return true
    }

    companion object {
        fun euCost(feCost: Int, ratio: Int): BigInteger {
            require(feCost >= 0 && ratio > 0)
            return BigInteger.valueOf(feCost.toLong())
                .add(BigInteger.valueOf(ratio.toLong() - 1))
                .divide(BigInteger.valueOf(ratio.toLong()))
        }
    }
}