package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val projectType: String, // "mobile_app", "video", "thumbnail", "web"
    val description: String,
    val status: String, // "CREATED", "GENERATING", "BUILDING", "COMPLETED", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val apkPath: String? = null,
    val apkSize: Long = 0L,
    val buildVariant: String = "debug",
    val rootDir: String
)
