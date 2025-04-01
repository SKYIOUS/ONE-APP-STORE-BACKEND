package org.one.oneappstorebackend.data.repositories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.one.oneappstorebackend.data.DatabaseFactory.dbQuery
import org.one.oneappstorebackend.data.dto.UserDto
import org.one.oneappstorebackend.data.models.Users
import java.time.format.DateTimeFormatter
import java.util.*

class UserRepository {
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    suspend fun createUser(
        username: String,
        email: String,
        passwordHash: String,
        isAdmin: Boolean = false
    ): UserDto? = dbQuery {
        val userId = UUID.randomUUID().toString()
        
        val existingUser = Users.select {
            (Users.email eq email) or (Users.username eq username)
        }.singleOrNull()
        
        if (existingUser != null) {
            return@dbQuery null
        }
        
        val id = Users.insert {
            it[Users.userId] = userId
            it[Users.username] = username
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.isAdmin] = isAdmin
        } get Users.id
        
        Users.select { Users.id eq id }
            .map { rowToUserDto(it) }
            .singleOrNull()
    }
    
    suspend fun getUserByEmail(email: String): UserDto? = dbQuery {
        Users.select { Users.email eq email }
            .map { rowToUserDto(it) }
            .singleOrNull()
    }
    
    suspend fun getUserById(userId: String): UserDto? = dbQuery {
        Users.select { Users.userId eq userId }
            .map { rowToUserDto(it) }
            .singleOrNull()
    }
    
    suspend fun getUserByUsername(username: String): UserDto? = dbQuery {
        Users.select { Users.username eq username }
            .map { rowToUserDto(it) }
            .singleOrNull()
    }
    
    suspend fun getUserPasswordHash(email: String): String? = dbQuery {
        Users.select { Users.email eq email }
            .map { it[Users.passwordHash] }
            .singleOrNull()
    }
    
    suspend fun updateUser(
        userId: String,
        username: String? = null,
        email: String? = null,
        passwordHash: String? = null,
        isAdmin: Boolean? = null
    ): Boolean = dbQuery {
        var updates = 0
        
        username?.let { value ->
            updates += Users.update({ Users.userId eq userId }) {
                it[Users.username] = value
            }
        }
        
        email?.let { value ->
            updates += Users.update({ Users.userId eq userId }) {
                it[Users.email] = value
            }
        }
        
        passwordHash?.let { value ->
            updates += Users.update({ Users.userId eq userId }) {
                it[Users.passwordHash] = value
            }
        }
        
        isAdmin?.let { value ->
            updates += Users.update({ Users.userId eq userId }) {
                it[Users.isAdmin] = value
            }
        }
        
        updates > 0
    }
    
    suspend fun deleteUser(userId: String): Boolean = dbQuery {
        Users.deleteWhere { Users.userId eq userId } > 0
    }
    
    suspend fun getAllUsers(page: Int, pageSize: Int): List<UserDto> = dbQuery {
        Users.selectAll()
            .orderBy(Users.dateRegistered, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { rowToUserDto(it) }
    }
    
    suspend fun countUsers(): Long = dbQuery {
        Users.selectAll().count()
    }
    
    private fun rowToUserDto(row: ResultRow): UserDto {
        return UserDto(
            id = row[Users.id].value,
            userId = row[Users.userId],
            username = row[Users.username],
            email = row[Users.email],
            dateRegistered = row[Users.dateRegistered].toString(),
            isAdmin = row[Users.isAdmin]
        )
    }
} 