package com.example.taskmanager.routes

import com.example.taskmanager.db.UserService
import com.example.taskmanager.models.User
import com.example.taskmanager.routes.dto.UserCreateRequest
import com.example.taskmanager.routes.dto.UserLoginRequest
import com.example.taskmanager.routes.dto.UserResponse
import com.example.taskmanager.routes.dto.UserUpdateRequest
import com.example.taskmanager.utils.DatabaseTestUtils
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.Application // Ensure Application is imported for extension function
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
// Remove local configureTestEnvironment if it was defined here, import from utils instead
import com.example.taskmanager.utils.configureTestEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class UserRoutesTest {

    private val userService = UserService() // Use actual service with test DB

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `POST users_register should create a user`() = testApplication {
        application {
            // Configure server for testing, similar to Application.kt
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }

        val request = UserCreateRequest("testuser", "test@example.com", "password123")
        val response = client.post("/users/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val userResponse = response.body<UserResponse>()
        assertEquals("testuser", userResponse.username)
        assertEquals("test@example.com", userResponse.email)
    }

    @Test
    fun `POST users_login should authenticate valid user`() = testApplication {
        application {
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }

        // Setup: Create a user first
        val initialUser = User(0, "loginuser", "login@example.com")
        val password = "password123"
        userService.createUser(initialUser, password)


        val request = UserLoginRequest("loginuser", password)
        val response = client.post("/users/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val userResponse = response.body<UserResponse>()
        assertEquals("loginuser", userResponse.username)
    }

    @Test
    fun `POST users_login should fail for invalid credentials`() = testApplication {
        application {
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }
        // User "wronguser" does not exist
        val request = UserLoginRequest("wronguser", "wrongpassword")
        val response = client.post("/users/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        assertEquals(HttpStatusCode.NotFound, response.status) // Or Unauthorized if user exists but pass is wrong
    }

    @Test
    fun `GET users_{id} should retrieve existing user`() = testApplication {
        application {
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }

        val createdUser = runBlocking {
            userService.createUser(User(0, "getuser", "get@example.com"), "pwd")!!
        }

        val response = client.get("/users/${createdUser.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val userResponse = response.body<UserResponse>()
        assertEquals(createdUser.username, userResponse.username)
    }

    @Test
    fun `GET users_{id} should return NotFound for non-existing user`() = testApplication {
        application {
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }
        val response = client.get("/users/9999") // Non-existent ID
        assertEquals(HttpStatusCode.NotFound, response.status)
    }


    @Test
    fun `PUT users_{id} should update user details`() = testApplication {
         application {
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }

        val initialPassword = "currentPassword"
        val createdUser = runBlocking {
            userService.createUser(User(0, "updateuser", "update@example.com"), initialPassword)!!
        }

        val updateRequest = UserUpdateRequest(email = "updated@example.com", currentPasswordHash = initialPassword, newPasswordHash = "newPassword")
        val response = client.put("/users/${createdUser.id}") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("User updated successfully", response.bodyAsText())

        // Verify update
        val updatedUserInDb = runBlocking { userService.getUserById(createdUser.id) }
        assertNotNull(updatedUserInDb)
        assertEquals("updated@example.com", updatedUserInDb.email)
        val newStoredHash = runBlocking { userService.getUserPasswordHash(createdUser.id) }
        assertEquals("newPassword", newStoredHash)
    }

    @Test
    fun `PUT users_{id} should fail with incorrect current password`() = testApplication {
        application {
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }
        val createdUser = runBlocking {
            userService.createUser(User(0, "updatefailuser", "updatefail@example.com"), "actualPassword")!!
        }
        val updateRequest = UserUpdateRequest(email = "new@email.com", currentPasswordHash = "wrongCurrentPassword", newPasswordHash = "newPwd")
        val response = client.put("/users/${createdUser.id}") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status) // Or a more specific error like BadRequest or Unauthorized
        // The service returns false, route translates to InternalServerError. Could be more specific.
         assertEquals("Failed to update user or invalid current password", response.bodyAsText())
    }


    @Test
    fun `DELETE users_{id} should remove a user`() = testApplication {
        application {
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }
        val createdUser = runBlocking {
            userService.createUser(User(0, "deleteuser", "delete@example.com"), "pwd")!!
        }

        val response = client.delete("/users/${createdUser.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("User deleted successfully", response.bodyAsText())

        assertNull(runBlocking { userService.getUserById(createdUser.id) }, "User should be deleted from DB")
    }

    @Test
    fun `DELETE users_{id} should fail for non-existing user`() = testApplication {
        application {
            configureTestEnvironment()
            routing { userRoutes(userService) }
        }
        val client = createClient {
            install(ContentNegotiation) { gson() }
        }
        val response = client.delete("/users/9999")
        // The service's deleteUser returns false for non-existing user,
        // The route translates this to InternalServerError. A 404 might be more appropriate.
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Failed to delete user", response.bodyAsText())
    }

}

// configureTestEnvironment is now imported from com.example.taskmanager.utils.DatabaseTestUtils
// If it was defined locally below this line, it should be removed.
