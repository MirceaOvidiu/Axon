package com.axon.data.remote.model

data class SessionRepResultFirestore(
    val rep: Int = 0,
    val start_idx: Int = 0,
    val end_idx: Int = 0,
    val duration: Double = 0.0,
    val score: Double = 0.0
)
