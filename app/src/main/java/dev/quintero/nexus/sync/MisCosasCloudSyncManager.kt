package dev.quintero.nexus.sync

import android.content.Context
import android.content.SharedPreferences
import dev.quintero.nexus.core.crypto.SyncCryptoManager
import dev.quintero.nexus.core.database.NexusDatabase
import dev.quintero.nexus.feature.focus.EnfoqueCloudAccount
import dev.quintero.nexus.feature.focus.EnfoqueNtfySyncService
import dev.quintero.nexus.feature.focus.EnfoqueRepository
import dev.quintero.nexus.sync.MisCosasSyncEngine.toCloudDto
import dev.quintero.nexus.feature.workout.WorkoutRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit

class MisCosasCloudSyncManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val db = NexusDatabase.getInstance(appContext)
    private val nightDao = db.nightTaskDao()
    private val workoutRepo = WorkoutRepository(db.workoutDao())
    private val enfoqueRepo = EnfoqueRepository(appContext, db.enfoqueDao())

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("nexus_cloud_prefs", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val workoutEnvAdapter = moshi.adapter(WorkoutCloudEnvelope::class.java)
    private val nightEnvAdapter = moshi.adapter(NightCloudEnvelope::class.java)
    private val tombstoneMapType =
        Types.newParameterizedType(Map::class.java, String::class.java, Long::class.javaObjectType)
    private val tombstoneAdapter = moshi.adapter<Map<String, Long>>(tombstoneMapType)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val streamClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val mutationJobs = mutableMapOf<SyncModule, Job>()
    private val recentSelfSyncIds: MutableSet<String> =
        Collections.synchronizedSet(linkedSetOf<String>())

    @Volatile
    private var isAppInForeground = false

    @Volatile
    private var liveStreamJob: Job? = null

    @Volatile
    private var activeStreamCall: Call? = null

    private val deviceId: String by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }

    @Volatile
    private var activeKeyBytes: ByteArray? = null

    private val _syncState = MutableStateFlow(loadInitialState())
    val syncState: StateFlow<MisCosasSyncUiState> = _syncState.asStateFlow()

    private fun loadInitialState(): MisCosasSyncUiState {
        val id = prefs.getString(KEY_IDENTIFIER, "").orEmpty()
        val keyB64 = prefs.getString(KEY_AES_KEY_B64, "").orEmpty()
        val keyBytes = SyncCryptoManager.decodeKeyBase64(keyB64)
        val enfoqueTopic = prefs.getString(KEY_ENFOQUE_TOPIC, "").orEmpty()
        val entrenoTopic = prefs.getString(KEY_ENTRENO_TOPIC, "").orEmpty()
        val nightTopic = prefs.getString(KEY_NIGHT_TOPIC, "").orEmpty()
        val lastSync = prefs.getLong(KEY_LAST_SYNC_MILLIS, 0L).takeIf { it > 0L }
        if (id.isBlank() || keyBytes == null || enfoqueTopic.isBlank() || entrenoTopic.isBlank() || nightTopic.isBlank()) {
            activeKeyBytes = null
            return MisCosasSyncUiState(isConnected = false)
        }
        activeKeyBytes = keyBytes
        return MisCosasSyncUiState(
            isConnected = true,
            accountIdentifier = id,
            enfoqueTopicId = enfoqueTopic,
            entrenoTopicId = entrenoTopic,
            nightTopicId = nightTopic,
            lastSyncAtMillis = lastSync
        )
    }

    fun getCurrentEnfoqueAccount(): EnfoqueCloudAccount? {
        val st = _syncState.value
        val keyBytes = activeKeyBytes ?: return null
        return if (st.isConnected && st.enfoqueTopicId.isNotBlank()) {
            EnfoqueCloudAccount(
                identifier = st.accountIdentifier,
                topicId = st.enfoqueTopicId,
                encryptionKeyBase64 = SyncCryptoManager.encodeKeyBase64(keyBytes)
            )
        } else {
            null
        }
    }

    fun connectAccount(
        identifier: String,
        password: String,
        onComplete: ((Result<String>) -> Unit)? = null
    ) {
        val cleanId = identifier.trim()
        val cleanPass = password.trim()
        if (cleanId.isBlank() || cleanPass.isBlank()) {
            onComplete?.invoke(Result.failure(IllegalArgumentException("Introduce usuario y PIN")))
            return
        }

        scope.launch {
            runCatching {
                val (enfoqueTopic, entrenoTopic, nightTopic) =
                    MisCosasSyncEngine.deriveAllTopics(cleanId, cleanPass)
                val keyBytes = SyncCryptoManager.deriveKeyBytes(cleanId, cleanPass)
                val keyB64 = SyncCryptoManager.encodeKeyBase64(keyBytes)
                activeKeyBytes = keyBytes
                prefs.edit()
                    .putString(KEY_IDENTIFIER, cleanId)
                    .putString(KEY_AES_KEY_B64, keyB64)
                    .putString(KEY_ENFOQUE_TOPIC, enfoqueTopic)
                    .putString(KEY_ENTRENO_TOPIC, entrenoTopic)
                    .putString(KEY_NIGHT_TOPIC, nightTopic)
                    .apply()
                enfoqueRepo.connectCloudAccount(cleanId, cleanPass)

                _syncState.update {
                    MisCosasSyncUiState(
                        isConnected = true,
                        accountIdentifier = cleanId,
                        enfoqueTopicId = enfoqueTopic,
                        entrenoTopicId = entrenoTopic,
                        nightTopicId = nightTopic,
                        isSyncing = true,
                        lastSyncAtMillis = it.lastSyncAtMillis,
                        lastError = null
                    )
                }

                syncAllModulesInternal()
                if (isAppInForeground) {
                    restartLiveStreamLocked()
                }
                "Nube E2EE conectada para $cleanId (Enfoque, Entreno y Night Planner)"
            }.onSuccess { msg ->
                scope.launch(Dispatchers.Main) {
                    onComplete?.invoke(Result.success(msg))
                }
            }.onFailure { err ->
                _syncState.update {
                    it.copy(isSyncing = false, lastError = err.message ?: "Error de conexión")
                }
                scope.launch(Dispatchers.Main) {
                    onComplete?.invoke(Result.failure(err))
                }
            }
        }
    }

    fun disconnectAccount() {
        scope.launch {
            liveStreamJob?.cancel()
            activeStreamCall?.cancel()
            liveStreamJob = null
            activeStreamCall = null
            mutationJobs.values.forEach { it.cancel() }
            mutationJobs.clear()
            activeKeyBytes = null

            prefs.edit()
                .remove(KEY_IDENTIFIER)
                .remove(KEY_AES_KEY_B64)
                .remove(KEY_ENFOQUE_TOPIC)
                .remove(KEY_ENTRENO_TOPIC)
                .remove(KEY_NIGHT_TOPIC)
                .remove(KEY_LAST_SYNC_MILLIS)
                .apply()
            enfoqueRepo.disconnectCloudAccount()
            _syncState.value = MisCosasSyncUiState(isConnected = false)
        }
    }

    fun onAppForegroundStart() {
        isAppInForeground = true
        if (_syncState.value.isConnected) {
            syncAllNow()
            restartLiveStreamLocked()
        }
    }

    fun onAppForegroundStop() {
        isAppInForeground = false
        liveStreamJob?.cancel()
        activeStreamCall?.cancel()
        liveStreamJob = null
        activeStreamCall = null
        _syncState.update { it.copy(isLiveConnected = false) }
    }

    fun syncAllNow(onComplete: ((Result<String>) -> Unit)? = null) {
        if (!_syncState.value.isConnected) {
            onComplete?.invoke(Result.failure(IllegalStateException("Conecta tu cuenta nube primero")))
            return
        }
        scope.launch {
            val res = runCatching {
                syncAllModulesInternal()
                "Móvil ↔ tablet sincronizados"
            }
            if (res.isFailure) {
                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        lastError = res.exceptionOrNull()?.message ?: "Error al sincronizar"
                    )
                }
            }
            scope.launch(Dispatchers.Main) {
                onComplete?.invoke(res)
            }
        }
    }

    fun markWorkoutSessionDeleted(sessionId: String) {
        if (sessionId.isBlank()) return
        val map = loadTombstones(KEY_WORKOUT_TOMBSTONES).toMutableMap()
        map[sessionId] = System.currentTimeMillis()
        saveTombstones(KEY_WORKOUT_TOMBSTONES, map)
        notifyLocalMutation(SyncModule.ENTRENO)
    }

    fun markAllWorkoutsCleared(sessionIds: List<String>) {
        if (sessionIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val map = loadTombstones(KEY_WORKOUT_TOMBSTONES).toMutableMap()
        sessionIds.forEach { id ->
            if (id.isNotBlank()) map[id] = now
        }
        saveTombstones(KEY_WORKOUT_TOMBSTONES, map)
        notifyLocalMutation(SyncModule.ENTRENO)
    }

    fun markNightTaskDeleted(syncUid: String) {
        if (syncUid.isBlank()) return
        val map = loadTombstones(KEY_NIGHT_TOMBSTONES).toMutableMap()
        map[syncUid] = System.currentTimeMillis()
        saveTombstones(KEY_NIGHT_TOMBSTONES, map)
        notifyLocalMutation(SyncModule.NIGHT_PLANNER)
    }

    fun notifyLocalMutation(module: SyncModule) {
        if (!_syncState.value.isConnected) return
        synchronized(mutationJobs) {
            mutationJobs[module]?.cancel()
            mutationJobs[module] = scope.launch {
                delay(900)
                runCatching {
                    syncMutex.withLock {
                        if (!_syncState.value.isConnected) return@withLock
                        when (module) {
                            SyncModule.ENTRENO -> syncEntrenoLocked(forcePushLocal = true)
                            SyncModule.NIGHT_PLANNER -> syncNightLocked(forcePushLocal = true)
                            SyncModule.ENFOQUE -> {
                                getCurrentEnfoqueAccount()?.let { acc ->
                                    enfoqueRepo.pushCloudSnapshot(acc)
                                }
                            }
                        }
                        markSyncSuccess(summaryFor(module))
                    }
                }.onFailure { err ->
                    _syncState.update {
                        it.copy(lastError = err.message ?: "Error al subir cambios")
                    }
                }
            }
        }
    }

    private suspend fun syncAllModulesInternal() {
        syncMutex.withLock {
            if (!_syncState.value.isConnected) return
            _syncState.update { it.copy(isSyncing = true, lastError = null) }

            val updates = mutableListOf<String>()

            val enfoqueUpdated = syncEnfoqueLocked()
            if (enfoqueUpdated) updates += "Enfoque"

            val entrenoUpdated = syncEntrenoLocked(forcePushLocal = false)
            if (entrenoUpdated) updates += "Entreno"

            val nightUpdated = syncNightLocked(forcePushLocal = false)
            if (nightUpdated) updates += "Night Planner"

            val summary = if (updates.isEmpty()) {
                "Todo al día"
            } else {
                "Actualizado: ${updates.joinToString(", ")}"
            }
            markSyncSuccess(summary)
        }
    }

    private suspend fun syncEnfoqueLocked(): Boolean {
        val account = getCurrentEnfoqueAccount() ?: return false
        val pullRes = enfoqueRepo.pullCloudSnapshot(account)
        val pulledChanged = pullRes.getOrDefault(false)
        if (!pulledChanged && enfoqueRepo.getLastSyncId().isBlank()) {
            enfoqueRepo.pushCloudSnapshot(account)
        }
        return pulledChanged
    }

    private suspend fun syncEntrenoLocked(forcePushLocal: Boolean): Boolean {
        val topicId = _syncState.value.entrenoTopicId
        val keyBytes = activeKeyBytes
        if (topicId.isBlank() || keyBytes == null) return false

        val localSessions = workoutRepo.getAllSessions()
        val localTombstones = loadTombstones(KEY_WORKOUT_TOMBSTONES)

        val remoteEnv = fetchLatestEnvelope(topicId, keyBytes) { json ->
            runCatching { workoutEnvAdapter.fromJson(json) }.getOrNull()
        }

        val remoteSessions = remoteEnv?.sessions.orEmpty()
        val remoteTombstones = remoteEnv?.deletedIds?.associate { it.id to it.deletedAtMillis }.orEmpty()

        val merge = MisCosasSyncEngine.mergeWorkoutSessions(
            localSessions = localSessions,
            localTombstones = localTombstones,
            remoteSessions = remoteSessions,
            remoteTombstones = remoteTombstones
        )

        saveTombstones(KEY_WORKOUT_TOMBSTONES, merge.mergedTombstones)

        if (merge.changedLocal) {
            workoutRepo.replaceSessionsFromSync(merge.mergedSessions)
        }

        val shouldPush = forcePushLocal ||
            remoteEnv == null ||
            merge.shouldPushBack ||
            merge.changedLocal

        if (shouldPush) {
            val syncId = UUID.randomUUID().toString()
            trackSelfSyncId(syncId)
            val outbound = WorkoutCloudEnvelope(
                version = 1,
                updatedAt = System.currentTimeMillis(),
                senderDeviceId = deviceId,
                syncId = syncId,
                sessions = merge.mergedSessions.map { s ->
                    WorkoutCloudSessionDto(
                        id = s.id,
                        dateIso = s.dateIso,
                        dayKey = s.dayKey,
                        dayLabel = s.dayLabel,
                        notes = s.notes,
                        savedAtIso = s.savedAtIso,
                        exercises = s.exercises
                    )
                },
                deletedIds = merge.mergedTombstones.map { (id, ts) ->
                    WorkoutDeletedEntry(id = id, deletedAtMillis = ts)
                }
            )
            publishEncryptedEnvelope(
                topicId = topicId,
                keyBytes = keyBytes,
                plaintextJson = workoutEnvAdapter.toJson(outbound),
                syncId = syncId,
                senderDeviceId = deviceId,
                title = "Nexus Workout E2EE"
            )
        }

        return merge.changedLocal
    }

    private suspend fun syncNightLocked(forcePushLocal: Boolean): Boolean {
        val topicId = _syncState.value.nightTopicId
        val keyBytes = activeKeyBytes
        if (topicId.isBlank() || keyBytes == null) return false

        ensureLocalNightSyncUids()
        val localTasks = nightDao.getAllTasksOnce()
        val localTombstones = loadTombstones(KEY_NIGHT_TOMBSTONES)

        val remoteEnv = fetchLatestEnvelope(topicId, keyBytes) { json ->
            runCatching { nightEnvAdapter.fromJson(json) }.getOrNull()
        }

        val remoteTasks = remoteEnv?.tasks.orEmpty()
        val remoteTombstones = remoteEnv?.deletedUids?.associate { it.syncUid to it.deletedAtMillis }.orEmpty()

        val merge = MisCosasSyncEngine.mergeNightTasks(
            localTasks = localTasks,
            localTombstones = localTombstones,
            remoteTasks = remoteTasks,
            remoteTombstones = remoteTombstones
        )

        saveTombstones(KEY_NIGHT_TOMBSTONES, merge.mergedTombstones)

        if (merge.deletedSyncUidsToRemoveLocally.isNotEmpty()) {
            merge.deletedSyncUidsToRemoveLocally.forEach { uid ->
                nightDao.deleteTaskBySyncUid(uid)
            }
        }
        if (merge.upserts.isNotEmpty()) {
            nightDao.insertAll(merge.upserts)
        }

        val shouldPush = forcePushLocal ||
            remoteEnv == null ||
            merge.shouldPushBack ||
            merge.changedLocal

        if (shouldPush) {
            val latestLocal = nightDao.getAllTasksOnce()
            val syncId = UUID.randomUUID().toString()
            trackSelfSyncId(syncId)
            val outbound = NightCloudEnvelope(
                version = 1,
                updatedAt = System.currentTimeMillis(),
                senderDeviceId = deviceId,
                syncId = syncId,
                tasks = latestLocal.filter { it.syncUid.isNotBlank() }.map { it.toCloudDto() },
                deletedUids = merge.mergedTombstones.map { (uid, ts) ->
                    NightDeletedUidEntry(syncUid = uid, deletedAtMillis = ts)
                }
            )
            publishEncryptedEnvelope(
                topicId = topicId,
                keyBytes = keyBytes,
                plaintextJson = nightEnvAdapter.toJson(outbound),
                syncId = syncId,
                senderDeviceId = deviceId,
                title = "Nexus Planner E2EE"
            )
        }

        return merge.changedLocal
    }

    private suspend fun ensureLocalNightSyncUids() {
        val tasks = nightDao.getAllTasksOnce()
        tasks.forEach { task ->
            if (task.syncUid.isBlank()) {
                val fixedUid = "local-${task.id}-${UUID.randomUUID().toString().take(8)}"
                nightDao.updateTask(
                    task.copy(
                        syncUid = fixedUid,
                        updatedAtMillis = maxOf(task.updatedAtMillis, task.createdAtMillis)
                    )
                )
            }
        }
    }

    private fun markSyncSuccess(summary: String) {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_SYNC_MILLIS, now).apply()
        _syncState.update {
            it.copy(
                isSyncing = false,
                lastSyncAtMillis = now,
                lastSyncSummary = summary,
                lastError = null
            )
        }
    }

    private fun summaryFor(module: SyncModule): String = when (module) {
        SyncModule.ENFOQUE -> "Enfoque enviado a la nube"
        SyncModule.ENTRENO -> "Entreno enviado a la nube"
        SyncModule.NIGHT_PLANNER -> "Night Planner enviado a la nube"
    }

    private fun restartLiveStreamLocked() {
        liveStreamJob?.cancel()
        activeStreamCall?.cancel()
        val st = _syncState.value
        if (!st.isConnected) return
        val topicsCsv = listOf(st.enfoqueTopicId, st.entrenoTopicId, st.nightTopicId)
            .filter { it.isNotBlank() }
            .joinToString(",") { URLEncoder.encode(it, "UTF-8") }
        if (topicsCsv.isBlank()) return

        liveStreamJob = scope.launch {
            var backoffMs = 3_000L
            while (isActive && isAppInForeground && _syncState.value.isConnected) {
                try {
                    val req = Request.Builder()
                        .url("${EnfoqueNtfySyncService.NTFY_BASE_URL}/$topicsCsv/json?since=30s")
                        .get()
                        .build()
                    val call = streamClient.newCall(req)
                    activeStreamCall = call
                    call.execute().use { res ->
                        if (!res.isSuccessful) {
                            error("HTTP ${res.code}")
                        }
                        backoffMs = 3_000L
                        _syncState.update { it.copy(isLiveConnected = true) }
                        val body = res.body ?: return@use
                        BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8)).use { reader ->
                            while (isActive && isAppInForeground) {
                                val line = reader.readLine() ?: break
                                if (line.isBlank()) continue
                                handleStreamLine(line)
                            }
                        }
                    }
                } catch (_: Throwable) {
                    _syncState.update { it.copy(isLiveConnected = false) }
                }
                if (isActive && isAppInForeground && _syncState.value.isConnected) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(20_000L)
                }
            }
            _syncState.update { it.copy(isLiveConnected = false) }
        }
    }

    private suspend fun handleStreamLine(line: String) {
        val evt = runCatching { JSONObject(line) }.getOrNull() ?: return
        if (evt.optString("event") != "message") return
        val topic = evt.optString("topic")
        val st = _syncState.value

        // Skip self-echoes using unencrypted routing metadata (syncId / senderDeviceId)
        val messageRaw = evt.optString("message", "")
        val meta = SyncCryptoManager.peekMetadata(messageRaw)
        if (meta != null) {
            val sid = meta.syncId
            val sender = meta.senderDeviceId
            if ((sid.isNotBlank() && sid in recentSelfSyncIds) || sender == deviceId) {
                return
            }
        }

        val targetModule = when (topic) {
            st.enfoqueTopicId -> SyncModule.ENFOQUE
            st.entrenoTopicId -> SyncModule.ENTRENO
            st.nightTopicId -> SyncModule.NIGHT_PLANNER
            else -> null
        } ?: return

        syncMutex.withLock {
            if (!_syncState.value.isConnected) return@withLock
            val changed = when (targetModule) {
                SyncModule.ENFOQUE -> {
                    getCurrentEnfoqueAccount()?.let { enfoqueRepo.pullCloudSnapshot(it).getOrDefault(false) } ?: false
                }
                SyncModule.ENTRENO -> syncEntrenoLocked(forcePushLocal = false)
                SyncModule.NIGHT_PLANNER -> syncNightLocked(forcePushLocal = false)
            }
            if (changed) {
                val label = when (targetModule) {
                    SyncModule.ENFOQUE -> "Enfoque actualizado desde otro dispositivo"
                    SyncModule.ENTRENO -> "Entreno actualizado desde otro dispositivo"
                    SyncModule.NIGHT_PLANNER -> "Night Planner actualizado desde otro dispositivo"
                }
                markSyncSuccess(label)
            }
        }
    }

    private fun <T> fetchLatestEnvelope(
        topicId: String,
        keyBytes: ByteArray,
        parser: (String) -> T?
    ): T? {
        val encoded = URLEncoder.encode(topicId, "UTF-8")
        val req = Request.Builder()
            .url("${EnfoqueNtfySyncService.NTFY_BASE_URL}/$encoded/json?poll=1&since=all")
            .get()
            .build()
        val bodyText = httpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code} al leer sincronización")
            res.body?.string().orEmpty()
        }
        val lines = bodyText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
            .asReversed()

        for (line in lines) {
            val evt = runCatching { JSONObject(line) }.getOrNull() ?: continue
            if (evt.optString("event") != "message") continue
            val attUrl = evt.optJSONObject("attachment")?.optString("url")
            val rawWire = when {
                !attUrl.isNullOrBlank() -> {
                    val attReq = Request.Builder().url(attUrl).get().build()
                    runCatching {
                        httpClient.newCall(attReq).execute().use { r ->
                            if (r.isSuccessful) r.body?.string().orEmpty() else ""
                        }
                    }.getOrDefault("")
                }
                else -> evt.optString("message", "")
            }
            if (rawWire.isBlank()) continue
            val decrypted = SyncCryptoManager.decryptEnvelope(rawWire, keyBytes) ?: continue
            val parsed = parser(decrypted.plaintextJson)
            if (parsed != null) return parsed
        }
        return null
    }

    private fun publishEncryptedEnvelope(
        topicId: String,
        keyBytes: ByteArray,
        plaintextJson: String,
        syncId: String,
        senderDeviceId: String,
        title: String
    ) {
        val encryptedWire = SyncCryptoManager.encryptEnvelope(
            plaintextJson = plaintextJson,
            keyBytes = keyBytes,
            syncId = syncId,
            senderDeviceId = senderDeviceId
        )
        publishJson(topicId, encryptedWire, title)
    }

    private fun publishJson(topicId: String, json: String, title: String) {
        val encoded = URLEncoder.encode(topicId, "UTF-8")
        val builder = Request.Builder()
            .url("${EnfoqueNtfySyncService.NTFY_BASE_URL}/$encoded")
            .addHeader("Title", title)
        if (json.toByteArray(Charsets.UTF_8).size > 3500) {
            builder.addHeader("Filename", "nexus-sync.enc.json")
        }
        val req = builder
            .post(json.toRequestBody("text/plain; charset=utf-8".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                error("HTTP ${res.code} al publicar en nube")
            }
        }
    }

    private fun trackSelfSyncId(syncId: String) {
        recentSelfSyncIds.add(syncId)
        if (recentSelfSyncIds.size > 40) {
            val first = recentSelfSyncIds.firstOrNull()
            if (first != null) recentSelfSyncIds.remove(first)
        }
    }

    private fun loadTombstones(key: String): Map<String, Long> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        val parsed = runCatching { tombstoneAdapter.fromJson(raw) }.getOrNull() ?: return emptyMap()
        val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        return parsed.filterValues { it >= cutoff }
    }

    private fun saveTombstones(key: String, map: Map<String, Long>) {
        val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val pruned = map.filterValues { it >= cutoff }
        prefs.edit().putString(key, tombstoneAdapter.toJson(pruned)).apply()
    }

    companion object {
        private const val KEY_IDENTIFIER = "account_identifier"
        private const val KEY_AES_KEY_B64 = "account_aes_key_b64"
        private const val KEY_ENFOQUE_TOPIC = "enfoque_topic_id"
        private const val KEY_ENTRENO_TOPIC = "entreno_topic_id"
        private const val KEY_NIGHT_TOPIC = "night_topic_id"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_SYNC_MILLIS = "last_sync_millis"
        private const val KEY_WORKOUT_TOMBSTONES = "workout_tombstones_json"
        private const val KEY_NIGHT_TOMBSTONES = "night_tombstones_json"

        @Volatile
        private var INSTANCE: MisCosasCloudSyncManager? = null

        fun getInstance(context: Context): MisCosasCloudSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MisCosasCloudSyncManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
