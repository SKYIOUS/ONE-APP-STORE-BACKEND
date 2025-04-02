package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

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