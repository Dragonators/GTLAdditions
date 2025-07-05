package com.gtladd.gtladditions.data.recipes.process

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.material.GTLAddMaterial
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.tterrag.registrate.util.entry.ItemEntry
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.Item
import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.common.recipe.condition.GravityCondition
import org.gtlcore.gtlcore.utils.Registries
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object socprocess {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        electricblastfurnace.init(provider)
        cutter.init(provider)
        engravingarray.init(provider)
        circuitassembler.init(provider)
        generatedRecipe(provider)
    }

    private fun generatedRecipe(provider : Consumer<FinishedRecipe?>) {
        GTRecipeTypes.CHEMICAL_BATH_RECIPES.recipeBuilder(GTLAdditions.id("bioware_echo_shard_wafer"))
            .inputItems(GTLAddItems.ECHO_SHARD_WAFER !!.asStack())
            .inputFluids(BiohmediumSterilized.getFluid(250))
            .outputItems(GTLAddItems.BIOWARE_ECHO_SHARD_BOULE !!.asStack())
            .EUt(GTValues.VA[GTValues.UHV].toLong()).duration(200)
            .cleanroom(CleanroomType.STERILE_CLEANROOM)
            .addCondition(GravityCondition()).save(provider)
        GTRecipeTypes.LARGE_CHEMICAL_RECIPES.recipeBuilder(GTLAdditions.id("prepare_extraordinary_soc_wafer"))
            .inputItems(GTLAddItems.HASSIUM_WAFER !!.asStack())
            .inputItems(dust, FranciumCaesiumCadmiumBromide, 2)
            .inputFluids(SeaborgiumDopedNanotubes.getFluid(144))
            .inputFluids(CarbonNanotubes.getFluid(144))
            .outputItems(GTLAddItems.PREPARE_EXTRAORDINARY_SOC_WAFER !!.asStack())
            .EUt(GTValues.VA[GTValues.UEV].toLong()).duration(200)
            .cleanroom(GTLCleanroomType.LAW_CLEANROOM)
            .save(provider)
        GTLRecipeTypes.SPS_CRAFTING_RECIPES.recipeBuilder(GTLAdditions.id("dragon_element_starmetal_wafer"))
            .inputItems(GTLAddItems.STARMETAL_WAFER!!, 4)
            .inputItems(getItemStack("kubejs:kinetic_matter"))
            .inputItems(getItemStack("kubejs:unstable_star", 2))
            .inputFluids(Mana.getFluid(10000))
            .inputFluids(DragonElement.getFluid(1000))
            .inputFluids(FreeAlphaGas.getFluid(500))
            .outputItems(GTLAddItems.DRAGON_ELEMENT_STARMETAL_WAFER!!, 4)
            .EUt(GTValues.VA[GTValues.UIV].toLong()).duration(200)
            .cleanroom(GTLCleanroomType.LAW_CLEANROOM)
            .addCondition(GravityCondition()).save(provider)
        GTLRecipeTypes.QFT_RECIPES.recipeBuilder(GTLAdditions.id("prepare_spacetime_soc_wafer"))
            .inputItems(GTLAddItems.PERIODICIUM_WAFER!!, 4)
            .inputItems(getItemStack("kubejs:charged_lepton_trap_crystal"))
            .inputItems(getItemStack("kubejs:nuclear_star", 4))
            .inputFluids(CosmicMesh.getFluid(FluidStorageKeys.PLASMA, 1000))
            .inputFluids(CosmicElement.getFluid(10000))
            .inputFluids(SpaceTime.getFluid(500))
            .outputItems(GTLAddItems.PREPARE_SPACETIME_SOC_WAFER!!, 4)
            .EUt(GTValues.VA[GTValues.OpV].toLong())
            .duration(200).save(provider)
        GTLRecipeTypes.DIMENSIONALLY_TRANSCENDENT_MIXER_RECIPES.recipeBuilder(GTLAdditions.id("prepare_primary_soc_wafer"))
            .notConsumable(getItemStack("kubejs:eternity_catalyst"))
            .inputItems(GTLAddItems.INFINITY_WAFER!!, 4)
            .inputItems(getItemStack("kubejs:quantum_anomaly"))
            .inputItems(getItemStack("kubejs:two_way_foil"))
            .inputItems(getItemStack("kubejs:void_matter"))
            .inputItems(dust, TranscendentMetal, 16)
            .inputFluids(RawStarMatter.getFluid(FluidStorageKeys.PLASMA, 10000))
            .inputFluids(SpaceTime.getFluid(1000))
            .inputFluids(PrimordialMatter.getFluid(1000))
            .outputItems(GTLAddItems.PREPARE_PRIMARY_SOC_WAFER!!, 4)
            .EUt(4L * GTValues.VA[GTValues.MAX]).duration(200)
            .cleanroom(GTLCleanroomType.LAW_CLEANROOM).addCondition(GravityCondition())
            .save(provider)
        GTLAddRecipeBuilder("spacetime_lens", GTLRecipeTypes.PRECISION_ASSEMBLER_RECIPES)
            .inputItems("kubejs:grating_lithography_mask")
            .inputItems("kubejs:topological_manipulator_unit")
            .inputItems("kubejs:ctc_computational_unit")
            .inputFluids(QuantumDots.getFluid(1000))
            .inputFluids(CosmicComputingMixture.getFluid(1000))
            .inputFluids(Krypton.getFluid(10000))
            .outputItems(GTLAddItems.SPACETIME_LENS!!)
            .EUt(GTValues.VA[GTValues.OpV].toLong()).duration(2000)
            .cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider)
    }

    internal object electricblastfurnace {
        fun init(provider : Consumer<FinishedRecipe?>) {
            GTRecipeTypes.BLAST_RECIPES.recipeBuilder(GTLAdditions.id("echo_shard_boule"))
                .inputItems(GTItems.SILICON_BOULE, 64)
                .inputItems(dust, GTLAddMaterial.GALLIUM_OXIDE!!, 16)
                .inputItems(getItemStack("gtceu:echo_shard_dust", 16))
                .inputFluids(Krypton.getFluid(16000))
                .outputItems(GTLAddItems.ECHO_SHARD_BOULE!!)
                .EUt(GTValues.VA[GTValues.UV].toLong()).duration(21000)
                .blastFurnaceTemp(14400).save(provider)
            addBlastRecipe(
                Hassium, GTLAddItems.HASSIUM_BOULE !!,
                GTValues.VA[GTValues.UHV], 24000, 18000, provider
            )
            addBlastRecipe(
                Starmetal, GTLAddItems.STARMETAL_BOULE !!,
                GTValues.VA[GTValues.UEV], 27000, 21000, provider
            )
            addBlastRecipe(
                Periodicium, GTLAddItems.PERIODICIUM_BOULE !!,
                GTValues.VA[GTValues.UXV], 30000, 36000, provider
            )
            addBlastRecipe(
                Infinity, GTLAddItems.INFINITY_BOULE !!,
                GTValues.VA[GTValues.OpV], 33000, 62000, provider
            )
            GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder(GTLAdditions.id("ammonium_gallium_sulfate"))
                .inputItems(dust, Gallium)
                .inputFluids(SulfuricAcid.getFluid(2000))
                .inputFluids(Ammonia.getFluid(1000))
                .outputItems(dust, GTLAddMaterial.AMMONIUM_GALIUM_SULFATE!!)
                .EUt(GTValues.VA[GTValues.EV].toLong()).duration(200).save(provider)
            GTRecipeTypes.LARGE_CHEMICAL_RECIPES.recipeBuilder(GTLAdditions.id("ammonium_gallium_sulfate"))
                .inputItems(dust, Gallium)
                .inputFluids(SulfuricAcid.getFluid(2000))
                .inputFluids(Ammonia.getFluid(1000))
                .outputItems(dust, GTLAddMaterial.AMMONIUM_GALIUM_SULFATE!!)
                .EUt(GTValues.VA[GTValues.EV].toLong()).duration(200).save(provider)
            GTRecipeTypes.LARGE_CHEMICAL_RECIPES.recipeBuilder(GTLAdditions.id("gallium_oxide"))
                .inputItems(dust, GTLAddMaterial.AMMONIUM_GALIUM_SULFATE!!, 4)
                .inputFluids(Oxygen.getFluid(15000))
                .outputItems(dust, GTLAddMaterial.GALLIUM_OXIDE!!, 2)
                .outputFluids(Nitrogen.getFluid(2000))
                .outputFluids(SulfurTrioxide.getFluid(8000))
                .outputFluids(Water.getFluid(8000))
                .EUt(GTValues.VA[GTValues.IV].toLong()).duration(200)
                .save(provider)
        }

        fun addBlastRecipe(
            input : Material,
            output : ItemEntry<Item?>,
            EUt : Int,
            duration : Int,
            temperature : Int,
            provider : Consumer<FinishedRecipe?>
        ) {
            GTRecipeTypes.BLAST_RECIPES.recipeBuilder(GTLAdditions.id(output.asItem().toString()))
                .inputItems(GTItems.SILICON_BOULE, 64)
                .inputItems(dust, GTLAddMaterial.GALLIUM_OXIDE!!, 16)
                .inputItems(dust, input, 16)
                .inputFluids(Krypton.getFluid(16000))
                .outputItems(output).EUt(EUt.toLong())
                .duration(duration).blastFurnaceTemp(temperature).save(provider)
        }
    }

    internal object cutter {
        fun init(provider : Consumer<FinishedRecipe?>) {
            addCutterRecipe(
                "echo_shard_wafer", GTLAddItems.ECHO_SHARD_BOULE !!,
                16, GTLAddItems.ECHO_SHARD_WAFER !!,
                GTValues.VA[GTValues.UV], CleanroomType.STERILE_CLEANROOM, provider
            )
            addCutterRecipe(
                "outstanding_soc", GTLAddItems.OUTSTANDING_SOC_WAFER !!,
                6, GTLAddItems.OUTSTANDING_SOC !!,
                GTValues.VA[GTValues.UV], CleanroomType.STERILE_CLEANROOM, provider
            )
            addCutterRecipe(
                "hassium_wafer", GTLAddItems.HASSIUM_BOULE !!,
                16, GTLAddItems.HASSIUM_WAFER !!,
                GTValues.VA[GTValues.UHV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addCutterRecipe(
                "extraordinary_soc_wafer", GTLAddItems.EXTRAORDINARY_SOC_WAFER !!,
                6, GTLAddItems.EXTRAORDINARY_SOC !!,
                GTValues.VA[GTValues.UHV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addCutterRecipe(
                "starmetal_wafer", GTLAddItems.STARMETAL_BOULE !!,
                16, GTLAddItems.STARMETAL_WAFER !!,
                GTValues.VA[GTValues.UEV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addCutterRecipe(
                "chaos_soc", GTLAddItems.CHAOS_SOC_WAFER !!,
                6, GTLAddItems.CHAOS_SOC !!,
                GTValues.VA[GTValues.UEV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addCutterRecipe(
                "periodicium_wafer", GTLAddItems.PERIODICIUM_BOULE !!,
                256, GTLAddItems.PERIODICIUM_WAFER !!,
                GTValues.VA[GTValues.UXV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addCutterRecipe(
                "spacetime_soc", GTLAddItems.SPACETIME_SOC_WAFER !!,
                6, GTLAddItems.SPACETIME_SOC !!,
                GTValues.VA[GTValues.UXV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addCutterRecipe(
                "infinity_wafer", GTLAddItems.INFINITY_BOULE !!,
                16, GTLAddItems.INFINITY_WAFER !!,
                GTValues.VA[GTValues.OpV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addCutterRecipe(
                "primary_soc", GTLAddItems.PRIMARY_SOC_WAFER !!,
                6, GTLAddItems.PRIMARY_SOC !!,
                GTValues.VA[GTValues.OpV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
        }

        private fun addCutterRecipe(
            id : String?,
            input : ItemEntry<Item?>,
            output : Int,
            outputitem : ItemEntry<Item?>,
            EUt : Int,
            cleanroomType : CleanroomType,
            provider : Consumer<FinishedRecipe?>
        ) {
            val builder = GTRecipeTypes.CUTTER_RECIPES.recipeBuilder(GTLAdditions.id(id + "_0"))
                .inputItems(input)
                .outputItems(outputitem, output)
                .EUt(EUt.toLong()).cleanroom(cleanroomType)
            if (EUt > GTValues.VA[GTValues.UEV]) {
                builder.inputFluids(GradePurifiedWater16.getFluid((if (EUt > GTValues.VA[GTValues.UXV]) 1000 else 500).toLong()))
                    .duration(450).save(provider)
                return
            }
            val recipe = builder.copy(GTLAdditions.id(id + "_1"))
            builder.inputFluids(GradePurifiedWater8.getFluid(500)).duration(900).save(provider)
            recipe.inputFluids(GradePurifiedWater16.getFluid(250)).duration(450).save(provider)
        }
    }

    internal object engravingarray {
        fun init(provider : Consumer<FinishedRecipe?>) {
            addEngravingRecipe(
                GTLAddItems.BIOWARE_ECHO_SHARD_BOULE !!, Photoresist,
                "kubejs:grating_lithography_mask", GTLAddItems.OUTSTANDING_SOC_WAFER !!,
                GTValues.VA[GTValues.UHV], 300, CleanroomType.STERILE_CLEANROOM, provider
            )
            addEngravingRecipe(
                GTLAddItems.PREPARE_EXTRAORDINARY_SOC_WAFER !!, Photoresist,
                "kubejs:grating_lithography_mask", GTLAddItems.EXTRAORDINARY_SOC_WAFER !!,
                GTValues.VA[GTValues.UEV], 400, GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addEngravingRecipe(
                GTLAddItems.DRAGON_ELEMENT_STARMETAL_WAFER !!, EuvPhotoresist,
                "kubejs:grating_lithography_mask", GTLAddItems.CHAOS_SOC_WAFER !!,
                GTValues.VA[GTValues.UIV], 500, GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addEngravingRecipe(
                GTLAddItems.PREPARE_SPACETIME_SOC_WAFER !!, GammaRaysPhotoresist,
                "gtladditions:spacetime_lens", GTLAddItems.SPACETIME_SOC_WAFER !!,
                GTValues.VA[GTValues.UXV], 600, GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addEngravingRecipe(
                GTLAddItems.PREPARE_PRIMARY_SOC_WAFER !!, GammaRaysPhotoresist,
                "gtladditions:spacetime_lens", GTLAddItems.PRIMARY_SOC_WAFER !!,
                GTValues.VA[GTValues.OpV], 800, GTLCleanroomType.LAW_CLEANROOM, provider
            )
        }

        private fun addEngravingRecipe(
            input : ItemEntry<Item?>,
            fluid : Material,
            noinput : String,
            output : ItemEntry<Item?>,
            EUt : Int,
            duration : Int,
            cleanroomType : CleanroomType,
            provider : Consumer<FinishedRecipe?>
        ) {
            GTLRecipeTypes.DIMENSIONAL_FOCUS_ENGRAVING_ARRAY_RECIPES.recipeBuilder(
                GTLAdditions.id(output.asItem().toString())
            )
                .inputItems(input).notConsumable(getItemStack(noinput))
                .inputFluids(fluid.getFluid(100))
                .outputItems(output).EUt(EUt.toLong()).duration(duration)
                .cleanroom(cleanroomType).save(provider)
            GTLAddRecipesTypes.PHOTON_MATRIX_ETCH.recipeBuilder(GTLAdditions.id(output.asItem().toString()))
                .inputItems(input).notConsumable(getItemStack(noinput))
                .inputFluids(fluid.getFluid(75))
                .outputItems(output).EUt((EUt / 4).toLong()).duration((duration * 0.75).toInt())
                .cleanroom(cleanroomType).save(provider)
        }
    }

    internal object circuitassembler {
        fun init(provider : Consumer<FinishedRecipe?>) {
            addCircuitRecipe(
                "bioware_processor",
                "kubejs:bioware_printed_circuit_board", GTLAddItems.OUTSTANDING_SOC !!,
                Naquadah, Quantanium,
                "kubejs:bioware_processor",
                GTValues.VA[GTValues.UHV], CleanroomType.STERILE_CLEANROOM, provider
            )
            addCircuitRecipe(
                "optical_processor",
                "kubejs:optical_printed_circuit_board", GTLAddItems.EXTRAORDINARY_SOC !!,
                Dubnium, Vibranium,
                "kubejs:optical_processor",
                GTValues.VA[GTValues.UEV], GTLCleanroomType.LAW_CLEANROOM, provider
            )
            addCircuitRecipe(
                "exotic_processor",
                "kubejs:exotic_printed_circuit_board", GTLAddItems.CHAOS_SOC !!,
                Cinobite, HastelloyX78,
                "kubejs:exotic_processor",
                GTValues.VA[GTValues.UIV], CleanroomType.STERILE_CLEANROOM, provider
            )
            generateCircuitRecipes(provider)
        }

        private fun addCircuitRecipe(
            id : String?,
            inputs : String,
            input : ItemEntry<Item?>,
            material1 : Material,
            material2 : Material,
            output : String,
            EUt : Int,
            cleanroomType : CleanroomType,
            provider : Consumer<FinishedRecipe?>
        ) {
            val builder = GTLAddRecipeBuilder(id + "_0", GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES)
                .inputItems(inputs).inputItems(input)
                .inputItems(wireFine, material1, 8)
                .inputItems(bolt, material2, 8)
                .outputItems(getItemStack(output, 4)).EUt(EUt.toLong())
                .cleanroom(cleanroomType) as GTLAddRecipeBuilder
            if (EUt > GTValues.VA[GTValues.UEV]) {
                builder.inputFluids(SuperMutatedLivingSolder.getFluid(144))
                    .duration(if (EUt <= GTValues.VA[GTValues.UEV]) 200 else 150).save(provider)
                return
            }
            val recipe = builder.copy(GTLAdditions.id(id + "_1"))
            builder.inputFluids(MutatedLivingSolder.getFluid(144))
                .duration(if (EUt <= GTValues.VA[GTValues.UEV]) 200 else 150).save(provider)
            recipe.inputFluids(SuperMutatedLivingSolder.getFluid(72))
                .duration(if (EUt <= GTValues.VA[GTValues.UEV]) 200 else 150).save(provider)
        }

        private fun generateCircuitRecipes(provider : Consumer<FinishedRecipe?>) {
            GTLAddRecipeBuilder("cosmic_processor", GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES)
                .inputItems("kubejs:cosmic_printed_circuit_board")
                .inputItems(GTLAddItems.SPACETIME_SOC!!)
                .inputItems(wireFine, HastelloyX78, 8)
                .inputItems(plate, Crystalmatrix)
                .inputFluids(SuperMutatedLivingSolder.getFluid(288))
                .EUt(GTValues.VA[GTValues.UXV].toLong()).cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                .outputItems(getItemStack("kubejs:cosmic_processor", 4))
                .duration(150).save(provider)
            GTLAddRecipeBuilder("supracausal_processor", GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES)
                .inputItems("kubejs:supracausal_printed_circuit_board")
                .inputItems(GTLAddItems.PRIMARY_SOC!!)
                .inputItems(wireGtDouble, Hypogen, 4)
                .inputItems(plate, DraconiumAwakened)
                .inputFluids(SuperMutatedLivingSolder.getFluid(360))
                .EUt(GTValues.VA[GTValues.OpV].toLong()).cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                .outputItems(getItemStack("kubejs:supracausal_processor", 4))
                .duration(150).save(provider)
        }
    }
}
