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
import org.spongepowered.asm.mixin.Unique;

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
        tag.putInt("batchSize", IGTRecipe.of(payload).getBatchSize());
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
                recipe.id = new ResourceLocation(compoundTag.getString("id"));
                IGTRecipe.of(recipe).setRealParallels(compoundTag.contains("realParallels") ? compoundTag.getLong("realParallels") : 1);
                IGTRecipe.of(recipe).setBatchSize(compoundTag.contains("batchSize") ? compoundTag.getInt("batchSize") : 1);
                recipe.ocTier = compoundTag.getInt("ocTier");
                if (wirelessEut == null) {
                    payload = recipe;
                } else {
                    payload = new WirelessGTRecipe(recipe, wirelessEut);
                    gTLAdditions$copyRecipeExtensionState(recipe, payload);
                }
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
        buf.writeInt(IGTRecipe.of(payload).getBatchSize());
        buf.writeInt(payload.ocTier);
        if (payload instanceof WirelessGTRecipe wirelessGTRecipe) {
            BigInteger wirelessEut = wirelessGTRecipe.getWirelessEuTickInputs();
            if (wirelessEut != null && wirelessEut.signum() != 0) {
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
                IGTRecipe.of(recipe).setRealParallels(buf.readLong());
                if (buf.isReadable()) {
                    IGTRecipe.of(recipe).setBatchSize(buf.readInt());
                    recipe.ocTier = buf.readInt();
                }
            }
            if (buf.isReadable()) {
                BigInteger wirelessEut = new BigInteger(buf.readByteArray());
                payload = new WirelessGTRecipe(recipe, wirelessEut);
                gTLAdditions$copyRecipeExtensionState(recipe, payload);
            } else payload = recipe;
        } else {
            RecipeManager recipeManager = Registries.getRecipeManager();
            this.payload = (GTRecipe) recipeManager.byKey(id).orElse(null);
        }
    }

    @Unique
    private static void gTLAdditions$copyRecipeExtensionState(GTRecipe source, GTRecipe target) {
        IGTRecipe sourceExtension = IGTRecipe.of(source);
        IGTRecipe targetExtension = IGTRecipe.of(target);
        targetExtension.setRealParallels(sourceExtension.getRealParallels());
        targetExtension.setBatchSize(sourceExtension.getBatchSize());
        targetExtension.setBatchProcessed(sourceExtension.isBatchProcessed());
        targetExtension.setSubTickParallelized(sourceExtension.isSubTickParallelized());
        target.ocTier = source.ocTier;
    }
}