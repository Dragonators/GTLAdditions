package com.gtladd.gtladditions.common.machine.multiblock.structure

import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.nio.charset.StandardCharsets

object StructureResourceLoader {
    data class RepeatableAisle(val aisleIndex: Int, val min: Int, val max: Int)

    private const val ROOT_PATH = "assets/gtladditions/structures"
    private const val KIND_FACTORY_PATTERN = 1
    private const val KIND_RING_SET = 2
    private val MAGIC = "GTLASB3".toByteArray(StandardCharsets.US_ASCII)
    private val LEGACY_MAGIC = "GTLASB2".toByteArray(StandardCharsets.US_ASCII)
    private val AXIS_ORDERS = arrayOf(
        intArrayOf(0, 1, 2),
        intArrayOf(0, 2, 1),
        intArrayOf(1, 0, 2),
        intArrayOf(1, 2, 0),
        intArrayOf(2, 0, 1),
        intArrayOf(2, 1, 0)
    )
    private val factoryPatternCache = Object2ReferenceOpenHashMap<String, Array<Array<String>>>(32)
    private val ringSetCache = Object2ReferenceOpenHashMap<String, Array<Array<Array<String>>>>(2)

    fun loadFactoryPattern(
        resourcePath: String,
        expectedId: String,
        vararg directions: RelativeDirection
    ): FactoryBlockPattern = loadFactoryPattern(resourcePath, expectedId, emptyList(), *directions)

    fun loadFactoryPattern(
        resourcePath: String,
        expectedId: String,
        repeatableAisles: List<RepeatableAisle>,
        vararg directions: RelativeDirection
    ): FactoryBlockPattern {
        val pattern = when (directions.size) {
            0 -> FactoryBlockPattern.start()
            3 -> FactoryBlockPattern.start(directions[0], directions[1], directions[2])
            else -> error(resourcePath, expectedId, "expected 0 or 3 relative directions, got ${directions.size}")
        }

        val repeatableByIndex = repeatableAisles.associateBy { it.aisleIndex }
        loadAisles(resourcePath, expectedId).forEachIndexed { aisleIndex, aisle ->
            val repeatable = repeatableByIndex[aisleIndex]
            if (repeatable == null) {
                pattern.aisle(*aisle)
            } else {
                pattern.aisleRepeatable(repeatable.min, repeatable.max, *aisle)
            }
        }
        return pattern
    }

    fun loadRings(resourcePath: String, expectedId: String): Array<Array<Array<String>>> =
        getOrLoad(ringSetCache, resourcePath) {
            readResource(resourcePath, expectedId, KIND_RING_SET) { input, fullPath, optimized ->
                val ringCount = input.readPositiveUnsignedShort(fullPath, expectedId, "ring count")
                Array(ringCount) { index ->
                    input.readStringGrid(fullPath, expectedId, "rings[$index]", optimized)
                }
            }
        }

    fun loadShapeInfoSlices(
        resourcePath: String,
        expectedId: String,
        symbolMapper: (planeIndex: Int, aisleIndex: Int, rowIndex: Int, symbol: Char) -> Char
    ): Array<Array<String>> {
        val aisles = loadAisles(resourcePath, expectedId)
        val rowCount = aisles[0].size
        val planeCount = aisles[0][0].length

        return Array(planeCount) { planeIndex ->
            Array(aisles.size) { aisleIndex ->
                buildString(rowCount) {
                    for (rowIndex in 0 until rowCount) {
                        val symbol = aisles[aisleIndex][rowIndex][planeIndex]
                        append(symbolMapper(planeIndex, aisleIndex, rowIndex, symbol))
                    }
                }
            }
        }
    }

    private fun loadAisles(resourcePath: String, expectedId: String): Array<Array<String>> =
        getOrLoad(factoryPatternCache, resourcePath) {
            readResource(resourcePath, expectedId, KIND_FACTORY_PATTERN) { input, fullPath, optimized ->
                input.readStringGrid(fullPath, expectedId, "aisles", optimized)
            }
        }

    private fun <T : Any> getOrLoad(
        cache: Object2ReferenceOpenHashMap<String, T>,
        resourcePath: String,
        loader: () -> T
    ): T {
        val cached = cache.get(resourcePath)
        if (cached != null) return cached

        val loaded = loader()
        cache.put(resourcePath, loaded)
        return loaded
    }

    private fun <T> readResource(
        resourcePath: String,
        expectedId: String,
        expectedKind: Int,
        reader: (DataInputStream, String, Boolean) -> T
    ): T {
        val fullPath = "$ROOT_PATH/${resourcePath.trimStart('/')}"
        val stream = StructureResourceLoader::class.java.classLoader.getResourceAsStream(fullPath)
            ?: error(fullPath, expectedId, "resource is missing")

        try {
            DataInputStream(BufferedInputStream(stream)).use { input ->
                val magic = ByteArray(MAGIC.size)
                input.readFully(magic)
                val optimized = magic.contentEquals(MAGIC)
                if (!optimized && !magic.contentEquals(LEGACY_MAGIC)) {
                    error(fullPath, expectedId, "invalid binary magic")
                }

                val kind = input.readUnsignedByte()
                if (kind != expectedKind) {
                    error(fullPath, expectedId, "expected kind $expectedKind, got $kind")
                }

                val id = input.readUtf8String(fullPath, expectedId, "id")
                if (id != expectedId) {
                    error(fullPath, expectedId, "expected id '$expectedId', got '$id'")
                }

                val result = reader(input, fullPath, optimized)
                if (input.read() != -1) {
                    error(fullPath, expectedId, "trailing bytes after structure payload")
                }
                return result
            }
        } catch (exception: IllegalStateException) {
            throw exception
        } catch (exception: EOFException) {
            throw IllegalStateException("Invalid structure resource $fullPath for id '$expectedId': truncated binary resource", exception)
        } catch (exception: Exception) {
            throw IllegalStateException("Invalid structure resource $fullPath for id '$expectedId': failed to read binary resource", exception)
        }
    }

    private fun DataInputStream.readStringGrid(
        resourcePath: String,
        expectedId: String,
        fieldName: String,
        optimized: Boolean
    ): Array<Array<String>> {
        val axisOrder = if (optimized) readUnsignedByte() else 0
        if (axisOrder >= AXIS_ORDERS.size) {
            error(resourcePath, expectedId, "$fieldName has invalid axis order $axisOrder")
        }
        val aisleCount = readPositiveUnsignedShort(resourcePath, expectedId, "$fieldName aisle count")
        val rowCount = readPositiveUnsignedShort(resourcePath, expectedId, "$fieldName row count")
        val width = readPositiveUnsignedShort(resourcePath, expectedId, "$fieldName row width")
        val dictionarySize = readPositiveUnsignedShort(resourcePath, expectedId, "$fieldName dictionary size")
        if (dictionarySize.toLong() > aisleCount.toLong() * rowCount) {
            error(resourcePath, expectedId, "$fieldName dictionary size exceeds row count")
        }
        val dictionary = Array(dictionarySize) { index ->
            readAsciiRow(width, resourcePath, expectedId, "$fieldName dictionary row $index")
        }

        val narrowIndexes = optimized && dictionarySize <= 256
        val stored = Array(aisleCount) { aisleIndex ->
            Array(rowCount) { rowIndex ->
                val index = if (narrowIndexes) readUnsignedByte() else readUnsignedShort()
                if (index >= dictionarySize) {
                    error(resourcePath, expectedId, "$fieldName[$aisleIndex][$rowIndex] index $index exceeds dictionary size $dictionarySize")
                }
                dictionary[index]
            }
        }
        if (axisOrder == 0) return stored
        if (axisOrder == 2) {
            return Array(rowCount) { aisleIndex -> Array(aisleCount) { rowIndex -> stored[rowIndex][aisleIndex] } }
        }

        val order = AXIS_ORDERS[axisOrder]
        val dimensions = IntArray(3)
        dimensions[order[0]] = aisleCount
        dimensions[order[1]] = rowCount
        dimensions[order[2]] = width
        // Intern only within this grid to retain the original loader's shared rows.
        val rows = HashMap<String, String>()
        val coordinates = IntArray(3)
        return Array(dimensions[0]) { aisleIndex ->
            coordinates[0] = aisleIndex
            Array(dimensions[1]) { rowIndex ->
                coordinates[1] = rowIndex
                val row = buildString(dimensions[2]) {
                    for (columnIndex in 0 until dimensions[2]) {
                        coordinates[2] = columnIndex
                        append(stored[coordinates[order[0]]][coordinates[order[1]]][coordinates[order[2]]])
                    }
                }
                rows.getOrPut(row) { row }
            }
        }
    }

    private fun DataInputStream.readAsciiRow(
        width: Int,
        resourcePath: String,
        expectedId: String,
        fieldName: String
    ): String {
        val bytes = ByteArray(width)
        readFully(bytes)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            if (value < 0x20 || value > 0x7E) {
                error(resourcePath, expectedId, "$fieldName contains non-printable byte $value")
            }
        }
        return String(bytes, StandardCharsets.US_ASCII)
    }

    private fun DataInputStream.readUtf8String(
        resourcePath: String,
        expectedId: String,
        fieldName: String
    ): String {
        val length = readPositiveUnsignedShort(resourcePath, expectedId, "$fieldName length")
        val bytes = ByteArray(length)
        readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun DataInputStream.readPositiveUnsignedShort(
        resourcePath: String,
        expectedId: String,
        fieldName: String
    ): Int {
        val value = readUnsignedShort()
        if (value == 0) {
            error(resourcePath, expectedId, "$fieldName must be non-zero")
        }
        return value
    }

    private fun error(resourcePath: String, expectedId: String, message: String): Nothing = throw IllegalStateException("Invalid structure resource $resourcePath for id '$expectedId': $message")
}