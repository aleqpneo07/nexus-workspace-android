package dev.quintero.nexus

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import dev.quintero.nexus.core.database.NexusDatabase
import dev.quintero.nexus.feature.progress.BodyProgressDomainHelper
import dev.quintero.nexus.feature.progress.BodyProgressEntryDto
import dev.quintero.nexus.feature.progress.BodyProgressEntryEntity
import dev.quintero.nexus.feature.progress.BodyProgressProfileDto
import dev.quintero.nexus.feature.progress.BodyProgressProfileEntity
import dev.quintero.nexus.feature.progress.BodyProgressRepository
import dev.quintero.nexus.sync.BodyProgressDeletedEntry
import dev.quintero.nexus.sync.MisCosasSyncEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BodyProgressTest {

    private lateinit var context: Context
    private lateinit var db: NexusDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun domainHelper_computesBmiMifflinDeltasAndNextWeighIn() {
        val bmi = BodyProgressDomainHelper.calculateBmi(weightKg = 80.0, heightCm = 180)
        assertEquals(24.69, bmi ?: 0.0, 0.01)

        val bmrMale = BodyProgressDomainHelper.calculateMifflinStJeorKcal(
            weightKg = 80.0,
            heightCm = 180,
            ageYears = 25
        )
        // 10*80 + 6.25*180 - 5*25 + 5 = 800 + 1125 - 125 + 5 = 1805
        assertEquals(1805, bmrMale)

        val prev = BodyProgressEntryEntity(
            id = "e1",
            dateIso = "2026-10-01",
            displayDate = "01/10",
            weightKg = 80.0,
            bodyFatPct = 15.0,
            muscleKg = 64.0
        )
        // Fat mass prev = 80 * 0.15 = 12.0 kg; curr = 80.5 * 0.145 = 11.6725 kg -> delta fat = -0.3 kg
        val (dw, df, dm) = BodyProgressDomainHelper.computeAutoDeltas(
            prevValid = prev,
            weightKg = 80.5,
            bodyFatPct = 14.5,
            muscleKg = 64.6
        )
        assertEquals(0.5, dw ?: 0.0, 0.01)
        assertEquals(-0.3, df ?: 0.0, 0.05)
        assertEquals(0.6, dm ?: 0.0, 0.01)

        val postponedAfter = BodyProgressEntryEntity(
            id = "e2",
            dateIso = "2026-10-15",
            displayDate = "15/10",
            isPostponed = true,
            postponeShortLabel = "Viaje"
        )
        // Postponed entries are ignored when finding last actual weigh-in reference
        val next = BodyProgressDomainHelper.computeNextWeighIn(
            entries = listOf(prev, postponedAfter),
            cycleDays = 14,
            todayIso = "2026-10-10"
        )
        assertNotNull(next)
        assertEquals("2026-10-15", next?.nextDateIso)
        assertEquals("15/10", next?.nextDisplayDate)
        assertEquals(5, next?.daysRemaining)
    }

    @Test
    fun repository_seedsAndSupportsBackupRoundtrip() = runBlocking {
        val repo = BodyProgressRepository(context, db.bodyProgressDao())
        repo.ensureSeededIfEmpty()

        val initialEntries = repo.getAllEntries()
        val initialProfile = repo.getProfileOrDefault()
        assertTrue("Expected seeded progress entries", initialEntries.size >= 2)
        assertTrue(initialProfile.athleteName.isNotBlank())
        assertTrue(initialProfile.heightCm in 100..240)

        // Adding a custom weigh-in persists cleanly and survives subsequent ensureSeededIfEmpty calls
        val customEntry = BodyProgressEntryEntity(
            id = "bp-custom-1",
            dateIso = "2026-12-01",
            displayDate = "01/12",
            weightKg = 79.4,
            deltaWeightKg = -0.6,
            bmi = 24.51,
            bodyFatPct = 13.8,
            deltaFatKg = -0.4,
            muscleKg = 65.2,
            deltaMuscleKg = 0.3,
            muscleQuality = 90,
            bodyType = 8,
            boneMassKg = 3.4,
            visceralFat = 2.0,
            bmrKcal = 1950,
            clinicalBmrKcal = 1799,
            metabolicAge = 15,
            waterPct = 61.5,
            notes = "Control de prueba",
            updatedAtMillis = 123456L
        )
        repo.saveEntry(customEntry)
        repo.ensureSeededIfEmpty()

        val afterReload = repo.getAllEntries()
        assertTrue(afterReload.any { it.id == "bp-custom-1" && it.weightKg == 79.4 })

        val exportedJson = repo.exportBackupJson(initialProfile, afterReload)
        db.bodyProgressDao().deleteAllEntries()
        assertTrue(repo.getAllEntries().isEmpty())

        val importResult = repo.importBackupJson(exportedJson)
        assertTrue(importResult.isSuccess)
        assertEquals(afterReload.size, importResult.getOrNull())
        assertTrue(repo.getAllEntries().any { it.id == "bp-custom-1" })
    }

    @Test
    fun migration3To4_matchesCompiledRoomSchemaAndPreservesExistingTables() {
        val roomColumnsEntries = readTableColumns(db.openHelper.writableDatabase, "body_progress_entries")
        val roomColumnsProfile = readTableColumns(db.openHelper.writableDatabase, "body_progress_profile")

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(sqDb: SupportSQLiteDatabase) {
                        sqDb.execSQL("CREATE TABLE IF NOT EXISTS night_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL)")
                        sqDb.execSQL("INSERT INTO night_tasks (title) VALUES ('Mantener historial previo')")
                    }

                    override fun onUpgrade(sqDb: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.use {
            val rawDb = it.writableDatabase
            NexusDatabase.MIGRATION_3_4.migrate(rawDb)

            val migratedEntriesCols = readTableColumns(rawDb, "body_progress_entries")
            val migratedProfileCols = readTableColumns(rawDb, "body_progress_profile")

            assertEquals(roomColumnsEntries, migratedEntriesCols)
            assertEquals(roomColumnsProfile, migratedProfileCols)

            rawDb.query("SELECT title FROM night_tasks").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Mantener historial previo", cursor.getString(0))
            }
        }
    }

    @Test
    fun syncEngine_mergesBodyProgressWithLwwAndTombstones() {
        val topicA = MisCosasSyncEngine.deriveBodyProgressTopic("athlete@example.com", "dGVzdC1rZXktMTIz")
        val topicB = MisCosasSyncEngine.deriveBodyProgressTopic("  ATHLETE@example.com ", "dGVzdC1rZXktMTIz")
        assertTrue(topicA.startsWith("nexus_progress_"))
        assertEquals(topicA, topicB)

        val localProfile = BodyProgressProfileEntity(
            athleteName = "Atleta",
            ageYears = 25,
            heightCm = 180,
            updatedAtMillis = 100L
        )
        val remoteProfile = BodyProgressProfileDto(
            athleteName = "Atleta Updated",
            ageYears = 26,
            heightCm = 180,
            updatedAtMillis = 500L
        )

        val localEntries = listOf(
            BodyProgressEntryEntity(
                id = "bp-1",
                dateIso = "2026-10-01",
                displayDate = "01/10",
                weightKg = 80.0,
                updatedAtMillis = 200L
            ),
            BodyProgressEntryEntity(
                id = "bp-delete-me",
                dateIso = "2026-10-08",
                displayDate = "08/10",
                weightKg = 79.9,
                updatedAtMillis = 150L
            )
        )

        val remoteEntries = listOf(
            BodyProgressEntryDto(
                id = "bp-1",
                dateIso = "2026-10-01",
                weightKg = 80.2,
                updatedAtMillis = 400L
            ),
            BodyProgressEntryDto(
                id = "bp-2",
                dateIso = "2026-10-15",
                weightKg = 79.5,
                updatedAtMillis = 300L
            )
        )

        val result = MisCosasSyncEngine.mergeBodyProgress(
            localProfile = localProfile,
            localEntries = localEntries,
            localTombstones = mapOf("bp-delete-me" to 900L),
            remoteProfile = remoteProfile,
            remoteEntries = remoteEntries,
            remoteTombstones = emptyMap()
        )

        assertTrue(result.changedLocal)
        assertFalse(result.shouldPushBack)
        assertEquals("Atleta Updated", result.mergedProfile.athleteName)
        assertEquals(26, result.mergedProfile.ageYears)
        assertEquals(2, result.mergedEntries.size)
        assertNull(result.mergedEntries.find { it.id == "bp-delete-me" })
        assertEquals(80.2, result.mergedEntries.first { it.id == "bp-1" }.weightKg ?: 0.0, 0.01)
        assertEquals(79.5, result.mergedEntries.first { it.id == "bp-2" }.weightKg ?: 0.0, 0.01)
        assertTrue(result.mergedTombstones.containsKey("bp-delete-me"))
    }

    private fun readTableColumns(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val cols = mutableListOf<String>()
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            val typeIdx = cursor.getColumnIndexOrThrow("type")
            val notNullIdx = cursor.getColumnIndexOrThrow("notnull")
            val pkIdx = cursor.getColumnIndexOrThrow("pk")
            while (cursor.moveToNext()) {
                val entry = "${cursor.getString(nameIdx)}|${cursor.getString(typeIdx)}|${cursor.getInt(notNullIdx)}|${cursor.getInt(pkIdx)}"
                cols += entry
            }
        }
        return cols.sorted()
    }
}
