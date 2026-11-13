package dev.quintero.nexus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.quintero.nexus.core.database.NexusDatabase
import dev.quintero.nexus.feature.focus.EnfoqueRepository
import dev.quintero.nexus.feature.schedule.UniversityScheduleRepository
import dev.quintero.nexus.feature.workout.WorkoutLoggedExerciseDto
import dev.quintero.nexus.feature.workout.WorkoutRepository
import dev.quintero.nexus.feature.workout.WorkoutSetDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

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
        UniversityScheduleRepository.resetToDemoForTest()
    }

    @Test
    fun appResourcesAndScheduleInitializeCleanly() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Nexus", appName)

        UniversityScheduleRepository.initialize(context)
        assertTrue(UniversityScheduleRepository.allSessions.isNotEmpty())
        assertTrue(UniversityScheduleRepository.buildings.isNotEmpty())
        assertTrue(UniversityScheduleRepository.subjects.isNotEmpty())
    }

    @Test
    fun workoutRepository_persistsAndRecoversPreviousWeightsAcrossDates() = runBlocking {
        val repo = WorkoutRepository(db.workoutDao())

        repo.saveSession(
            dateIso = "2026-11-11",
            dayKey = "Día 1",
            dayLabel = "DÍA 1 · Empuje Pesado",
            notes = "Sesión de referencia",
            exercises = listOf(
                WorkoutLoggedExerciseDto(
                    name = "Press Inclinado en Multipower o Mancuernas",
                    sets = listOf(WorkoutSetDto(weight = 75.0, reps = 8))
                )
            )
        )

        val stored = repo.getAllSessions()
        assertEquals(1, stored.size)

        val history = repo.findLastExerciseSets(
            sessions = stored,
            exerciseName = "Press Inclinado en Multipower o Mancuernas",
            preferredDayKey = "Día 1"
        )
        assertEquals("2026-11-11", history.dateIso)
        assertEquals(75.0, history.sets.first().weight ?: 0.0, 0.01)
        assertEquals(8, history.sets.first().reps)
    }

    @Test
    fun enfoqueRepository_seedsCleanDemoAndRoundtripsBackup() = runBlocking {
        val repo = EnfoqueRepository(context, db.enfoqueDao())
        repo.ensureSeededIfEmpty()

        val initialTasks = db.enfoqueDao().getAllTasks()
        val initialSubtasks = db.enfoqueDao().getAllSubtasks()
        assertTrue(initialTasks.any { it.title == "Arquitectura Offline-First" })
        assertFalse(initialTasks.any { it.title.contains("Lumen", ignoreCase = true) })

        val backupJson = repo.exportBackupJson(initialTasks, initialSubtasks)
        db.enfoqueDao().deleteAllSubtasks()
        db.enfoqueDao().deleteAllTasks()

        val importResult = repo.importBackupJson(backupJson)
        assertTrue(importResult.isSuccess)
        assertEquals(initialTasks.size to initialSubtasks.size, importResult.getOrNull())
    }
}
