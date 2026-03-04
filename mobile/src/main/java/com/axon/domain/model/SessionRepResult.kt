package com.axon.domain.model

data class SessionRepResult(
    val rep: Int = 0,
    val startIdx: Int = 0,
    val endIdx: Int = 0,
    val duration: Double = 0.0,
    val score: Double = 0.0
)
