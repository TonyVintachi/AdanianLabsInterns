package com.example.taskmanager.db

import com.example.taskmanager.models.User
import com.example.taskmanager.models.Team
import com.example.taskmanager.models.Board
import com.example.taskmanager.utils.DatabaseTestUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class TaskListServiceTest {

    private val userService = UserService()
    private val teamService = TeamService()
    private val boardService = BoardService()
    private val taskListService = TaskListService()

    private lateinit var testUser: User
    private lateinit var testTeam: Team
    private lateinit var testBoard: Board

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()

        runBlocking {
            testUser = userService.createUser(User(0, "taskListUser-${System.nanoTime()}", "tl-${System.nanoTime()}@test.com"), "pwd")!!
            testTeam = teamService.createTeam("Test Team for TaskLists", testUser.id)!!
            testBoard = boardService.createBoard("Test Board for TaskLists", testTeam.id)!!
        }
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `createTaskList should create a list on a board and set initial position`() = runBlocking {
        val listName = "To Do"
        val createdList = taskListService.createTaskList(listName, testBoard.id)

        assertNotNull(createdList)
        assertEquals(listName, createdList.name)
        assertEquals(testBoard.id, createdList.boardId)
        assertEquals(1, createdList.position, "First list should be at position 1")

        val fetchedList = taskListService.getTaskListById(createdList.id)
        assertNotNull(fetchedList)
        assertEquals(listName, fetchedList.name)
    }

    @Test
    fun `createTaskList should increment position for subsequent lists`() = runBlocking {
        taskListService.createTaskList("List 1", testBoard.id) // Position 1
        val list2 = taskListService.createTaskList("List 2", testBoard.id) // Position 2
        val list3 = taskListService.createTaskList("List 3", testBoard.id) // Position 3

        assertNotNull(list2)
        assertEquals(2, list2.position)
        assertNotNull(list3)
        assertEquals(3, list3.position)
    }

    @Test
    fun `getTaskListById should retrieve an existing task list`() = runBlocking {
        val list = taskListService.createTaskList("My List", testBoard.id)!!
        val foundList = taskListService.getTaskListById(list.id)
        assertNotNull(foundList)
        assertEquals(list.name, foundList.name)
        assertEquals(list.position, foundList.position)
    }

    @Test
    fun `getTaskListById should return null for non-existing task list`() = runBlocking {
        assertNull(taskListService.getTaskListById(999))
    }

    @Test
    fun `getTaskListsByBoard should return lists ordered by position`() = runBlocking {
        val list3 = taskListService.createTaskList("List C", testBoard.id)!! // Pos 1
        val list1 = taskListService.createTaskList("List A", testBoard.id)!! // Pos 2
        val list2 = taskListService.createTaskList("List B", testBoard.id)!! // Pos 3

        // Manually update positions to test ordering if create doesn't allow specific position setting
        // For this test, we rely on auto-incrementing positions from create
        // To truly test order by position, one might need to update positions after creation
        // or assume createTaskList positions correctly (1, 2, 3)

        val lists = taskListService.getTaskListsByBoard(testBoard.id)
        assertEquals(3, lists.size)
        assertEquals(list3.id, lists[0].id) // "List C" was created first, so position 1
        assertEquals(list1.id, lists[1].id) // "List A" was created second, so position 2
        assertEquals(list2.id, lists[2].id) // "List B" was created third, so position 3
    }

    @Test
    fun `updateTaskListName should change the name of a list`() = runBlocking {
        val list = taskListService.createTaskList("Old Name", testBoard.id)!!
        val newName = "New Name"

        val updated = taskListService.updateTaskListName(list.id, newName)
        assertTrue(updated)

        val fetchedList = taskListService.getTaskListById(list.id)
        assertEquals(newName, fetchedList?.name)
    }

    @Test
    fun `updateTaskListPosition should change the position of a list`() = runBlocking {
        // Note: This test assumes a simple position update.
        // A full reordering logic is more complex and not yet implemented in the service.
        val list = taskListService.createTaskList("My List", testBoard.id)!! // Position 1
        val newPosition = 5

        val updated = taskListService.updateTaskListPosition(list.id, newPosition)
        assertTrue(updated)

        val fetchedList = taskListService.getTaskListById(list.id)
        assertEquals(newPosition, fetchedList?.position)
    }

    @Test
    fun `updateTaskListPosition should handle non-existing list`() = runBlocking {
        val updated = taskListService.updateTaskListPosition(999, 2)
        assertFalse(updated)
    }


    @Test
    fun `deleteTaskList should remove a task list`() = runBlocking {
        val list = taskListService.createTaskList("To Delete", testBoard.id)!!
        val deleted = taskListService.deleteTaskList(list.id)
        assertTrue(deleted)
        assertNull(taskListService.getTaskListById(list.id))
    }

    @Test
    fun `deleteTaskList should return false for non-existing list`() = runBlocking {
        assertFalse(taskListService.deleteTaskList(999))
    }

    @Test
    fun `createTaskList should fail for non-existent boardId`() = runBlocking {
        // Similar to BoardService, relies on FK constraint in DB
        // Service method expected to return null if insert fails
        val listName = "Orphan List"
        val createdList = taskListService.createTaskList(listName, 999) // 999 is non-existent boardId
        assertNull(createdList, "TaskList creation should fail if the boardId does not exist.")
    }
}
