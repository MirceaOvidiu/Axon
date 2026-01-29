package com.axon.domain.model

data class SessionStats(
    val averageHeartRate: Double?,
    val maxHeartRate: Double?,
    val minHeartRate: Double?,
    val duration: Long, // in milliseconds
    val dataPointCount: Int,
)
