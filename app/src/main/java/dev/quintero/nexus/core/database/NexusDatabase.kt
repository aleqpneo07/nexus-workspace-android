package dev.quintero.nexus.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.quintero.nexus.core.util.DateTimeHelper
import dev.quintero.nexus.feature.focus.EnfoqueDao
import dev.quintero.nexus.feature.focus.EnfoqueSubtaskEntity
import dev.quintero.nexus.feature.focus.EnfoqueTaskEntity
import dev.quintero.nexus.feature.planner.NightTaskDao
import dev.quintero.nexus.feature.planner.NightTaskEntity
import dev.quintero.nexus.feature.planner.SyncState
import dev.quintero.nexus.feature.planner.TaskCategory
import dev.quintero.nexus.feature.planner.TaskPriority
import dev.quintero.nexus.feature.workout.WorkoutDao
import dev.quintero.nexus.feature.workout.WorkoutDraftEntity
import dev.quintero.nexus.feature.workout.WorkoutSessionEntity

@Database(
    entities = [
        NightTaskEntity::class,
        WorkoutSessionEntity::class,
        WorkoutDraftEntity::class,
        EnfoqueTaskEntity::class,
        EnfoqueSubtaskEntity::class,
        dev.quintero.nexus.feature.progress.BodyProgressEntryEntity::class,
        dev.quintero.nexus.feature.progress.BodyProgressProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class NexusDatabase : RoomDatabase() {

    abstract fun nightTaskDao(): NightTaskDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun enfoqueDao(): EnfoqueDao
    abstract fun bodyProgressDao(): dev.quintero.nexus.feature.progress.BodyProgressDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `night_tasks` ADD COLUMN `syncUid` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `night_tasks` ADD COLUMN `updatedAtMillis` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `night_tasks` SET `syncUid` = 'local-' || `id` WHERE `syncUid` = ''")
                db.execSQL("UPDATE `night_tasks` SET `updatedAtMillis` = `createdAtMillis` WHERE `updatedAtMillis` = 0")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_sessions` (
                        `id` TEXT NOT NULL,
                        `dateIso` TEXT NOT NULL,
                        `dayKey` TEXT NOT NULL,
                        `dayLabel` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `savedAtIso` TEXT NOT NULL,
                        `exercisesJson` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_drafts` (
                        `dayKey` TEXT NOT NULL,
                        `dateIso` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `exercisesJson` TEXT NOT NULL,
                        `savedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`dayKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `enfoque_tasks` (
                        `id` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `dueDateIso` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `totalSubtasks` INTEGER NOT NULL,
                        `completedSubtasks` INTEGER NOT NULL,
                        `sortOrderHint` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `enfoque_subtasks` (
                        `id` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `taskId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `scheduleLabel` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `body_progress_entries` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `dateIso` TEXT NOT NULL,
                        `displayDate` TEXT NOT NULL,
                        `isBaseEntry` INTEGER NOT NULL,
                        `isPostponed` INTEGER NOT NULL,
                        `weightKg` REAL,
                        `deltaWeightKg` REAL,
                        `bmi` REAL,
                        `bodyFatPct` REAL,
                        `deltaFatKg` REAL,
                        `muscleKg` REAL,
                        `deltaMuscleKg` REAL,
                        `muscleQuality` INTEGER,
                        `bodyType` INTEGER,
                        `boneMassKg` REAL,
                        `visceralFat` REAL,
                        `bmrKcal` INTEGER,
                        `clinicalBmrKcal` INTEGER,
                        `metabolicAge` INTEGER,
                        `waterPct` REAL,
                        `postponeShortLabel` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `body_progress_profile` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `athleteName` TEXT NOT NULL,
                        `ageYears` INTEGER NOT NULL,
                        `heightCm` INTEGER NOT NULL,
                        `scaleModel` TEXT NOT NULL,
                        `cycleDays` INTEGER NOT NULL,
                        `baselineDateLabel` TEXT NOT NULL,
                        `summerRefSummary` TEXT NOT NULL,
                        `targetDescription` TEXT NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_workspace.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialDataIfEmpty(dao: NightTaskDao) {
            if (dao.countTasks() > 0) return

            val today = DateTimeHelper.todayIso()
            val tomorrow = DateTimeHelper.tomorrowIso()

            val starterTasks = listOf(
                NightTaskEntity(
                    title = "Práctica de Estructuras de Datos",
                    notes = "Completar ejercicios de árboles y análisis asintótico.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "08:30",
                    endTime = "09:45",
                    durationMinutes = 75,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false,
                    createdAtMillis = 1L,
                    syncUid = "seed-planner-1",
                    updatedAtMillis = 1L
                ),
                NightTaskEntity(
                    title = "Diseño Relacional y Consultas SQL",
                    notes = "Revisar índices y normalización de tablas.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "18:00",
                    endTime = "19:30",
                    durationMinutes = 90,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false,
                    createdAtMillis = 2L,
                    syncUid = "seed-planner-2",
                    updatedAtMillis = 2L
                ),
                NightTaskEntity(
                    title = "Sesión de Entrenamiento de Fuerza",
                    notes = "Bloque principal + 15 min de movilidad.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.GIMNASIO.name,
                    priority = TaskPriority.MEDIA.name,
                    startTime = "20:00",
                    endTime = "21:15",
                    durationMinutes = 75,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false,
                    createdAtMillis = 3L,
                    syncUid = "seed-planner-3",
                    updatedAtMillis = 3L
                ),
                NightTaskEntity(
                    title = "Revisión de Algoritmos de Grafos",
                    notes = "Implementación y cobertura de casos límite.",
                    targetDateIso = today,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "08:30",
                    endTime = "09:45",
                    durationMinutes = 75,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = true,
                    completedAtMillis = System.currentTimeMillis() - 7_200_000L,
                    createdAtMillis = 4L,
                    syncUid = "seed-planner-4",
                    updatedAtMillis = 4L
                ),
                NightTaskEntity(
                    title = "Lectura técnica · Redes y Transporte",
                    notes = "Repasar control de congestión y subredes.",
                    targetDateIso = today,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.MEDIA.name,
                    startTime = "15:00",
                    endTime = "16:00",
                    durationMinutes = 60,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false,
                    createdAtMillis = 5L,
                    syncUid = "seed-planner-5",
                    updatedAtMillis = 5L
                ),
                NightTaskEntity(
                    title = "Reorganizar pendientes semanales",
                    notes = "Priorizar objetivos técnicos sin fricción.",
                    targetDateIso = today,
                    category = TaskCategory.PROYECTOS.name,
                    priority = TaskPriority.BAJA.name,
                    startTime = null,
                    endTime = null,
                    durationMinutes = 30,
                    syncWithCalendar = false,
                    syncState = SyncState.LOCAL_ONLY.name,
                    completed = false,
                    createdAtMillis = 6L,
                    syncUid = "seed-planner-6",
                    updatedAtMillis = 6L
                )
            )

            dao.insertAll(starterTasks)
        }
    }
}
