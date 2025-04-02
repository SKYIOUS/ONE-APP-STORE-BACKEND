package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

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