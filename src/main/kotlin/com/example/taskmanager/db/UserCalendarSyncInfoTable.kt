package com.example.taskmanager.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
// Assuming UserTable is already in this package or correctly imported
// import com.example.taskmanager.db.UserTable (if in different file but same package, direct use is fine)

object UserCalendarSyncInfoTable : Table("user_calendar_sync_info") {
    val userId = integer("user_id").references(UserTable.id)
    val accessToken = text("access_token") // Can be long
    val refreshToken = text("refresh_token").nullable() // Can be long
    val tokenExpiry = datetime("token_expiry").nullable()

    override val primaryKey = PrimaryKey(userId)
}
