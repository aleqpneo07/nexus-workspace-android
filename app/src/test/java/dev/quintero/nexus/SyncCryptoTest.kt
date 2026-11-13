package dev.quintero.nexus

import dev.quintero.nexus.core.crypto.SyncCryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCryptoTest {

    @Test
    fun deriveKeyBytes_isDeterministicAndCaseNormalized() {
        val k1 = SyncCryptoManager.deriveKeyBytes("  AlexDev ", "1234")
        val k2 = SyncCryptoManager.deriveKeyBytes("alexdev", "1234")
        val kDifferentPin = SyncCryptoManager.deriveKeyBytes("alexdev", "9999")
        val kDifferentUser = SyncCryptoManager.deriveKeyBytes("other", "1234")

        assertEquals(32, k1.size)
        assertArrayEquals(k1, k2)
        assertFalse(k1.contentEquals(kDifferentPin))
        assertFalse(k1.contentEquals(kDifferentUser))
    }

    @Test
    fun base64Codec_roundtripWorksCleanly() {
        val key = SyncCryptoManager.deriveKeyBytes("alejandro", "pin-42")
        val encoded = SyncCryptoManager.encodeKeyBase64(key)
        val decoded = SyncCryptoManager.decodeKeyBase64(encoded)
        assertNotNull(decoded)
        assertArrayEquals(key, decoded)
        assertNull(SyncCryptoManager.decodeKeyBase64("not-valid-base64"))
    }

    @Test
    fun encryptAndDecryptEnvelope_roundtripWithRandomIvAndAad() {
        val key = SyncCryptoManager.deriveKeyBytes("user1", "secret-pin")
        val payload = """{"type":"state_sync","tasks":[{"title":"Práctica Sistemas Operativos ✅"}]}"""

        val wire1 = SyncCryptoManager.encryptEnvelope(payload, key, syncId = "sync-1", senderDeviceId = "dev-phone")
        val wire2 = SyncCryptoManager.encryptEnvelope(payload, key, syncId = "sync-1", senderDeviceId = "dev-phone")

        assertTrue(SyncCryptoManager.isEncryptedEnvelope(wire1))
        assertNotEquals(wire1, wire2)
        assertFalse(wire1.contains("Práctica Sistemas Operativos"))

        val meta = SyncCryptoManager.peekMetadata(wire1)
        assertNotNull(meta)
        assertEquals("sync-1", meta!!.syncId)
        assertEquals("dev-phone", meta.senderDeviceId)

        val decrypted = SyncCryptoManager.decryptEnvelope(wire1, key)
        assertNotNull(decrypted)
        assertEquals("sync-1", decrypted!!.syncId)
        assertEquals("dev-phone", decrypted.senderDeviceId)
        assertEquals(payload, decrypted.plaintextJson)
    }

    @Test
    fun decryptEnvelope_failsClosedOnWrongKeyTamperedAadOrPlaintext() {
        val key = SyncCryptoManager.deriveKeyBytes("user1", "correct-pin")
        val wrongKey = SyncCryptoManager.deriveKeyBytes("user1", "wrong-pin")
        val payload = """{"version":1,"sessions":[{"id":"2026-11-13-Día 1"}]}"""

        val wire = SyncCryptoManager.encryptEnvelope(payload, key, syncId = "sync-abc", senderDeviceId = "dev-1")

        // Wrong PIN key must fail GCM tag verification
        assertNull(SyncCryptoManager.decryptEnvelope(wire, wrongKey))

        // Tampering authenticated header metadata (AAD mismatch) must fail
        val tamperedSyncId = wire.replace("sync-abc", "sync-xyz")
        assertNull(SyncCryptoManager.decryptEnvelope(tamperedSyncId, key))

        // Unencrypted legacy JSON must be rejected
        assertNull(SyncCryptoManager.decryptEnvelope(payload, key))
    }
}
