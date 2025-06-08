package com.example.taskmanager.db

import com.example.taskmanager.models.User
import com.example.taskmanager.utils.DatabaseTestUtils
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class UserServiceTest {

    private val userService = UserService()

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase() // Ensures DB is connected and schema is ready
        DatabaseTestUtils.clearAllTables() // Clears data before each test
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables() // Clears data after each test
    }

    @Test
    fun `createUser should add a new user and return it`() = runBlocking {
        val userModel = User(id = 0, username = "testuser", email = "test@example.com")
        val passwordHash = "password123"

        val createdUser = userService.createUser(userModel, passwordHash)

        assertNotNull(createdUser)
        assertEquals("testuser", createdUser.username)
        assertEquals("test@example.com", createdUser.email)
        assertTrue(createdUser.id > 0)

        // Verify password hash is stored (though we don't expose it in User model)
        val storedHash = userService.getUserPasswordHash(createdUser.id)
        assertEquals(passwordHash, storedHash)
    }

    @Test
    fun `getUserById should retrieve an existing user`() = runBlocking {
        val userModel = User(id = 0, username = "testuser", email = "test@example.com")
        val createdUser = userService.createUser(userModel, "password123")
        assertNotNull(createdUser)

        val foundUser = userService.getUserById(createdUser.id)

        assertNotNull(foundUser)
        assertEquals(createdUser.id, foundUser.id)
        assertEquals(createdUser.username, foundUser.username)
    }

    @Test
    fun `getUserById should return null for non-existing user`() = runBlocking {
        val foundUser = userService.getUserById(999)
        assertNull(foundUser)
    }

    @Test
    fun `getUserByUsername should retrieve an existing user`() = runBlocking {
        val userModel = User(id = 0, username = "testuser", email = "test@example.com")
        userService.createUser(userModel, "password123")

        val foundUser = userService.getUserByUsername("testuser")

        assertNotNull(foundUser)
        assertEquals("testuser", foundUser.username)
    }

    @Test
    fun `getUserByUsername should return null for non-existing username`() = runBlocking {
        val foundUser = userService.getUserByUsername("nonexistent")
        assertNull(foundUser)
    }

    @Test
    fun `updateUser should modify user details with correct current password`() = runBlocking {
        val userModel = User(id = 0, username = "testuser", email = "initial@example.com")
        val initialPassword = "password123"
        val createdUser = userService.createUser(userModel, initialPassword)
        assertNotNull(createdUser)

        val newEmail = "updated@example.com"
        val newPassword = "newPassword456"

        val updated = userService.updateUser(createdUser.id, newEmail, initialPassword, newPassword)
        assertTrue(updated)

        val fetchedUser = userService.getUserById(createdUser.id)
        assertNotNull(fetchedUser)
        assertEquals(newEmail, fetchedUser.email)

        val storedNewHash = userService.getUserPasswordHash(createdUser.id)
        assertEquals(newPassword, storedNewHash)
    }

    @Test
    fun `updateUser should only update email if new password is null`() = runBlocking {
        val userModel = User(id = 0, username = "testuser", email = "initial@example.com")
        val initialPassword = "password123"
        val createdUser = userService.createUser(userModel, initialPassword)
        assertNotNull(createdUser)

        val newEmail = "updated@example.com"

        val updated = userService.updateUser(createdUser.id, newEmail, initialPassword, null)
        assertTrue(updated)

        val fetchedUser = userService.getUserById(createdUser.id)
        assertNotNull(fetchedUser)
        assertEquals(newEmail, fetchedUser.email) // Email updated

        val storedHash = userService.getUserPasswordHash(createdUser.id)
        assertEquals(initialPassword, storedHash) // Password should not change
    }


    @Test
    fun `updateUser should fail with incorrect current password`() = runBlocking {
        val userModel = User(id = 0, username = "testuser", email = "initial@example.com")
        val createdUser = userService.createUser(userModel, "password123")
        assertNotNull(createdUser)

        val updated = userService.updateUser(createdUser.id, "updated@example.com", "wrongPassword", "newPassword456")
        assertFalse(updated)
    }

    @Test
    fun `updateUser should fail if current password is required but not provided`() = runBlocking {
        val userModel = User(id = 0, username = "testuser", email = "initial@example.com")
        val createdUser = userService.createUser(userModel, "password123")
        assertNotNull(createdUser)

        // Attempting to change email without providing currentPasswordHash
        val updated = userService.updateUser(createdUser.id, "updated@example.com", null, null)
        assertFalse(updated, "Update should fail if current password is required for email change but not provided.")

        // Attempting to change password without providing currentPasswordHash
        val updated2 = userService.updateUser(createdUser.id, null, null, "newPassword")
        assertFalse(updated2, "Update should fail if current password is required for password change but not provided.")
    }


    @Test
    fun `deleteUser should remove a user`() = runBlocking {
        val userModel = User(id = 0, username = "testuser", email = "test@example.com")
        val createdUser = userService.createUser(userModel, "password123")
        assertNotNull(createdUser)

        val deleted = userService.deleteUser(createdUser.id)
        assertTrue(deleted)

        val foundUser = userService.getUserById(createdUser.id)
        assertNull(foundUser)
    }

    @Test
    fun `deleteUser should return false for non-existing user`() = runBlocking {
        val deleted = userService.deleteUser(999)
        assertFalse(deleted)
    }

    @Test
    fun `creating a user with an existing username should fail`() = runBlocking {
        val userModel1 = User(id = 0, username = "duplicateuser", email = "test1@example.com")
        userService.createUser(userModel1, "password123")

        val userModel2 = User(id = 0, username = "duplicateuser", email = "test2@example.com")
        var exceptionThrown = false
        try {
            // Exposed typically throws an exception on unique constraint violation during insert
            // The service's createUser might return null or propagate the exception depending on its error handling.
            // Current implementation of createUser returns null if insertStatement.resultedValues is null or empty.
            val createdUser2 = userService.createUser(userModel2, "password456")
            assertNull(createdUser2, "createUser should return null for duplicate username if underlying insert fails that way.")
            // If it's expected to throw, then the test changes:
            // fail("Should have thrown an exception for duplicate username")
        } catch (e: Exception) {
            // Depending on DB and Exposed settings, this might be a specific ExposedSQLException or PSQLException, etc.
            // For H2, it's often an org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
            // Checking for a generic Exception here for broader compatibility in test,
            // but specific exception check is better if known.
            exceptionThrown = true // If service is designed to throw.
                                   // Current service design catches this and returns null. So this path won't be hit.
        }
        // Based on current service impl:
        // assertTrue(exceptionThrown, "Exception for duplicate username was expected.")
        // No exception expected, null is returned by service. The assertNull above handles this.
    }
}
