package com.axon.domain.model

data class Session(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val receivedAt: Long = System.currentTimeMillis(),
    val dataPointCount: Int = 0,
)
