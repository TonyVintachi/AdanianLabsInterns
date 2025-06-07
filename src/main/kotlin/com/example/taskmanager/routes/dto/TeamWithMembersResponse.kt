package com.example.taskmanager.routes.dto

// UserResponse is already defined in com.example.taskmanager.routes.dto.UserResponse

data class TeamWithMembersResponse(
    val id: Int,
    val name: String,
    val members: List<UserResponse>
)
