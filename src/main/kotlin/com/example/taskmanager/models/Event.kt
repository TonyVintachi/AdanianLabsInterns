package com.example.taskmanager.models

import java.time.LocalDateTime

data class Event(
    val id: Int, // Local DB ID
    var googleCalendarEventId: String?, // ID from Google Calendar
    var title: String,
    var description: String?,
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
    var location: String?,
    var virtualMeetingLink: String?,
    val teamId: Int?, // Optional: if the event is for a whole team
    val creatorId: Int // User ID from our system
)
