package dev.quintero.nexus.feature.workout

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Handles workout persistence, auto-drafts, historical lookup with alias matching,
 * and portable JSON backup import/export.
 */
class WorkoutRepository(private val dao: WorkoutDao) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val exercisesListType = Types.newParameterizedType(List::class.java, WorkoutLoggedExerciseDto::class.java)
    private val exercisesAdapter = moshi.adapter<List<WorkoutLoggedExerciseDto>>(exercisesListType)

    private val draftListType = Types.newParameterizedType(List::class.java, WorkoutDraftExerciseDto::class.java)
    private val draftAdapter = moshi.adapter<List<WorkoutDraftExerciseDto>>(draftListType)

    private val backupAdapter = moshi.adapter(WorkoutBackupPayload::class.java).indent("  ")
    private val bareSessionsAdapter = moshi.adapter<List<WorkoutSessionDto>>(
        Types.newParameterizedType(List::class.java, WorkoutSessionDto::class.java)
    )

    val sessionsFlow: Flow<List<WorkoutSessionModel>> = dao.observeSessions().map { entities ->
        entities.map { entity -> entity.toDomain() }
    }

    suspend fun getAllSessions(): List<WorkoutSessionModel> =
        dao.getAllSessions().map { it.toDomain() }

    suspend fun saveSession(
        dateIso: String,
        dayKey: String,
        dayLabel: String,
        notes: String,
        exercises: List<WorkoutLoggedExerciseDto>
    ) {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val entity = WorkoutSessionEntity(
            id = "$dateIso-$dayKey",
            dateIso = dateIso,
            dayKey = dayKey,
            dayLabel = dayLabel,
            notes = notes,
            savedAtIso = nowIso,
            exercisesJson = exercisesAdapter.toJson(exercises)
        )
        dao.upsertSession(entity)
        dao.deleteDraft(dayKey)
    }

    suspend fun deleteSession(id: String) {
        dao.deleteSessionById(id)
    }

    suspend fun clearAll() {
        dao.deleteAllSessions()
        dao.deleteAllDrafts()
    }

    suspend fun getValidTodayDraft(
        dayKey: String,
        todayIso: String
    ): Pair<WorkoutDraftEntity, List<WorkoutDraftExerciseDto>>? {
        val draft = dao.getDraft(dayKey) ?: return null
        val isExpired = draft.dateIso != todayIso ||
            (System.currentTimeMillis() - draft.savedAtMillis > 16L * 3600L * 1000L)
        if (isExpired) {
            dao.deleteDraft(dayKey)
            return null
        }
        val parsed = runCatching { draftAdapter.fromJson(draft.exercisesJson) }.getOrNull() ?: return null
        return draft to parsed
    }

    suspend fun saveDraft(
        dayKey: String,
        dateIso: String,
        notes: String,
        exercises: List<WorkoutDraftExerciseDto>
    ) {
        dao.saveDraft(
            WorkoutDraftEntity(
                dayKey = dayKey,
                dateIso = dateIso,
                notes = notes,
                exercisesJson = draftAdapter.toJson(exercises),
                savedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearDraft(dayKey: String) {
        dao.deleteDraft(dayKey)
    }

    fun exportBackupJson(sessions: List<WorkoutSessionModel>): String {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val payload = WorkoutBackupPayload(
            exportDate = nowIso,
            sessions = sessions.map { s ->
                WorkoutSessionDto(
                    id = s.id,
                    date = s.dateIso,
                    dayKey = s.dayKey,
                    dayLabel = s.dayLabel,
                    notes = s.notes,
                    savedAt = s.savedAtIso,
                    exercises = s.exercises
                )
            }
        )
        return backupAdapter.toJson(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> = runCatching {
        val cleanedJson = rawJson.trim().removePrefix("\uFEFF").trim()
        require(cleanedJson.isNotEmpty()) { "El archivo JSON está vacío." }

        val importedDtos: List<WorkoutSessionDto> = parseWorkoutImportDtos(cleanedJson)
            ?: error("El archivo JSON no tiene un formato de sesiones válido.")

        val existingById = dao.getAllSessions().associateBy { it.id }
        val toUpsert = importedDtos.mapNotNull { dto ->
            val sessionId = dto.id.trim()
            if (sessionId.isBlank()) return@mapNotNull null

            val resolvedDayKey = WorkoutRoutineCatalog.resolveCanonicalDayKey(dto.dayKey)
            val normalizedSavedAt = dto.savedAt?.takeIf { it.isNotBlank() } ?: "${dto.date}T12:00:00.000Z"
            val newExercisesJson = exercisesAdapter.toJson(dto.exercises)

            val existing = existingById[sessionId]
            val shouldWrite = when {
                existing == null -> true
                normalizedSavedAt > existing.savedAtIso -> true
                else -> false
            }
            if (!shouldWrite) return@mapNotNull null

            WorkoutSessionEntity(
                id = sessionId,
                dateIso = dto.date,
                dayKey = dto.dayKey.ifBlank { resolvedDayKey },
                dayLabel = dto.dayLabel?.takeIf { it.isNotBlank() } ?: resolvedDayKey,
                notes = dto.notes.orEmpty(),
                savedAtIso = normalizedSavedAt,
                exercisesJson = newExercisesJson
            )
        }
        if (toUpsert.isNotEmpty()) {
            dao.upsertSessions(toUpsert)
        }
        toUpsert.size
    }

    suspend fun replaceSessionsFromSync(merged: List<WorkoutSessionModel>) {
        val entities = merged.map { m ->
            WorkoutSessionEntity(
                id = m.id,
                dateIso = m.dateIso,
                dayKey = m.dayKey,
                dayLabel = m.dayLabel,
                notes = m.notes,
                savedAtIso = m.savedAtIso,
                exercisesJson = exercisesAdapter.toJson(m.exercises)
            )
        }
        val currentIds = dao.getAllSessions().map { it.id }.toSet()
        val targetIds = entities.map { it.id }.toSet()
        (currentIds - targetIds).forEach { removedId ->
            dao.deleteSessionById(removedId)
        }
        if (entities.isNotEmpty()) {
            dao.upsertSessions(entities)
        }
    }

    private fun parseWorkoutImportDtos(cleanedJson: String): List<WorkoutSessionDto>? {
        runCatching { backupAdapter.fromJson(cleanedJson)?.sessions }.getOrNull()?.let { return it }
        runCatching { bareSessionsAdapter.fromJson(cleanedJson) }.getOrNull()?.let { return it }

        // Tolerant fallback for external JSON backups with stringified numbers, empty strings, or numeric IDs
        return runCatching {
            val sessionsArr: JSONArray = when {
                cleanedJson.startsWith("{") -> {
                    val root = JSONObject(cleanedJson)
                    root.optJSONArray("sessions") ?: return@runCatching null
                }
                cleanedJson.startsWith("[") -> JSONArray(cleanedJson)
                else -> return@runCatching null
            }

            (0 until sessionsArr.length()).mapNotNull { idx ->
                val obj = sessionsArr.optJSONObject(idx) ?: return@mapNotNull null
                val dateStr = obj.optString("date", obj.optString("dateIso", "")).trim()
                val dayKeyStr = obj.optString("dayKey", "").trim().ifBlank { "Día 1" }
                val rawId = obj.opt("id")?.toString()?.trim().orEmpty()
                val resolvedId = rawId.takeIf { it.isNotBlank() && it != "null" }
                    ?: if (dateStr.isNotBlank()) "$dateStr-$dayKeyStr" else UUID.randomUUID().toString()
                if (dateStr.isBlank()) return@mapNotNull null

                val exArr = obj.optJSONArray("exercises") ?: JSONArray()
                val parsedExercises = (0 until exArr.length()).mapNotNull { eIdx ->
                    val exObj = exArr.optJSONObject(eIdx) ?: return@mapNotNull null
                    val exName = exObj.optString("name", "").trim()
                    if (exName.isBlank()) return@mapNotNull null
                    val setsArr = exObj.optJSONArray("sets") ?: JSONArray()
                    val parsedSets = (0 until setsArr.length()).mapNotNull { sIdx ->
                        val sObj = setsArr.optJSONObject(sIdx) ?: return@mapNotNull null
                        val rawW = sObj.opt("weight")
                        val weightVal: Double? = when (rawW) {
                            null, JSONObject.NULL -> null
                            is Number -> rawW.toDouble()
                            else -> rawW.toString().trim().replace(',', '.').toDoubleOrNull()
                        }
                        val rawR = sObj.opt("reps")
                        val repsVal: Int? = when (rawR) {
                            null, JSONObject.NULL -> null
                            is Number -> rawR.toInt()
                            else -> rawR.toString().trim().toIntOrNull()
                                ?: rawR.toString().trim().toDoubleOrNull()?.toInt()
                        }
                        if (weightVal == null && repsVal == null) null
                        else WorkoutSetDto(weight = weightVal, reps = repsVal)
                    }
                    WorkoutLoggedExerciseDto(name = exName, sets = parsedSets)
                }

                WorkoutSessionDto(
                    id = resolvedId,
                    date = dateStr,
                    dayKey = dayKeyStr,
                    dayLabel = obj.optString("dayLabel", "").takeIf { it.isNotBlank() && it != "null" },
                    notes = obj.optString("notes", "").takeIf { it != "null" } ?: "",
                    savedAt = obj.optString("savedAt", obj.optString("savedAtIso", ""))
                        .takeIf { it.isNotBlank() && it != "null" },
                    exercises = parsedExercises
                )
            }
        }.getOrNull()
    }

    fun findLastExerciseSets(
        sessions: List<WorkoutSessionModel>,
        exerciseName: String,
        preferredDayKey: String? = null
    ): PreviousExerciseHistory {
        val target = WorkoutRoutineCatalog.normName(exerciseName)
        val aliases = (WorkoutRoutineCatalog.EXERCISE_ALIASES[target] ?: emptyList())
            .map { WorkoutRoutineCatalog.normName(it) }
            .toSet()

        fun matches(loggedName: String): Boolean {
            val n = WorkoutRoutineCatalog.normName(loggedName)
            return n == target || n in aliases
        }

        if (preferredDayKey != null) {
            for (s in sessions) {
                val resolvedDay = WorkoutRoutineCatalog.resolveCanonicalDayKey(s.dayKey)
                if (resolvedDay == preferredDayKey || s.dayKey == preferredDayKey) {
                    val found = s.exercises.firstOrNull { matches(it.name) && it.sets.isNotEmpty() }
                    if (found != null) {
                        return PreviousExerciseHistory(sets = found.sets, dateIso = s.dateIso)
                    }
                }
            }
        }

        for (s in sessions) {
            val found = s.exercises.firstOrNull { matches(it.name) && it.sets.isNotEmpty() }
            if (found != null) {
                return PreviousExerciseHistory(sets = found.sets, dateIso = s.dateIso)
            }
        }

        return PreviousExerciseHistory()
    }

    private fun WorkoutSessionEntity.toDomain(): WorkoutSessionModel {
        val exercises = runCatching { exercisesAdapter.fromJson(exercisesJson) }.getOrNull().orEmpty()
        return WorkoutSessionModel(
            id = id,
            dateIso = dateIso,
            dayKey = dayKey,
            dayLabel = dayLabel,
            notes = notes,
            savedAtIso = savedAtIso,
            exercises = exercises
        )
    }
}
