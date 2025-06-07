package com.example.taskmanager.models

data class TaskList(
    val id: Int,
    val name: String,
    val boardId: Int, // Foreign key to Board
    val position: Int
)
