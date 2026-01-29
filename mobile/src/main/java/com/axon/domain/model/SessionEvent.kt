package com.axon.domain.model

data class SessionEvent(
    val sessionId: Long,
    val dataPointCount: Int,
)
