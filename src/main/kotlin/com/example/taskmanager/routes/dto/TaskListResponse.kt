package com.example.taskmanager.routes.dto

data class TaskListResponse(
    val id: Int,
    val name: String,
    val boardId: Int,
    val position: Int
)
