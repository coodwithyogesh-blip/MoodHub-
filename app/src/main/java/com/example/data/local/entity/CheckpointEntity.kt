package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkpoints")
data class CheckpointEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val checkpointName: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileCount: Int = 0
)
