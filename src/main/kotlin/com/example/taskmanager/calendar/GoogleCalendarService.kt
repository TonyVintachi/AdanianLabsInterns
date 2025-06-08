package com.example.taskmanager.calendar

import com.example.taskmanager.db.UserCalendarSyncService
import com.example.taskmanager.models.UserCalendarSyncInfo
import com.example.taskmanager.models.Event as LocalEventModel // Alias to avoid confusion with Google's Event
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event as GoogleCalendarEvent
import com.google.api.services.calendar.model.EventDateTime
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Using constants defined in OAuthRoutes.kt or a shared config file eventually
import com.example.taskmanager.routes.GOOGLE_CLIENT_ID_PLACEHOLDER
import com.example.taskmanager.routes.GOOGLE_CLIENT_SECRET_PLACEHOLDER

const val APPLICATION_NAME = "TaskManagerApp" // Your application's name


class GoogleCalendarService(
    private val userCalendarSyncService: UserCalendarSyncService
) {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    // --- Token Refresh and Update Logic ---
    private suspend fun updateStoredTokenIfChanged(
        userId: Int,
        oldSyncInfo: UserCalendarSyncInfo,
        credential: GoogleCredential
    ) {
        // Check if access token was refreshed. GoogleCredential updates its own accessToken internally.
        // The expiresInSeconds on the credential *might* be updated after a refresh.
        // However, credential.getExpiresInSeconds() often returns null or the original duration.
        // A more reliable way to get new expiry is if the refresh response itself is accessible,
        // or by calculating from "now" + original duration if we assume refresh gives same duration.
        // For simplicity, if token string changes, we update it and its fetched time.

        if (credential.accessToken != oldSyncInfo.accessToken) {
            val newExpiry = credential.expiresInSeconds?.let { LocalDateTime.now().plusSeconds(it) }
                            ?: oldSyncInfo.tokenExpiry // Keep old expiry if new one not available

            val newSyncInfo = oldSyncInfo.copy(
                accessToken = credential.accessToken,
                // refreshToken usually doesn't change, but if it could:
                // refreshToken = credential.refreshToken ?: oldSyncInfo.refreshToken,
                tokenExpiry = newExpiry
            )
            userCalendarSyncService.saveOrUpdateSyncInfo(newSyncInfo)
        }
    }


    // --- Get Authorized Calendar Client ---
    @Throws(IOException::class, IllegalStateException::class)
    suspend fun getAuthorizedCalendarClient(userId: Int): Pair<Calendar, UserCalendarSyncInfo> {
        val syncInfo = userCalendarSyncService.getSyncInfoByUserId(userId)
            ?: throw IllegalStateException("User $userId has not authorized Google Calendar access or token not found.")

        if (syncInfo.accessToken.isBlank()) {
             throw IllegalStateException("User $userId has an invalid access token.")
        }

        val credential = GoogleCredential.Builder()
            .setJsonFactory(jsonFactory)
            .setTransport(httpTransport)
            .setClientSecrets(GOOGLE_CLIENT_ID_PLACEHOLDER, GOOGLE_CLIENT_SECRET_PLACEHOLDER)
            .build()

        credential.accessToken = syncInfo.accessToken
        syncInfo.refreshToken?.let { credential.refreshToken = it }
        // Expiry: If token is expired, the library attempts refresh if refreshToken is available.
        // We may need to manually check syncInfo.tokenExpiry here if proactive refresh is desired,
        // but typically the library handles it on first failed API call.

        // Example of proactive refresh check (optional, library often handles this)
        // if (syncInfo.tokenExpiry != null && syncInfo.tokenExpiry!!.isBefore(LocalDateTime.now().minusMinutes(1))) {
        //     if (credential.refreshToken != null) {
        //         try {
        //             credential.refreshToken() // Attempt refresh
        //             updateStoredTokenIfChanged(userId, syncInfo, credential) // Update stored token
        //         } catch (e: IOException) {
        //             // Handle refresh failure, maybe re-throw or ask user to re-authenticate
        //             throw IOException("Failed to refresh token for user $userId: ${e.message}", e)
        //         }
        //     } else {
        //         // No refresh token, and access token is expired - user needs to re-authenticate
        //         throw IllegalStateException("Access token expired and no refresh token available for user $userId. Please re-authenticate.")
        //     }
        // }


        val calendarClient = Calendar.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(APPLICATION_NAME)
            .build()

        return Pair(calendarClient, syncInfo)
    }

    // --- CRUD Operations ---

    suspend fun createEvent(userId: Int, localEvent: LocalEventModel): String? {
        return try {
            val (calendar, syncInfo) = getAuthorizedCalendarClient(userId)

            val googleEvent = GoogleCalendarEvent().apply {
                summary = localEvent.title
                // Start with localEvent.description, then append virtualMeetingLink if present
                var eventDescription = localEvent.description ?: ""
                if (!localEvent.virtualMeetingLink.isNullOrBlank()) {
                    if (eventDescription.isNotBlank()) {
                        eventDescription += "\n\nVirtual Meeting: ${localEvent.virtualMeetingLink}"
                    } else {
                        eventDescription = "Virtual Meeting: ${localEvent.virtualMeetingLink}"
                    }
                }
                description = if (eventDescription.isNotBlank()) eventDescription else null

                location = localEvent.location
                // TODO: Add attendees, recurrence, etc. if needed from localEvent

                start = EventDateTime().apply {
                    dateTime = DateTime(localEvent.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    // timeZone = ZoneId.systemDefault().id // Optional: set if different from user's primary calendar TZ
                }
                end = EventDateTime().apply {
                    dateTime = DateTime(localEvent.endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    // timeZone = ZoneId.systemDefault().id
                }
                // Store local ID in extended properties for later mapping/sync if needed
                extendedProperties = Event.ExtendedProperties().setShared(mapOf("localAppEventId" to localEvent.id.toString()))
            }

            val createdEvent = calendar.events().insert("primary", googleEvent).execute()
            updateStoredTokenIfChanged(userId, syncInfo, calendar.credential as GoogleCredential) // calendar.credential should be the one we built
            createdEvent.id // Return Google Calendar Event ID
        } catch (e: Exception) {
            // Log error (e.g., using application.log or specific logger)
            println("Error creating Google Calendar event for user $userId: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun getEvent(userId: Int, googleEventId: String): GoogleCalendarEvent? {
        return try {
            val (calendar, syncInfo) = getAuthorizedCalendarClient(userId)
            val event = calendar.events().get("primary", googleEventId).execute()
            updateStoredTokenIfChanged(userId, syncInfo, calendar.credential as GoogleCredential)
            event
        } catch (e: Exception) {
            println("Error getting Google Calendar event $googleEventId for user $userId: ${e.message}")
            null
        }
    }

    suspend fun updateEvent(userId: Int, googleEventId: String, localEvent: LocalEventModel): GoogleCalendarEvent? {
         return try {
            val (calendar, syncInfo) = getAuthorizedCalendarClient(userId)

            // Fetch the existing event from Google Calendar
            val existingGoogleEvent = calendar.events().get("primary", googleEventId).execute()
                ?: throw IOException("Google Calendar event with ID $googleEventId not found.")

            // Update fields
            existingGoogleEvent.summary = localEvent.title

            // Set description from localEvent, then append virtualMeetingLink if present
            var eventDescription = localEvent.description ?: ""
            if (!localEvent.virtualMeetingLink.isNullOrBlank()) {
                if (eventDescription.isNotBlank()) {
                    eventDescription += "\n\nVirtual Meeting: ${localEvent.virtualMeetingLink}"
                } else {
                    eventDescription = "Virtual Meeting: ${localEvent.virtualMeetingLink}"
                }
            }
            existingGoogleEvent.description = if (eventDescription.isNotBlank()) eventDescription else null

            existingGoogleEvent.location = localEvent.location
            existingGoogleEvent.start = EventDateTime().apply {
                dateTime = DateTime(localEvent.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            }
            existingGoogleEvent.end = EventDateTime().apply {
                dateTime = DateTime(localEvent.endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            }
            // Update extended properties if necessary
            // existingGoogleEvent.extendedProperties?.shared?.put("localAppEventId", localEvent.id.toString())


            val updatedEvent = calendar.events().update("primary", googleEventId, existingGoogleEvent).execute()
            updateStoredTokenIfChanged(userId, syncInfo, calendar.credential as GoogleCredential)
            updatedEvent
        } catch (e: Exception) {
            println("Error updating Google Calendar event $googleEventId for user $userId: ${e.message}")
            null
        }
    }

    suspend fun deleteEvent(userId: Int, googleEventId: String): Boolean {
        return try {
            val (calendar, syncInfo) = getAuthorizedCalendarClient(userId)
            calendar.events().delete("primary", googleEventId).execute()
            updateStoredTokenIfChanged(userId, syncInfo, calendar.credential as GoogleCredential)
            true
        } catch (e: Exception) {
            println("Error deleting Google Calendar event $googleEventId for user $userId: ${e.message}")
            false
        }
    }
}
