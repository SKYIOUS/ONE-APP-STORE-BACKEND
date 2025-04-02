package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val userId = varchar("user_id", 36)
    val username = varchar("username", 100)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255).nullable()
    val isAdmin = bool("is_admin").default(false)
    val isDeveloper = bool("is_developer").default(false)
    val dateRegistered = datetime("date_registered")
    val lastLogin = datetime("last_login").nullable()
    val avatarUrl = varchar("avatar_url", 255).nullable()
    val githubId = varchar("github_id", 50).nullable().uniqueIndex()
    val githubUsername = varchar("github_username", 100).nullable()
    val bio = text("bio").nullable()
    
    override val primaryKey = PrimaryKey(id)
} 