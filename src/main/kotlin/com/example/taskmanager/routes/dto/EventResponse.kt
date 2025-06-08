package com.example.taskmanager.routes.dto

// Dates are ISO Local Date Time Strings e.g., "2024-03-10T10:00:00"
data class EventResponse(
    val id: Int, // Local DB ID
    val googleCalendarEventId: String?,
    val title: String,
    val description: String?,
    val startTime: String, // ISO LocalDateTime string
    val endTime: String,   // ISO LocalDateTime string
    val location: String?,
    val virtualMeetingLink: String?,
    val teamId: Int?,
    val creatorId: Int
)
