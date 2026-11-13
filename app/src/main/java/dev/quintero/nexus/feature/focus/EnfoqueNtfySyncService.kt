package dev.quintero.nexus.feature.focus

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.quintero.nexus.core.crypto.SyncCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * End-to-end encrypted (`AES-256-GCM`) cloud snapshot transport for the Focus module.
 * Envelopes leave the device encrypted so the relay only observes authenticated ciphertext.
 */
class EnfoqueNtfySyncService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(EnfoqueNtfyPayload::class.java)
    private val envelopeAdapter = moshi.adapter(EnfoqueNtfyEnvelope::class.java)

    suspend fun publishSnapshot(
        topicId: String,
        keyBytes: ByteArray,
        payload: EnfoqueNtfyPayload
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val encodedTopic = URLEncoder.encode(topicId, "UTF-8")
            val plaintextJson = payloadAdapter.toJson(payload)
            val encryptedWireJson = SyncCryptoManager.encryptEnvelope(
                plaintextJson = plaintextJson,
                keyBytes = keyBytes,
                syncId = payload.syncId,
                senderDeviceId = payload.deviceId
            )
            val builder = Request.Builder()
                .url("$NTFY_BASE_URL/$encodedTopic")
                .addHeader("Title", "Nexus Focus E2EE")
            if (encryptedWireJson.toByteArray(Charsets.UTF_8).size > 3500) {
                builder.addHeader("Filename", "nexus-focus-sync.enc.json")
            }
            val request = builder
                .post(encryptedWireJson.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrDefault(false)
    }

    suspend fun fetchLatestSnapshot(
        topicId: String,
        keyBytes: ByteArray
    ): EnfoqueNtfyPayload? = withContext(Dispatchers.IO) {
        runCatching {
            val encodedTopic = URLEncoder.encode(topicId, "UTF-8")
            val request = Request.Builder()
                .url("$NTFY_BASE_URL/$encodedTopic/json?poll=1&since=all")
                .get()
                .build()
            val bodyText = client.newCall(request).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string().orEmpty()
            }
            val lines = bodyText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            for (i in lines.indices.reversed()) {
                val env = runCatching { envelopeAdapter.fromJson(lines[i]) }.getOrNull() ?: continue
                if (env.event != "message") continue
                val rawWire = when {
                    !env.attachment?.url.isNullOrBlank() -> {
                        val attReq = Request.Builder().url(env.attachment!!.url!!).get().build()
                        client.newCall(attReq).execute().use { attRes ->
                            if (attRes.isSuccessful) attRes.body?.string().orEmpty() else ""
                        }
                    }
                    !env.message.isNullOrBlank() -> env.message.trimStart('\uFEFF').trim()
                    else -> ""
                }
                if (!rawWire.startsWith("{")) continue
                val decrypted = SyncCryptoManager.decryptEnvelope(rawWire, keyBytes) ?: continue
                val parsed = runCatching { payloadAdapter.fromJson(decrypted.plaintextJson) }.getOrNull() ?: continue
                if (parsed.type == "state_sync") {
                    return@withContext parsed
                }
            }
            null
        }.getOrNull()
    }

    companion object {
        const val NTFY_BASE_URL = "https://ntfy.sh"
    }
}
