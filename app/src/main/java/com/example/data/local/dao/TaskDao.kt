package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED') ORDER BY createdAt DESC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskFlow(id: String): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, currentStep = :step, progress = :progress, updatedAt = :time WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, step: String, progress: Float, time: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET logs = logs || :newLogLine || '\n', updatedAt = :time WHERE id = :id")
    suspend fun appendLog(id: String, newLogLine: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = 'CANCELLED', currentStep = 'Cancelled by user', updatedAt = :time WHERE id = :id")
    suspend fun cancelTask(id: String, time: Long = System.currentTimeMillis())
}
