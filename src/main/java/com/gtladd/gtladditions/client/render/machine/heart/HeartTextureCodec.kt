package com.gtladd.gtladditions.client.render.machine.heart

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.InflaterInputStream

internal object HeartTextureCodec {
    fun read(input: InputStream, width: Int, height: Int, channels: Int): ByteArray {
        val count = width * height
        val expected = 8 + count * channels * Int.SIZE_BYTES
        val stream = DataInputStream(input)
        stream.skipNBytes(6) // GTHT, version, channels
        val delta = stream.readBoolean()
        stream.skipNBytes(1) // reserved
        val planes = InflaterInputStream(stream).use { it.readAllBytes() }
        if (planes.size != expected) throw IOException("Incorrect black-hole texture length")
        val result = ByteBuffer.allocate(expected).order(ByteOrder.LITTLE_ENDIAN)
        result.put(planes, 0, 8)
        val stride = width * channels * Int.SIZE_BYTES

        for (pixel in 0 until count) {
            for (channel in 0 until channels) {
                var value = 0

                for (lane in 0..3) {
                    val offset = 8 + (lane * channels + channel) * count + pixel
                    value = value or ((planes[offset].toInt() and 255) shl (lane * 8))
                }

                val offset = result.position()
                if (delta) {
                    // Int overflow is intentional: prediction operates on raw uint32 bits modulo 2^32.
                    if (pixel % width != 0) value += result.getInt(offset - channels * Int.SIZE_BYTES)
                    if (pixel >= width) {
                        value += result.getInt(offset - stride)
                        if (pixel % width != 0) value -= result.getInt(offset - stride - channels * Int.SIZE_BYTES)
                    }
                }

                result.putInt(value)
            }
        }

        return result.array()
    }
}