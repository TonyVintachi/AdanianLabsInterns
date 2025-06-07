package com.example.taskmanager.routes.dto

data class UserCreateRequest(
    val username: String,
    val email: String,
    val passwordHash: String // Plain text password for now, will be hashed by the service/logic later
)
