package com.axon.domain.repository

import com.axon.domain.model.Task

interface TaskRepository {
    suspend fun getTasks(): List<Task>

    suspend fun getTask(id: String): Task?

    suspend fun saveTask(task: Task)

    suspend fun removeTask(id: String)
}
