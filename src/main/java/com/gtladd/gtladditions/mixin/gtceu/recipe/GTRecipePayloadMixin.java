package com.gtladd.gtladditions.mixin.gtceu.recipe;

import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.syncdata.GTRecipePayload;

import com.lowdragmc.lowdraglib.syncdata.payload.ObjectTypedPayload;

import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.math.BigInteger;

@Mixin(value = GTRecipePayload.class, priority = 2000)
public abstract class GTRecipePayloadMixin extends ObjectTypedPayload<GTRecipe> {

    @Nullable
    @Override
    public Tag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", payload.id.toString());
        tag.put("recipe", GTRecipeSerializer.CODEC.encodeStart(NbtOps.INSTANCE, payload).result().orElse(new CompoundTag()));
        tag.putLong("realParallels", IGTRecipe.of(payload).getRealParallels());
        tag.putInt("ocTier", payload.ocTier);
        if (payload instanceof WirelessGTRecipe wirelessGTRecipe) {
            BigInteger wirelessEut = wirelessGTRecipe.getWirelessEuTickInputs();
            if (wirelessEut != null && wirelessEut.signum() != 0) {
                tag.putByteArray("wirelessEut", wirelessEut.toByteArray());
            }
        }
        return tag;
    }

    @Override
    public void deserializeNBT(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            GTRecipe recipe = GTRecipeSerializer.CODEC.parse(NbtOps.INSTANCE, compoundTag.get("recipe")).result().orElse(null);
            if (recipe != null) {
                BigInteger wirelessEut = null;
                if (compoundTag.contains("wirelessEut")) {
                    byte[] bytes = compoundTag.getByteArray("wirelessEut");
                    wirelessEut = new BigInteger(bytes);
                }
                payload = wirelessEut == null ? recipe : new WirelessGTRecipe(recipe, wirelessEut);
                payload.id = new ResourceLocation(compoundTag.getString("id"));
                IGTRecipe.of(payload).setRealParallels(compoundTag.contains("realParallels") ? compoundTag.getLong("realParallels") : 1);
                payload.ocTier = compoundTag.getInt("ocTier");
            }
        } else if (tag instanceof StringTag stringTag) {
            var recipe = Registries.getRecipeManager().byKey(new ResourceLocation(stringTag.getAsString())).orElse(null);
            if (recipe instanceof GTRecipe gtRecipe) {
                payload = gtRecipe;
            } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
                payload = GTRecipeTypes.FURNACE_RECIPES.toGTrecipe(new ResourceLocation(stringTag.getAsString()),
                        smeltingRecipe);
            } else {
                payload = null;
            }
        } else if (tag instanceof ByteArrayTag byteArray) {
            ByteBuf copiedDataBuffer = Unpooled.copiedBuffer(byteArray.getAsByteArray());
            FriendlyByteBuf buf = new FriendlyByteBuf(copiedDataBuffer);
            payload = (GTRecipe) Registries.getRecipeManager().byKey(buf.readResourceLocation()).orElse(null);
            buf.release();
        }
    }

    @Override
    public void writePayload(FriendlyByteBuf buf) {
        buf.writeResourceLocation(payload.id);
        GTRecipeSerializer.SERIALIZER.toNetwork(buf, payload);
        buf.writeLong(IGTRecipe.of(payload).getRealParallels());
        buf.writeInt(payload.ocTier);
        if (payload instanceof WirelessGTRecipe wirelessGTRecipe) {
            BigInteger wirelessEut = wirelessGTRecipe.getWirelessEuTickInputs();
            if (wirelessEut != null && wirelessEut.shortValue() != 0) {
                buf.writeByteArray(wirelessEut.toByteArray());
            }
        }
    }

    @Override
    public void readPayload(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        if (buf.isReadable()) {
            GTRecipe recipe = GTRecipeSerializer.SERIALIZER.fromNetwork(id, buf);
            if (buf.isReadable()) {
                IGTRecipe.of(payload).setRealParallels(buf.readLong());
                payload.ocTier = buf.readInt();
            }
            if (buf.isReadable()) {
                BigInteger wirelessEut = new BigInteger(buf.readByteArray());
                payload = new WirelessGTRecipe(recipe, wirelessEut);
                IGTRecipe.of(payload).setRealParallels(IGTRecipe.of(recipe).getRealParallels());
                payload.ocTier = recipe.ocTier;
            } else payload = recipe;
        } else {
            RecipeManager recipeManager = Registries.getRecipeManager();
            this.payload = (GTRecipe) recipeManager.byKey(id).orElse(null);
        }
    }
}
