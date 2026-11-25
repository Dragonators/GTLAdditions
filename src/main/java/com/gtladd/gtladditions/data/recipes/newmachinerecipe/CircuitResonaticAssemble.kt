package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.utils.GTUtil
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CIRCUIT_RESONATIC_ASSEMBLE
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object CircuitResonaticAssemble {
    @JvmStatic
    fun init(provider: Consumer<FinishedRecipe?>) {
        fun circuit(name: String, eut: Long, duration: Int, vararg inputs: ItemStack) {
            val builder = CIRCUIT_RESONATIC_ASSEMBLE.recipeBuilder(GTLAdditions.id("circuit_resonatic_$name"))
                .inputItems(*inputs)
                .outputItems(getItemStack("kubejs:circuit_resonatic_$name", 5))
                .EUt(eut)
                .duration(duration)

            val tier = GTUtil.getFloorTierByVoltage(eut).toInt()
            if (tier <= GTValues.LuV) {
                builder.inputFluids(SolderingAlloy.getFluid(36))
            } else {
                builder.inputFluids(SuperMutatedLivingSolder.getFluid(36L * (tier - GTValues.LuV)))
            }

            builder.save(provider)
        }

        circuit(
            "ulv",
            30L,
            75,
            getItemStack("gtceu:tantalum_capacitor", 3),
            getItemStack("gtceu:resistor", 3),
            getItemStack("gtceu:inductor", 3),
            getItemStack("kubejs:imprinted_resonatic_circuit_board"),
            getItemStack("gtceu:magneto_resonatic_gem"),
            getItemStack("gtceu:vacuum_tube")
        )

        circuit(
            "lv",
            120L,
            135,
            getItemStack("gtceu:diode", 3),
            getItemStack("gtceu:capacitor", 3),
            getItemStack("gtceu:transistor", 3),
            getItemStack("kubejs:imprinted_resonatic_circuit_board"),
            getItemStack("gtceu:magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_ulv")
        )

        circuit(
            "mv",
            480L,
            225,
            getItemStack("gtceu:diode", 6),
            getItemStack("gtceu:capacitor", 6),
            getItemStack("gtceu:transistor", 6),
            getItemStack("kubejs:imprinted_resonatic_circuit_board"),
            getItemStack("gtceu:magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_lv")
        )

        circuit(
            "hv",
            1920L,
            345,
            getItemStack("gtceu:smd_diode", 3),
            getItemStack("gtceu:smd_capacitor", 3),
            getItemStack("gtceu:smd_transistor", 3),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 2),
            getItemStack("gtceu:flawless_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_mv")
        )

        circuit(
            "ev",
            7680L,
            495,
            getItemStack("gtceu:smd_diode", 6),
            getItemStack("gtceu:smd_capacitor", 6),
            getItemStack("gtceu:smd_transistor", 6),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 4),
            getItemStack("gtceu:flawless_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_hv")
        )

        circuit(
            "iv",
            30720L,
            675,
            getItemStack("gtceu:advanced_smd_diode", 3),
            getItemStack("gtceu:advanced_smd_capacitor", 3),
            getItemStack("gtceu:advanced_smd_transistor", 3),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 4),
            getItemStack("gtceu:flawless_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_ev")
        )

        circuit(
            "luv",
            122880L,
            855,
            getItemStack("gtceu:advanced_smd_diode", 6),
            getItemStack("gtceu:advanced_smd_capacitor", 6),
            getItemStack("gtceu:advanced_smd_transistor", 6),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 4),
            getItemStack("gtceu:flawless_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_iv")
        )

        circuit(
            "zpm",
            491520L,
            1065,
            getItemStack("kubejs:smd_diode_bioware", 12),
            getItemStack("kubejs:smd_capacitor_bioware", 12),
            getItemStack("kubejs:smd_transistor_bioware", 12),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 8),
            getItemStack("gtceu:exquisite_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_luv")
        )

        circuit(
            "uv",
            1966080L,
            1095,
            getItemStack("kubejs:smd_diode_optical", 12),
            getItemStack("kubejs:smd_capacitor_optical", 12),
            getItemStack("kubejs:smd_transistor_optical", 12),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 8),
            getItemStack("gtceu:exquisite_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_zpm")
        )

        circuit(
            "uhv",
            7864320L,
            1125,
            getItemStack("kubejs:smd_diode_exotic", 12),
            getItemStack("kubejs:smd_capacitor_exotic", 12),
            getItemStack("kubejs:smd_transistor_exotic", 12),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 8),
            getItemStack("gtceu:exquisite_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_uv")
        )

        circuit(
            "uev",
            31457280L,
            1155,
            getItemStack("kubejs:smd_diode_cosmic", 12),
            getItemStack("kubejs:smd_capacitor_cosmic", 12),
            getItemStack("kubejs:smd_transistor_cosmic", 12),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 8),
            getItemStack("gtceu:exquisite_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_uhv")
        )

        circuit(
            "uiv",
            125829120L,
            1185,
            getItemStack("kubejs:smd_diode_supracausal", 12),
            getItemStack("kubejs:smd_capacitor_supracausal", 12),
            getItemStack("kubejs:smd_transistor_supracausal", 12),
            getItemStack("kubejs:imprinted_resonatic_circuit_board", 8),
            getItemStack("gtceu:exquisite_magneto_resonatic_gem"),
            getItemStack("kubejs:circuit_resonatic_uev")
        )
    }
}
