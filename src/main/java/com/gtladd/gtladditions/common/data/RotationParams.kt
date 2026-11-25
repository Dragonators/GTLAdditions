package com.gtladd.gtladditions.common.data

import org.joml.Vector3f

data class RotationParams(val axis: Vector3f, val speed: Float, val offset: Float) {
    fun getAngle(tick: Float): Float = getAngle(tick.toDouble())

    fun getAngle(tick: Double): Float = ((offset + tick * speed) % 360.0).toFloat()
}
