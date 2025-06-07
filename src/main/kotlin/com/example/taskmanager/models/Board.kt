package com.example.taskmanager.models

data class Board(
    val id: Int,
    val name: String,
    val teamId: Int // Foreign key to Team
)
