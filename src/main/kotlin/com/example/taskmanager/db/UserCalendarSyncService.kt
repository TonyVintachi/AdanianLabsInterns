package com.example.taskmanager.db

import com.example.taskmanager.models.UserCalendarSyncInfo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.example.taskmanager.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction

class UserCalendarSyncService {

    suspend fun init() {
        dbQuery {
            SchemaUtils.create(UserCalendarSyncInfoTable)
        }
    }

    private fun rowToUserCalendarSyncInfo(row: ResultRow): UserCalendarSyncInfo = UserCalendarSyncInfo(
        userId = row[UserCalendarSyncInfoTable.userId],
        accessToken = row[UserCalendarSyncInfoTable.accessToken],
        refreshToken = row[UserCalendarSyncInfoTable.refreshToken],
        tokenExpiry = row[UserCalendarSyncInfoTable.tokenExpiry]
    )

    suspend fun saveOrUpdateSyncInfo(syncInfo: UserCalendarSyncInfo): Unit = dbQuery {
        // Use upsert for H2/PostgreSQL. For broader compatibility, check existence then insert/update.
        // Exposed's insertOrUpdate might be an option if primary key is part of the columns to update.
        // Here, userId is the PK.

        val existing = UserCalendarSyncInfoTable
            .select { UserCalendarSyncInfoTable.userId eq syncInfo.userId }
            .singleOrNull()

        if (existing != null) {
            // Update
            UserCalendarSyncInfoTable.update({ UserCalendarSyncInfoTable.userId eq syncInfo.userId }) {
                it[accessToken] = syncInfo.accessToken
                it[refreshToken] = syncInfo.refreshToken
                it[tokenExpiry] = syncInfo.tokenExpiry
            }
        } else {
            // Insert
            UserCalendarSyncInfoTable.insert {
                it[userId] = syncInfo.userId
                it[accessToken] = syncInfo.accessToken
                it[refreshToken] = syncInfo.refreshToken
                it[tokenExpiry] = syncInfo.tokenExpiry
            }
        }
    }

    suspend fun getSyncInfoByUserId(userId: Int): UserCalendarSyncInfo? = dbQuery {
        UserCalendarSyncInfoTable.select { UserCalendarSyncInfoTable.userId eq userId }
            .map { rowToUserCalendarSyncInfo(it) }
            .singleOrNull()
    }

    suspend fun deleteSyncInfoByUserId(userId: Int): Boolean = dbQuery {
        UserCalendarSyncInfoTable.deleteWhere { UserCalendarSyncInfoTable.userId eq userId } > 0
    }
}
