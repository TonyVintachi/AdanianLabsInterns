package com.example.taskmanager.db

import com.example.taskmanager.models.User
import com.example.taskmanager.models.UserCalendarSyncInfo
import com.example.taskmanager.utils.DatabaseTestUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.*

class UserCalendarSyncServiceTest {

    private val userService = UserService() // To create a user first
    private val userCalendarSyncService = UserCalendarSyncService()
    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()
        runBlocking {
            testUser = userService.createUser(User(0, "syncUser-${System.nanoTime()}", "sync@test.com"), "password")!!
        }
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `saveOrUpdateSyncInfo should insert new sync info`() = runBlocking {
        val expiry = LocalDateTime.now().plusHours(1)
        val syncInfo = UserCalendarSyncInfo(
            userId = testUser.id,
            accessToken = "new_access_token",
            refreshToken = "new_refresh_token",
            tokenExpiry = expiry
        )

        userCalendarSyncService.saveOrUpdateSyncInfo(syncInfo)

        val retrievedInfo = userCalendarSyncService.getSyncInfoByUserId(testUser.id)
        assertNotNull(retrievedInfo)
        assertEquals("new_access_token", retrievedInfo.accessToken)
        assertEquals("new_refresh_token", retrievedInfo.refreshToken)
        assertEquals(expiry.withNano(0), retrievedInfo.tokenExpiry?.withNano(0)) // Compare without nanos for DB precision
    }

    @Test
    fun `saveOrUpdateSyncInfo should update existing sync info`() = runBlocking {
        val initialExpiry = LocalDateTime.now().plusHours(1)
        val initialSyncInfo = UserCalendarSyncInfo(
            userId = testUser.id,
            accessToken = "initial_access",
            refreshToken = "initial_refresh",
            tokenExpiry = initialExpiry
        )
        userCalendarSyncService.saveOrUpdateSyncInfo(initialSyncInfo)

        val updatedExpiry = LocalDateTime.now().plusHours(2)
        val updatedSyncInfo = UserCalendarSyncInfo(
            userId = testUser.id,
            accessToken = "updated_access",
            refreshToken = "updated_refresh", // Refresh token can also be updated
            tokenExpiry = updatedExpiry
        )
        userCalendarSyncService.saveOrUpdateSyncInfo(updatedSyncInfo)

        val retrievedInfo = userCalendarSyncService.getSyncInfoByUserId(testUser.id)
        assertNotNull(retrievedInfo)
        assertEquals("updated_access", retrievedInfo.accessToken)
        assertEquals("updated_refresh", retrievedInfo.refreshToken)
        assertEquals(updatedExpiry.withNano(0), retrievedInfo.tokenExpiry?.withNano(0))
    }

    @Test
    fun `saveOrUpdateSyncInfo should correctly handle null refresh token and expiry`() = runBlocking {
        val syncInfo = UserCalendarSyncInfo(
            userId = testUser.id,
            accessToken = "access_only",
            refreshToken = null,
            tokenExpiry = null
        )
        userCalendarSyncService.saveOrUpdateSyncInfo(syncInfo)
        var retrievedInfo = userCalendarSyncService.getSyncInfoByUserId(testUser.id)
        assertNotNull(retrievedInfo)
        assertEquals("access_only", retrievedInfo.accessToken)
        assertNull(retrievedInfo.refreshToken)
        assertNull(retrievedInfo.tokenExpiry)

        // Update it with values
        val expiry = LocalDateTime.now().plusHours(1)
        val updatedSyncInfo = UserCalendarSyncInfo(
            userId = testUser.id,
            accessToken = "access_again",
            refreshToken = "refresh_now",
            tokenExpiry = expiry
        )
        userCalendarSyncService.saveOrUpdateSyncInfo(updatedSyncInfo)
        retrievedInfo = userCalendarSyncService.getSyncInfoByUserId(testUser.id)
        assertNotNull(retrievedInfo)
        assertEquals("access_again", retrievedInfo.accessToken)
        assertEquals("refresh_now", retrievedInfo.refreshToken)
        assertNotNull(retrievedInfo.tokenExpiry)
    }


    @Test
    fun `getSyncInfoByUserId should return null if no info exists`() = runBlocking {
        val retrievedInfo = userCalendarSyncService.getSyncInfoByUserId(testUser.id)
        assertNull(retrievedInfo)

        val nonExistentUserId = 999
        val nonExistentInfo = userCalendarSyncService.getSyncInfoByUserId(nonExistentUserId)
        assertNull(nonExistentInfo)
    }

    @Test
    fun `deleteSyncInfoByUserId should remove existing sync info`() = runBlocking {
        val syncInfo = UserCalendarSyncInfo(
            userId = testUser.id,
            accessToken = "to_delete_access",
            refreshToken = "to_delete_refresh",
            tokenExpiry = LocalDateTime.now().plusHours(1)
        )
        userCalendarSyncService.saveOrUpdateSyncInfo(syncInfo)

        var retrievedInfo = userCalendarSyncService.getSyncInfoByUserId(testUser.id)
        assertNotNull(retrievedInfo, "Sync info should exist before deletion.")

        val deleted = userCalendarSyncService.deleteSyncInfoByUserId(testUser.id)
        assertTrue(deleted, "Deletion should be successful for existing info.")

        retrievedInfo = userCalendarSyncService.getSyncInfoByUserId(testUser.id)
        assertNull(retrievedInfo, "Sync info should not exist after deletion.")
    }

    @Test
    fun `deleteSyncInfoByUserId should return false if no info exists to delete`() = runBlocking {
        val deleted = userCalendarSyncService.deleteSyncInfoByUserId(testUser.id)
        assertFalse(deleted, "Deletion should fail or return false for non-existing info.")
    }
}
