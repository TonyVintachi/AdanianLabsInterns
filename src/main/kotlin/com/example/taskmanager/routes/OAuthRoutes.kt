package com.example.taskmanager.routes

import com.example.taskmanager.db.UserCalendarSyncService
import com.example.taskmanager.models.UserCalendarSyncInfo
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.io.IOException

// --- Configuration Placeholders ---
const val GOOGLE_CLIENT_ID_PLACEHOLDER = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com" // Replace with your actual Client ID
const val GOOGLE_CLIENT_SECRET_PLACEHOLDER = "YOUR_GOOGLE_CLIENT_SECRET" // Replace with your actual Client Secret
const val GOOGLE_CALENDAR_REDIRECT_URI = "http://localhost:8080/oauth/google/calendar/callback"

val GOOGLE_CALENDAR_SCOPES = listOf(
    "https.www.googleapis.com/auth/calendar.events", // Manage calendar events
    "https.www.googleapis.com/auth/userinfo.email"   // Get user's email address
)
// --- End Configuration Placeholders ---


// Helper function to build GoogleAuthorizationCodeFlow
fun buildGoogleFlow(): GoogleAuthorizationCodeFlow {
    return GoogleAuthorizationCodeFlow.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(), // Ktor's Gson might not be directly usable here, default should be fine
        GOOGLE_CLIENT_ID_PLACEHOLDER,
        GOOGLE_CLIENT_SECRET_PLACEHOLDER,
        GOOGLE_CALENDAR_SCOPES
    ).setAccessType("offline") // Request refresh token
     .setApprovalPrompt("force") // Force approval to ensure refresh token is sent, good for dev/first use
     .build()
}

fun Route.oAuthRoutes(userCalendarSyncService: UserCalendarSyncService) {

    val flow = buildGoogleFlow()

    get("/oauth/google/calendar/authorize") {
        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
        if (userId == null) {
            call.respond(io.ktor.http.HttpStatusCode.BadRequest, "userId query parameter is required.")
            return@get
        }

        // In a production app, generate a secure random state, store it with userId, and validate it in callback
        val state = userId.toString()
        val authorizationUrl = flow.newAuthorizationUrl()
            .setRedirectUri(GOOGLE_CALENDAR_REDIRECT_URI)
            .setState(state)

        call.respondRedirect(authorizationUrl.build())
    }

    get("/oauth/google/calendar/callback") {
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]
        val error = call.request.queryParameters["error"]

        if (error != null) {
            call.respond(io.ktor.http.HttpStatusCode.BadRequest, "OAuth error: $error")
            return@get
        }

        if (code == null) {
            call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Authorization code not found.")
            return@get
        }

        val userId = state?.toIntOrNull()
        if (userId == null) {
            call.respond(io.ktor.http.HttpStatusCode.BadRequest, "Invalid state parameter. Could not extract userId.")
            return@get
        }

        try {
            val tokenResponse: GoogleTokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(GOOGLE_CALENDAR_REDIRECT_URI)
                .execute()

            val accessToken = tokenResponse.accessToken
            val refreshToken = tokenResponse.refreshToken // May be null if already granted and not forced
            val expiresInSeconds = tokenResponse.expiresInSeconds
            val tokenExpiry = if (expiresInSeconds != null) LocalDateTime.now().plusSeconds(expiresInSeconds) else null

            val syncInfo = UserCalendarSyncInfo(
                userId = userId,
                accessToken = accessToken,
                refreshToken = refreshToken, // Store refresh token if available
                tokenExpiry = tokenExpiry
            )

            userCalendarSyncService.saveOrUpdateSyncInfo(syncInfo)

            call.respondText("OAuth successful for user $userId! Tokens stored. You can close this window.")

        } catch (e: IOException) {
            // Handle exceptions like network issues or invalid code
            application.log.error("Exception during token exchange: ${e.message}", e)
            call.respond(io.ktor.http.HttpStatusCode.InternalServerError, "Failed to exchange token: ${e.message}")
        }
    }
}
