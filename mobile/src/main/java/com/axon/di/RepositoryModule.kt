package com.axon.di

import com.axon.data.impl.SessionRepositoryImplementation
import com.axon.data.impl.WearableRepositoryImpl
import com.axon.domain.repository.SessionRepository
import com.axon.domain.repository.WearableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSessionRepository(sessionRepositoryImplementation: SessionRepositoryImplementation): SessionRepository

    @Binds
    @Singleton
    abstract fun bindWearableRepository(wearableRepositoryImpl: WearableRepositoryImpl): WearableRepository
}
