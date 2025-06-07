package com.example.taskmanager.routes.dto

data class UserUpdateRequest(
    val email: String?,
    val currentPasswordHash: String?, // Current password, for verification before update
    val newPasswordHash: String?      // New password
)
