package com.example.taskmanager.db

import com.example.taskmanager.models.User
import com.example.taskmanager.models.Team
import com.example.taskmanager.models.Event // Local Event model
import com.example.taskmanager.utils.DatabaseTestUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.*

class EventServiceTest {

    private val userService = UserService()
    private val teamService = TeamService()
    private val eventService = EventService()

    private lateinit var testUser: User
    private lateinit var testTeam: Team // Optional, for team-associated events

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()
        runBlocking {
            testUser = userService.createUser(User(0, "eventUser-${System.nanoTime()}", "event@test.com"), "password")!!
            testTeam = teamService.createTeam("EventTest Team", testUser.id)!!
        }
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    private fun assertDateTimeAlmostEqual(expected: LocalDateTime?, actual: LocalDateTime?, toleranceSeconds: Long = 1) {
        if (expected == null && actual == null) return
        assertNotNull(expected, "Expected DateTime was null while actual was not (or vice-versa)")
        assertNotNull(actual, "Actual DateTime was null while expected was not (or vice-versa)")
        assertTrue(ChronoUnit.SECONDS.between(expected, actual).let { kotlin.math.abs(it) } <= toleranceSeconds,
            "Expected $expected but got $actual (tolerance ${toleranceSeconds}s)")
    }


    @Test
    fun `createEvent should add a new event and return it`() = runBlocking {
        val now = LocalDateTime.now()
        val createdEvent = eventService.createEvent(
            title = "Test Event",
            description = "Event Description",
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            location = "Test Location",
            virtualMeetingLink = "http://meet.example.com/test",
            teamId = testTeam.id,
            creatorId = testUser.id,
            googleCalendarEventId = "gcal-id-123"
        )

        assertNotNull(createdEvent)
        assertEquals("Test Event", createdEvent.title)
        assertEquals(testUser.id, createdEvent.creatorId)
        assertEquals(testTeam.id, createdEvent.teamId)
        assertEquals("gcal-id-123", createdEvent.googleCalendarEventId)
        assertDateTimeAlmostEqual(now.plusHours(1), createdEvent.startTime)

        val fetchedEvent = eventService.getEventById(createdEvent.id)
        assertNotNull(fetchedEvent)
        assertEquals("Test Event", fetchedEvent.title)
    }

    @Test
    fun `getEventById should retrieve an existing event`() = runBlocking {
        val event = eventService.createEvent("GetMe", null, LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, null, testUser.id)!!
        val foundEvent = eventService.getEventById(event.id)
        assertNotNull(foundEvent)
        assertEquals(event.id, foundEvent.id)
    }

    @Test
    fun `getEventsByCreator should return events for a specific creator`() = runBlocking {
        eventService.createEvent("Event 1 by User", null, LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, null, testUser.id)
        eventService.createEvent("Event 2 by User", null, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), null, null, null, testUser.id)

        val otherUser = userService.createUser(User(0, "otherEventUser", "other@event.com"), "pwd")!!
        eventService.createEvent("Event by OtherUser", null, LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, null, otherUser.id)

        val userEvents = eventService.getEventsByCreator(testUser.id)
        assertEquals(2, userEvents.size)
        assertTrue(userEvents.all { it.creatorId == testUser.id })
    }

    @Test
    fun `getEventsByTeam should return events for a specific team`() = runBlocking {
        eventService.createEvent("Event 1 for Team", null, LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, testTeam.id, testUser.id)

        val otherTeam = teamService.createTeam("Another Team", testUser.id)!!
        eventService.createEvent("Event for Another Team", null, LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, otherTeam.id, testUser.id)

        val teamEvents = eventService.getEventsByTeam(testTeam.id)
        assertEquals(1, teamEvents.size)
        assertTrue(teamEvents.all { it.teamId == testTeam.id })
    }

    @Test
    fun `updateEvent should modify event details`() = runBlocking {
        val event = eventService.createEvent("Old Title", "Old Desc", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Old Loc", null, null, testUser.id)!!

        val newTitle = "New Title"
        val newDesc = "New Description"
        val newStartTime = LocalDateTime.now().plusDays(2)
        val newEndTime = newStartTime.plusHours(2)
        val newGCalId = "gcal-updated-id"

        val updated = eventService.updateEvent(
            id = event.id,
            title = newTitle,
            description = newDesc,
            startTime = newStartTime,
            endTime = newEndTime,
            location = null, virtualMeetingLink = null, teamId = null, // Not changing these here
            googleCalendarEventId = newGCalId,
            clearDescription = false, clearLocation = true, clearVirtualMeetingLink = true, clearTeamId = true, clearGoogleCalendarEventId = false
        )
        assertTrue(updated)

        val fetchedEvent = eventService.getEventById(event.id)!!
        assertEquals(newTitle, fetchedEvent.title)
        assertEquals(newDesc, fetchedEvent.description) // Should be new desc
        assertNull(fetchedEvent.location) // Should be cleared
        assertEquals(newGCalId, fetchedEvent.googleCalendarEventId)
        assertDateTimeAlmostEqual(newStartTime, fetchedEvent.startTime)
    }

    @Test
    fun `updateEvent should clear nullable fields when flags are true`() = runBlocking {
        val event = eventService.createEvent("Event to Clear", "Desc", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Location", "http://link", testTeam.id, testUser.id, "gcalid")!!

        val updated = eventService.updateEvent(
            id = event.id, title = "Still There", description = null, startTime = null, endTime = null, location = null, virtualMeetingLink = null, teamId = null, googleCalendarEventId = null,
            clearDescription = true, clearLocation = true, clearVirtualMeetingLink = true, clearTeamId = true, clearGoogleCalendarEventId = true
        )
        assertTrue(updated)
        val fetched = eventService.getEventById(event.id)!!
        assertEquals("Still There", fetched.title)
        assertNull(fetched.description)
        assertNull(fetched.location)
        assertNull(fetched.virtualMeetingLink)
        assertNull(fetched.teamId)
        assertNull(fetched.googleCalendarEventId)
    }


    @Test
    fun `updateEventGoogleId should only update the googleCalendarEventId`() = runBlocking {
        val event = eventService.createEvent("Event For GCalID", null, LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, null, testUser.id)!!
        assertNull(event.googleCalendarEventId)

        val newGCalId = "gcal-specific-update"
        val updated = eventService.updateEventGoogleId(event.id, newGCalId)
        assertTrue(updated)

        val fetchedEvent = eventService.getEventById(event.id)!!
        assertEquals(newGCalId, fetchedEvent.googleCalendarEventId)
        assertEquals("Event For GCalID", fetchedEvent.title) // Ensure other fields didn't change
    }

    @Test
    fun `deleteEvent should remove an event`() = runBlocking {
        val event = eventService.createEvent("To Delete", null, LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, null, testUser.id)!!
        val deleted = eventService.deleteEvent(event.id)
        assertTrue(deleted)
        assertNull(eventService.getEventById(event.id))
    }

    @Test
    fun `deleteEvent should return false for non-existing event`() = runBlocking {
        assertFalse(eventService.deleteEvent(9999))
    }
}
