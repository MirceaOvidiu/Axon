package com.axon.data.repository

import com.axon.data.mapper.TaskMapper
import com.axon.data.source.LocalDataSource
import com.axon.domain.model.Task
import com.axon.domain.repository.TaskRepository

class TaskRepositoryImpl(
    private val local: LocalDataSource
    // add remote data source or network client as needed
) : TaskRepository {
    override suspend fun getTasks(): List<Task> {
        val entities = local.fetchAll()
        return entities.map { TaskMapper.fromEntity(it) }
    }

    override suspend fun getTask(id: String): Task? {
        val e = local.fetchById(id) ?: return null
        return TaskMapper.fromEntity(e)
    }

    override suspend fun saveTask(task: Task) {
        local.insert(TaskMapper.toEntity(task))
    }

    override suspend fun removeTask(id: String) {
        local.delete(id)
    }
}
