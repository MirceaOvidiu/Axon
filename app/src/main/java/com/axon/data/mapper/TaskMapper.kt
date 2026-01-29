package com.axon.data.mapper

import com.axon.data.model.TaskEntity
import com.axon.domain.model.Task

object TaskMapper {
    fun fromEntity(e: TaskEntity): Task {
        return Task(id = e.id, title = e.title, description = e.description, completed = e.completed)
    }

    fun toEntity(t: Task): TaskEntity {
        return TaskEntity(id = t.id, title = t.title, description = t.description, completed = t.completed)
    }
}
