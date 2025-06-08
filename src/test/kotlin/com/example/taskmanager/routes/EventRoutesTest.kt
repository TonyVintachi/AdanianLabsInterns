package com.example.taskmanager.routes

import com.example.taskmanager.calendar.GoogleCalendarService
import com.example.taskmanager.db.EventService
import com.example.taskmanager.models.Event as LocalEvent
import com.example.taskmanager.routes.dto.*
import com.example.taskmanager.utils.configureTestEnvironment
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.*

class EventRoutesTest {

    private lateinit var mockEventService: EventService
    private lateinit var mockGoogleCalendarService: GoogleCalendarService
    private val isoDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val testUserId = 1

    @BeforeEach
    fun setup() {
        // No DB setup needed as we mock services
        mockEventService = mockk(relaxed = true)
        mockGoogleCalendarService = mockk(relaxed = true)
    }

    private fun createTestLocalEvent(id: Int, googleId: String? = null, creatorId: Int = testUserId): LocalEvent {
        return LocalEvent(
            id = id,
            googleCalendarEventId = googleId,
            title = "Test Event $id",
            description = "Description for event $id",
            startTime = LocalDateTime.now().plusHours(id.toLong()),
            endTime = LocalDateTime.now().plusHours(id.toLong() + 1),
            location = "Test Location $id",
            virtualMeetingLink = "http://meet.test/$id",
            teamId = null,
            creatorId = creatorId
        )
    }

    @Test
    fun `POST events should create local event and attempt Google Calendar sync`() = testApplication {
        application {
            configureTestEnvironment()
            routing { eventRoutes(mockEventService, mockGoogleCalendarService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }

        val startTimeStr = LocalDateTime.now().plusHours(1).format(isoDateTimeFormatter)
        val endTimeStr = LocalDateTime.now().plusHours(2).format(isoDateTimeFormatter)
        val request = EventCreateRequest("New Event", "Desc", startTimeStr, endTimeStr, "Loc", "Link", null)

        val createdLocalEvent = createTestLocalEvent(1).copy(googleCalendarEventId = null) // Simulates return from DB after first insert
        val googleCalId = "gcal-event-id"

        coEvery { mockEventService.createEvent(any(), any(), any(), any(), any(), any(), any(), eq(testUserId)) } returns createdLocalEvent
        coEvery { mockGoogleCalendarService.createEvent(testUserId, createdLocalEvent) } returns googleCalId
        coEvery { mockEventService.updateEventGoogleId(createdLocalEvent.id, googleCalId) } returns true
        // For the final response, EventService will be called again to get the event with Google ID
        coEvery { mockEventService.getEventById(createdLocalEvent.id) } returns createdLocalEvent.copy(googleCalendarEventId = googleCalId)


        val response = client.post("/users/$testUserId/events") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val eventResponse = response.body<EventResponse>()
        assertEquals("Test Event 1", eventResponse.title) // Title from `createTestLocalEvent`
        assertEquals(googleCalId, eventResponse.googleCalendarEventId)

        coVerifyOrder {
            mockEventService.createEvent(any(), any(), any(), any(), any(), any(), any(), eq(testUserId))
            mockGoogleCalendarService.createEvent(testUserId, createdLocalEvent)
            mockEventService.updateEventGoogleId(createdLocalEvent.id, googleCalId)
        }
    }

    @Test
    fun `POST events should still succeed locally if Google Calendar sync fails (e_g_, user not authorized)`() = testApplication {
        application {
            configureTestEnvironment()
            routing { eventRoutes(mockEventService, mockGoogleCalendarService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }

        val startTimeStr = LocalDateTime.now().plusHours(1).format(isoDateTimeFormatter)
        val endTimeStr = LocalDateTime.now().plusHours(2).format(isoDateTimeFormatter)
        val request = EventCreateRequest("Local Only Event", "Desc", startTimeStr, endTimeStr, null, null, null)
        val createdLocalEvent = createTestLocalEvent(2, creatorId = testUserId).copy(googleCalendarEventId = null)

        coEvery { mockEventService.createEvent(any(), any(), any(), any(), any(), any(), any(), eq(testUserId)) } returns createdLocalEvent
        // Simulate Google Calendar auth failure
        coEvery { mockGoogleCalendarService.createEvent(testUserId, createdLocalEvent) } throws IllegalStateException("User not authorized")
        // getEventById will be called to build the final response
        coEvery { mockEventService.getEventById(createdLocalEvent.id) } returns createdLocalEvent


        val response = client.post("/users/$testUserId/events") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val eventResponse = response.body<EventResponse>()
        assertEquals(createdLocalEvent.title, eventResponse.title)
        assertNull(eventResponse.googleCalendarEventId, "Google Calendar ID should be null if sync failed")

        coVerify { mockEventService.updateEventGoogleId(any(), any()) wasNot Called }
    }


    @Test
    fun `GET events by user should return list of events`() = testApplication {
        application {
            configureTestEnvironment()
            routing { eventRoutes(mockEventService, mockGoogleCalendarService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val eventList = listOf(createTestLocalEvent(1), createTestLocalEvent(2))
        coEvery { mockEventService.getEventsByCreator(testUserId) } returns eventList

        val response = client.get("/users/$testUserId/events")
        assertEquals(HttpStatusCode.OK, response.status)
        val eventResponses = response.body<List<EventResponse>>()
        assertEquals(2, eventResponses.size)
        assertEquals(eventList[0].title, eventResponses[0].title)
    }

    @Test
    fun `GET event by id should return single event if creator matches`() = testApplication {
        application {
            configureTestEnvironment()
            routing { eventRoutes(mockEventService, mockGoogleCalendarService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val eventId = 1
        val event = createTestLocalEvent(eventId, creatorId = testUserId)
        coEvery { mockEventService.getEventById(eventId) } returns event

        val response = client.get("/users/$testUserId/events/$eventId")
        assertEquals(HttpStatusCode.OK, response.status)
        val eventResponse = response.body<EventResponse>()
        assertEquals(event.title, eventResponse.title)
    }

    @Test
    fun `GET event by id should return NotFound if creator does not match`() = testApplication {
        application {
            configureTestEnvironment()
            routing { eventRoutes(mockEventService, mockGoogleCalendarService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val eventId = 1
        val otherUserId = 99
        val event = createTestLocalEvent(eventId, creatorId = otherUserId) // Event created by someone else
        coEvery { mockEventService.getEventById(eventId) } returns event

        val response = client.get("/users/$testUserId/events/$eventId") // testUserId tries to access
        assertEquals(HttpStatusCode.NotFound, response.status) // Or Forbidden, depending on desired strictness
    }


    @Test
    fun `PUT event should update local and Google Calendar event`() = testApplication {
        application {
            configureTestEnvironment()
            routing { eventRoutes(mockEventService, mockGoogleCalendarService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val eventId = 1
        val googleId = "gcal-to-update"
        val existingEvent = createTestLocalEvent(eventId, googleId = googleId, creatorId = testUserId)
        val updatedEventData = existingEvent.copy(title = "Updated Title via API")

        val updateRequest = EventUpdateRequest(title = "Updated Title via API")

        coEvery { mockEventService.getEventById(eventId) } returns existingEvent // Initial fetch
        coEvery { mockEventService.updateEvent(eq(eventId), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true
        coEvery { mockEventService.getEventById(eventId) } returns updatedEventData // Fetch after local update
        coEvery { mockGoogleCalendarService.updateEvent(testUserId, googleId, updatedEventData) } returns mockk() // Mock Google's response

        val response = client.put("/users/$testUserId/events/$eventId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val eventResponse = response.body<EventResponse>()
        assertEquals("Updated Title via API", eventResponse.title)

        coVerifyOrder {
            mockEventService.getEventById(eventId) // Initial fetch for auth
            mockEventService.updateEvent(eq(eventId), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            mockEventService.getEventById(eventId) // Fetch after local update
            mockGoogleCalendarService.updateEvent(testUserId, googleId, updatedEventData)
        }
    }

    @Test
    fun `DELETE event should delete local and Google Calendar event`() = testApplication {
        application {
            configureTestEnvironment()
            routing { eventRoutes(mockEventService, mockGoogleCalendarService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val eventId = 1
        val googleId = "gcal-to-delete"
        val eventToDelete = createTestLocalEvent(eventId, googleId = googleId, creatorId = testUserId)

        coEvery { mockEventService.getEventById(eventId) } returns eventToDelete
        coEvery { mockGoogleCalendarService.deleteEvent(testUserId, googleId) } returns true
        coEvery { mockEventService.deleteEvent(eventId) } returns true

        val response = client.delete("/users/$testUserId/events/$eventId")
        assertEquals(HttpStatusCode.NoContent, response.status)

        coVerifyOrder {
            mockEventService.getEventById(eventId)
            mockGoogleCalendarService.deleteEvent(testUserId, googleId)
            mockEventService.deleteEvent(eventId)
        }
    }
}
