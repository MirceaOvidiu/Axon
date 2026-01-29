package com.axon.di

import com.axon.data.repository.TaskRepositoryImpl
import com.axon.data.source.LocalDataSource
import com.axon.domain.repository.TaskRepository
import com.axon.domain.usecase.GetTasksUseCase

// ...existing DI setup (Hilt/Koin/manual)...
// Example (pseudo): bind TaskRepository to TaskRepositoryImpl and provide GetTasksUseCase
// In Hilt you'd annotate @Module @InstallIn(SingletonComponent::class) and @Provides functions.
