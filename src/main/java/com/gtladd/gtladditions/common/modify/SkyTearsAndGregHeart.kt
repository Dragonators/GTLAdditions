package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.GTValues.UXV
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.toolHeadDrill
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.wireGtHex
import com.gregtechceu.gtceu.api.recipe.ResearchRecipeBuilder.StationRecipeBuilder
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.data.GTItems.*
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.data.GTMaterials.Oganesson
import com.gregtechceu.gtceu.common.data.GTMaterials.UUMatter
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES
import com.gregtechceu.gtceu.data.recipe.CustomTags
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.data.GTLBlocks.RHENIUM_REINFORCED_ENERGY_GLASS
import org.gtlcore.gtlcore.common.data.GTLItems.*
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine.COMPRESSED_FUSION_REACTOR
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineB.LARGE_FRAGMENT_WORLD_COLLECTION_MACHINE
import org.gtlcore.gtlcore.config.ConfigHolder
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

object SkyTearsAndGregHeart {

    private val exceptDrills by lazy {
        ObjectOpenCustomHashSet(
            arrayOf(
                ChemicalHelper.get(toolHeadDrill, GTMaterials.Steel),
                ChemicalHelper.get(toolHeadDrill, GTMaterials.Titanium),
                ChemicalHelper.get(toolHeadDrill, GTMaterials.NaquadahAlloy),
                ChemicalHelper.get(toolHeadDrill, GTMaterials.Neutronium),
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

    private val itemModifier = ContentModifier.multiplier(min(4.0 / max(ConfigHolder.INSTANCE.durationMultiplier, 1.0), 4096.0))

    fun init() {
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

            val newBuilder = GTLAddRecipesTypes.STAR_CORE_STRIPPER.copyFrom(recipeBuilder).EUt(VA[GTValues.UHV].toLong())

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
        val newBuilder = GTLAddRecipesTypes.STAR_CORE_STRIPPER.copyFrom(recipeBuilder).EUt(VA[GTValues.UHV].toLong())
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

    fun buildController(provider : Consumer<FinishedRecipe?>){
        ASSEMBLY_LINE_RECIPES.recipeBuilder(id("macro_atomic_resonant_fragment_stripper"))
            .inputItems(LARGE_FRAGMENT_WORLD_COLLECTION_MACHINE, 768)
            .inputItems(CustomTags.OpV_CIRCUITS, 64)
            .inputItems(CustomTags.OpV_CIRCUITS, 64)
            .inputItems(LARGE_FRAGMENT_WORLD_COLLECTION_MACHINE, 768)
            .inputItems(getItemStack("kubejs:dark_matter", 64))
            .inputItems(getItemStack("kubejs:dark_matter", 64))
            .inputItems(getItemStack("kubejs:dark_matter", 64))
            .inputItems(getItemStack("kubejs:dark_matter", 64))
            .inputItems(getItemStack("kubejs:space_drone_mk5", 64))
            .inputItems(COMPRESSED_FUSION_REACTOR[GTValues.UEV], 64)
            .inputItems(RHENIUM_REINFORCED_ENERGY_GLASS, 48)
            .inputItems(FIELD_GENERATOR_UXV, 32)
            .inputItems(ROBOT_ARM_UXV, 48)
            .inputItems(REALLY_ULTIMATE_BATTERY, 48)
            .inputItems(getItemStack("kubejs:awakened_core", 64))
            .inputItems(wireGtHex, NaquadriaticTaranium, 64)
            .inputFluids(Starmetal.getFluid(14600))
            .inputFluids(CelestialTungsten.getFluid(58900))
            .inputFluids(Oganesson.getFluid(200000))
            .inputFluids(UUMatter.getFluid(128000))
            .outputItems(MultiBlockMachine.MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER!!)
            .EUt(VA[UXV].toLong()).duration(9600)
            .stationResearch { b : StationRecipeBuilder? ->
                b !!.researchStack(LARGE_FRAGMENT_WORLD_COLLECTION_MACHINE.asStack())
                    .dataStack(TOOL_DATA_MODULE.asStack())
                    .EUt(VA[UXV]).CWUt(2048, 4096000)
            }
            .save(provider)
    }
}