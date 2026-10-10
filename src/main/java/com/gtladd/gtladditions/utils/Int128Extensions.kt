package com.gtladd.gtladditions.utils

import org.gtlcore.gtlcore.utils.datastructure.Int128

fun Int128.longValue(): Long = (this as Number).toLong()

operator fun Int128.compareTo(other: Long): Int {
    val otherHigh = if (other < 0L) -1L else 0L
    val highComparison = high.compareTo(otherHigh)

    return if (highComparison != 0) highComparison else java.lang.Long.compareUnsigned(low, other)
}

operator fun Int128.compareTo(other: Int): Int = compareTo(other.toLong())

fun Int128.saturatedAdd(other: Int128): Int128 {
    val negative = isNegative
    val otherNegative = other.isNegative

    add(other)
    if (negative == otherNegative && isNegative != negative) {
        set(if (negative) Long.MIN_VALUE else Long.MAX_VALUE, if (negative) 0L else -1L)
    }

    return this
}

fun Int128.saturatedAdd(other: Long): Int128 {
    val negative = isNegative

    add(other)
    if (negative == (other < 0L) && isNegative != negative) {
        set(if (negative) Long.MIN_VALUE else Long.MAX_VALUE, if (negative) 0L else -1L)
    }

    return this
}