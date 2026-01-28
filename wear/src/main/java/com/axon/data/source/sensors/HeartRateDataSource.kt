package com.axon.data.source.sensors

import androidx.health.services.client.data.Availability
import com.axon.data.source.manager.HealthServicesManager
import kotlinx.coroutines.flow.StateFlow

interface HeartRateDataSource {
    val heartRate: StateFlow<Double>
    val availability: StateFlow<Availability?>

    fun register()

    suspend fun unregister()
}

class HeartRateDataSourceAdapter(
    private val manager: HealthServicesManager,
) : HeartRateDataSource {
    override val heartRate: StateFlow<Double> = manager.heartRateBpm
    override val availability: StateFlow<Availability?> = manager.availability

    override fun register() = manager.registerForHeartRateData()

    override suspend fun unregister() = manager.unregisterForHeartRateData()
}
