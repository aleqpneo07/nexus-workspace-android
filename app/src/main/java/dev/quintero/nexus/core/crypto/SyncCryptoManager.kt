package dev.quintero.nexus.core.crypto

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private data class EncryptedEnvelopeWire(
    val v: Int = 1,
    val alg: String = "AES-256-GCM",
    val syncId: String = "",
    val senderDeviceId: String = "",
    val iv: String = "",
    val ct: String = ""
)

/**
 * End-to-end authenticated encryption (AES-256-GCM) with PBKDF2-HMAC-SHA256 key derivation.
 * Keys are derived deterministically on each paired device from the user's account + PIN
 * so payloads never leave the device in plaintext.
 */
object SyncCryptoManager {

    private const val ENVELOPE_VERSION = 1
    private const val ALGORITHM = "AES-256-GCM"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PBKDF2_ITERATIONS = 31_000
    private const val AES_KEY_BYTES = 32
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private val secureRandom = SecureRandom()
    private val keyCache = ConcurrentHashMap<String, ByteArray>()
    private val envelopeAdapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(EncryptedEnvelopeWire::class.java)

    data class EncryptedMetadata(
        val syncId: String,
        val senderDeviceId: String
    )

    data class DecryptedEnvelope(
        val syncId: String,
        val senderDeviceId: String,
        val plaintextJson: String
    )

    fun deriveKeyBytes(accountIdentifier: String, pinOrPassword: String): ByteArray {
        val normalizedId = accountIdentifier.trim().lowercase(Locale.ROOT)
        val cacheKey = sha256Hex("$normalizedId\u0000${pinOrPassword.trim()}")
        keyCache[cacheKey]?.let { return it }

        val salt = MessageDigest.getInstance("SHA-256")
            .digest("nexus:e2ee:v1:$normalizedId".toByteArray(Charsets.UTF_8))
        val derived = pbkdf2HmacSha256(
            password = pinOrPassword.trim().toByteArray(Charsets.UTF_8),
            salt = salt,
            iterations = PBKDF2_ITERATIONS,
            keyLengthBytes = AES_KEY_BYTES
        )
        keyCache[cacheKey] = derived
        return derived
    }

    fun encodeKeyBase64(keyBytes: ByteArray): String = Base64Codec.encode(keyBytes)

    fun decodeKeyBase64(keyBase64: String): ByteArray? = runCatching {
        Base64Codec.decode(keyBase64).takeIf { it.size == AES_KEY_BYTES }
    }.getOrNull()

    fun encryptEnvelope(
        plaintextJson: String,
        keyBytes: ByteArray,
        syncId: String,
        senderDeviceId: String
    ): String {
        require(keyBytes.size == AES_KEY_BYTES) { "AES-256 key must be 32 bytes" }

        val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        cipher.updateAAD(buildAad(syncId, senderDeviceId))
        val ciphertext = cipher.doFinal(plaintextJson.toByteArray(Charsets.UTF_8))

        val wire = EncryptedEnvelopeWire(
            v = ENVELOPE_VERSION,
            alg = ALGORITHM,
            syncId = syncId,
            senderDeviceId = senderDeviceId,
            iv = Base64Codec.encode(iv),
            ct = Base64Codec.encode(ciphertext)
        )
        return envelopeAdapter.toJson(wire)
    }

    fun peekMetadata(rawWire: String): EncryptedMetadata? {
        val trimmed = rawWire.trimStart('\uFEFF').trim()
        if (!trimmed.startsWith("{")) return null
        val obj = runCatching { envelopeAdapter.fromJson(trimmed) }.getOrNull() ?: return null
        return if (obj.syncId.isNotBlank() || obj.senderDeviceId.isNotBlank()) {
            EncryptedMetadata(syncId = obj.syncId, senderDeviceId = obj.senderDeviceId)
        } else {
            null
        }
    }

    fun isEncryptedEnvelope(rawWire: String): Boolean {
        val trimmed = rawWire.trimStart('\uFEFF').trim()
        if (!trimmed.startsWith("{")) return false
        val obj = runCatching { envelopeAdapter.fromJson(trimmed) }.getOrNull() ?: return false
        return obj.v == ENVELOPE_VERSION &&
            obj.alg == ALGORITHM &&
            obj.iv.isNotBlank() &&
            obj.ct.isNotBlank()
    }

    fun decryptEnvelope(rawWire: String, keyBytes: ByteArray): DecryptedEnvelope? = runCatching {
        if (keyBytes.size != AES_KEY_BYTES) return null
        val trimmed = rawWire.trimStart('\uFEFF').trim()
        if (!trimmed.startsWith("{")) return null

        val obj = envelopeAdapter.fromJson(trimmed) ?: return null
        if (obj.v != ENVELOPE_VERSION || obj.alg != ALGORITHM || obj.iv.isBlank() || obj.ct.isBlank()) {
            return null
        }

        val syncId = obj.syncId
        val senderDeviceId = obj.senderDeviceId
        val iv = Base64Codec.decode(obj.iv)
        if (iv.size != GCM_IV_BYTES) return null
        val ciphertext = Base64Codec.decode(obj.ct)
        if (ciphertext.isEmpty()) return null

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        cipher.updateAAD(buildAad(syncId, senderDeviceId))
        val plaintext = cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        DecryptedEnvelope(
            syncId = syncId,
            senderDeviceId = senderDeviceId,
            plaintextJson = plaintext
        )
    }.getOrNull()

    private fun buildAad(syncId: String, senderDeviceId: String): ByteArray =
        "v1|$syncId|$senderDeviceId".toByteArray(Charsets.UTF_8)

    private fun pbkdf2HmacSha256(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLengthBytes: Int
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val macLen = mac.macLength
        val blockCount = (keyLengthBytes + macLen - 1) / macLen
        val out = ByteArray(blockCount * macLen)

        for (block in 1..blockCount) {
            val blockInput = ByteArray(salt.size + 4)
            System.arraycopy(salt, 0, blockInput, 0, salt.size)
            blockInput[salt.size] = (block ushr 24).toByte()
            blockInput[salt.size + 1] = (block ushr 16).toByte()
            blockInput[salt.size + 2] = (block ushr 8).toByte()
            blockInput[salt.size + 3] = block.toByte()

            var u = mac.doFinal(blockInput)
            val t = u.copyOf()
            for (i in 2..iterations) {
                u = mac.doFinal(u)
                for (j in t.indices) {
                    t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
                }
            }
            System.arraycopy(t, 0, out, (block - 1) * macLen, macLen)
        }
        return out.copyOf(keyLengthBytes)
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Pure Kotlin RFC-4648 Base64 codec independent of Android SDK level and Robolectric shadows.
 */
private object Base64Codec {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val DECODE_TABLE = IntArray(256) { -1 }.apply {
        ALPHABET.forEachIndexed { index, c -> this[c.code] = index }
    }

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val sb = StringBuilder(((data.size + 2) / 3) * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val hasB1 = i + 1 < data.size
            val hasB2 = i + 2 < data.size
            val b1 = if (hasB1) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (hasB2) data[i + 2].toInt() and 0xFF else 0
            val triple = (b0 shl 16) or (b1 shl 8) or b2

            sb.append(ALPHABET[(triple ushr 18) and 0x3F])
            sb.append(ALPHABET[(triple ushr 12) and 0x3F])
            sb.append(if (hasB1) ALPHABET[(triple ushr 6) and 0x3F] else '=')
            sb.append(if (hasB2) ALPHABET[triple and 0x3F] else '=')
            i += 3
        }
        return sb.toString()
    }

    fun decode(input: String): ByteArray {
        val clean = input.trim()
        if (clean.isEmpty()) return ByteArray(0)
        require(clean.length % 4 == 0) { "Invalid Base64 length" }
        var padding = 0
        if (clean.endsWith("==")) padding = 2 else if (clean.endsWith("=")) padding = 1

        val outLen = (clean.length / 4) * 3 - padding
        val out = ByteArray(outLen)
        var outIdx = 0
        var i = 0
        while (i < clean.length) {
            val c0 = decodeChar(clean[i])
            val c1 = decodeChar(clean[i + 1])
            val c2 = if (clean[i + 2] == '=') 0 else decodeChar(clean[i + 2])
            val c3 = if (clean[i + 3] == '=') 0 else decodeChar(clean[i + 3])
            val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3

            if (outIdx < outLen) out[outIdx++] = ((triple ushr 16) and 0xFF).toByte()
            if (outIdx < outLen) out[outIdx++] = ((triple ushr 8) and 0xFF).toByte()
            if (outIdx < outLen) out[outIdx++] = (triple and 0xFF).toByte()
            i += 4
        }
        return out
    }

    private fun decodeChar(c: Char): Int {
        val v = if (c.code < 256) DECODE_TABLE[c.code] else -1
        require(v >= 0) { "Invalid Base64 char" }
        return v
    }
}
