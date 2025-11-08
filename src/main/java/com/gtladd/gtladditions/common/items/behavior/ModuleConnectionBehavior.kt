package com.gtladd.gtladditions.common.items.behavior

import com.gregtechceu.gtceu.api.item.component.IInteractionItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import org.apache.logging.log4j.LogManager

object ModuleConnectionBehavior : IInteractionItem {

    private val LOGGER = LogManager.getLogger(ModuleConnectionBehavior::class.java)

    private const val NBT_HOST_X = "HostX"
    private const val NBT_HOST_Y = "HostY"
    private const val NBT_HOST_Z = "HostZ"
    private const val NBT_MODULES = "Modules"
    private const val NBT_HAS_HOST = "HasHost"

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player as? ServerPlayer ?: return InteractionResult.PASS
        val itemStack = context.itemInHand
        val pos = context.clickedPos

        if (player.isShiftKeyDown) {
            val tag = itemStack.orCreateTag
            tag.putInt(NBT_HOST_X, pos.x)
            tag.putInt(NBT_HOST_Y, pos.y)
            tag.putInt(NBT_HOST_Z, pos.z)
            tag.putBoolean(NBT_HAS_HOST, true)
            tag.put(NBT_MODULES, ListTag())

            player.sendSystemMessage(
                Component.literal("§a已记录主机坐标: (${pos.x}, ${pos.y}, ${pos.z})")
            )

            return InteractionResult.SUCCESS
        } else {
            val tag = itemStack.orCreateTag

            if (!tag.getBoolean(NBT_HAS_HOST)) {
                player.sendSystemMessage(
                    Component.literal("§c请先使用 Shift + 右键记录主机坐标！")
                )
                return InteractionResult.FAIL
            }

            val hostX = tag.getInt(NBT_HOST_X)
            val hostY = tag.getInt(NBT_HOST_Y)
            val hostZ = tag.getInt(NBT_HOST_Z)

            val offsetX = pos.x - hostX
            val offsetY = pos.y - hostY
            val offsetZ = pos.z - hostZ

            val modulesList = tag.getList(NBT_MODULES, 10) // 10 = CompoundTag type
            val moduleTag = CompoundTag()
            moduleTag.putInt("x", offsetX)
            moduleTag.putInt("y", offsetY)
            moduleTag.putInt("z", offsetZ)
            modulesList.add(moduleTag)

            tag.put(NBT_MODULES, modulesList)

            player.sendSystemMessage(
                Component.literal("§a已记录第 ${modulesList.size} 个模块，偏移量: ($offsetX, $offsetY, $offsetZ)")
            )

            return InteractionResult.SUCCESS
        }
    }

    override fun use(
        item: Item,
        level: Level,
        player: Player,
        hand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))

        val serverPlayer = player as? ServerPlayer ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
        val itemStack = player.getItemInHand(hand)

        if (serverPlayer.isShiftKeyDown) {
            val tag = itemStack.orCreateTag

            if (!tag.getBoolean(NBT_HAS_HOST)) {
                serverPlayer.sendSystemMessage(
                    Component.literal("§c没有记录任何数据！")
                )
                return InteractionResultHolder.fail(itemStack)
            }

            val hostX = tag.getInt(NBT_HOST_X)
            val hostY = tag.getInt(NBT_HOST_Y)
            val hostZ = tag.getInt(NBT_HOST_Z)
            val modulesList = tag.getList(NBT_MODULES, 10)

            val output = buildString {
                appendLine("=".repeat(60))
                appendLine("Module Connection Record")
                appendLine("=".repeat(60))
                appendLine("Host Position: ($hostX, $hostY, $hostZ)")
                appendLine("Total Modules: ${modulesList.size}")
                appendLine()

                if (modulesList.isEmpty()) {
                    appendLine("  (No modules recorded)")
                } else {
                    appendLine("Module Offsets:")
                    for (i in 0 until modulesList.size) {
                        val module = modulesList.getCompound(i)
                        val x = module.getInt("x")
                        val y = module.getInt("y")
                        val z = module.getInt("z")
                        appendLine("  [$i] Offset: ($x, $y, $z)")
                    }
                }

                appendLine()
                appendLine("Formatted Array Output (Copy below):")
                appendLine("-".repeat(60))
                append("val offsets = arrayOf(")
                if (modulesList.isNotEmpty()) {
                    appendLine()
                    for (i in 0 until modulesList.size) {
                        val module = modulesList.getCompound(i)
                        val x = module.getInt("x")
                        val y = module.getInt("y")
                        val z = module.getInt("z")
                        append("    intArrayOf($x, $y, $z)")
                        if (i < modulesList.size - 1) appendLine(",")
                        else appendLine()
                    }
                    append(")")
                } else {
                    append(")")
                }
                appendLine()
                appendLine("-".repeat(60))
                appendLine("=".repeat(60))
            }

            LOGGER.info("\n$output")

            serverPlayer.sendSystemMessage(
                Component.literal("§a已将记录输出到日志！共 ${modulesList.size} 个模块")
            )

            return InteractionResultHolder.success(itemStack)
        }

        return InteractionResultHolder.pass(itemStack)
    }
}
