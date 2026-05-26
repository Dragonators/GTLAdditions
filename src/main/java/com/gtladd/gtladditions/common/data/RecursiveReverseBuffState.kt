package com.gtladd.gtladditions.common.data

data class RecursiveReverseBuffState(
    val commonActive: Boolean = false,
    val boostEngineInstalled: Boolean = false,
    val boostEngineOptimal: Boolean = false,
    val boostEngineOverheated: Boolean = false,
    val starRitualGateActive: Boolean = false,
    val catalyticCascadeActive: Boolean = false,
    val catalyticCascadeEuActive: Boolean = false,
    val magnetorheologicalConvergenceActive: Boolean = false,
    val spacetimeStasisActive: Boolean = false,
    val outputMultiplier: Double = 1.0,
    val euMultiplier: Double = 1.0,
    val diagnostics: List<String> = emptyList()
)