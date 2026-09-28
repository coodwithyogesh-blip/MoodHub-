package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_messages")
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val role: String, // "user", "arushi", "system"
    val text: String,
    val detectedLanguage: String = "auto", // "hindi", "english", "hinglish", etc.
    val actionExecuted: String? = null,
    val actionSuccess: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
