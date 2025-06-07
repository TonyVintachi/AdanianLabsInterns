package com.example.taskmanager.routes.dto

data class TeamCreateRequest(
    val name: String,
    val creatorId: Int // Will be derived from auth context in a later stage
)
