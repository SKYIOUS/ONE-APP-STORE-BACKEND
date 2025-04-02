package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

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