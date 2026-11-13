package dev.quintero.nexus.feature.planner

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.quintero.nexus.feature.planner.NightTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NightTaskDao {

    @Query("SELECT * FROM night_tasks ORDER BY targetDateIso ASC, completed ASC, COALESCE(startTime, '99:99') ASC, createdAtMillis DESC")
    fun observeAllTasks(): Flow<List<NightTaskEntity>>

    @Query("SELECT * FROM night_tasks WHERE targetDateIso = :dateIso ORDER BY completed ASC, COALESCE(startTime, '99:99') ASC, createdAtMillis DESC")
    fun observeTasksForDate(dateIso: String): Flow<List<NightTaskEntity>>

    @Query("SELECT * FROM night_tasks WHERE targetDateIso = :dateIso")
    suspend fun getTasksForDateOnce(dateIso: String): List<NightTaskEntity>

    @Query("SELECT * FROM night_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): NightTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: NightTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<NightTaskEntity>)

    @Update
    suspend fun updateTask(task: NightTaskEntity)

    @Delete
    suspend fun deleteTask(task: NightTaskEntity)

    @Query("DELETE FROM night_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("SELECT * FROM night_tasks ORDER BY targetDateIso ASC, COALESCE(startTime, '99:99') ASC, createdAtMillis ASC")
    suspend fun getAllTasksOnce(): List<NightTaskEntity>

    @Query("SELECT * FROM night_tasks WHERE syncUid = :syncUid LIMIT 1")
    suspend fun getTaskBySyncUid(syncUid: String): NightTaskEntity?

    @Query("DELETE FROM night_tasks WHERE syncUid = :syncUid")
    suspend fun deleteTaskBySyncUid(syncUid: String)

    @Query("SELECT COUNT(*) FROM night_tasks")
    suspend fun countTasks(): Int

    @Query(
        """
        UPDATE night_tasks 
        SET targetDateIso = :toDateIso,
            updatedAtMillis = :updatedAtMillis,
            syncState = CASE WHEN syncWithCalendar = 1 THEN 'PENDING_SYNC' ELSE 'LOCAL_ONLY' END
        WHERE targetDateIso = :fromDateIso AND completed = 0
        """
    )
    suspend fun migrateIncompleteTasks(
        fromDateIso: String,
        toDateIso: String,
        updatedAtMillis: Long = System.currentTimeMillis()
    ): Int
}
