package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
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

object AppsTable : Table("apps") {
    val id = integer("id").autoIncrement()
    val appId = varchar("app_id", 100).uniqueIndex()
    val name = varchar("name", 100)
    val developer = varchar("developer", 100)
    val description = text("description")
    val category = varchar("category", 50)
    val releaseDate = date("release_date")
    val isFeatured = bool("is_featured").default(false)
    val dateAdded = datetime("date_added")
    val lastUpdated = datetime("last_updated")
    val approvalStatus = varchar("approval_status", 20).default("PENDING")
    val submittedBy = varchar("submitted_by", 36).nullable().references(UsersTable.userId)
    val githubRepo = varchar("github_repo", 255).nullable()
    
    override val primaryKey = PrimaryKey(id)
}

object AppVersionsTable : Table("app_versions") {
    val id = integer("id").autoIncrement()
    val appId = varchar("app_id", 100).references(AppsTable.appId)
    val version = varchar("version", 50)
    val releaseNotes = text("release_notes").nullable()
    val releaseDate = date("release_date")
    val minOsVersion = varchar("min_os_version", 50).nullable()
    val sizeBytes = long("size_bytes").nullable()
    val approvalStatus = varchar("approval_status", 20).default("PENDING")
    val submittedBy = varchar("submitted_by", 36).nullable().references(UsersTable.userId)
    val releaseId = integer("release_id").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

object PlatformsTable : Table("platforms") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50).uniqueIndex()
    val displayName = varchar("display_name", 100)
    
    override val primaryKey = PrimaryKey(id)
}

object AppPlatformSupportTable : Table("app_platform_support") {
    val id = integer("id").autoIncrement()
    val appId = varchar("app_id", 100).references(AppsTable.appId)
    val platformId = integer("platform_id").references(PlatformsTable.id)
    val versionId = integer("version_id").references(AppVersionsTable.id)
    val downloadUrl = varchar("download_url", 255)
    val price = decimal("price", 10, 2).default(java.math.BigDecimal.ZERO)
    
    override val primaryKey = PrimaryKey(id)
}

object ReviewsTable : Table("reviews") {
    val id = integer("id").autoIncrement()
    val appId = varchar("app_id", 100).references(AppsTable.appId)
    val userId = varchar("user_id", 36).references(UsersTable.userId)
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val datePosted = datetime("date_posted")
    val versionId = integer("version_id").nullable().references(AppVersionsTable.id)
    
    override val primaryKey = PrimaryKey(id)
}

object AppApprovalHistoryTable : Table("app_approval_history") {
    val id = integer("id").autoIncrement()
    val appId = varchar("app_id", 100).references(AppsTable.appId)
    val versionId = integer("version_id").nullable().references(AppVersionsTable.id)
    val status = varchar("status", 20)
    val reviewedBy = varchar("reviewed_by", 36).nullable().references(UsersTable.userId)
    val reviewNotes = text("review_notes").nullable()
    val timestamp = datetime("timestamp")
    
    override val primaryKey = PrimaryKey(id)
}

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