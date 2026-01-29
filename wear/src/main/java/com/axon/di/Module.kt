package com.axon.di

import android.content.Context
import com.axon.data.source.health.HealthServicesDataSource
import com.axon.data.source.health.HealthServicesDataSourceAdapter
import com.axon.data.source.manager.HealthServicesManager
import com.axon.data.source.sensors.GyroDataSource
import com.axon.domain.repository.recording.RecordingRepository
import com.axon.domain.repository.sync.SyncRepository
import com.axon.domain.repository.sync.SyncRepositoryImplementation
import com.axon.domain.usecase.RecordingUseCase
import com.axon.domain.usecase.SyncUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {
    @Provides
    @Singleton
    fun provideGyroDataSource(
        @ApplicationContext context: Context,
    ): GyroDataSource = GyroDataSource(context)

    @Provides
    @Singleton
    fun provideHealthServicesManager(
        @ApplicationContext context: Context,
    ): HealthServicesManager = HealthServicesManager(context)

    @Provides
    @Singleton
    fun provideHealthServicesDataSource(manager: HealthServicesManager): HealthServicesDataSource = HealthServicesDataSourceAdapter(manager)

    @Provides
    @Singleton
    fun provideSyncRepository(
        @ApplicationContext context: Context,
    ): SyncRepository = SyncRepositoryImplementation(context)

    @Provides
    @Singleton
    fun provideRecordingRepository(
        @ApplicationContext context: Context,
    ): RecordingRepository = RecordingRepository(context)

    @Provides
    @Singleton
    fun provideRecordingUseCase(
        recordingRepository: RecordingRepository,
        gyroDataSource: GyroDataSource,
        healthDataSource: HealthServicesDataSource,
    ): RecordingUseCase = RecordingUseCase(recordingRepository, gyroDataSource, healthDataSource)

    @Provides
    @Singleton
    fun provideSyncUseCase(
        recordingRepository: RecordingRepository,
        syncRepository: SyncRepository,
        gyroDataSource: GyroDataSource,
        healthDataSource: HealthServicesDataSource,
    ): SyncUseCase = SyncUseCase(recordingRepository, syncRepository, gyroDataSource, healthDataSource)
}
