package com.axon.data.source

import com.axon.data.model.TaskEntity

interface LocalDataSource {
    suspend fun fetchAll(): List<TaskEntity>
    suspend fun fetchById(id: String): TaskEntity?
    suspend fun insert(entity: TaskEntity)

    suspend fun delete(id: String)
}

// ...existing code...
// Example: Room DAO implementation would live in the same data package and implement LocalDataSource.
// Keep Room annotations and Android imports confined to this package.
