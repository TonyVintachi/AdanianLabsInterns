package com.example.taskmanager.utils

import com.example.taskmanager.db.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object DatabaseTestUtils {

    private var dbInitialized = false

    // Services (add all your services here)
    private val userService = UserService()
    private val teamService = TeamService()
    private val boardService = BoardService()
    private val taskListService = TaskListService()
    private val taskService = TaskService()
    private val eventService = EventService() // Add EventService
    private val userCalendarSyncService = UserCalendarSyncService() // Add UserCalendarSyncService

    private val allTables = arrayOf(
        EventTable, UserCalendarSyncInfoTable, // Add new tables
        TaskTable, TaskListTable, BoardTable, TeamMemberTable, TeamTable, UserTable
    )

    fun initTestDatabase() {
        if (!dbInitialized) {
            // Use an in-memory H2 database for tests
            // The DB_CLOSE_DELAY=-1 ensures the DB is not lost between connections if multiple are made in a test suite
            // For Ktor tests with testApplication, each test often runs in a somewhat isolated environment,
            // so re-initializing might be necessary or ensuring a persistent in-memory DB for the test suite.
            // Using a unique name for test DB to avoid clashes if main DB is also H2 mem.
            val driver = "org.h2.Driver"
            val url = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
            val user = "sa"
            val password = ""
            Database.connect(url, driver, user, password)

            // Initialize schema via services
            runBlocking {
                userService.init()
                teamService.init()
                boardService.init()
                taskListService.init()
                taskService.init()
                eventService.init() // Initialize EventService
                userCalendarSyncService.init() // Initialize UserCalendarSyncService
            }
            dbInitialized = true // Mark as initialized for the current test run context
                                 // This might need adjustment if tests run in separate classloaders/JVMs
        } else {
             // If already initialized, ensure tables are present (they should be)
             // This logic might be simplified if tests ensure initTestDatabase() is called appropriately once per suite run.
            transaction {
                SchemaUtils.createMissingTablesAndColumns(*allTables)
            }
        }
    }

    fun reinitializeDatabase() {
        // For H2 in-memory, simply clearing tables and re-running schema creation if needed is often enough.
        // Aggressive disconnect/reconnect can be complex.
        dbInitialized = false // Force re-evaluation of schema creation logic in initTestDatabase
        initTestDatabase()
    }


    fun clearAllTables() {
        if (!dbInitialized) {
            // If db was not initialized (e.g. test calls clear before init),
            // ensure it's up so transaction can run, then clear.
            // This path should ideally not be hit if @BeforeEach calls initTestDatabase.
            initTestDatabase()
        }
        transaction {
            // Disable foreign key checks for H2 if needed for out-of-order deletion, then re-enable
            // For H2, it's usually:
            // TransactionManager.current().connection.createStatement().executeUpdate("SET REFERENTIAL_INTEGRITY FALSE")
            // However, Exposed should handle deletion order correctly if tables are in `allTables` in reverse order of dependency.
            // Let's try with direct deleteAll first.
            // Order: Task -> TaskList -> Board -> TeamMember -> Team / User (User might be tricky if TeamMember not deleted)
            // Delete in an order that respects foreign key constraints:
            // Tables that are depended upon by other tables should be cleared after their dependents.
            EventTable.deleteAll()              // Depends on UserTable, TeamTable
            TaskTable.deleteAll()               // Depends on TaskListTable, UserTable
            TeamMemberTable.deleteAll()         // Depends on UserTable, TeamTable
            UserCalendarSyncInfoTable.deleteAll() // Depends on UserTable

            // Now tables that were depended upon by the above set (if not already cleared)
            TaskListTable.deleteAll()           // TaskTable depended on it
            BoardTable.deleteAll()              // TaskListTable depended on it

            // Finally, base tables like User and Team
            TeamTable.deleteAll()               // EventTable, BoardTable, TeamMemberTable depended on it
            UserTable.deleteAll()               // Many tables depended on it
            // TransactionManager.current().connection.createStatement().executeUpdate("SET REFERENTIAL_INTEGRITY TRUE")
        }
    }
}

// Helper for Ktor test application setup
fun io.ktor.server.application.Application.configureTestEnvironment() {
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        gson {
            setPrettyPrinting() // Optional: For readable JSON output
            // disableHtmlEscaping() // Optional: Depending on your needs
        }
    }
    // Add other common plugins if needed for most route tests, e.g., WebSockets if testing those.
}
