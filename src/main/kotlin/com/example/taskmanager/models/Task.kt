package com.example.taskmanager.models

import java.time.LocalDateTime

data class Task(
    val id: Int,
    val title: String,
    val description: String?,
    val taskListId: Int, // Foreign key to TaskList
    val creatorId: Int, // Foreign key to User
    val assigneeId: Int?, // Foreign key to User (nullable)
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dueDate: LocalDateTime?,
    val position: Int
)
