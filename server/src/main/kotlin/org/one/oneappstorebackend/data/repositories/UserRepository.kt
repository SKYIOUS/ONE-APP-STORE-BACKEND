package org.one.oneappstorebackend.data.repositories

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.one.oneappstorebackend.data.DatabaseFactory.dbQuery
import org.one.oneappstorebackend.data.dto.UserDto
import org.one.oneappstorebackend.data.models.Users
import java.time.format.DateTimeFormatter
import java.util.*
import java.time.LocalDateTime

class UserRepository {
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    suspend fun createUser(
        username: String,
        email: String,
        passwordHash: String,
        isAdmin: Boolean = false,
        isDeveloper: Boolean = false
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
            it[Users.isDeveloper] = isDeveloper
        } get Users.id
        
        Users.select { Users.id eq id }
            .map { rowToUserDto(it) }
            .singleOrNull()
    }
    
    suspend fun findOrCreateGithubUser(
        githubId: String,
        githubUsername: String,
        email: String?,
        accessToken: String,
        refreshToken: String?,
        tokenExpiry: LocalDateTime?
    ): UserDto = dbQuery {
        // Look for existing user by GitHub ID
        val existingUserByGithubId = Users.select { Users.githubId eq githubId }
            .singleOrNull()
            
        if (existingUserByGithubId != null) {
            // Update the GitHub tokens
            Users.update({ Users.githubId eq githubId }) {
                it[Users.githubAccessToken] = accessToken
                if (refreshToken != null) it[Users.githubRefreshToken] = refreshToken
                if (tokenExpiry != null) it[Users.githubTokenExpiry] = tokenExpiry
            }
            
            return@dbQuery rowToUserDto(existingUserByGithubId)
        }
        
        // Look for existing user by email if provided
        val existingUserByEmail = email?.let {
            Users.select { Users.email eq email }
                .singleOrNull()
        }
        
        if (existingUserByEmail != null) {
            // Link GitHub account to existing user
            Users.update({ Users.id eq existingUserByEmail[Users.id] }) {
                it[Users.githubId] = githubId
                it[Users.githubUsername] = githubUsername
                it[Users.githubAccessToken] = accessToken
                if (refreshToken != null) it[Users.githubRefreshToken] = refreshToken
                if (tokenExpiry != null) it[Users.githubTokenExpiry] = tokenExpiry
                it[Users.isDeveloper] = true
            }
            
            return@dbQuery rowToUserDto(existingUserByEmail)
        }
        
        // Create new user
        val userId = UUID.randomUUID().toString()
        val username = generateUniqueUsername(githubUsername)
        
        val id = Users.insert {
            it[Users.userId] = userId
            it[Users.username] = username
            it[Users.email] = email ?: "$githubUsername@github.user" // Fallback email
            it[Users.passwordHash] = "" // Empty password for GitHub users
            it[Users.isAdmin] = false
            it[Users.isDeveloper] = true
            it[Users.githubId] = githubId
            it[Users.githubUsername] = githubUsername
            it[Users.githubAccessToken] = accessToken
            if (refreshToken != null) it[Users.githubRefreshToken] = refreshToken
            if (tokenExpiry != null) it[Users.githubTokenExpiry] = tokenExpiry
        } get Users.id
        
        Users.select { Users.id eq id }
            .map { rowToUserDto(it) }
            .single()
    }
    
    private suspend fun generateUniqueUsername(baseUsername: String): String = dbQuery {
        var username = baseUsername
        var counter = 1
        
        while (true) {
            val exists = Users.select { Users.username eq username }.count() > 0
            if (!exists) {
                break
            }
            username = "$baseUsername$counter"
            counter++
        }
        
        username
    }
    
    suspend fun updateGithubTokens(
        userId: String,
        accessToken: String,
        refreshToken: String?,
        tokenExpiry: LocalDateTime?
    ): Boolean = dbQuery {
        var updates = 0
        
        updates += Users.update({ Users.userId eq userId }) {
            it[Users.githubAccessToken] = accessToken
            if (refreshToken != null) it[Users.githubRefreshToken] = refreshToken
            if (tokenExpiry != null) it[Users.githubTokenExpiry] = tokenExpiry
        }
        
        updates > 0
    }
    
    suspend fun getUserByGithubId(githubId: String): UserDto? = dbQuery {
        Users.select { Users.githubId eq githubId }
            .map { rowToUserDto(it) }
            .singleOrNull()
    }
    
    suspend fun getGithubAccessToken(userId: String): String? = dbQuery {
        Users.select { Users.userId eq userId }
            .map { it[Users.githubAccessToken] }
            .singleOrNull()
    }
    
    suspend fun getGithubRefreshToken(userId: String): String? = dbQuery {
        Users.select { Users.userId eq userId }
            .map { it[Users.githubRefreshToken] }
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
        isAdmin: Boolean? = null,
        isDeveloper: Boolean? = null
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
        
        isDeveloper?.let { value ->
            updates += Users.update({ Users.userId eq userId }) {
                it[Users.isDeveloper] = value
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
            isAdmin = row[Users.isAdmin],
            isDeveloper = row[Users.isDeveloper],
            githubUsername = row[Users.githubUsername]
        )
    }
} 