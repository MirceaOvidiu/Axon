package com.axon.data.source.health

import androidx.health.services.client.data.Availability
import kotlinx.coroutines.flow.StateFlow

interface HealthServicesDataSource {
    val heartRateBpm: StateFlow<Double>
    val availability: StateFlow<Availability?>

    fun register()

    suspend fun unregister()
}
