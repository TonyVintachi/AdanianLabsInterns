package com.example.taskmanager.routes.dto

// dueDate will be an ISO Local Date Time String e.g., "2023-10-26T10:00:00"
data class TaskUpdateRequest(
    val title: String?,
    val description: String?,
    val taskListId: Int?,
    val assigneeId: Int?,
    val dueDate: String?, // Nullable to allow clearing, or new value
    val position: Int?,
    val clearDescription: Boolean? = false, // Explicit flags for clearing nullable fields
    val clearAssigneeId: Boolean? = false,
    val clearDueDate: Boolean? = false
)
