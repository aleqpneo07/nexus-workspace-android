package dev.quintero.nexus.sync

import dev.quintero.nexus.feature.planner.NightTaskEntity
import dev.quintero.nexus.feature.progress.BodyProgressEntryDto
import dev.quintero.nexus.feature.progress.BodyProgressEntryEntity
import dev.quintero.nexus.feature.progress.BodyProgressProfileDto
import dev.quintero.nexus.feature.progress.BodyProgressProfileEntity
import dev.quintero.nexus.feature.progress.toEntity
import dev.quintero.nexus.feature.workout.WorkoutLoggedExerciseDto
import dev.quintero.nexus.feature.workout.WorkoutSessionModel
import com.squareup.moshi.Json
import java.security.MessageDigest
import java.time.Instant

enum class SyncModule {
    ENFOQUE,
    ENTRENO,
    NIGHT_PLANNER,
    PROGRESO_FISICO
}

data class MisCosasSyncUiState(
    val isConnected: Boolean = false,
    val accountIdentifier: String = "",
    val enfoqueTopicId: String = "",
    val entrenoTopicId: String = "",
    val nightTopicId: String = "",
    val progressTopicId: String = "",
    val isSyncing: Boolean = false,
    val isLiveConnected: Boolean = false,
    val lastSyncAtMillis: Long? = null,
    val lastSyncSummary: String? = null,
    val lastError: String? = null
)

data class WorkoutDeletedEntry(
    @Json(name = "id") val id: String,
    @Json(name = "deletedAt") val deletedAtMillis: Long = 0L
)

data class WorkoutCloudEnvelope(
    @Json(name = "version") val version: Int = 1,
    @Json(name = "updatedAt") val updatedAt: Long = 0L,
    @Json(name = "senderDeviceId") val senderDeviceId: String = "",
    @Json(name = "syncId") val syncId: String = "",
    @Json(name = "sessions") val sessions: List<WorkoutCloudSessionDto> = emptyList(),
    @Json(name = "deletedIds") val deletedIds: List<WorkoutDeletedEntry> = emptyList()
)

data class WorkoutCloudSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "dateIso") val dateIso: String,
    @Json(name = "dayKey") val dayKey: String,
    @Json(name = "dayLabel") val dayLabel: String,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "savedAtIso") val savedAtIso: String = "",
    @Json(name = "exercises") val exercises: List<WorkoutLoggedExerciseDto> = emptyList()
)

data class NightDeletedUidEntry(
    @Json(name = "syncUid") val syncUid: String,
    @Json(name = "deletedAt") val deletedAtMillis: Long = 0L
)

data class NightTaskCloudDto(
    @Json(name = "syncUid") val syncUid: String,
    @Json(name = "title") val title: String,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "targetDateIso") val targetDateIso: String,
    @Json(name = "category") val category: String,
    @Json(name = "priority") val priority: String,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "endTime") val endTime: String? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "syncWithCalendar") val syncWithCalendar: Boolean = true,
    @Json(name = "completed") val completed: Boolean = false,
    @Json(name = "completedAtMillis") val completedAtMillis: Long? = null,
    @Json(name = "createdAtMillis") val createdAtMillis: Long = 0L,
    @Json(name = "updatedAtMillis") val updatedAtMillis: Long = 0L
)

data class NightCloudEnvelope(
    @Json(name = "version") val version: Int = 1,
    @Json(name = "updatedAt") val updatedAt: Long = 0L,
    @Json(name = "senderDeviceId") val senderDeviceId: String = "",
    @Json(name = "syncId") val syncId: String = "",
    @Json(name = "tasks") val tasks: List<NightTaskCloudDto> = emptyList(),
    @Json(name = "deletedUids") val deletedUids: List<NightDeletedUidEntry> = emptyList()
)

data class WorkoutMergeResult(
    val mergedSessions: List<WorkoutSessionModel>,
    val mergedTombstones: Map<String, Long>,
    val changedLocal: Boolean,
    val shouldPushBack: Boolean
)

data class NightMergeResult(
    val upserts: List<NightTaskEntity>,
    val deletedSyncUidsToRemoveLocally: List<String>,
    val mergedTombstones: Map<String, Long>,
    val changedLocal: Boolean,
    val shouldPushBack: Boolean
)

data class BodyProgressDeletedEntry(
    @Json(name = "id") val id: String,
    @Json(name = "deletedAt") val deletedAtMillis: Long = 0L
)

data class BodyProgressCloudEnvelope(
    @Json(name = "version") val version: Int = 1,
    @Json(name = "updatedAt") val updatedAt: Long = 0L,
    @Json(name = "senderDeviceId") val senderDeviceId: String = "",
    @Json(name = "syncId") val syncId: String = "",
    @Json(name = "profile") val profile: BodyProgressProfileDto = BodyProgressProfileDto(),
    @Json(name = "entries") val entries: List<BodyProgressEntryDto> = emptyList(),
    @Json(name = "deletedIds") val deletedIds: List<BodyProgressDeletedEntry> = emptyList()
)

data class BodyProgressMergeResult(
    val mergedProfile: BodyProgressProfileEntity,
    val mergedEntries: List<BodyProgressEntryEntity>,
    val mergedTombstones: Map<String, Long>,
    val changedLocal: Boolean,
    val shouldPushBack: Boolean
)

object MisCosasSyncEngine {

    fun deriveTopicId(
        prefix: String,
        scope: String?,
        identifier: String,
        password: String
    ): String {
        val normId = identifier.trim().lowercase()
        val rawInput = if (scope.isNullOrEmpty()) {
            "$normId::$password"
        } else {
            "$scope::$normId::$password"
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(rawInput.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(32)
        return "$prefix$hex"
    }

    fun deriveAllTopics(identifier: String, password: String): Triple<String, String, String> {
        val enfoque = deriveTopicId("nexus_focus_", "nexus_focus", identifier, password.trim())
        val entreno = deriveTopicId("nexus_workout_", "nexus_workout", identifier, password.trim())
        val night = deriveTopicId("nexus_planner_", "nexus_planner", identifier, password.trim())
        return Triple(enfoque, entreno, night)
    }

    fun deriveBodyProgressTopic(identifier: String, keyBase64: String): String {
        return deriveTopicId("nexus_progress_", "nexus_progress", identifier, keyBase64.trim())
    }

    fun parseIsoToMillis(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        return runCatching { Instant.parse(iso.trim()).toEpochMilli() }.getOrDefault(0L)
    }

    /**
     * Conflict-free union merge for Workout sessions using savedAtIso + deletion tombstones.
     */
    fun mergeWorkoutSessions(
        localSessions: List<WorkoutSessionModel>,
        localTombstones: Map<String, Long>,
        remoteSessions: List<WorkoutCloudSessionDto>,
        remoteTombstones: Map<String, Long>
    ): WorkoutMergeResult {
        val mergedTombstones = HashMap<String, Long>(localTombstones)
        remoteTombstones.forEach { (id, ts) ->
            val prev = mergedTombstones[id] ?: 0L
            if (ts > prev) mergedTombstones[id] = ts
        }

        val localMap = localSessions.associateBy { it.id }
        val remoteMap = remoteSessions.associate {
            it.id to WorkoutSessionModel(
                id = it.id,
                dateIso = it.dateIso,
                dayKey = it.dayKey,
                dayLabel = it.dayLabel,
                notes = it.notes,
                savedAtIso = it.savedAtIso,
                exercises = it.exercises
            )
        }.toMap()

        val allIds = (localMap.keys + remoteMap.keys).toSet()
        val mergedList = mutableListOf<WorkoutSessionModel>()
        var changedLocal = false
        var shouldPushBack = false

        for (id in allIds) {
            val l = localMap[id]
            val r = remoteMap[id]
            val deletedTs = mergedTombstones[id] ?: 0L

            val winner: WorkoutSessionModel? = when {
                l != null && r != null -> {
                    val lTs = parseIsoToMillis(l.savedAtIso)
                    val rTs = parseIsoToMillis(r.savedAtIso)
                    when {
                        rTs > lTs -> {
                            changedLocal = true
                            r
                        }
                        lTs > rTs -> {
                            shouldPushBack = true
                            l
                        }
                        l != r -> {
                            // Prefer fuller exercise payload or remote on tie
                            if (r.exercises.size >= l.exercises.size && r.savedAtIso >= l.savedAtIso) {
                                changedLocal = true
                                r
                            } else {
                                shouldPushBack = true
                                l
                            }
                        }
                        else -> l
                    }
                }
                l != null -> {
                    val lTs = parseIsoToMillis(l.savedAtIso)
                    if (deletedTs > 0L && deletedTs >= lTs) {
                        changedLocal = true
                        null
                    } else {
                        shouldPushBack = true
                        l
                    }
                }
                r != null -> {
                    val rTs = parseIsoToMillis(r.savedAtIso)
                    if (deletedTs > 0L && deletedTs >= rTs) {
                        shouldPushBack = true
                        null
                    } else {
                        changedLocal = true
                        r
                    }
                }
                else -> null
            }

            if (winner != null) {
                mergedList += winner
            }
        }

        val sorted = mergedList.sortedWith(
            compareByDescending<WorkoutSessionModel> { it.dateIso }
                .thenByDescending { it.savedAtIso }
        )

        return WorkoutMergeResult(
            mergedSessions = sorted,
            mergedTombstones = mergedTombstones,
            changedLocal = changedLocal,
            shouldPushBack = shouldPushBack
        )
    }

    /**
     * Conflict-free union merge for Night Planner tasks keyed by `syncUid`,
     * preserving local Room row `id` and local Google Calendar link metadata when remote doesn't override content.
     */
    fun mergeNightTasks(
        localTasks: List<NightTaskEntity>,
        localTombstones: Map<String, Long>,
        remoteTasks: List<NightTaskCloudDto>,
        remoteTombstones: Map<String, Long>
    ): NightMergeResult {
        val mergedTombstones = HashMap<String, Long>(localTombstones)
        remoteTombstones.forEach { (uid, ts) ->
            val prev = mergedTombstones[uid] ?: 0L
            if (ts > prev) mergedTombstones[uid] = ts
        }

        val localByUid = localTasks
            .filter { it.syncUid.isNotBlank() }
            .associateBy { it.syncUid }
        val remoteByUid = remoteTasks
            .filter { it.syncUid.isNotBlank() }
            .associateBy { it.syncUid }

        val allUids = (localByUid.keys + remoteByUid.keys).toSet()
        val upserts = mutableListOf<NightTaskEntity>()
        val deletedToRemove = mutableListOf<String>()
        var changedLocal = false
        var shouldPushBack = false

        for (uid in allUids) {
            val l = localByUid[uid]
            val r = remoteByUid[uid]
            val deletedTs = mergedTombstones[uid] ?: 0L

            when {
                l != null && r != null -> {
                    val lUpd = maxOf(l.updatedAtMillis, l.createdAtMillis)
                    val rUpd = maxOf(r.updatedAtMillis, r.createdAtMillis)
                    if (deletedTs > maxOf(lUpd, rUpd)) {
                        deletedToRemove += uid
                        changedLocal = true
                    } else if (rUpd > lUpd) {
                        val mergedEntity = r.toEntity(existingLocal = l)
                        if (mergedEntity != l) {
                            upserts += mergedEntity
                            changedLocal = true
                        }
                    } else if (lUpd > rUpd) {
                        shouldPushBack = true
                    }
                }
                l != null -> {
                    val lUpd = maxOf(l.updatedAtMillis, l.createdAtMillis)
                    if (deletedTs > 0L && deletedTs >= lUpd) {
                        deletedToRemove += uid
                        changedLocal = true
                    } else if (!uid.startsWith("seed-")) {
                        shouldPushBack = true
                    } else if (lUpd > 10L) {
                        // Edited starter seed task locally
                        shouldPushBack = true
                    }
                }
                r != null -> {
                    val rUpd = maxOf(r.updatedAtMillis, r.createdAtMillis)
                    if (deletedTs > 0L && deletedTs >= rUpd) {
                        shouldPushBack = true
                    } else {
                        upserts += r.toEntity(existingLocal = null)
                        changedLocal = true
                    }
                }
            }
        }

        return NightMergeResult(
            upserts = upserts,
            deletedSyncUidsToRemoveLocally = deletedToRemove,
            mergedTombstones = mergedTombstones,
            changedLocal = changedLocal,
            shouldPushBack = shouldPushBack
        )
    }

    fun NightTaskCloudDto.toEntity(existingLocal: NightTaskEntity?): NightTaskEntity {
        return NightTaskEntity(
            id = existingLocal?.id ?: 0L,
            title = title,
            notes = notes,
            targetDateIso = targetDateIso,
            category = category,
            priority = priority,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
            syncWithCalendar = syncWithCalendar,
            googleEventId = existingLocal?.googleEventId,
            googleHtmlLink = existingLocal?.googleHtmlLink,
            syncState = existingLocal?.syncState ?: dev.quintero.nexus.feature.planner.SyncState.PENDING_SYNC.name,
            completed = completed,
            completedAtMillis = completedAtMillis,
            createdAtMillis = if (createdAtMillis > 0L) createdAtMillis else (existingLocal?.createdAtMillis ?: System.currentTimeMillis()),
            syncUid = syncUid,
            updatedAtMillis = updatedAtMillis
        )
    }

    fun NightTaskEntity.toCloudDto(): NightTaskCloudDto {
        return NightTaskCloudDto(
            syncUid = syncUid,
            title = title,
            notes = notes,
            targetDateIso = targetDateIso,
            category = category,
            priority = priority,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
            syncWithCalendar = syncWithCalendar,
            completed = completed,
            completedAtMillis = completedAtMillis,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = maxOf(updatedAtMillis, createdAtMillis)
        )
    }

    /**
     * Conflict-free LWW union merge for Progreso físico weigh-in entries and athlete profile.
     */
    fun mergeBodyProgress(
        localProfile: BodyProgressProfileEntity,
        localEntries: List<BodyProgressEntryEntity>,
        localTombstones: Map<String, Long>,
        remoteProfile: BodyProgressProfileDto?,
        remoteEntries: List<BodyProgressEntryDto>,
        remoteTombstones: Map<String, Long>
    ): BodyProgressMergeResult {
        val mergedTombstones = HashMap<String, Long>(localTombstones)
        remoteTombstones.forEach { (id, ts) ->
            val prev = mergedTombstones[id] ?: 0L
            if (ts > prev) mergedTombstones[id] = ts
        }

        var changedLocal = false
        var shouldPushBack = false

        val mergedProfile = if (remoteProfile != null && remoteProfile.athleteName.isNotBlank()) {
            when {
                remoteProfile.updatedAtMillis > localProfile.updatedAtMillis -> {
                    val entity = remoteProfile.toEntity()
                    if (entity != localProfile) changedLocal = true
                    entity
                }
                localProfile.updatedAtMillis > remoteProfile.updatedAtMillis -> {
                    shouldPushBack = true
                    localProfile
                }
                else -> localProfile
            }
        } else {
            shouldPushBack = true
            localProfile
        }

        val localMap = localEntries.associateBy { it.id }
        val remoteMap = remoteEntries.associate { it.id to it.toEntity() }
        val allIds = (localMap.keys + remoteMap.keys).toSet()
        val mergedList = mutableListOf<BodyProgressEntryEntity>()

        for (id in allIds) {
            val l = localMap[id]
            val r = remoteMap[id]
            val deletedTs = mergedTombstones[id] ?: 0L

            val winner: BodyProgressEntryEntity? = when {
                l != null && r != null -> {
                    val maxTs = maxOf(l.updatedAtMillis, r.updatedAtMillis)
                    if (deletedTs > maxTs) {
                        changedLocal = true
                        null
                    } else if (r.updatedAtMillis > l.updatedAtMillis) {
                        if (r != l) changedLocal = true
                        r
                    } else if (l.updatedAtMillis > r.updatedAtMillis) {
                        shouldPushBack = true
                        l
                    } else {
                        l
                    }
                }
                l != null -> {
                    if (deletedTs > 0L && deletedTs >= l.updatedAtMillis) {
                        changedLocal = true
                        null
                    } else {
                        shouldPushBack = true
                        l
                    }
                }
                r != null -> {
                    if (deletedTs > 0L && deletedTs >= r.updatedAtMillis) {
                        shouldPushBack = true
                        null
                    } else {
                        changedLocal = true
                        r
                    }
                }
                else -> null
            }

            if (winner != null) {
                mergedList += winner
            }
        }

        val sorted = mergedList.sortedWith(
            compareBy<BodyProgressEntryEntity> { it.dateIso }.thenBy { it.id }
        )

        return BodyProgressMergeResult(
            mergedProfile = mergedProfile,
            mergedEntries = sorted,
            mergedTombstones = mergedTombstones,
            changedLocal = changedLocal,
            shouldPushBack = shouldPushBack
        )
    }
}
