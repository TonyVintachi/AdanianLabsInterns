package com.example.taskmanager.routes.dto

// Dates are expected to be ISO Local Date Time Strings e.g., "2024-03-10T10:00:00"
data class EventUpdateRequest(
    val title: String?,
    val description: String?,
    val startTime: String?, // ISO LocalDateTime string
    val endTime: String?,   // ISO LocalDateTime string
    val location: String?,
    val virtualMeetingLink: String?,
    val teamId: Int?,
    val clearDescription: Boolean? = false,
    val clearLocation: Boolean? = false,
    val clearVirtualMeetingLink: Boolean? = false,
    val clearTeamId: Boolean? = false
    // Note: googleCalendarEventId is not directly updatable by client via this DTO.
    // It's handled internally by sync logic.
)
