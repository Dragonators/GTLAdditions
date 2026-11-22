package com.gtladd.gtladditions.common.machine.multiblock.structure.layout

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.pattern.BlockPattern
import com.gregtechceu.gtceu.api.pattern.Predicates.*
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate
import com.gtladd.gtladditions.api.machine.GTLAddPartAbility
import com.gtladd.gtladditions.api.pattern.StructureTierLayout
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureResourceLoader
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries

object ExoticChemicalBlockLayout {

    private const val ID = "exotic_chemical_complex"
    private const val FILE = "multiblock/$ID.bin"

    private data class Role(
        val structureTier: Int,
        val material: String
    ) {
        val owner: Int? = material.substringAfter("port:", "").toIntOrNull()
    }

    // ========================================
    // PALETTE & STRUCTURE DATA
    // ========================================

    private val roles: Map<Char, Role> by lazy {
        requireNotNull(javaClass.getResourceAsStream("/assets/gtladditions/palettes/$ID.palette"))
            .bufferedReader()
            .useLines { lines ->
                lines.filter { it.isNotBlank() && !it.startsWith('#') }
                    .associate { line ->
                        val (symbol, structureTier, material) = line.split(' ')
                        symbol.single() to Role(structureTier.toInt(), material)
                    }
            }
    }

    private val aisles: Array<Array<String>> by lazy {
        StructureResourceLoader.loadAisles(FILE, ID)
    }

    val tiers: StructureTierLayout = StructureTierLayout(
        1,
        3,
        { MultiBlockMachine.EXOTIC_CHEMICAL_COMPLEX.patternFactory.get() }
    ) { a, r, c ->
        roles.getValue(aisles[a][r][c]).structureTier
    }

    val portOwners: Map<Int, Int> by lazy {
        buildMap {
            for (cell in tiers.cells(0)) {
                val symbol = aisles[StructureTierLayout.aisle(cell)][StructureTierLayout.row(cell)][StructureTierLayout.column(cell)]
                roles.getValue(symbol).owner?.let { put(cell, it) }
            }
        }
    }

    // ========================================
    // BLOCK PATTERN
    // ========================================

    fun pattern(definition: MultiblockMachineDefinition): BlockPattern {
        val pattern = StructureResourceLoader.loadFactoryPattern(FILE, ID).where(' ', any())
        val mesh = blocks(GTLAddBlocks.NANOCHIP_CASING.get())
        val io = abilities(
            PartAbility.IMPORT_ITEMS,
            PartAbility.EXPORT_ITEMS,
            PartAbility.IMPORT_FLUIDS,
            PartAbility.EXPORT_FLUIDS
        ).setPreviewCount(0)

        for ((symbol, role) in roles) {
            val predicate = when {
                role.owner != null -> mesh.or(io)
                role.material == "controller" -> controller(blocks(definition.get()))
                role.material == "maintenance" -> abilities(PartAbility.MAINTENANCE).setExactLimit(1)
                role.material == "thread" -> mesh.or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                role.material == "coil" -> heatingCoils()
                role.material == "air" -> air()
                else -> {
                    val key = ResourceLocation(role.material)
                    if (ForgeRegistries.BLOCKS.containsKey(key)) {
                        blocks(ForgeRegistries.BLOCKS.getValue(key)!!)
                    } else {
                        TraceabilityPredicate({ false }, { emptyArray() })
                    }
                }
            }
            pattern.where(symbol, predicate)
        }

        return pattern.build()
    }
}