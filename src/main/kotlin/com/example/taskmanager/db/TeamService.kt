package com.example.taskmanager.db

import com.example.taskmanager.models.Team
import com.example.taskmanager.models.User
import org.jetbrains.exposed.sql.*
import com.example.taskmanager.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class TeamService {

    suspend fun init() {
        dbQuery {
            SchemaUtils.create(TeamTable, TeamMemberTable)
        }
    }

    suspend fun createTeam(name: String, creatorId: Int): Team? = dbQuery {
        val teamId = TeamTable.insert {
            it[TeamTable.name] = name
        } get TeamTable.id

        TeamMemberTable.insert {
            it[userId] = creatorId
            it[TeamMemberTable.teamId] = teamId
        }
        Team(teamId, name)
    }

    suspend fun getTeamById(id: Int): Team? = dbQuery {
        TeamTable.select { TeamTable.id eq id }
            .map { Team(it[TeamTable.id], it[TeamTable.name]) }
            .singleOrNull()
    }

    suspend fun getAllTeams(): List<Team> = dbQuery {
        TeamTable.selectAll().map { Team(it[TeamTable.id], it[TeamTable.name]) }
    }

    suspend fun updateTeamName(id: Int, newName: String): Boolean = dbQuery {
        TeamTable.update({ TeamTable.id eq id }) {
            it[name] = newName
        } > 0
    }

    suspend fun deleteTeam(id: Int): Boolean = dbQuery {
        TeamMemberTable.deleteWhere { teamId eq id } // Remove all members first
        TeamTable.deleteWhere { TeamTable.id eq id } > 0
    }

    suspend fun addUserToTeam(userId: Int, teamId: Int): Boolean = dbQuery {
        val result = TeamMemberTable.insertIgnore {
            it[TeamMemberTable.userId] = userId
            it[TeamMemberTable.teamId] = teamId
        }
        result.insertedCount > 0
    }

    suspend fun removeUserFromTeam(userId: Int, teamId: Int): Boolean = dbQuery {
        TeamMemberTable.deleteWhere { (TeamMemberTable.userId eq userId) and (TeamMemberTable.teamId eq teamId) } > 0
    }

    suspend fun getTeamMembers(teamId: Int): List<User> = dbQuery {
        (TeamMemberTable innerJoin UserTable)
            .select { TeamMemberTable.teamId eq teamId }
            .map {
                User(
                    id = it[UserTable.id],
                    username = it[UserTable.username],
                    email = it[UserTable.email]
                )
            }
    }

    suspend fun getUserTeams(userId: Int): List<Team> = dbQuery {
        (TeamMemberTable innerJoin TeamTable)
            .select { TeamMemberTable.userId eq userId }
            .map { Team(it[TeamTable.id], it[TeamTable.name]) }
    }
}
