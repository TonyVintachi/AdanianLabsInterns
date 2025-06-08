package com.example.taskmanager.db

import com.example.taskmanager.models.User
import com.example.taskmanager.utils.DatabaseTestUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.*

// No @TestInstance, default is PER_METHOD
class TeamServiceTest {

    private val userService = UserService()
    private val teamService = TeamService()
    private lateinit var testUser: User // Will be created fresh for each test

    @BeforeEach
    fun setup() { // Runs before each test method
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables() // Clear all data

        // Create a fresh user for each test to act as creator/member
        runBlocking {
            testUser = userService.createUser(
                User(0, "testUser-${System.nanoTime()}", "user-${System.nanoTime()}@test.com"),
                "password"
            )!!
        }
    }

    @AfterEach
    fun tearDown() { // Runs after each test method
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `createTeam should create a team and add creator as member`() = runBlocking {
        val teamName = "Awesome Team"
        val createdTeam = teamService.createTeam(teamName, testUser.id)

        assertNotNull(createdTeam)
        assertEquals(teamName, createdTeam.name)
        assertTrue(createdTeam.id > 0)

        val members = teamService.getTeamMembers(createdTeam.id)
        assertEquals(1, members.size)
        assertEquals(testUser.id, members.first().id)

        val userTeams = teamService.getUserTeams(testUser.id)
        assertEquals(1, userTeams.size)
        assertEquals(createdTeam.id, userTeams.first().id)
    }

    @Test
    fun `getTeamById should retrieve an existing team`() = runBlocking {
        val team = teamService.createTeam("Test Team", testUser.id)!!
        val foundTeam = teamService.getTeamById(team.id)
        assertNotNull(foundTeam)
        assertEquals(team.name, foundTeam.name)
    }

    @Test
    fun `getTeamById should return null for non-existing team`() = runBlocking {
        val foundTeam = teamService.getTeamById(999)
        assertNull(foundTeam)
    }

    @Test
    fun `getAllTeams should return all created teams`() = runBlocking {
        teamService.createTeam("Team Alpha", testUser.id)
        teamService.createTeam("Team Beta", testUser.id)

        val teams = teamService.getAllTeams()
        assertEquals(2, teams.size)
    }

    @Test
    fun `updateTeamName should change the name of an existing team`() = runBlocking {
        val team = teamService.createTeam("Original Name", testUser.id)!!
        val newName = "Updated Name"

        val updated = teamService.updateTeamName(team.id, newName)
        assertTrue(updated)

        val fetchedTeam = teamService.getTeamById(team.id)
        assertEquals(newName, fetchedTeam?.name)
    }

    @Test
    fun `updateTeamName should return false for non-existing team`() = runBlocking {
         val updated = teamService.updateTeamName(999, "New Name")
         assertFalse(updated)
    }

    @Test
    fun `deleteTeam should remove the team and its memberships`() = runBlocking {
        val team = teamService.createTeam("Team To Delete", testUser.id)!!
        // Add another user to the team to test membership deletion
        val anotherUser = userService.createUser(User(0, "anotherUser", "another@test.com"), "pwd")!!
        teamService.addUserToTeam(anotherUser.id, team.id)

        val deleted = teamService.deleteTeam(team.id)
        assertTrue(deleted)

        assertNull(teamService.getTeamById(team.id))
        assertTrue(teamService.getTeamMembers(team.id).isEmpty())
        assertTrue(teamService.getUserTeams(testUser.id).isEmpty())
        assertTrue(teamService.getUserTeams(anotherUser.id).isEmpty())
    }

    @Test
    fun `addUserToTeam and removeUserFromTeam should manage team membership`() = runBlocking {
        val team = teamService.createTeam("Membership Test Team", testUser.id)!! // testUser is already a member
        val newUser = userService.createUser(User(0, "newUser", "new@test.com"), "password")!!

        // Add newUser
        var added = teamService.addUserToTeam(newUser.id, team.id)
        assertTrue(added)
        var members = teamService.getTeamMembers(team.id)
        assertEquals(2, members.size)
        assertTrue(members.any { it.id == newUser.id })

        // Try adding again (should return false or not change count)
        added = teamService.addUserToTeam(newUser.id, team.id) // insertIgnore handles this
        assertFalse(added, "Adding an existing user again should return false as no new row was inserted.")
        members = teamService.getTeamMembers(team.id)
        assertEquals(2, members.size)


        // Remove newUser
        val removed = teamService.removeUserFromTeam(newUser.id, team.id)
        assertTrue(removed)
        members = teamService.getTeamMembers(team.id)
        assertEquals(1, members.size)
        assertFalse(members.any { it.id == newUser.id })
    }

    @Test
    fun `addUserToTeam should fail if user or team does not exist`() = runBlocking {
        // Valid team, non-existent user
        val team = teamService.createTeam("Test Team", testUser.id)!!
        var added = teamService.addUserToTeam(999, team.id) // Assuming user 999 does not exist
        assertFalse(added, "Should fail to add non-existent user to team.") // Fails due to FK constraint

        // Non-existent team, valid user
        val user = userService.createUser(User(0, "someUser", "some@user.com"), "pwd")!!
        added = teamService.addUserToTeam(user.id, 999) // Assuming team 999 does not exist
        assertFalse(added, "Should fail to add user to non-existent team.") // Fails due to FK constraint
    }


    @Test
    fun `getTeamMembers should return users in a team`() = runBlocking {
        val team = teamService.createTeam("Team With Members", testUser.id)!!
        val user2 = userService.createUser(User(0, "user2", "user2@test.com"), "pwd")!!
        teamService.addUserToTeam(user2.id, team.id)

        val members = teamService.getTeamMembers(team.id)
        assertEquals(2, members.size)
        assertTrue(members.any { it.id == testUser.id })
        assertTrue(members.any { it.id == user2.id })
    }

    @Test
    fun `getUserTeams should return teams a user belongs to`() = runBlocking {
        val team1 = teamService.createTeam("User's Team 1", testUser.id)!!
        val team2 = teamService.createTeam("User's Team 2", testUser.id)!!

        val anotherUser = userService.createUser(User(0, "another", "another@test.com"), "pwd")!!
        teamService.createTeam("Another's Team", anotherUser.id) // Team testUser is not part of

        val userTeams = teamService.getUserTeams(testUser.id)
        assertEquals(2, userTeams.size)
        assertTrue(userTeams.any { it.id == team1.id })
        assertTrue(userTeams.any { it.id == team2.id })
    }
}
