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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.test.*

class TaskRoutesTest {

    private val taskService = TaskService()
    private val taskListService = TaskListService() // For setup
    private val boardService = BoardService()     // For setup
    private val teamService = TeamService()       // For setup
    private val userService = UserService()       // For setup

    private lateinit var testUserCreator: User
    private lateinit var testUserAssignee: User
    private lateinit var testTeam: Team
    private lateinit var testBoard: Board
    private lateinit var testTaskList: TaskList
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()
        runBlocking {
            testUserCreator = userService.createUser(User(0, "taskRouteCreator", "trc@test.com"), "pwd1")!!
            testUserAssignee = userService.createUser(User(0, "taskRouteAssignee", "tra@test.com"), "pwd2")!!
            testTeam = teamService.createTeam("TaskTest Team", testUserCreator.id)!!
            testBoard = boardService.createBoard("TaskTest Board", testTeam.id)!!
            testTaskList = taskListService.createTaskList("TaskTest List", testBoard.id)!!
        }
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `POST tasks should create a new task`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskRoutes(taskService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }

        val dueDate = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS)
        val request = TaskCreateRequest(
            title = "New API Task",
            description = "API Task Description",
            taskListId = testTaskList.id,
            creatorId = testUserCreator.id,
            assigneeId = testUserAssignee.id,
            dueDate = dueDate.format(dateTimeFormatter)
        )

        val response = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val taskResponse = response.body<TaskResponse>()
        assertEquals("New API Task", taskResponse.title)
        assertEquals(testUserAssignee.id, taskResponse.assigneeId)
        assertEquals(dueDate.format(dateTimeFormatter), taskResponse.dueDate)
        assertEquals(1, taskResponse.position) // First task in list
    }

    @Test
    fun `GET tasks_{id} should retrieve task details`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskRoutes(taskService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdTask = runBlocking {
            taskService.createTask("GetMe Task", null, testTaskList.id, testUserCreator.id, null, null)!!
        }

        val response = client.get("/tasks/${createdTask.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val taskResponse = response.body<TaskResponse>()
        assertEquals(createdTask.title, taskResponse.title)
        assertEquals(createdTask.position, taskResponse.position)
    }

    @Test
    fun `GET tasks_{id} with non-existent id should return NotFound`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskRoutes(taskService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val response = client.get("/tasks/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }


    @Test
    fun `GET tasklists_{taskListId}_tasks should retrieve tasks for a list`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskRoutes(taskService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        runBlocking {
            taskService.createTask("Task A", null, testTaskList.id, testUserCreator.id, null, null) // Pos 1
            taskService.createTask("Task B", null, testTaskList.id, testUserCreator.id, null, null) // Pos 2
        }

        val response = client.get("/tasklists/${testTaskList.id}/tasks")
        assertEquals(HttpStatusCode.OK, response.status)
        val tasks = response.body<List<TaskResponse>>()
        assertEquals(2, tasks.size)
        assertEquals("Task A", tasks[0].title) // Assuming order by position
    }

    @Test
    fun `PUT tasks_{id} should update task details`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskRoutes(taskService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdTask = runBlocking {
            taskService.createTask("Initial Task", "Desc", testTaskList.id, testUserCreator.id, null, null)!!
        }
        val newDueDate = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.SECONDS)
        val updateRequest = TaskUpdateRequest(
            title = "Updated Task Title",
            description = "Updated Desc",
            assigneeId = testUserAssignee.id,
            dueDate = newDueDate.format(dateTimeFormatter),
            position = 2,
            clearDescription = false, // Explicitly false
            clearAssigneeId = false,
            clearDueDate = false
        )

        val response = client.put("/tasks/${createdTask.id}") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val updatedTaskResponse = response.body<TaskResponse>()
        assertEquals("Updated Task Title", updatedTaskResponse.title)
        assertEquals("Updated Desc", updatedTaskResponse.description)
        assertEquals(testUserAssignee.id, updatedTaskResponse.assigneeId)
        assertEquals(newDueDate.format(dateTimeFormatter), updatedTaskResponse.dueDate)
        assertEquals(2, updatedTaskResponse.position)
    }

    @Test
    fun `PUT tasks_{id} should clear nullable fields`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskRoutes(taskService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val initialDueDate = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS)
        val createdTask = runBlocking {
            taskService.createTask("Task To Clear Fields", "Desc to clear", testTaskList.id, testUserCreator.id, testUserAssignee.id, initialDueDate)!!
        }
        val updateRequest = TaskUpdateRequest(
            title = "Title Remains",
            clearDescription = true,
            clearAssigneeId = true,
            clearDueDate = true
        )
        val response = client.put("/tasks/${createdTask.id}") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val updatedTaskResponse = response.body<TaskResponse>()
        assertEquals("Title Remains", updatedTaskResponse.title)
        assertNull(updatedTaskResponse.description)
        assertNull(updatedTaskResponse.assigneeId)
        assertNull(updatedTaskResponse.dueDate)
    }


    @Test
    fun `DELETE tasks_{id} should delete a task`() = testApplication {
        application {
            configureTestEnvironment()
            routing { taskRoutes(taskService) }
        }
        val client = createClient { install(ContentNegotiation) { gson() } }
        val createdTask = runBlocking {
            taskService.createTask("Delete This Task", null, testTaskList.id, testUserCreator.id, null, null)!!
        }

        val response = client.delete("/tasks/${createdTask.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Task deleted successfully", response.bodyAsText())
        assertNull(runBlocking { taskService.getTaskById(createdTask.id) })
    }
}
