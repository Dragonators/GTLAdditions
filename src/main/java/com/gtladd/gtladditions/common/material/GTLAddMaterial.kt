package com.gtladd.gtladditions.common.material

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty
import com.gregtechceu.gtceu.api.fluids.FluidBuilder
import com.gregtechceu.gtceu.api.fluids.FluidState
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.common.data.GTMaterials.Gallium
import com.gregtechceu.gtceu.common.data.GTMaterials.Iridium
import com.gregtechceu.gtceu.common.data.GTMaterials.Oxygen
import com.gregtechceu.gtceu.common.data.GTMaterials.Ruthenium
import com.gtladd.gtladditions.GTLAdditions
import org.gtlcore.gtlcore.common.data.GTLMaterials.*

object GTLAddMaterial {
    val GALLIUM_OXIDE: Material = Material.Builder(GTLAdditions.id("gallium_oxide")).dust().color(15720677)
        .components(Gallium, 2, Oxygen, 3).iconSet(MaterialIconSet.DULL).buildAndRegister()
    val AMMONIUM_GALIUM_SULFATE: Material =
        Material.Builder(GTLAdditions.id("ammonium_gallium_sulfate")).dust().color(0xFFF6E9)
            .iconSet(MaterialIconSet.DULL).buildAndRegister().setFormula("Ga(NH₄)(SO₄)₂")
    val MINING_ESSENCE: Material =
        Material.Builder(GTLAdditions.id("mining_essence"))
            .liquid(FluidBuilder().block())
            .color(0x835141)
            .buildAndRegister()
    val TREASURES_ESSENCE: Material =
        Material.Builder(GTLAdditions.id("treasures_essence"))
            .liquid(FluidBuilder().block())
            .color(0x9C24FF)
            .buildAndRegister()
    val CRYSTALLINE_PROTOPLASM: Material =
        Material.Builder(GTLAdditions.id("crystalline_protoplasm"))
            .liquid(FluidBuilder().block())
            .color(0x2ECF03)
            .buildAndRegister()
    val MOLTEN_RURIDIT: Material =
        Material.Builder(GTLAdditions.id("ruridit"))
            .fluid(FluidStorageKeys.MOLTEN, FluidBuilder().state(FluidState.LIQUID).temperature(2300))
            .color(0x86b3b7)
            .components(Ruthenium, 2, Iridium, 1)
            .flags(MaterialFlags.DISABLE_DECOMPOSITION)
            .iconSet(MaterialIconSet.METALLIC)
            .buildAndRegister()
    val LIQUID_RURIDIT: Material =
        Material.Builder(GTLAdditions.id("liquid_ruridit"))
            .fluid(FluidStorageKeys.LIQUID, FluidBuilder().state(FluidState.LIQUID).temperature(2300))
            .components(Ruthenium, 2, Iridium, 1)
            .flags(MaterialFlags.DISABLE_DECOMPOSITION)
            .color(0x86b3b7)
            .buildAndRegister()
    val CREON: Material =
        Material.Builder(GTLAdditions.id("creon"))
            .ingot()
            .liquid(1000)
            .plasma(10000)
            .flags(
                MaterialFlags.GENERATE_PLATE,
                MaterialFlags.GENERATE_GEAR,
                MaterialFlags.GENERATE_SMALL_GEAR,
                MaterialFlags.GENERATE_DENSE,
                MaterialFlags.GENERATE_LONG_ROD
            )
            .element(GTLAddElements.CREON)
            .color(0x460046)
            .iconSet(MaterialIconSet.METALLIC)
            .buildAndRegister()
    val MELLION: Material =
        Material.Builder(GTLAdditions.id("mellion"))
            .ingot()
            .liquid(1000)
            .blastTemp(14000)
            .flags(
                MaterialFlags.GENERATE_FRAME,
                MaterialFlags.GENERATE_SMALL_GEAR,
                MaterialFlags.GENERATE_GEAR,
                MaterialFlags.GENERATE_DENSE,
                MaterialFlags.GENERATE_LONG_ROD,
                MaterialFlags.DISABLE_DECOMPOSITION
            )
            .color(0x3c0505)
            .iconSet(MaterialIconSet.SHINY)
            .buildAndRegister().setFormula("Tn₁₁Or₈Rb₁₁?₇?₁₃?₁₃")
    val PROTO_HALKONITE_BASE: Material =
        Material.Builder(GTLAdditions.id("proto_halkonite_base"))
            .fluid(FluidStorageKeys.MOLTEN, FluidBuilder().state(FluidState.LIQUID).customStill())
            .flags(MaterialFlags.DISABLE_DECOMPOSITION)
            .color(0x01943c)
            .components(TranscendentMetal, 2, Tairitsu, 2, Tartarite, 2, TitanPrecisionSteel, 1, Eternity, 1)
            .iconSet(MaterialIconSet.METALLIC)
            .buildAndRegister().setFormula("(TsЖ)₂(W₈Nq*₇(SiO₂)₄C₄V₃SpPu)₂Tt₂((CW)₇Ti₃)₃⊕☄⚛If*")
    val PROTO_HALKONITE: Material =
        Material.Builder(GTLAdditions.id("proto_halkonite"))
            .ingot()
            .fluid(FluidStorageKeys.LIQUID, FluidState.LIQUID)
            .blastTemp(48000, BlastProperty.GasTier.HIGHEST, GTValues.VA[GTValues.OpV], 680)
            .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_DENSE, MaterialFlags.GENERATE_LONG_ROD)
            .color(0x01943c)
            .iconSet(MaterialIconSet.METALLIC)
            .buildAndRegister().setFormula("(TsЖ)₂(W₈Nq*₇(SiO₂)₄C₄V₃SpPu)₂Tt₂((CW)₇Ti₃)₃⊕☄⚛If*")
    val PHONON_MEDIUM: Material =
        Material.Builder(GTLAdditions.id("phonon_medium"))
            .fluid(FluidStorageKeys.LIQUID, FluidBuilder().state(FluidState.LIQUID).temperature(500).customStill())
            .color(0xffffff)
            .iconSet(MaterialIconSet.DULL)
            .buildAndRegister()
            .setFormula("((Si₅O₁₀Fe)₃(Bi₂Te₃)₄ZrO₂Fe₅₀C)₅Og*Pr₁₅((C₁₄Os₁₁O₇Ag₃SpH₂O)₄?₁₀(Fs⚶)₆(⌘☯☯⌘)₅)₆〄₄")
    val PHONON_CRYSTAL_SOLUTION: Material =
        Material.Builder(GTLAdditions.id("phonon_crystal_solution"))
            .fluid(FluidStorageKeys.LIQUID, FluidBuilder().state(FluidState.LIQUID).temperature(500).customStill())
            .color(0xffffff)
            .iconSet(MaterialIconSet.DULL)
            .buildAndRegister().setFormula("〄")

    val STAR_GATE_CRYSTAL_SLURRY: Material =
        Material.Builder(GTLAdditions.id("star_gate_crystal_slurry"))
            .fluid(FluidStorageKeys.LIQUID, FluidBuilder().state(FluidState.LIQUID).temperature(295).customStill())
            .color(0xffffff)
            .iconSet(MaterialIconSet.DULL)
            .buildAndRegister().setFormula("✟✵✟")
    fun init() {
    }
}