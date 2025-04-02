package org.one.oneappstorebackend.data.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

// Apps table
object Apps : IntIdTable() {
    val appId = varchar("app_id", 50).uniqueIndex()
    val name = varchar("name", 100)
    val developer = varchar("developer", 100)
    val description = text("description")
    val category = varchar("category", 50)
    val releaseDate = date("release_date")
    val isFeatured = bool("is_featured").default(false)
    val dateAdded = timestamp("date_added").defaultExpression(CurrentTimestamp())
    val lastUpdated = timestamp("last_updated").defaultExpression(CurrentTimestamp())
    val approvalStatus = varchar("approval_status", 20).default("PENDING")
    val submittedBy = varchar("submitted_by", 50).nullable()
}

// App Status enumeration values: PENDING, APPROVED, REJECTED

// App versions table
object AppVersions : IntIdTable() {
    val appId = varchar("app_id", 50).references(Apps.appId)
    val version = varchar("version", 20)
    val releaseNotes = text("release_notes").nullable()
    val releaseDate = date("release_date")
    val minOsVersion = varchar("min_os_version", 20).nullable()
    val sizeBytes = long("size_bytes").nullable()
    val approvalStatus = varchar("approval_status", 20).default("PENDING")
    val submittedBy = varchar("submitted_by", 50).nullable()
    
    init {
        uniqueIndex("app_version_idx", appId, version)
    }
}

// Platforms table
object Platforms : IntIdTable() {
    val name = varchar("name", 20).uniqueIndex()
    val displayName = varchar("display_name", 50)
}

// App Platform Support table
object AppPlatformSupport : IntIdTable() {
    val appId = varchar("app_id", 50).references(Apps.appId)
    val platformId = integer("platform_id").references(Platforms.id)
    val versionId = integer("version_id").references(AppVersions.id)
    val downloadUrl = varchar("download_url", 255)
    val price = decimal("price", 10, 2)
    
    init {
        uniqueIndex("app_platform_version_idx", appId, platformId, versionId)
    }
}

// Reviews table
object Reviews : IntIdTable() {
    val appId = varchar("app_id", 50).references(Apps.appId)
    val userId = varchar("user_id", 50)
    val userName = varchar("user_name", 100)
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val datePosted = timestamp("date_posted").defaultExpression(CurrentTimestamp())
}

// Users table
object Users : IntIdTable() {
    val userId = varchar("user_id", 50).uniqueIndex()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val dateRegistered = timestamp("date_registered").defaultExpression(CurrentTimestamp())
    val isAdmin = bool("is_admin").default(false)
    val isDeveloper = bool("is_developer").default(false)
    
    // GitHub OAuth fields
    val githubId = varchar("github_id", 50).nullable().uniqueIndex()
    val githubUsername = varchar("github_username", 100).nullable()
    val githubAccessToken = varchar("github_access_token", 255).nullable()
    val githubRefreshToken = varchar("github_refresh_token", 255).nullable()
    val githubTokenExpiry = timestamp("github_token_expiry").nullable()
}

// App Approval History table
object AppApprovalHistory : IntIdTable() {
    val appId = varchar("app_id", 50).references(Apps.appId)
    val versionId = integer("version_id").references(AppVersions.id).nullable()
    val status = varchar("status", 20) // PENDING, APPROVED, REJECTED
    val reviewedBy = varchar("reviewed_by", 50).references(Users.userId).nullable()
    val reviewNotes = text("review_notes").nullable()
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp())
}

// User Installations table
object UserInstallations : IntIdTable() {
    val userId = varchar("user_id", 50).references(Users.userId)
    val appId = varchar("app_id", 50).references(Apps.appId)
    val versionId = integer("version_id").references(AppVersions.id)
    val platformId = integer("platform_id").references(Platforms.id)
    val installDate = timestamp("install_date").defaultExpression(CurrentTimestamp())
    val isActive = bool("is_active").default(true)
}

// User Wishlist table
object UserWishlist : IntIdTable() {
    val userId = varchar("user_id", 50).references(Users.userId)
    val appId = varchar("app_id", 50).references(Apps.appId)
    val dateAdded = timestamp("date_added").defaultExpression(CurrentTimestamp())
    
    init {
        uniqueIndex("user_app_wishlist_idx", userId, appId)
    }
}

// Collections table
object Collections : IntIdTable() {
    val collectionId = varchar("collection_id", 50).uniqueIndex()
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val isSystem = bool("is_system").default(false)
    val createdBy = varchar("created_by", 50).references(Users.userId)
    val dateCreated = timestamp("date_created").defaultExpression(CurrentTimestamp())
}

// Collection Apps table
object CollectionApps : IntIdTable() {
    val collectionId = varchar("collection_id", 50).references(Collections.collectionId)
    val appId = varchar("app_id", 50).references(Apps.appId)
    val dateAdded = timestamp("date_added").defaultExpression(CurrentTimestamp())
    
    init {
        uniqueIndex("collection_app_idx", collectionId, appId)
    }
}

// Notifications table
object Notifications : IntIdTable() {
    val userId = varchar("user_id", 50).references(Users.userId)
    val title = varchar("title", 100)
    val message = text("message")
    val isRead = bool("is_read").default(false)
    val dateCreated = timestamp("date_created").defaultExpression(CurrentTimestamp())
} 