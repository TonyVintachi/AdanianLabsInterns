package com.example.taskmanager.calendar

import com.example.taskmanager.db.UserCalendarSyncService
import com.example.taskmanager.models.UserCalendarSyncInfo
import com.example.taskmanager.models.Event as LocalEvent
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event as GoogleEventModel
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.*

class GoogleCalendarServiceTest {

    private lateinit var mockUserCalendarSyncService: UserCalendarSyncService
    private lateinit var googleCalendarService: GoogleCalendarService

    // Mocks for Google's Calendar API client chain
    private lateinit var mockCalendarClient: Calendar
    private lateinit var mockEvents: Calendar.Events
    private lateinit var mockInsert: Calendar.Events.Insert
    private lateinit var mockGet: Calendar.Events.Get
    private lateinit var mockUpdate: Calendar.Events.Update
    private lateinit var mockDelete: Calendar.Events.Delete

    // Mock GoogleCredential to inspect/control its state
    private lateinit var mockGoogleCredential: GoogleCredential


    @BeforeEach
    fun setUp() {
        mockUserCalendarSyncService = mockk()

        // Mock the GoogleCredential
        mockGoogleCredential = spyk(GoogleCredential.Builder() // Use spyk to allow some real methods if needed, or mockk too
            .setJsonFactory(mockk<JsonFactory>(relaxed = true))
            .setTransport(mockk<HttpTransport>(relaxed = true))
            .setClientSecrets("mockClientId", "mockClientSecret") // These need to be non-null
            .build())

        // Setup the chain of Google Calendar API calls
        mockCalendarClient = mockk()
        mockEvents = mockk()
        mockInsert = mockk(relaxed = true) // relaxed for execute()
        mockGet = mockk(relaxed = true)
        mockUpdate = mockk(relaxed = true)
        mockDelete = mockk(relaxed = true)

        every { mockCalendarClient.events() } returns mockEvents
        every { mockEvents.insert(any(), any()) } returns mockInsert
        every { mockEvents.get(any(), any()) } returns mockGet
        every { mockEvents.update(any(), any(), any()) } returns mockUpdate
        every { mockEvents.delete(any(), any()) } returns mockDelete

        // Make the Calendar client return our spied/mocked GoogleCredential
        every { mockCalendarClient.credential } returns mockGoogleCredential


        // Instantiate the service under test
        // We need a way to inject the mocked Calendar client or mock its builder.
        // For simplicity, we'll make getAuthorizedCalendarClient a bit more mockable,
        // or use a more involved mocking strategy if it were final/hard to mock.
        // For now, we assume getAuthorizedCalendarClient can be spied or we test its parts.
        // Actual GoogleCalendarService will be created per test for better control over getAuthorizedCalendarClient mocking.
    }

    private fun createServiceWithMockedClient(userId: Int, syncInfo: UserCalendarSyncInfo): GoogleCalendarService {
        val service = spyk(GoogleCalendarService(mockUserCalendarSyncService), recordPrivateCalls = true)
        // Mock the getAuthorizedCalendarClient to return our pre-configured mock client and syncInfo
        coEvery { service.getAuthorizedCalendarClient(userId) } returns Pair(mockCalendarClient, syncInfo)
        return service
    }


    @Test
    fun `getAuthorizedCalendarClient should retrieve sync info and build credential`() = runBlocking {
        val userId = 1
        val expiry = LocalDateTime.now().plusHours(1)
        val syncInfo = UserCalendarSyncInfo(userId, "access", "refresh", expiry)
        coEvery { mockUserCalendarSyncService.getSyncInfoByUserId(userId) } returns syncInfo

        // Actual service, not spied for this specific test of getAuthorizedCalendarClient itself
        val realService = GoogleCalendarService(mockUserCalendarSyncService)

        val (client,
            retrievedSyncInfo) = realService.getAuthorizedCalendarClient(userId)

        assertNotNull(client)
        assertEquals(syncInfo, retrievedSyncInfo)
        val credential = client.credential as GoogleCredential
        assertEquals("access", credential.accessToken)
        assertEquals("refresh", credential.refreshToken)

        coVerify { mockUserCalendarSyncService.getSyncInfoByUserId(userId) }
    }

    @Test
    fun `getAuthorizedCalendarClient should throw if sync info not found`() = runBlocking {
        val userId = 1
        coEvery { mockUserCalendarSyncService.getSyncInfoByUserId(userId) } returns null
        val realService = GoogleCalendarService(mockUserCalendarSyncService)

        assertFailsWith<IllegalStateException> {
            realService.getAuthorizedCalendarClient(userId)
        }
    }


    @Test
    fun `createEvent should insert event to Google Calendar and update token if changed`() = runBlocking {
        val userId = 1
        val initialAccessToken = "initial_access_token"
        val refreshedAccessToken = "refreshed_access_token"
        val syncInfo = UserCalendarSyncInfo(userId, initialAccessToken, "refresh", LocalDateTime.now().plusHours(1))

        val googleCalendarServiceSpy = createServiceWithMockedClient(userId, syncInfo)

        val localEvent = LocalEvent(1, null, "Test Event", "Desc", LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, null, userId)
        val googleEventId = "google-event-id"
        val mockCreatedGoogleEvent = GoogleEventModel().setId(googleEventId)

        every { mockInsert.execute() } returns mockCreatedGoogleEvent
        // Simulate token refresh: credential's accessToken changes after execute()
        every { mockGoogleCredential.accessToken } returns initialAccessToken andThen refreshedAccessToken
        coEvery { mockUserCalendarSyncService.saveOrUpdateSyncInfo(any()) } just runs


        val resultEventId = googleCalendarServiceSpy.createEvent(userId, localEvent)

        assertEquals(googleEventId, resultEventId)
        coVerify { mockEvents.insert("primary", any<GoogleEventModel>()) }
        coVerify { mockInsert.execute() }
        // Verify token update was called because accessToken changed
        coVerify { mockUserCalendarSyncService.saveOrUpdateSyncInfo(match { it.accessToken == refreshedAccessToken }) }
    }

    @Test
    fun `getEvent should retrieve event from Google Calendar`() = runBlocking {
        val userId = 1
        val googleEventId = "gEventId"
        val syncInfo = UserCalendarSyncInfo(userId, "access", "refresh", LocalDateTime.now().plusHours(1))
        val googleCalendarServiceSpy = createServiceWithMockedClient(userId, syncInfo)

        val mockGoogleEvent = GoogleEventModel().setSummary("Fetched Event")
        every { mockGet.execute() } returns mockGoogleEvent
        every { mockGoogleCredential.accessToken } returns "access" // No refresh in this path for simplicity

        val resultEvent = googleCalendarServiceSpy.getEvent(userId, googleEventId)

        assertNotNull(resultEvent)
        assertEquals("Fetched Event", resultEvent.summary)
        coVerify { mockEvents.get("primary", googleEventId) }
        coVerify { mockGet.execute() }
         // Verify token update was called (even if token didn't change, the check happens)
        coVerify { mockUserCalendarSyncService.saveOrUpdateSyncInfo(any()) }
    }

    @Test
    fun `updateEvent should update event on Google Calendar`() = runBlocking {
        val userId = 1
        val googleEventId = "gEventIdToUpdate"
        val syncInfo = UserCalendarSyncInfo(userId, "access", "refresh", LocalDateTime.now().plusHours(1))
        val googleCalendarServiceSpy = createServiceWithMockedClient(userId, syncInfo)

        val localEvent = LocalEvent(1, googleEventId, "Updated Title", "Up Desc", LocalDateTime.now(), LocalDateTime.now().plusHours(1), null, null, null, userId)
        val mockExistingGoogleEvent = GoogleEventModel().setId(googleEventId).setSummary("Old Title")
        val mockUpdatedGoogleEvent = GoogleEventModel().setId(googleEventId).setSummary("Updated Title")

        every { mockGet.execute() } returns mockExistingGoogleEvent // For the initial fetch in updateEvent
        every { mockUpdate.execute() } returns mockUpdatedGoogleEvent
        every { mockGoogleCredential.accessToken } returns "access"

        val resultEvent = googleCalendarServiceSpy.updateEvent(userId, googleEventId, localEvent)

        assertNotNull(resultEvent)
        assertEquals("Updated Title", resultEvent.summary)
        coVerify { mockEvents.get("primary", googleEventId) } // Get before update
        coVerify { mockEvents.update("primary", googleEventId, any<GoogleEventModel>()) }
        coVerify { mockUpdate.execute() }
        coVerify { mockUserCalendarSyncService.saveOrUpdateSyncInfo(any()) }
    }

    @Test
    fun `deleteEvent should delete event from Google Calendar`() = runBlocking {
        val userId = 1
        val googleEventId = "gEventIdToDelete"
        val syncInfo = UserCalendarSyncInfo(userId, "access", "refresh", LocalDateTime.now().plusHours(1))
        val googleCalendarServiceSpy = createServiceWithMockedClient(userId, syncInfo)

        // Google's delete often returns void, so execute might return null or a specific object if library wraps it.
        // For a void method, relaxed mock for execute is fine. If it returns an object:
        // every { mockDelete.execute() } returns mockk() (or null if appropriate)
        // Assuming relaxed mock handles this (returns null for non-void, or just works for void)
        every { mockGoogleCredential.accessToken } returns "access"


        val success = googleCalendarServiceSpy.deleteEvent(userId, googleEventId)

        assertTrue(success)
        coVerify { mockEvents.delete("primary", googleEventId) }
        coVerify { mockDelete.execute() }
        coVerify { mockUserCalendarSyncService.saveOrUpdateSyncInfo(any()) }
    }
}
