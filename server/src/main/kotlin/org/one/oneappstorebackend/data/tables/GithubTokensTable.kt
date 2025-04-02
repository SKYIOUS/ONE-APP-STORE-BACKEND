package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object GithubTokensTable : Table("github_tokens") {
    val id = integer("id").autoIncrement()
    val userId = varchar("user_id", 36).references(UsersTable.userId)
    val accessToken = varchar("access_token", 255)
    val tokenType = varchar("token_type", 50)
    val refreshToken = varchar("refresh_token", 255).nullable()
    val expiresAt = datetime("expires_at").nullable()
    val scope = varchar("scope", 255)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    
    override val primaryKey = PrimaryKey(id)
} 