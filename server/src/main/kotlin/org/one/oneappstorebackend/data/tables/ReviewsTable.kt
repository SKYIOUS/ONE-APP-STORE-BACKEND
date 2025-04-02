package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

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