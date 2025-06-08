package com.example.taskmanager.routes

import com.example.taskmanager.db.UserCalendarSyncService
import com.example.taskmanager.models.UserCalendarSyncInfo
import com.example.taskmanager.utils.configureTestEnvironment
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.*

// Mocking static/global GsonFactory or NetHttpTransport is tricky.
// We'll rely on them working and mock the flow generation or specific calls.

class OAuthRoutesTest {

    private lateinit var mockUserCalendarSyncService: UserCalendarSyncService
    // We need to mock the flow behavior, especially token exchange.
    private lateinit var mockAuthFlow: GoogleAuthorizationCodeFlow
    private lateinit var mockTokenRequest: GoogleAuthorizationCodeFlow. इसरो.टोकन अनुरोध // Placeholder for token request mock
    private lateinit var mockAuthUrl: GoogleAuthorizationCodeRequestUrl


    @BeforeEach
    fun setup() {
        mockUserCalendarSyncService = mockk(relaxed = true)
        mockAuthFlow = mockk(relaxed = true)
        mockTokenRequest = mockk(relaxed = true) // GoogleAuthorizationCodeTokenRequest
        mockAuthUrl = mockk(relaxed = true)

        // Mock the static `buildGoogleFlow()` or inject the flow if refactored.
        // For now, we can't easily mock the top-level buildGoogleFlow() without PowerMock or refactoring.
        // Let's assume buildGoogleFlow() works and test the route logic given a flow.
        // The tests will focus on what the routes *do* with the flow.
        // A better way would be to make `flow` a parameter or injectable property in `oAuthRoutes`.
        // For this test, we will have to assume `buildGoogleFlow()` is called and proceed from there.
        // We can't easily inject a mock flow into the extension function `Route.oAuthRoutes` as is.
        // This means testing the *actual* Google library calls for URL building, which is fine for authorize.
        // For callback, we'd ideally mock `flow.newTokenRequest(code).execute()`.
    }

    @Test
    fun `GET oauth_google_calendar_authorize should redirect to Google with correct parameters`() = testApplication {
        application {
            configureTestEnvironment() // Installs ContentNegotiation if needed, etc.
            // How to provide a mock flow to oAuthRoutes?
            // Option 1: Refactor oAuthRoutes to accept flow as a parameter (preferred for testability)
            // Option 2: Use a DI framework.
            // Option 3: Test the actual URL construction (less isolated, but tests the real thing)
            // For now, let's go with Option 3 for the authorize part, as it's just URL building.
            routing { oAuthRoutes(mockUserCalendarSyncService) }
        }

        val userId = 123
        val response = client.get("/oauth/google/calendar/authorize?userId=$userId")

        assertEquals(HttpStatusCode.Found, response.status)
        val location = response.headers[HttpHeaders.Location]
        assertNotNull(location)
        assertTrue(location.startsWith("https://accounts.google.com/o/oauth2/auth"))
        assertTrue(location.contains("client_id=${GOOGLE_CLIENT_ID_PLACEHOLDER}"))
        assertTrue(location.contains("redirect_uri=${encodeURLParameter(GOOGLE_CALENDAR_REDIRECT_URI)}"))
        assertTrue(location.contains("scope=${encodeURLParameter(GOOGLE_CALENDAR_SCOPES.joinToString(" "))}"))
        assertTrue(location.contains("state=$userId"))
        assertTrue(location.contains("access_type=offline"))
        assertTrue(location.contains("approval_prompt=force"))
    }

    @Test
    fun `GET oauth_google_calendar_authorize should require userId`() = testApplication {
        application {
            configureTestEnvironment()
            routing { oAuthRoutes(mockUserCalendarSyncService) }
        }
        val response = client.get("/oauth/google/calendar/authorize")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("userId query parameter is required.", response.bodyAsText())
    }


    // Testing the callback is harder without refactoring to inject the mocked flow.
    // The following test would be how you'd do it IF the flow was injectable and mockable:
    /*
    @Test
    fun `GET oauth_google_calendar_callback should exchange code and save tokens`() = testApplication {
        // This test requires flow to be injectable into oAuthRoutes or buildGoogleFlow to be mockable
        val testUserId = 456
        val authCode = "test_auth_code"
        val mockTokenResponse = GoogleTokenResponse().apply {
            accessToken = "test_access_token"
            refreshToken = "test_refresh_token"
            expiresInSeconds = 3600L
        }

        // Prepare the mockAuthFlow to be used by the route
        every { mockAuthFlow.newTokenRequest(authCode) } returns mockTokenRequest
        every { mockTokenRequest.setRedirectUri(GOOGLE_CALENDAR_REDIRECT_URI) } returns mockTokenRequest
        every { mockTokenRequest.execute() } returns mockTokenResponse

        coEvery { mockUserCalendarSyncService.saveOrUpdateSyncInfo(any()) } just runs

        application {
            configureTestEnvironment()
            // Assumes oAuthRoutes is refactored: fun Route.oAuthRoutes(service, flowToUse)
            // routing { oAuthRoutes(mockUserCalendarSyncService, mockAuthFlow) }
            // If not refactored, this test will use the real flow and can't mock execute() easily.
             routing { oAuthRoutes(mockUserCalendarSyncService) } // Using real flow for now
        }

        // This test will likely fail to mock `execute()` unless `buildGoogleFlow()` is refactored
        // or PowerMock/JMockit is used for static/constructor mocking.
        // For now, this test demonstrates the ideal structure but acknowledges current limitations.

        val response = client.get("/oauth/google/calendar/callback?code=$authCode&state=$testUserId")

        // Due to inability to easily mock flow.newTokenRequest().execute() without refactor or PowerMock,
        // this part of the test will likely fail or make a real HTTP request if not careful.
        // We'll assume for now we are testing the path where execute() would succeed.
        // A real integration test would hit this, but for a unit test, mocking is preferred.

        // If the above execute() could be mocked:
        // assertEquals(HttpStatusCode.OK, response.status)
        // assertEquals("OAuth successful for user $testUserId! Tokens stored. You can close this window.", response.bodyAsText())
        // coVerify { mockUserCalendarSyncService.saveOrUpdateSyncInfo(
        //    match { it.userId == testUserId && it.accessToken == "test_access_token" }
        // )}

        // If we can't mock execute(), we can only test up to the point of failure or if it tries a real call.
        // For now, let's assert that it tries to process, and if it fails (e.g. invalid client ID), that's expected without mocks.
        // This is more of an integration test for the callback without proper mocking for `execute()`.
        if (response.status != HttpStatusCode.OK) {
             println("Callback test actual status: ${response.status}")
             println("Callback test actual body: ${runBlocking{response.bodyAsText()}}")
        }
        // This test will be partial / an integration test for the callback due to mocking limitations of static buildGoogleFlow
        assertTrue(response.status == HttpStatusCode.OK || response.status == HttpStatusCode.InternalServerError || response.status == HttpStatusCode.BadRequest,
            "Response should be OK if successful, or an error status if token exchange fails (which it will without valid client secrets/code)")
    }
    */

    @Test
    fun `GET oauth_google_calendar_callback should handle error parameter`() = testApplication {
        application {
            configureTestEnvironment()
            routing { oAuthRoutes(mockUserCalendarSyncService) }
        }
        val errorMsg = "access_denied"
        val response = client.get("/oauth/google/calendar/callback?error=$errorMsg")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("OAuth error: $errorMsg", response.bodyAsText())
    }

    @Test
    fun `GET oauth_google_calendar_callback should require code`() = testApplication {
        application {
            configureTestEnvironment()
            routing { oAuthRoutes(mockUserCalendarSyncService) }
        }
        val response = client.get("/oauth/google/calendar/callback?state=123")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Authorization code not found.", response.bodyAsText())
    }

    @Test
    fun `GET oauth_google_calendar_callback should require valid state (userId)`() = testApplication {
        application {
            configureTestEnvironment()
            routing { oAuthRoutes(mockUserCalendarSyncService) }
        }
        val response = client.get("/oauth/google/calendar/callback?code=somecode&state=invalid_state")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid state parameter. Could not extract userId.", response.bodyAsText())
    }

}


// Placeholder for GoogleAuthorizationCodeFlow.GoogleAuthorizationCodeTokenRequest for mocking structure
// In a real scenario, ensure this matches the actual class structure or use MockK's ability to mock chains.
private class `GoogleAuthorizationCodeFlow$GoogleAuthorizationCodeTokenRequest` {}
