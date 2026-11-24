package com.gtladd.gtladditions.api.recipe

import com.google.gson.JsonObject
import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraftforge.common.crafting.IIngredientSerializer
import org.gtlcore.gtlcore.utils.Registries.getItem
import java.util.stream.Stream

/** GT indexes bare stacks by item; ME pattern buffers match the concrete alternatives by AE key. */
class UntypedSingularityIngredient private constructor(private val singularity: Item) :
    Ingredient(
        Stream.of(
            ItemValue(ItemStack(singularity)),
            ItemValue(ItemStack(singularity).apply { tag = CompoundTag() })
        )
    ) {
    override fun test(stack: ItemStack?): Boolean = stack != null && stack.`is`(singularity) && !stack.hasTag()

    override fun isSimple(): Boolean = false

    override fun getSerializer(): IIngredientSerializer<UntypedSingularityIngredient> = Serializer

    override fun toJson(): JsonObject = JsonObject().apply { addProperty("type", ID.toString()) }

    companion object Serializer : IIngredientSerializer<UntypedSingularityIngredient> {
        val ID = GTLAdditions.id("untyped_singularity")
        val INSTANCE by lazy { UntypedSingularityIngredient(getItem("avaritia:singularity")) }

        override fun parse(json: JsonObject): UntypedSingularityIngredient = INSTANCE

        override fun parse(buffer: FriendlyByteBuf): UntypedSingularityIngredient = INSTANCE

        override fun write(buffer: FriendlyByteBuf, ingredient: UntypedSingularityIngredient) {}
    }
}