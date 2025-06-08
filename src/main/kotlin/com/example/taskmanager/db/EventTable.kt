package com.example.taskmanager.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
// Assuming UserTable and TeamTable are already in this package or correctly imported
// For foreign keys:
// import com.example.taskmanager.db.UserTable (if in different file but same package, direct use is fine)
// import com.example.taskmanager.db.TeamTable (if in different file but same package, direct use is fine)

object EventTable : Table("events") {
    val id = integer("id").autoIncrement()
    val googleCalendarEventId = varchar("google_calendar_event_id", 255).nullable()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val location = varchar("location", 255).nullable()
    val virtualMeetingLink = varchar("virtual_meeting_link", 1024).nullable()
    val teamId = integer("team_id").references(TeamTable.id).nullable()
    val creatorId = integer("creator_id").references(UserTable.id)

    override val primaryKey = PrimaryKey(id)
}
