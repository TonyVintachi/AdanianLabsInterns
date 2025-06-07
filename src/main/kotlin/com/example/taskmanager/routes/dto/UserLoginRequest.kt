package com.example.taskmanager.routes.dto

data class UserLoginRequest(
    val username: String,
    val passwordHash: String // Plain text password for now
)
