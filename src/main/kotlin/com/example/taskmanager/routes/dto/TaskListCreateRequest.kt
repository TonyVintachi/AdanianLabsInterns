package com.example.taskmanager.routes.dto

data class TaskListCreateRequest(
    val name: String,
    val boardId: Int
)
