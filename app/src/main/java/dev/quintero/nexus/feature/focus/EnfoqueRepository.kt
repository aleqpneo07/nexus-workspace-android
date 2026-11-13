package dev.quintero.nexus.feature.focus

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.quintero.nexus.core.crypto.SyncCryptoManager
import dev.quintero.nexus.core.util.DateTimeHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Context.enfoquePrefs by preferencesDataStore(name = "nexus_focus_prefs")

data class EnfoqueCloudAccount(
    val identifier: String,
    val topicId: String,
    val encryptionKeyBase64: String
)

class EnfoqueRepository(
    private val context: Context,
    private val dao: EnfoqueDao,
    private val ntfySyncService: EnfoqueNtfySyncService = EnfoqueNtfySyncService()
) {
    private object PrefKeys {
        val ACCOUNT_IDENTIFIER = stringPreferencesKey("enfoque_account_identifier")
        val ACCOUNT_TOPIC_ID = stringPreferencesKey("enfoque_account_topic_id")
        val ACCOUNT_KEY_B64 = stringPreferencesKey("enfoque_account_key_b64")
        val LAST_SYNC_ID = stringPreferencesKey("enfoque_last_sync_id")
        val DEVICE_ID = stringPreferencesKey("enfoque_device_id")
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(EnfoqueBackupPayload::class.java).indent("  ")

    val tasksFlow: Flow<List<EnfoqueTaskEntity>> = dao.observeTasks()
    val subtasksFlow: Flow<List<EnfoqueSubtaskEntity>> = dao.observeSubtasks()

    val cloudAccountFlow: Flow<EnfoqueCloudAccount?> = context.enfoquePrefs.data.map { prefs ->
        val id = prefs[PrefKeys.ACCOUNT_IDENTIFIER].orEmpty()
        val topic = prefs[PrefKeys.ACCOUNT_TOPIC_ID].orEmpty()
        val keyB64 = prefs[PrefKeys.ACCOUNT_KEY_B64].orEmpty()
        if (id.isNotBlank() && topic.isNotBlank() && keyB64.isNotBlank()) {
            EnfoqueCloudAccount(id, topic, keyB64)
        } else {
            null
        }
    }

    suspend fun ensureSeededIfEmpty() {
        if (dao.countTasks() == 0) {
            val (tasks, subtasks) = EnfoqueDomainHelper.getInitialSeedData(DateTimeHelper.todayIso())
            dao.replaceAll(tasks, subtasks)
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val current = context.enfoquePrefs.data.first()[PrefKeys.DEVICE_ID]
        if (!current.isNullOrBlank()) return current
        val created = EnfoqueDomainHelper.makeSafeId("dev")
        context.enfoquePrefs.edit { it[PrefKeys.DEVICE_ID] = created }
        return created
    }

    suspend fun getLastSyncId(): String =
        context.enfoquePrefs.data.first()[PrefKeys.LAST_SYNC_ID].orEmpty()

    suspend fun setLastSyncId(syncId: String) {
        context.enfoquePrefs.edit { it[PrefKeys.LAST_SYNC_ID] = syncId }
    }

    suspend fun connectCloudAccount(identifier: String, password: String): EnfoqueCloudAccount {
        val cleanId = identifier.trim()
        val cleanPass = password.trim()
        val topicId = EnfoqueDomainHelper.deriveAccountTopicId(cleanId, cleanPass)
        val keyBytes = SyncCryptoManager.deriveKeyBytes(cleanId, cleanPass)
        val keyB64 = SyncCryptoManager.encodeKeyBase64(keyBytes)
        val account = EnfoqueCloudAccount(cleanId, topicId, keyB64)
        context.enfoquePrefs.edit {
            it[PrefKeys.ACCOUNT_IDENTIFIER] = account.identifier
            it[PrefKeys.ACCOUNT_TOPIC_ID] = account.topicId
            it[PrefKeys.ACCOUNT_KEY_B64] = account.encryptionKeyBase64
        }
        return account
    }

    suspend fun disconnectCloudAccount() {
        context.enfoquePrefs.edit {
            it.remove(PrefKeys.ACCOUNT_IDENTIFIER)
            it.remove(PrefKeys.ACCOUNT_TOPIC_ID)
            it.remove(PrefKeys.ACCOUNT_KEY_B64)
        }
    }

    suspend fun upsertTaskAndSubtasks(
        task: EnfoqueTaskEntity,
        subtasks: List<EnfoqueSubtaskEntity>? = null
    ) {
        dao.upsertTask(task)
        if (subtasks != null) {
            dao.deleteSubtasksByTaskId(task.id)
            if (subtasks.isNotEmpty()) dao.upsertSubtasks(subtasks)
        }
    }

    suspend fun upsertSubtask(subtask: EnfoqueSubtaskEntity) {
        dao.upsertSubtask(subtask)
    }

    suspend fun deleteSubtask(subtaskId: String) {
        dao.deleteSubtaskById(subtaskId)
    }

    suspend fun deleteTask(taskId: String) {
        dao.deleteTaskWithSubtasks(taskId)
    }

    suspend fun resetDemo(todayIso: String) {
        val (tasks, subtasks) = EnfoqueDomainHelper.getInitialSeedData(todayIso)
        dao.replaceAll(tasks, subtasks)
    }

    fun exportBackupJson(
        tasks: List<EnfoqueTaskEntity>,
        subtasks: List<EnfoqueSubtaskEntity>
    ): String {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val payload = EnfoqueBackupPayload(
            version = 1,
            exportedAt = nowIso,
            tasks = tasks.map { it.toDto() },
            subtasks = subtasks.map { it.toDto() }
        )
        return backupAdapter.toJson(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Pair<Int, Int>> = runCatching {
        val parsed = backupAdapter.fromJson(rawJson)
            ?: error("Archivo JSON de Enfoque no válido.")
        val taskEntities = parsed.tasks.map { it.toEntity() }
        val subEntities = parsed.subtasks.map { it.toEntity() }
        dao.replaceAll(taskEntities, subEntities)
        taskEntities.size to subEntities.size
    }

    suspend fun pushCloudSnapshot(account: EnfoqueCloudAccount): Boolean {
        val keyBytes = SyncCryptoManager.decodeKeyBase64(account.encryptionKeyBase64) ?: return false
        val tasks = dao.getAllTasks()
        val subtasks = dao.getAllSubtasks()
        val syncId = EnfoqueDomainHelper.makeSafeId("sync")
        setLastSyncId(syncId)
        val payload = EnfoqueNtfyPayload(
            type = "state_sync",
            syncId = syncId,
            deviceId = getOrCreateDeviceId(),
            accountId = account.identifier,
            tasks = tasks.map { it.toDto() },
            subtasks = subtasks.map { it.toDto() }
        )
        return ntfySyncService.publishSnapshot(account.topicId, keyBytes, payload)
    }

    suspend fun pullCloudSnapshot(account: EnfoqueCloudAccount): Result<Boolean> = runCatching {
        val keyBytes = SyncCryptoManager.decodeKeyBase64(account.encryptionKeyBase64)
            ?: error("Clave E2EE inválida")
        val remote = ntfySyncService.fetchLatestSnapshot(account.topicId, keyBytes)
        val lastSyncId = getLastSyncId()
        if (remote != null) {
            if (remote.syncId != lastSyncId && remote.tasks.isNotEmpty()) {
                dao.replaceAll(
                    remote.tasks.map { it.toEntity() },
                    remote.subtasks.map { it.toEntity() }
                )
                setLastSyncId(remote.syncId)
                return@runCatching true
            }
            return@runCatching false
        } else {
            pushCloudSnapshot(account)
            return@runCatching true
        }
    }

    private fun EnfoqueTaskEntity.toDto() = EnfoqueTaskDto(
        id = id,
        ownerId = ownerId,
        title = title,
        subtitle = subtitle,
        category = category,
        status = status,
        dueDate = dueDateIso,
        tag = tag,
        progress = progress,
        totalSubtasks = totalSubtasks,
        completedSubtasks = completedSubtasks
    )

    private fun EnfoqueSubtaskEntity.toDto() = EnfoqueSubtaskDto(
        id = id,
        ownerId = ownerId,
        taskId = taskId,
        title = title,
        completed = completed,
        scheduleLabel = scheduleLabel,
        sortOrder = sortOrder
    )

    private fun EnfoqueTaskDto.toEntity() = EnfoqueTaskEntity(
        id = id,
        ownerId = ownerId ?: "demo-user",
        title = title,
        subtitle = subtitle.orEmpty(),
        category = category,
        status = status,
        dueDateIso = dueDate,
        tag = tag ?: "Entrega",
        progress = progress,
        totalSubtasks = totalSubtasks,
        completedSubtasks = completedSubtasks
    )

    private fun EnfoqueSubtaskDto.toEntity() = EnfoqueSubtaskEntity(
        id = id,
        ownerId = ownerId ?: "demo-user",
        taskId = taskId,
        title = title,
        completed = completed,
        scheduleLabel = scheduleLabel.orEmpty(),
        sortOrder = sortOrder
    )
}
