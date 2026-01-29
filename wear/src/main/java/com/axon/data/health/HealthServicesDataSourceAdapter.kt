package com.axon.data.health

import androidx.health.services.client.data.Availability
import com.axon.data.manager.HealthServicesManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class HealthServicesDataSourceAdapter
    @Inject
    constructor(
        private val manager: HealthServicesManager,
    ) : HealthServicesDataSource {
        override val heartRateBpm: StateFlow<Double> = manager.heartRateBpm
        override val availability: StateFlow<Availability?> = manager.availability

        override fun register() = manager.registerForHeartRateData()

        override suspend fun unregister() = manager.unregisterForHeartRateData()
    }
