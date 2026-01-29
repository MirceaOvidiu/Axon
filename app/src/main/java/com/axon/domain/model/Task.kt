package com.axon.domain.model

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val completed: Boolean
)
