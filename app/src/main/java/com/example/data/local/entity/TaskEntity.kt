package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val title: String,
    val taskType: String, // "APP_BUILD", "THUMBNAIL_GEN", "VIDEO_EDIT", "CODE_FIX"
    val status: String, // "QUEUED", "PLANNING", "GENERATING_FILES", "BUILDING", "ANALYZING_ERROR", "FIXING", "COMPLETED", "FAILED", "CANCELLED"
    val progress: Float = 0f, // 0.0 to 1.0
    val currentStep: String,
    val logs: String = "",
    val resultArtifactPath: String? = null,
    val resultArtifactType: String? = null, // "apk", "image", "video", "code"
    val resultArtifactSize: Long = 0L,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
