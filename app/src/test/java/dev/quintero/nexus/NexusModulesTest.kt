package dev.quintero.nexus

import dev.quintero.nexus.feature.focus.EnfoqueDomainHelper
import dev.quintero.nexus.feature.focus.EnfoqueSubtaskEntity
import dev.quintero.nexus.feature.planner.NightTaskEntity
import dev.quintero.nexus.feature.planner.QuickInputParser
import dev.quintero.nexus.feature.planner.TaskCategory
import dev.quintero.nexus.feature.planner.TaskPriority
import dev.quintero.nexus.feature.schedule.LiveStatusKind
import dev.quintero.nexus.feature.schedule.ScheduleEntry
import dev.quintero.nexus.feature.schedule.UniDay
import dev.quintero.nexus.feature.schedule.UniversityScheduleRepository
import dev.quintero.nexus.feature.workout.WorkoutLoggedExerciseDto
import dev.quintero.nexus.feature.workout.WorkoutRoutineCatalog
import dev.quintero.nexus.feature.workout.WorkoutSessionModel
import dev.quintero.nexus.feature.workout.WorkoutSetDto
import dev.quintero.nexus.sync.MisCosasSyncEngine
import dev.quintero.nexus.sync.NightTaskCloudDto
import dev.quintero.nexus.sync.WorkoutCloudSessionDto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class NexusModulesTest {

    @Before
    fun setUp() {
        UniversityScheduleRepository.resetToDemoForTest()
    }

    @After
    fun tearDown() {
        UniversityScheduleRepository.resetToDemoForTest()
    }

    @Test
    fun demoSchedule_loadsCleanlyWithBreaksAndVirtualEvents() {
        assertEquals(10, UniversityScheduleRepository.allSessions.size)
        assertEquals(5, UniversityScheduleRepository.subjects.size)
        assertEquals(3, UniversityScheduleRepository.buildings.size)

        val mondayEntries = UniversityScheduleRepository.entriesForSelection(
            selectedDay = UniDay.LUNES,
            onlyPractices = false
        )
        val breaks = mondayEntries.filterIsInstance<ScheduleEntry.BreakIndicator>()
        assertEquals(2, breaks.size)
        assertTrue(breaks.any { it.label.contains("Almuerzo") })

        // Monday 2026-11-16 should expose Monday's 3 demo classes as agenda virtual blocks
        val events = UniversityScheduleRepository.toVirtualExternalEvents("2026-11-16")
        assertEquals(3, events.size)
        assertEquals("09:00", events.first().startTimeHHmm)
    }

    @Test
    fun dynamicScheduleEngine_detectsOverlappingSessions() {
        val overlapJson = """
            {
              "courseSubtitle": "Demo Overlap Test",
              "buildings": [
                { "id": "A", "displayName": "Edificio A", "shortLabel": "Ed. A", "colorHex": "#5BAED6" }
              ],
              "subjects": [
                { "id": "S1", "abbr": "S1", "fullName": "Sistemas I", "colorHex": "#5BAED6" },
                { "id": "S2", "abbr": "S2", "fullName": "Redes I", "colorHex": "#DE6473" }
              ],
              "sessions": [
                { "id": "c1", "day": "LUNES", "subjectId": "S1", "startMinutes": 540, "endMinutes": 660, "isPractice": false, "room": "Aula 1", "buildingId": "A" },
                { "id": "c2", "day": "LUNES", "subjectId": "S2", "startMinutes": 600, "endMinutes": 720, "isPractice": true, "groupLabel": "G1", "room": "Lab 2", "buildingId": "A" }
              ]
            }
        """.trimIndent()

        UniversityScheduleRepository.loadFromJsonForTest(overlapJson)
        val entries = UniversityScheduleRepository.entriesForSelection(UniDay.LUNES, onlyPractices = false)
        val overlaps = entries.filterIsInstance<ScheduleEntry.OverlapNotice>()

        assertEquals(1, overlaps.size)
        assertTrue(overlaps.first().message.contains("10:00–11:00"))
    }

    @Test
    fun liveStatus_resolvesActiveNextAndWeekend() {
        val mondayDuringSo = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 16, 9, 30, 0)
        }
        val activeStatus = UniversityScheduleRepository.resolveLiveStatus(mondayDuringSo)
        assertEquals(LiveStatusKind.ACTIVE_NOW, activeStatus.kind)
        assertTrue(activeStatus.mainText.contains("SO"))

        val sunday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 11, 0, 0)
        }
        val weekendStatus = UniversityScheduleRepository.resolveLiveStatus(sunday)
        assertEquals(LiveStatusKind.WEEKEND, weekendStatus.kind)
    }

    @Test
    fun quickInputParser_extractsTagsPriorityAndTimeBounds() {
        val parsed = QuickInputParser.parse("Práctica Sistemas #uni !alta @16:30-18:00")
        assertEquals("Práctica Sistemas", parsed.cleanTitle)
        assertEquals(TaskCategory.UNIVERSIDAD, parsed.detectedCategory)
        assertEquals(TaskPriority.ALTA, parsed.detectedPriority)
        assertEquals("16:30", parsed.detectedStartTime)
        assertEquals("18:00", parsed.detectedEndTime)
    }

    @Test
    fun enfoqueHelper_providesNeutralSeedAndCalculatesProgress() {
        val (tasks, subtasks) = EnfoqueDomainHelper.getInitialSeedData("2026-11-13")
        assertTrue(tasks.any { it.title == "Arquitectura Offline-First" })
        assertFalse(tasks.any { it.title.contains("Lumen", ignoreCase = true) })
        assertTrue(subtasks.isNotEmpty())

        val sampleSubs = listOf(
            EnfoqueSubtaskEntity("1", "u", "t", "Paso 1", completed = true, "", 1),
            EnfoqueSubtaskEntity("2", "u", "t", "Paso 2", completed = true, "", 2),
            EnfoqueSubtaskEntity("3", "u", "t", "Paso 3", completed = false, "", 3),
            EnfoqueSubtaskEntity("4", "u", "t", "Paso 4", completed = false, "", 4)
        )
        val stats = EnfoqueDomainHelper.calculateTaskProgress(sampleSubs, "en_marcha")
        assertEquals(50, stats.progress)
        assertEquals(2, stats.completedSubtasks)
        assertEquals(4, stats.totalSubtasks)

        val t1 = EnfoqueDomainHelper.deriveAccountTopicId("Alex", "1234")
        val t2 = EnfoqueDomainHelper.deriveAccountTopicId(" alex ", "1234")
        val t3 = EnfoqueDomainHelper.deriveAccountTopicId("alex", "9999")
        assertTrue(t1.startsWith("nexus_focus_"))
        assertEquals(t1, t2)
        assertNotEquals(t1, t3)
    }

    @Test
    fun workoutCatalog_mapsLegacyDayKeysAndPrBadges() {
        assertEquals(5, WorkoutRoutineCatalog.PLANS.size)
        assertEquals("Día 1", WorkoutRoutineCatalog.resolveCanonicalDayKey("Empuje"))
        assertEquals("+2.5 kg", WorkoutRoutineCatalog.computePrBadgeLabel(80.0, 8, 82.5, 8))
        assertEquals("+2 reps", WorkoutRoutineCatalog.computePrBadgeLabel(80.0, 8, 80.0, 10))
    }

    @Test
    fun syncEngine_mergesWorkoutsAndNightTasksWithTombstones() {
        val (focusTopic, workoutTopic, nightTopic) = MisCosasSyncEngine.deriveAllTopics("user", "pin")
        assertTrue(focusTopic.startsWith("nexus_focus_"))
        assertTrue(workoutTopic.startsWith("nexus_workout_"))
        assertTrue(nightTopic.startsWith("nexus_planner_"))

        val localSession = WorkoutSessionModel(
            id = "2026-11-12-Día 1",
            dateIso = "2026-11-12",
            dayKey = "Día 1",
            dayLabel = "DÍA 1 · Empuje Pesado",
            notes = "Buen entreno",
            savedAtIso = "2026-11-12T18:00:00.000Z",
            exercises = listOf(WorkoutLoggedExerciseDto("Press Inclinado", listOf(WorkoutSetDto(80.0, 8))))
        )
        val remoteNewer = WorkoutCloudSessionDto(
            id = "2026-11-12-Día 1",
            dateIso = "2026-11-12",
            dayKey = "Día 1",
            dayLabel = "DÍA 1 · Empuje Pesado",
            notes = "Actualizado en tablet",
            savedAtIso = "2026-11-12T19:00:00.000Z",
            exercises = listOf(WorkoutLoggedExerciseDto("Press Inclinado", listOf(WorkoutSetDto(82.5, 8))))
        )
        val wMerge = MisCosasSyncEngine.mergeWorkoutSessions(
            localSessions = listOf(localSession),
            localTombstones = emptyMap(),
            remoteSessions = listOf(remoteNewer),
            remoteTombstones = emptyMap()
        )
        assertTrue(wMerge.changedLocal)
        assertEquals("Actualizado en tablet", wMerge.mergedSessions.first().notes)

        val localTask = NightTaskEntity(
            id = 1L,
            title = "Entrega Lab",
            targetDateIso = "2026-11-13",
            syncUid = "uid-1",
            createdAtMillis = 1_000L,
            updatedAtMillis = 1_000L
        )
        val remoteNewerTask = NightTaskCloudDto(
            syncUid = "uid-1",
            title = "Entrega Lab Revisada",
            targetDateIso = "2026-11-13",
            category = TaskCategory.UNIVERSIDAD.name,
            priority = TaskPriority.ALTA.name,
            createdAtMillis = 1_000L,
            updatedAtMillis = 5_000L
        )
        val nMerge = MisCosasSyncEngine.mergeNightTasks(
            localTasks = listOf(localTask),
            localTombstones = emptyMap(),
            remoteTasks = listOf(remoteNewerTask),
            remoteTombstones = emptyMap()
        )
        assertTrue(nMerge.changedLocal)
        assertEquals(1, nMerge.upserts.size)
        assertEquals("Entrega Lab Revisada", nMerge.upserts.first().title)
    }
}
