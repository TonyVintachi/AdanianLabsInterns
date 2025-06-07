package com.example.taskmanager.models

data class User(
    val id: Int,
    val username: String,
    val email: String
    // password_hash will be added later when implementing authentication
)
