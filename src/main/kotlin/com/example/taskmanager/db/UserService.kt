package com.example.taskmanager.db

import com.example.taskmanager.models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.example.taskmanager.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.transactions.transaction

class UserService {

    suspend fun init() {
        dbQuery {
            SchemaUtils.create(UserTable)
        }
    }

    suspend fun createUser(user: User, passwordHash: String): User? = dbQuery {
        val insertStatement = UserTable.insert {
            it[username] = user.username
            it[email] = user.email
            it[UserTable.passwordHash] = passwordHash
        }
        insertStatement.resultedValues?.singleOrNull()?.let { rowToUser(it) }
    }

    suspend fun getUserById(id: Int): User? = dbQuery {
        UserTable.select { UserTable.id eq id }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    suspend fun getUserByUsername(username: String): User? = dbQuery {
        UserTable.select { UserTable.username eq username }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    suspend fun getUserPasswordHash(id: Int): String? = dbQuery {
        UserTable.select { UserTable.id eq id }
            .map { it[UserTable.passwordHash] }
            .singleOrNull()
    }

    suspend fun updateUser(id: Int, email: String?, currentPasswordHash: String?, newPasswordHash: String?): Boolean = dbQuery {
        // User needs to provide current password to update email or password
        if (email != null || newPasswordHash != null) {
            if (currentPasswordHash == null) return@dbQuery false
            val user = UserTable.select { UserTable.id eq id }.singleOrNull() ?: return@dbQuery false
            if (user[UserTable.passwordHash] != currentPasswordHash) return@dbQuery false
        }

        UserTable.update({ UserTable.id eq id }) {
            email?.let { nonNullEmail -> it[UserTable.email] = nonNullEmail }
            newPasswordHash?.let { nonNullPassword -> it[passwordHash] = nonNullPassword }
        } > 0
    }

    suspend fun deleteUser(id: Int): Boolean = dbQuery {
        UserTable.deleteWhere { UserTable.id eq id } > 0
    }

    private fun rowToUser(row: ResultRow): User = User(
        id = row[UserTable.id],
        username = row[UserTable.username],
        email = row[UserTable.email]
    )
}
