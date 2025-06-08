package com.example.taskmanager.db

import com.example.taskmanager.models.User
import com.example.taskmanager.models.Team
import com.example.taskmanager.utils.DatabaseTestUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

class BoardServiceTest {

    private val userService = UserService()
    private val teamService = TeamService()
    private val boardService = BoardService()

    private lateinit var testUser: User
    private lateinit var testTeam: Team

    @BeforeEach
    fun setup() {
        DatabaseTestUtils.initTestDatabase()
        DatabaseTestUtils.clearAllTables()

        runBlocking {
            testUser = userService.createUser(User(0, "boardUser-${System.nanoTime()}", "board-${System.nanoTime()}@test.com"), "pwd")!!
            testTeam = teamService.createTeam("Test Team for Boards", testUser.id)!!
        }
    }

    @AfterEach
    fun tearDown() {
        DatabaseTestUtils.clearAllTables()
    }

    @Test
    fun `createBoard should create a board associated with a team`() = runBlocking {
        val boardName = "Project Alpha Board"
        val createdBoard = boardService.createBoard(boardName, testTeam.id)

        assertNotNull(createdBoard)
        assertEquals(boardName, createdBoard.name)
        assertEquals(testTeam.id, createdBoard.teamId)
        assertTrue(createdBoard.id > 0)

        val fetchedBoard = boardService.getBoardById(createdBoard.id)
        assertNotNull(fetchedBoard)
        assertEquals(boardName, fetchedBoard.name)
    }

    @Test
    fun `getBoardById should retrieve an existing board`() = runBlocking {
        val board = boardService.createBoard("My Board", testTeam.id)!!
        val foundBoard = boardService.getBoardById(board.id)
        assertNotNull(foundBoard)
        assertEquals(board.name, foundBoard.name)
    }

    @Test
    fun `getBoardById should return null for non-existing board`() = runBlocking {
        val foundBoard = boardService.getBoardById(999)
        assertNull(foundBoard)
    }

    @Test
    fun `getBoardsByTeam should return all boards for a specific team`() = runBlocking {
        boardService.createBoard("Board 1", testTeam.id)!!
        boardService.createBoard("Board 2", testTeam.id)!!

        // Create another team and a board for it to ensure we only get boards for testTeam.id
        val anotherUser = userService.createUser(User(0, "anotherBoardUser", "another@board.com"), "pwd")!!
        val anotherTeam = teamService.createTeam("Another Team", anotherUser.id)!!
        boardService.createBoard("Board 3", anotherTeam.id)!!

        val teamBoards = boardService.getBoardsByTeam(testTeam.id)
        assertEquals(2, teamBoards.size)
        assertTrue(teamBoards.all { it.teamId == testTeam.id })
    }

    @Test
    fun `getBoardsByTeam should return empty list for team with no boards`() = runBlocking {
        val teamBoards = boardService.getBoardsByTeam(testTeam.id)
        assertTrue(teamBoards.isEmpty())
    }


    @Test
    fun `updateBoardName should change the name of an existing board`() = runBlocking {
        val board = boardService.createBoard("Old Board Name", testTeam.id)!!
        val newName = "New Board Name"

        val updated = boardService.updateBoardName(board.id, newName)
        assertTrue(updated)

        val fetchedBoard = boardService.getBoardById(board.id)
        assertEquals(newName, fetchedBoard?.name)
    }

    @Test
    fun `updateBoardName should return false for non-existing board`() = runBlocking {
        val updated = boardService.updateBoardName(999, "New Name")
        assertFalse(updated)
    }

    @Test
    fun `deleteBoard should remove a board`() = runBlocking {
        val board = boardService.createBoard("Board to Delete", testTeam.id)!!

        val deleted = boardService.deleteBoard(board.id)
        assertTrue(deleted)

        assertNull(boardService.getBoardById(board.id))
    }

    @Test
    fun `deleteBoard should return false for non-existing board`() = runBlocking {
        val deleted = boardService.deleteBoard(999)
        assertFalse(deleted)
    }

    @Test
    fun `createBoard should fail for non-existent teamId`() = runBlocking {
        // Current BoardService.createBoard does not explicitly check if teamId exists.
        // The database foreign key constraint on BoardTable.teamId would cause the insert to fail.
        // The dbQuery and insert handling in BaseService might translate this to null or throw.
        // Assuming it results in a null return from createBoard if underlying insert fails.
        val boardName = "Orphan Board"
        var createdBoard = boardService.createBoard(boardName, 999) // 999 is a non-existent teamId

        // If Exposed/JDBC throws an exception due to FK constraint, this test needs to catch it.
        // If service returns null on such failure:
        assertNull(createdBoard, "Board creation should fail if the teamId does not exist.")
    }
}
