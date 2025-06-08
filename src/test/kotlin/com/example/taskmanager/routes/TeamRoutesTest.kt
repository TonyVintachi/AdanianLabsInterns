package com.example.taskmanager.routes

import com.example.taskmanager.db.TeamService
import com.example.taskmanager.db.UserService
import com.example.taskmanager.models.User
import com.example.taskmanager.routes.dto.*
import com.example.taskmanager.utils.DatabaseTestUtils
import com.example.taskmanager.utils.configureTestEnvironment
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class TeamRoutesTest {

    private val teamService = TeamService()
    private val userService = UserService()
    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()
        runBlocking {
            testUser = userService.createUser(User(0, "teamRouteUser", "teamroute@test.com"), "password")!!
        }
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `POST teams should create a new team`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }

        val request = TeamCreateRequest("New Test Team", testUser.id)
        val response = client.post("/teams") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val teamResponse = response.body<TeamResponse>()
        assertEquals("New Test Team", teamResponse.name)
        assertTrue(teamResponse.id > 0)

        // Verify creator is member
        val members = runBlocking { teamService.getTeamMembers(teamResponse.id) }
        assertTrue(members.any { it.id == testUser.id })
    }

    @Test
    fun `GET teams_{id} should retrieve team details`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdTeam = runBlocking { teamService.createTeam("GetMe Team", testUser.id)!! }

        val response = client.get("/teams/${createdTeam.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val teamResponse = response.body<TeamResponse>()
        assertEquals(createdTeam.name, teamResponse.name)
    }

    @Test
    fun `GET teams should retrieve all teams`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        runBlocking {
            teamService.createTeam("Team A", testUser.id)
            teamService.createTeam("Team B", testUser.id)
        }

        val response = client.get("/teams")
        assertEquals(HttpStatusCode.OK, response.status)
        val teams = response.body<List<TeamResponse>>()
        assertEquals(2, teams.size)
    }


    @Test
    fun `PUT teams_{id} should update team name`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdTeam = runBlocking { teamService.createTeam("Old Name", testUser.id)!! }
        val updateRequest = TeamUpdateRequest("New Name")

        val response = client.put("/teams/${createdTeam.id}") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Team updated successfully", response.bodyAsText())

        val updatedTeamInDb = runBlocking { teamService.getTeamById(createdTeam.id) }
        assertEquals("New Name", updatedTeamInDb?.name)
    }

    @Test
    fun `DELETE teams_{id} should delete a team`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdTeam = runBlocking { teamService.createTeam("DeleteMe Team", testUser.id)!! }

        val response = client.delete("/teams/${createdTeam.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Team deleted successfully", response.bodyAsText())
        assertNull(runBlocking { teamService.getTeamById(createdTeam.id) })
    }

    @Test
    fun `POST teams_{id}_members should add user to team`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val team = runBlocking { teamService.createTeam("Team For Members", testUser.id)!! } // testUser is already member
        val newUser = runBlocking { userService.createUser(User(0, "newMember", "member@test.com"), "pwd")!! }
        val request = TeamMemberRequest(newUser.id)

        val response = client.post("/teams/${team.id}/members") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("User added to team", response.bodyAsText())

        val members = runBlocking { teamService.getTeamMembers(team.id) }
        assertTrue(members.any { it.id == newUser.id } && members.any {it.id == testUser.id} )
        assertEquals(2, members.size)
    }

    @Test
    fun `DELETE teams_{teamId}_members_{userId} should remove user from team`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val team = runBlocking { teamService.createTeam("Team For Removal", testUser.id)!! }
        val userToRemove = runBlocking { userService.createUser(User(0, "removeMe", "remove@test.com"), "pwd")!! }
        runBlocking { teamService.addUserToTeam(userToRemove.id, team.id) } // Add user first

        val response = client.delete("/teams/${team.id}/members/${userToRemove.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("User removed from team", response.bodyAsText())

        val members = runBlocking { teamService.getTeamMembers(team.id) }
        assertFalse(members.any { it.id == userToRemove.id })
    }

    @Test
    fun `GET teams_{id}_members should retrieve team members`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val team = runBlocking { teamService.createTeam("Team With Members", testUser.id)!! }
        val anotherUser = runBlocking { userService.createUser(User(0, "anotherInTeam", "another@example.com"), "pwd")!! }
        runBlocking { teamService.addUserToTeam(anotherUser.id, team.id) }

        val response = client.get("/teams/${team.id}/members")
        assertEquals(HttpStatusCode.OK, response.status)
        val teamWithMembers = response.body<TeamWithMembersResponse>()
        assertEquals(team.id, teamWithMembers.id)
        assertEquals(team.name, teamWithMembers.name)
        assertEquals(2, teamWithMembers.members.size)
        assertTrue(teamWithMembers.members.any { it.id == testUser.id })
        assertTrue(teamWithMembers.members.any { it.id == anotherUser.id })
    }

    @Test
    fun `GET users_{userId}_teams should retrieve user's teams`() = testApplication {
        application {
            configureTestEnvironment()
            routing { teamRoutes(teamService, userService) } // Also user routes if separate
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        runBlocking {
            teamService.createTeam("UserTeam1", testUser.id)
            teamService.createTeam("UserTeam2", testUser.id)
        }

        val response = client.get("/users/${testUser.id}/teams")
        assertEquals(HttpStatusCode.OK, response.status)
        val teams = response.body<List<TeamResponse>>()
        assertEquals(2, teams.size)
        assertTrue(teams.any { it.name == "UserTeam1" })
    }
}
