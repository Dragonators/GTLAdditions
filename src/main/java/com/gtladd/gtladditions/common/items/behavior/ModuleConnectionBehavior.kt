package com.gtladd.gtladditions.common.items.behavior

import com.gregtechceu.gtceu.api.item.component.IInteractionItem
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
import net.minecraft.world.level.LevelReader
import org.apache.logging.log4j.LogManager

object ModuleConnectionBehavior : IInteractionItem {

    private val LOGGER = LogManager.getLogger(ModuleConnectionBehavior::class.java)

    private const val NBT_HOST_X = "HostX"
    private const val NBT_HOST_Y = "HostY"
    private const val NBT_HOST_Z = "HostZ"
    private const val NBT_HOST_FRONT = "HostFront"
    private const val NBT_HOST_UP = "HostUp"

    private const val NBT_MODULES = "Modules"
    private const val NBT_HAS_HOST = "HasHost"

    override fun onItemUseFirst(itemStack: ItemStack, context: UseOnContext): InteractionResult? {
        val player = context.player as? ServerPlayer ?: return InteractionResult.PASS
        val pos = context.clickedPos
        val level = context.level

        if (!player.isShiftKeyDown) {
            return InteractionResult.PASS
        }

        val blockEntity = level.getBlockEntity(pos)
        val controller = ((blockEntity as? IMachineBlockEntity)?.metaMachine as? MultiblockControllerMachine)
        val front : Direction
        val up : Direction

        if (controller == null) {
            front = Direction.NORTH
            up = Direction.UP
            player.sendSystemMessage(
                Component.literal("§e警告: 此方块不是多方块机器，使用默认朝向 (NORTH, UP)")
            )
        } else {
            front = controller.frontFacing
            up = if(controller.allowExtendedFacing() && controller.frontFacing.axis == Direction.Axis.Y) controller.upwardsFacing else Direction.UP
        }

        val tag = itemStack.orCreateTag

        if (!tag.getBoolean(NBT_HAS_HOST)) {
            tag.putInt(NBT_HOST_X, pos.x)
            tag.putInt(NBT_HOST_Y, pos.y)
            tag.putInt(NBT_HOST_Z, pos.z)
            tag.putString(NBT_HOST_FRONT, front.name)
            tag.putString(NBT_HOST_UP, up.name)
            tag.putBoolean(NBT_HAS_HOST, true)
            tag.put(NBT_MODULES, ListTag())

            player.sendSystemMessage(
                Component.literal("§a已记录主机坐标: (${pos.x}, ${pos.y}, ${pos.z})")
            )
            player.sendSystemMessage(
                Component.literal("§a主机朝向: Front=$front, Up=$up")
            )
        } else {
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
            moduleTag.putString("front", front.name)
            moduleTag.putString("up", up.name)
            controller?.definition?.name?.let { moduleTag.putString("definition", it.uppercase()) }
            modulesList.add(moduleTag)

            tag.put(NBT_MODULES, modulesList)

            player.sendSystemMessage(
                Component.literal("§a已记录第 ${modulesList.size} 个模块，偏移量: ($offsetX, $offsetY, $offsetZ)")
            )
            player.sendSystemMessage(
                Component.literal("§a子机朝向: Front=$front, Up=$up")
            )
        }

        return InteractionResult.SUCCESS
    }

    @Suppress("DuplicatedCode")
    override fun use(
        item: Item,
        level: Level,
        player: Player,
        hand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        if (level.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand))

        val serverPlayer = player as? ServerPlayer ?: return InteractionResultHolder.pass(player.getItemInHand(hand))
        val itemStack = player.getItemInHand(hand)
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
        val hostFront = tag.getString(NBT_HOST_FRONT)
        val hostUp = tag.getString(NBT_HOST_UP)
        val modulesList = tag.getList(NBT_MODULES, 10)

        // 生成输出信息
        val output = buildString {
            appendLine("=".repeat(60))
            appendLine("Module Connection Record (ModuleRenderInfo)")
            appendLine("=".repeat(60))
            appendLine("Host Position: ($hostX, $hostY, $hostZ)")
            appendLine("Host Front: $hostFront")
            appendLine("Host Up: $hostUp")
            appendLine("Total Modules: ${modulesList.size}")
            appendLine()

            if (modulesList.isEmpty()) {
                appendLine("  (No modules recorded)")
            } else {
                appendLine("Module Details:")
                for (i in 0 until modulesList.size) {
                    val module = modulesList.getCompound(i)
                    val x = module.getInt("x")
                    val y = module.getInt("y")
                    val z = module.getInt("z")
                    val front = module.getString("front")
                    val up = module.getString("up")
                    appendLine("  [$i] Offset: ($x, $y, $z), Front: $front, Up: $up")
                }
            }

            appendLine()
            appendLine("Kotlin - ModuleRenderInfo Array (Copy below):")
            appendLine("-".repeat(60))
            if (modulesList.isNotEmpty()) {
                appendLine("val moduleInfos = arrayOf(")
                for (i in 0 until modulesList.size) {
                    val module = modulesList.getCompound(i)
                    val x = module.getInt("x")
                    val y = module.getInt("y")
                    val z = module.getInt("z")
                    val front = module.getString("front")
                    val up = module.getString("up")
                    val definition = module.getString("definition")
                    appendLine("    ModuleRenderInfo(")
                    appendLine("        BlockPos($x, $y, $z),")
                    appendLine("        Direction.$hostFront,")
                    appendLine("        Direction.$hostUp,")
                    appendLine("        Direction.$front,")
                    appendLine("        Direction.$up,")
                    appendLine("        $definition")
                    append("    )")
                    if (i < modulesList.size - 1) appendLine(",")
                    else appendLine()
                }
                appendLine(")")
            } else {
                appendLine("// No modules to generate")
            }
            appendLine("-".repeat(60))

            appendLine()
            appendLine("Java - List.of() Format (Copy below):")
            appendLine("-".repeat(60))
            if (modulesList.isNotEmpty()) {
                appendLine("List.of(")
                for (i in 0 until modulesList.size) {
                    val module = modulesList.getCompound(i)
                    val x = module.getInt("x")
                    val y = module.getInt("y")
                    val z = module.getInt("z")
                    val front = module.getString("front")
                    val up = module.getString("up")
                    val definition = module.getString("definition")
                    appendLine("    new ModuleRenderInfo(")
                    appendLine("        new BlockPos($x, $y, $z),")
                    appendLine("        Direction.$hostFront,")
                    appendLine("        Direction.$hostUp,")
                    appendLine("        Direction.$front,")
                    appendLine("        Direction.$up,")
                    appendLine("        $definition")
                    append("    )")
                    if (i < modulesList.size - 1) appendLine(",")
                    else appendLine()
                }
                appendLine(");")
            } else {
                appendLine("// No modules to generate")
            }
            appendLine("-".repeat(60))
            appendLine("=".repeat(60))
        }

        LOGGER.info("\n$output")

        serverPlayer.sendSystemMessage(
            Component.literal("§a已将记录输出到日志！共 ${modulesList.size} 个模块")
        )
        serverPlayer.sendSystemMessage(
            Component.literal("§e已清除所有记录数据")
        )

        tag.remove(NBT_HOST_X)
        tag.remove(NBT_HOST_Y)
        tag.remove(NBT_HOST_Z)
        tag.remove(NBT_HOST_FRONT)
        tag.remove(NBT_HOST_UP)
        tag.remove(NBT_MODULES)
        tag.remove(NBT_HAS_HOST)

        return InteractionResultHolder.success(itemStack)
    }

    override fun sneakBypassUse(stack: ItemStack?, level: LevelReader?, pos: BlockPos?, player: Player?): Boolean {
        return true
    }
}
