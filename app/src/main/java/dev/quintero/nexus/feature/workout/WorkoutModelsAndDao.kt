package dev.quintero.nexus.feature.workout

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val dateIso: String,
    val dayKey: String,
    val dayLabel: String,
    val notes: String,
    val savedAtIso: String,
    val exercisesJson: String
)

@Entity(tableName = "workout_drafts")
data class WorkoutDraftEntity(
    @PrimaryKey val dayKey: String,
    val dateIso: String,
    val notes: String,
    val exercisesJson: String,
    val savedAtMillis: Long
)

@JsonClass(generateAdapter = true)
data class WorkoutSetDto(
    @Json(name = "weight") val weight: Double? = null,
    @Json(name = "reps") val reps: Int? = null
)

@JsonClass(generateAdapter = true)
data class WorkoutLoggedExerciseDto(
    @Json(name = "name") val name: String,
    @Json(name = "sets") val sets: List<WorkoutSetDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WorkoutSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "date") val date: String,
    @Json(name = "dayKey") val dayKey: String,
    @Json(name = "dayLabel") val dayLabel: String? = null,
    @Json(name = "notes") val notes: String? = "",
    @Json(name = "savedAt") val savedAt: String? = null,
    @Json(name = "exercises") val exercises: List<WorkoutLoggedExerciseDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WorkoutBackupPayload(
    @Json(name = "exportDate") val exportDate: String,
    @Json(name = "sessions") val sessions: List<WorkoutSessionDto>
)

@JsonClass(generateAdapter = true)
data class WorkoutDraftSetDto(
    @Json(name = "weight") val weight: String = "",
    @Json(name = "reps") val reps: String = ""
)

@JsonClass(generateAdapter = true)
data class WorkoutDraftExerciseDto(
    @Json(name = "name") val name: String,
    @Json(name = "sets") val sets: List<WorkoutDraftSetDto> = emptyList()
)

data class WorkoutSessionModel(
    val id: String,
    val dateIso: String,
    val dayKey: String,
    val dayLabel: String,
    val notes: String,
    val savedAtIso: String,
    val exercises: List<WorkoutLoggedExerciseDto>
) {
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
}

data class PreviousExerciseHistory(
    val sets: List<WorkoutSetDto> = emptyList(),
    val dateIso: String? = null
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY dateIso DESC, savedAtIso DESC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateIso DESC, savedAtIso DESC")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessions(sessions: List<WorkoutSessionEntity>)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT * FROM workout_drafts WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getDraft(dayKey: String): WorkoutDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: WorkoutDraftEntity)

    @Query("DELETE FROM workout_drafts WHERE dayKey = :dayKey")
    suspend fun deleteDraft(dayKey: String)

    @Query("DELETE FROM workout_drafts")
    suspend fun deleteAllDrafts()
}
