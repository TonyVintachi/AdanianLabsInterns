package com.example.taskmanager.models

import java.time.LocalDateTime

data class UserCalendarSyncInfo(
    val userId: Int, // Foreign key to User, and Primary Key for this model
    var accessToken: String,
    var refreshToken: String?,
    var tokenExpiry: LocalDateTime? // Indicates when the access token expires
)
