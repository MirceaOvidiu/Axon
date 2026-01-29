package com.axon.data.model

// ...existing imports...
data class TaskEntity(
    val id: String,
    val title: String,
    val description: String?,
    val completed: Boolean,
)
