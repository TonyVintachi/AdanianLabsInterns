package com.example.taskmanager.routes

import com.example.taskmanager.db.*
import com.example.taskmanager.models.*
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

class TaskListRoutesTest {

    private val taskListService = TaskListService()
    private val boardService = BoardService() // For setup
    private val teamService = TeamService()   // For setup
    private val userService = UserService()   // For setup

    private lateinit var testUser: User
    private lateinit var testTeam: Team
    private lateinit var testBoard: Board

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()
        runBlocking {
            testUser = userService.createUser(User(0, "tlRouteUser", "tlr@test.com"), "password")!!
            testTeam = teamService.createTeam("TLTest Team", testUser.id)!!
            testBoard = boardService.createBoard("TLTest Board", testTeam.id)!!
        }
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `POST tasklists should create a new task list`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskListRoutes(taskListService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }

        val request = TaskListCreateRequest("To Do", testBoard.id)
        val response = client.post("/tasklists") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val listResponse = response.body<TaskListResponse>()
        assertEquals("To Do", listResponse.name)
        assertEquals(testBoard.id, listResponse.boardId)
        assertEquals(1, listResponse.position) // First list
    }

    @Test
    fun `GET tasklists_{id} should retrieve task list details`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskListRoutes(taskListService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdList = runBlocking { taskListService.createTaskList("My List", testBoard.id)!! }

        val response = client.get("/tasklists/${createdList.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val listResponse = response.body<TaskListResponse>()
        assertEquals(createdList.name, listResponse.name)
        assertEquals(createdList.position, listResponse.position)
    }

    @Test
    fun `GET tasklists_{id} with non-existent id should return NotFound`() = testApplication {
         application {
            configureTestEnvironment()
            routing { taskListRoutes(taskListService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val response = client.get("/tasklists/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET boards_{boardId}_tasklists should retrieve lists for a board`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskListRoutes(taskListService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        runBlocking {
            taskListService.createTaskList("List A", testBoard.id) // Pos 1
            taskListService.createTaskList("List B", testBoard.id) // Pos 2
        }

        val response = client.get("/boards/${testBoard.id}/tasklists")
        assertEquals(HttpStatusCode.OK, response.status)
        val lists = response.body<List<TaskListResponse>>()
        assertEquals(2, lists.size)
        assertEquals("List A", lists[0].name) // Assuming order by position
        assertEquals(1, lists[0].position)
        assertEquals("List B", lists[1].name)
        assertEquals(2, lists[1].position)
    }

    @Test
    fun `PUT tasklists_{id} should update task list name and position`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskListRoutes(taskListService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdList = runBlocking { taskListService.createTaskList("Old Name", testBoard.id)!! } // Pos 1
        val updateRequest = TaskListUpdateRequest("New Name", 5)

        val response = client.put("/tasklists/${createdList.id}") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val updatedListResponse = response.body<TaskListResponse>()
        assertEquals("New Name", updatedListResponse.name)
        assertEquals(5, updatedListResponse.position)

        val updatedListInDb = runBlocking { taskListService.getTaskListById(createdList.id) }
        assertEquals("New Name", updatedListInDb?.name)
        assertEquals(5, updatedListInDb?.position)
    }

    @Test
    fun `DELETE tasklists_{id} should delete a task list`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskListRoutes(taskListService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdList = runBlocking { taskListService.createTaskList("Delete Me", testBoard.id)!! }

        val response = client.delete("/tasklists/${createdList.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Task list deleted successfully", response.bodyAsText())
        assertNull(runBlocking { taskListService.getTaskListById(createdList.id) })
    }
}
