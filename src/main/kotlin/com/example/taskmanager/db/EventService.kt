package com.example.taskmanager.db

import com.example.taskmanager.models.Event
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.example.taskmanager.db.DatabaseFactory.dbQuery
import java.time.LocalDateTime

class EventService {

    suspend fun init() {
        dbQuery {
            SchemaUtils.create(EventTable)
        }
    }

    private fun rowToEvent(row: ResultRow): Event = Event(
        id = row[EventTable.id],
        googleCalendarEventId = row[EventTable.googleCalendarEventId],
        title = row[EventTable.title],
        description = row[EventTable.description],
        startTime = row[EventTable.startTime],
        endTime = row[EventTable.endTime],
        location = row[EventTable.location],
        virtualMeetingLink = row[EventTable.virtualMeetingLink],
        teamId = row[EventTable.teamId],
        creatorId = row[EventTable.creatorId]
    )

    suspend fun createEvent(
        title: String, description: String?, startTime: LocalDateTime, endTime: LocalDateTime,
        location: String?, virtualMeetingLink: String?, teamId: Int?, creatorId: Int,
        googleCalendarEventId: String? = null
    ): Event? = dbQuery {
        val insertStatement = EventTable.insert {
            it[EventTable.title] = title
            it[EventTable.description] = description
            it[EventTable.startTime] = startTime
            it[EventTable.endTime] = endTime
            it[EventTable.location] = location
            it[EventTable.virtualMeetingLink] = virtualMeetingLink
            it[EventTable.teamId] = teamId
            it[EventTable.creatorId] = creatorId
            it[EventTable.googleCalendarEventId] = googleCalendarEventId
        }
        insertStatement.resultedValues?.singleOrNull()?.let { rowToEvent(it) }
    }

    suspend fun getEventById(id: Int): Event? = dbQuery {
        EventTable.select { EventTable.id eq id }
            .map { rowToEvent(it) }
            .singleOrNull()
    }

    suspend fun getEventsByCreator(creatorId: Int): List<Event> = dbQuery {
        EventTable.select { EventTable.creatorId eq creatorId }
            .orderBy(EventTable.startTime)
            .map { rowToEvent(it) }
    }

    suspend fun getEventsByTeam(teamId: Int): List<Event> = dbQuery {
        EventTable.select { EventTable.teamId eq teamId }
            .orderBy(EventTable.startTime)
            .map { rowToEvent(it) }
    }

    // For getEventsByUser (creator or attendee - attendee not modeled yet)
    // suspend fun getEventsForUser(userId: Int): List<Event> = dbQuery {
    //     EventTable.select { (EventTable.creatorId eq userId) /* or attendee logic */ }
    //         .orderBy(EventTable.startTime)
    //         .map { rowToEvent(it) }
    // }


    suspend fun updateEvent(
        id: Int, title: String?, description: String?, startTime: LocalDateTime?, endTime: LocalDateTime?,
        location: String?, virtualMeetingLink: String?, teamId: Int?, googleCalendarEventId: String?,
        clearGoogleCalendarEventId: Boolean = false, clearTeamId: Boolean = false,
        clearDescription: Boolean = false, clearLocation: Boolean = false, clearVirtualMeetingLink: Boolean = false
    ): Boolean = dbQuery {
        EventTable.update({ EventTable.id eq id }) {
            title?.let { nonNullTitle -> it[EventTable.title] = nonNullTitle }

            if (clearDescription) it[EventTable.description] = null
            else description?.let { nonNullDesc -> it[EventTable.description] = nonNullDesc }

            startTime?.let { nonNullStartTime -> it[EventTable.startTime] = nonNullStartTime }
            endTime?.let { nonNullEndTime -> it[EventTable.endTime] = nonNullEndTime }

            if (clearLocation) it[EventTable.location] = null
            else location?.let { nonNullLocation -> it[EventTable.location] = nonNullLocation }

            if (clearVirtualMeetingLink) it[EventTable.virtualMeetingLink] = null
            else virtualMeetingLink?.let { nonNullLink -> it[EventTable.virtualMeetingLink] = nonNullLink }

            if (clearTeamId) it[EventTable.teamId] = null
            else teamId?.let { nonNullTeamId -> it[EventTable.teamId] = nonNullTeamId }

            if (clearGoogleCalendarEventId) it[EventTable.googleCalendarEventId] = null
            else googleCalendarEventId?.let { nonNullGCalId -> it[EventTable.googleCalendarEventId] = nonNullGCalId }
        } > 0
    }

    suspend fun updateEventGoogleId(localEventId: Int, googleCalendarEventId: String): Boolean = dbQuery {
        EventTable.update({ EventTable.id eq localEventId }) {
            it[EventTable.googleCalendarEventId] = googleCalendarEventId
        } > 0
    }

    suspend fun deleteEvent(id: Int): Boolean = dbQuery {
        EventTable.deleteWhere { EventTable.id eq id } > 0
    }
}
