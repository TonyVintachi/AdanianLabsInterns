package com.example.taskmanager.routes

import com.example.taskmanager.db.BoardService
import com.example.taskmanager.db.TeamService
import com.example.taskmanager.db.UserService
import com.example.taskmanager.models.Team
import com.example.taskmanager.models.User
import com.example.taskmanager.routes.dto.BoardCreateRequest
import com.example.taskmanager.routes.dto.BoardResponse
import com.example.taskmanager.routes.dto.BoardUpdateRequest
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

class BoardRoutesTest {

    private val boardService = BoardService()
    private val teamService = TeamService() // Needed to create a team first
    private val userService = UserService() // Needed for team creation

    private lateinit var testUser: User
    private lateinit var testTeam: Team

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()
        runBlocking {
            testUser = userService.createUser(User(0, "boardRouteUser", "br@test.com"), "password")!!
            testTeam = teamService.createTeam("BoardTest Team", testUser.id)!!
        }
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `POST boards should create a new board`() = testApplication {
        application {
            configureTestEnvironment()
            routing { boardRoutes(boardService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }

        val request = BoardCreateRequest("Project X Board", testTeam.id)
        val response = client.post("/boards") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val boardResponse = response.body<BoardResponse>()
        assertEquals("Project X Board", boardResponse.name)
        assertEquals(testTeam.id, boardResponse.teamId)
    }

    @Test
    fun `GET boards_{id} should retrieve board details`() = testApplication {
        application {
            configureTestEnvironment()
            routing { boardRoutes(boardService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdBoard = runBlocking { boardService.createBoard("My Test Board", testTeam.id)!! }

        val response = client.get("/boards/${createdBoard.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val boardResponse = response.body<BoardResponse>()
        assertEquals(createdBoard.name, boardResponse.name)
        assertEquals(testTeam.id, boardResponse.teamId)
    }

    @Test
    fun `GET boards_{id} with non-existent id should return NotFound`() = testApplication {
        application {
            configureTestEnvironment()
            routing { boardRoutes(boardService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val response = client.get("/boards/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }


    @Test
    fun `GET teams_{teamId}_boards should retrieve all boards for a team`() = testApplication {
        application {
            configureTestEnvironment()
            routing { boardRoutes(boardService) } // Assumes teamId is valid from setup
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        runBlocking {
            boardService.createBoard("Board Alpha", testTeam.id)
            boardService.createBoard("Board Beta", testTeam.id)
        }

        val response = client.get("/teams/${testTeam.id}/boards")
        assertEquals(HttpStatusCode.OK, response.status)
        val boards = response.body<List<BoardResponse>>()
        assertEquals(2, boards.size)
        assertTrue(boards.all { it.teamId == testTeam.id })
    }

    @Test
    fun `PUT boards_{id} should update board name`() = testApplication {
        application {
            configureTestEnvironment()
            routing { boardRoutes(boardService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdBoard = runBlocking { boardService.createBoard("Initial Board Name", testTeam.id)!! }
        val updateRequest = BoardUpdateRequest("Updated Board Name")

        val response = client.put("/boards/${createdBoard.id}") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Board updated successfully", response.bodyAsText())

        val updatedBoardInDb = runBlocking { boardService.getBoardById(createdBoard.id) }
        assertEquals("Updated Board Name", updatedBoardInDb?.name)
    }

    @Test
    fun `DELETE boards_{id} should delete a board`() = testApplication {
        application {
            configureTestEnvironment()
            routing { boardRoutes(boardService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdBoard = runBlocking { boardService.createBoard("Board To Be Deleted", testTeam.id)!! }

        val response = client.delete("/boards/${createdBoard.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Board deleted successfully", response.bodyAsText())
        assertNull(runBlocking { boardService.getBoardById(createdBoard.id) })
    }
}
