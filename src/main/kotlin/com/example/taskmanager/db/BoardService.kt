package com.example.taskmanager.db

import com.example.taskmanager.models.Board
import org.jetbrains.exposed.sql.*
import com.example.taskmanager.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class BoardService {

    suspend fun init() {
        dbQuery {
            SchemaUtils.create(BoardTable)
        }
    }

    suspend fun createBoard(name: String, teamId: Int): Board? = dbQuery {
        val insertStatement = BoardTable.insert {
            it[BoardTable.name] = name
            it[BoardTable.teamId] = teamId
        }
        insertStatement.resultedValues?.singleOrNull()?.let {
            Board(
                id = it[BoardTable.id],
                name = it[BoardTable.name],
                teamId = it[BoardTable.teamId]
            )
        }
    }

    suspend fun getBoardById(id: Int): Board? = dbQuery {
        BoardTable.select { BoardTable.id eq id }
            .map { Board(it[BoardTable.id], it[BoardTable.name], it[BoardTable.teamId]) }
            .singleOrNull()
    }

    suspend fun getBoardsByTeam(teamId: Int): List<Board> = dbQuery {
        BoardTable.select { BoardTable.teamId eq teamId }
            .map { Board(it[BoardTable.id], it[BoardTable.name], it[BoardTable.teamId]) }
    }

    suspend fun updateBoardName(id: Int, newName: String): Boolean = dbQuery {
        BoardTable.update({ BoardTable.id eq id }) {
            it[name] = newName
        } > 0
    }

    suspend fun deleteBoard(id: Int): Boolean = dbQuery {
        // For now, just deletes the board. Cascading deletes for task lists and tasks
        // will be handled in their respective services or at a higher level if needed.
        BoardTable.deleteWhere { BoardTable.id eq id } > 0
    }
}
