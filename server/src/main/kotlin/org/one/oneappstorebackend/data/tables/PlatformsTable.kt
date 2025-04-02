package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table

object PlatformsTable : Table("platforms") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50).uniqueIndex()
    val displayName = varchar("display_name", 100)
    
    override val primaryKey = PrimaryKey(id)
} 