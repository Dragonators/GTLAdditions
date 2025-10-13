package com.gtladd.gtladditions.common.data

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.data.GTLItems.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.config.ConfigHolder
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

object SkyTearsAndGregHeart {

    private val exceptDrills by lazy {
        ObjectOpenCustomHashSet(
            arrayOf(
                ChemicalHelper.get(TagPrefix.toolHeadDrill, GTMaterials.Steel),
                ChemicalHelper.get(TagPrefix.toolHeadDrill, GTMaterials.Titanium),
                ChemicalHelper.get(TagPrefix.toolHeadDrill, GTMaterials.NaquadahAlloy),
                ChemicalHelper.get(TagPrefix.toolHeadDrill, GTMaterials.Neutronium),
            ),
            ItemStackHashStrategy.comparingAllButCount()
        )
    }

    private val targetDrill: ItemStack by lazy { getItemStack("kubejs:machine_casing_grinding_head") }

    private val worldFragments by lazy {
        ObjectOpenHashSet(
            arrayOf(
                WORLD_FRAGMENTS_OVERWORLD.asItem(),
                WORLD_FRAGMENTS_NETHER.asItem(),
                WORLD_FRAGMENTS_END.asItem(),
                WORLD_FRAGMENTS_REACTOR.asItem(),
                WORLD_FRAGMENTS_MOON.asItem(),
                WORLD_FRAGMENTS_MARS.asItem(),
                WORLD_FRAGMENTS_VENUS.asItem(),
                WORLD_FRAGMENTS_MERCURY.asItem(),
                WORLD_FRAGMENTS_CERES.asItem(),
                WORLD_FRAGMENTS_IO.asItem(),
                WORLD_FRAGMENTS_GANYMEDE.asItem(),
                WORLD_FRAGMENTS_PLUTO.asItem(),
                WORLD_FRAGMENTS_ENCELADUS.asItem(),
                WORLD_FRAGMENTS_TITAN.asItem(),
                WORLD_FRAGMENTS_GLACIO.asItem(),
                WORLD_FRAGMENTS_BARNARDA.asItem()
            )
        )
    }

    private val itemModifier = ContentModifier.multiplier(min(4.0 / max(ConfigHolder.INSTANCE.durationMultiplier, 1.0), 33554431.0))

    fun initSkyBlock() {
        GTLRecipeTypes.FRAGMENT_WORLD_COLLECTION.onRecipeBuild { recipeBuilder: GTRecipeBuilder, provider: Consumer<FinishedRecipe> ->
            recipeBuilder.input[ItemRecipeCapability.CAP]?.let {
                for (content in it) {
                    val stacks = ItemRecipeCapability.CAP.of(content.content).items
                    if (stacks.size > 0 && exceptDrills.contains(stacks[0])) return@onRecipeBuild
                    else if (stacks[0].`is`(targetDrill.item)) {
                        genTargetDrillRecipe(recipeBuilder, provider)
                        return@onRecipeBuild
                    }
                }
            }

            val newBuilder = GTLAddRecipesTypes.STAR_CORE_STRIPPER.copyFrom(recipeBuilder).EUt(GTValues.VA[GTValues.UHV].toLong())

            for (entry in newBuilder.output) {
                val cap = entry.key
                val contentList = entry.value
                val copyList = ObjectArrayList<Content>(contentList.size)

                if (cap == ItemRecipeCapability.CAP) {
                    for (content in contentList) {
                        val stacks = ItemRecipeCapability.CAP.of(content.content).items
                        if (stacks.size > 0 && MIRACLE_CRYSTAL.asItem() != stacks[0].item) {
                            if (content.chance >= content.maxChance && !worldFragments.contains(stacks[0].item)) {
                                copyList.add(content.copy(cap, itemModifier))
                            } else {
                                val temp = content.copy(cap, null)
                                temp.chance = min(temp.maxChance, temp.chance * 100)
                                copyList.add(temp)
                            }
                        } else {
                            copyList.add(content)
                        }
                    }
                } else {
                    for (content in contentList) {
                        if (content.chance >= content.maxChance) copyList.add(content)
                        else {
                            val temp = content.copy(cap, null)
                            temp.chance = temp.maxChance
                            copyList.add(temp)
                        }
                    }
                }

                contentList.clear()
                contentList.addAll(copyList)
            }
            newBuilder.save(provider)
        }
    }

    fun genTargetDrillRecipe(recipeBuilder: GTRecipeBuilder, provider: Consumer<FinishedRecipe>) {
        val newBuilder = GTLAddRecipesTypes.STAR_CORE_STRIPPER.copyFrom(recipeBuilder).EUt(GTValues.VA[GTValues.UHV].toLong())
        newBuilder.input[ItemRecipeCapability.CAP]!!.removeIf {
            ItemRecipeCapability.CAP.of(it.content).items[0].`is`(
                targetDrill.item
            )
        }
        newBuilder.chancedInput(targetDrill, 1, 0)

        newBuilder.output[ItemRecipeCapability.CAP]?.let { it ->
            val copyList = ObjectArrayList<Content>(it.size)
            for (content in it) {
                val temp = content.copy(ItemRecipeCapability.CAP, itemModifier)
                temp.chance = min(temp.maxChance, temp.chance * 200)
                copyList.add(temp)
            }
            it.clear()
            it.addAll(copyList)
        }

        newBuilder.save(provider)
    }
}