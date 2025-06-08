package com.example.taskmanager.routes.dto

// createdAt, updatedAt, dueDate will be ISO Local Date Time Strings
data class TaskResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val taskListId: Int,
    val creatorId: Int,
    val assigneeId: Int?,
    val createdAt: String,
    val updatedAt: String,
    val dueDate: String?,
    val position: Int
)
