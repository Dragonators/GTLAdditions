package com.gtladd.gtladditions.common.data.multiblockMachine

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gregtechceu.gtceu.common.data.GTBlocks
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gtladd.gtladditions.api.machine.GTLAddPartAbility
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Function

@SuppressWarnings("DuplicatedCode")
object MultiRecipeMultiBlocks {

    val ADVANCED_INTEGRATED_ORE_PROCESSOR = Function { definition: MultiblockMachineDefinition ->
        FactoryBlockPattern.start()
            .aisle("    AAAAAAAAAA ", "    AAAGGGGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "   AAAAGHHGAAAA", "     AAGHHGAA  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("   AAAAAAAAAAAA", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "    A        A ", "   AAA      AAA", "     A      A  ", "      AGGGGA   ")
            .aisle("IIIAAAAAAAAAAAA", "IIIBADEE  EEDAB", "IIIBADEE  EEDAB", "IIIBADEE  EEDAB", "IIIBADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("IIIAAAAAAAAAAAA", "IDIBADEE  EEDAB", "IDIBADEE  EEDAB", "IDIBADEE  EEDAB", "IIIBADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   BADEE  EEDAB", "   AAAFF  FFAAA", "     AFF  FFA  ", "      AGCCGA   ")
            .aisle("III AAAAAAAAAA ", "III AAAGGGGAAA ", "I~I AAAGHHGAAA ", "III AAAGHHGAAA ", "III AAAGHHGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "    AAAGHHGAAA ", "   AAAAGHHGAAAA", "     AAGHHGAA  ", "      AGGGGA   ")
            .where("~", Predicates.controller(Predicates.blocks(definition.get())))
            .where("A", Predicates.blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get()))
            .where("B", Predicates.blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.HSSS)))
            .where("C", Predicates.blocks(Registries.getBlock("kubejs:restraint_device")))
            .where("D", Predicates.blocks(GTBlocks.CASING_TUNGSTENSTEEL_PIPE.get()))
            .where("E", Predicates.blocks(GTBlocks.CASING_TUNGSTENSTEEL_GEARBOX.get()))
            .where("F", Predicates.blocks(GTBlocks.CASING_GRATE.get()))
            .where("G", Predicates.blocks(GTBlocks.CASING_HSSE_STURDY.get()))
            .where("H", Predicates.blocks(GTLBlocks.HSSS_REINFORCED_BOROSILICATE_GLASS.get()))
            .where("I", Predicates.blocks(GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get())
                .or(Predicates.abilities(PartAbility.INPUT_LASER))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
            .where(" ", Predicates.any())
            .build()
    }
}