package com.example.taskmanager.db

import org.jetbrains.exposed.sql.Table

object TeamMemberTable : Table("team_members") {
    val userId = integer("user_id").references(UserTable.id)
    val teamId = integer("team_id").references(TeamTable.id)

    override val primaryKey = PrimaryKey(userId, teamId)
}
