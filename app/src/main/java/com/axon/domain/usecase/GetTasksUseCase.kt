package com.axon.domain.usecase

import com.axon.domain.model.Task
import com.axon.domain.repository.TaskRepository

class GetTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(): List<Task> {
        // business rules can be applied here (filtering, sorting, caching, etc.)
        return repository.getTasks()
    }
}
