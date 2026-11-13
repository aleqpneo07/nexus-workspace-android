package dev.quintero.nexus.feature.focus

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import dev.quintero.nexus.core.util.DateTimeHelper
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class EnfoqueCategory(val id: String, val label: String) {
    TRABAJO("trabajo", "Trabajos"),
    EXAMEN("examen", "Exámenes"),
    DIARIO("diario", "Día a día");

    companion object {
        fun fromId(id: String): EnfoqueCategory =
            entries.find { it.id == id } ?: TRABAJO
    }
}

enum class EnfoqueStatus(val id: String, val label: String) {
    PENDIENTE("pendiente", "Pendiente"),
    EN_MARCHA("en_marcha", "En marcha"),
    TERMINADO("terminado", "Terminado");

    companion object {
        fun fromId(id: String): EnfoqueStatus =
            entries.find { it.id == id } ?: PENDIENTE
    }
}

enum class EnfoqueViewMode(val label: String) {
    TABLERO("Tablero"),
    LISTA("Lista"),
    CALENDARIO("Calendario")
}

enum class EnfoqueSortBy(val label: String) {
    FECHA("Fecha"),
    PROGRESO("Progreso"),
    TITULO("Título")
}

@Entity(tableName = "enfoque_tasks")
data class EnfoqueTaskEntity(
    @PrimaryKey val id: String,
    val ownerId: String = "demo-user",
    val title: String,
    val subtitle: String,
    val category: String,
    val status: String,
    val dueDateIso: String,
    val tag: String,
    val progress: Int,
    val totalSubtasks: Int,
    val completedSubtasks: Int,
    val sortOrderHint: Long = System.currentTimeMillis()
)

@Entity(tableName = "enfoque_subtasks")
data class EnfoqueSubtaskEntity(
    @PrimaryKey val id: String,
    val ownerId: String = "demo-user",
    val taskId: String,
    val title: String,
    val completed: Boolean,
    val scheduleLabel: String,
    val sortOrder: Int
)

@JsonClass(generateAdapter = true)
data class EnfoqueTaskDto(
    @Json(name = "id") val id: String,
    @Json(name = "ownerId") val ownerId: String? = "demo-user",
    @Json(name = "title") val title: String,
    @Json(name = "subtitle") val subtitle: String? = "",
    @Json(name = "category") val category: String = "trabajo",
    @Json(name = "status") val status: String = "pendiente",
    @Json(name = "dueDate") val dueDate: String,
    @Json(name = "tag") val tag: String? = "Entrega",
    @Json(name = "progress") val progress: Int = 0,
    @Json(name = "totalSubtasks") val totalSubtasks: Int = 0,
    @Json(name = "completedSubtasks") val completedSubtasks: Int = 0
)

@JsonClass(generateAdapter = true)
data class EnfoqueSubtaskDto(
    @Json(name = "id") val id: String,
    @Json(name = "ownerId") val ownerId: String? = "demo-user",
    @Json(name = "taskId") val taskId: String,
    @Json(name = "title") val title: String,
    @Json(name = "completed") val completed: Boolean = false,
    @Json(name = "scheduleLabel") val scheduleLabel: String? = "",
    @Json(name = "sortOrder") val sortOrder: Int = 1
)

@JsonClass(generateAdapter = true)
data class EnfoqueBackupPayload(
    @Json(name = "version") val version: Int = 1,
    @Json(name = "exportedAt") val exportedAt: String,
    @Json(name = "tasks") val tasks: List<EnfoqueTaskDto>,
    @Json(name = "subtasks") val subtasks: List<EnfoqueSubtaskDto>
)

@JsonClass(generateAdapter = true)
data class EnfoqueNtfyPayload(
    @Json(name = "type") val type: String = "state_sync",
    @Json(name = "syncId") val syncId: String = "",
    @Json(name = "deviceId") val deviceId: String = "",
    @Json(name = "accountId") val accountId: String = "",
    @Json(name = "tasks") val tasks: List<EnfoqueTaskDto> = emptyList(),
    @Json(name = "subtasks") val subtasks: List<EnfoqueSubtaskDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EnfoqueNtfyAttachment(
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class EnfoqueNtfyEnvelope(
    @Json(name = "event") val event: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "attachment") val attachment: EnfoqueNtfyAttachment? = null
)

data class EnfoqueProgressStats(
    val progress: Int,
    val totalSubtasks: Int,
    val completedSubtasks: Int
)

data class EnfoqueSpanishDateInfo(
    val fullDate: String,
    val shortLabel: String,
    val isToday: Boolean,
    val isTomorrow: Boolean,
    val isOverdue: Boolean,
    val isThisWeek: Boolean
)

data class EnfoqueCalendarDayCell(
    val isoDate: String,
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

@Dao
interface EnfoqueDao {
    @Query("SELECT * FROM enfoque_tasks ORDER BY dueDateIso ASC, title ASC")
    fun observeTasks(): Flow<List<EnfoqueTaskEntity>>

    @Query("SELECT * FROM enfoque_subtasks ORDER BY sortOrder ASC")
    fun observeSubtasks(): Flow<List<EnfoqueSubtaskEntity>>

    @Query("SELECT * FROM enfoque_tasks")
    suspend fun getAllTasks(): List<EnfoqueTaskEntity>

    @Query("SELECT * FROM enfoque_subtasks")
    suspend fun getAllSubtasks(): List<EnfoqueSubtaskEntity>

    @Query("SELECT COUNT(*) FROM enfoque_tasks")
    suspend fun countTasks(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: EnfoqueTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<EnfoqueTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtask(subtask: EnfoqueSubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubtasks(subtasks: List<EnfoqueSubtaskEntity>)

    @Query("DELETE FROM enfoque_subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtaskById(subtaskId: String)

    @Query("DELETE FROM enfoque_subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: String)

    @Query("DELETE FROM enfoque_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM enfoque_subtasks")
    suspend fun deleteAllSubtasks()

    @Query("DELETE FROM enfoque_tasks")
    suspend fun deleteAllTasks()

    @Transaction
    suspend fun replaceAll(tasks: List<EnfoqueTaskEntity>, subtasks: List<EnfoqueSubtaskEntity>) {
        deleteAllSubtasks()
        deleteAllTasks()
        if (tasks.isNotEmpty()) upsertTasks(tasks)
        if (subtasks.isNotEmpty()) upsertSubtasks(subtasks)
    }

    @Transaction
    suspend fun deleteTaskWithSubtasks(taskId: String) {
        deleteSubtasksByTaskId(taskId)
        deleteTaskById(taskId)
    }
}

object EnfoqueDomainHelper {

    fun makeSafeId(prefix: String): String =
        "${prefix}-${UUID.randomUUID().toString().take(8)}"

    fun cycleScheduleLabel(current: String): String {
        val order = listOf("", "En curso", "Hoy", "Mañana")
        val idx = order.indexOf(current)
        return order[(idx + 1) % order.size]
    }

    fun calculateTaskProgress(
        subtasks: List<EnfoqueSubtaskEntity>,
        fallbackStatus: String
    ): EnfoqueProgressStats {
        val total = minOf(subtasks.size, 50)
        if (total == 0) {
            val fallback = when (fallbackStatus) {
                "terminado" -> 100
                "en_marcha" -> 50
                else -> 0
            }
            return EnfoqueProgressStats(fallback, 0, 0)
        }
        val completed = subtasks.take(total).count { it.completed }
        val progress = Math.round((completed.toDouble() / total.toDouble()) * 100.0).toInt()
        return EnfoqueProgressStats(progress, total, completed)
    }

    fun formatSpanishDateInfo(isoDate: String, todayIso: String): EnfoqueSpanishDateInfo {
        val diffDays = DateTimeHelper.daysBetweenIso(todayIso, isoDate)
        val fullDate = DateTimeHelper.formatSpanishLongDate(isoDate)
        val shortLabel = when {
            diffDays < 0 -> "Atrasada"
            diffDays == 0 -> "Hoy"
            diffDays == 1 -> "Mañana"
            diffDays in 2..6 -> "En $diffDays días"
            else -> DateTimeHelper.formatShortDayMonthEs(isoDate)
        }
        return EnfoqueSpanishDateInfo(
            fullDate = fullDate,
            shortLabel = shortLabel,
            isToday = diffDays == 0,
            isTomorrow = diffDays == 1,
            isOverdue = diffDays < 0,
            isThisWeek = diffDays in 0..7
        )
    }

    /**
     * Derives the deterministic 32-hex ntfy channel identifier from account + PIN using SHA-256.
     */
    fun deriveAccountTopicId(identifier: String, password: String): String {
        val cleanUser = identifier.trim().lowercase(Locale.ROOT)
        val cleanPass = password.trim()
        val raw = "nexus_focus::$cleanUser::$cleanPass"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(32)
        return "nexus_focus_$hex"
    }

    fun getCalendarMonthCells(year: Int, monthIndex0: Int, todayIso: String): List<EnfoqueCalendarDayCell> {
        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex0)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
        val mondayOffset = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY

        val cursor = (firstOfMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -mondayOffset)
        }

        val cells = mutableListOf<EnfoqueCalendarDayCell>()
        for (i in 0 until 42) {
            val cy = cursor.get(Calendar.YEAR)
            val cm = cursor.get(Calendar.MONTH) + 1
            val cd = cursor.get(Calendar.DAY_OF_MONTH)
            val iso = "%04d-%02d-%02d".format(cy, cm, cd)
            cells.add(
                EnfoqueCalendarDayCell(
                    isoDate = iso,
                    dayNumber = cd,
                    isCurrentMonth = (cursor.get(Calendar.MONTH) == monthIndex0 && cy == year),
                    isToday = iso == todayIso
                )
            )
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cells
    }

    fun getInitialSeedData(
        todayIso: String,
        ownerId: String = "demo-user"
    ): Pair<List<EnfoqueTaskEntity>, List<EnfoqueSubtaskEntity>> {
        val tasks = listOf(
            EnfoqueTaskEntity(
                id = "task-arquitectura-core",
                ownerId = ownerId,
                title = "Arquitectura Offline-First",
                subtitle = "Módulo Sync E2EE",
                category = "trabajo",
                status = "en_marcha",
                dueDateIso = todayIso,
                tag = "Diseño Técnico",
                progress = 60,
                totalSubtasks = 5,
                completedSubtasks = 3
            ),
            EnfoqueTaskEntity(
                id = "task-examen-mercantil",
                ownerId = ownerId,
                title = "Examen Parcial de Estadística",
                subtitle = "Universidad · Aula B4",
                category = "examen",
                status = "en_marcha",
                dueDateIso = DateTimeHelper.addDaysToIso(todayIso, 3),
                tag = "Temas 1 al 5",
                progress = 50,
                totalSubtasks = 4,
                completedSubtasks = 2
            ),
            EnfoqueTaskEntity(
                id = "task-memoria-tfg",
                ownerId = ownerId,
                title = "Informe Técnico de Arquitectura",
                subtitle = "Proyecto Ingeniería",
                category = "trabajo",
                status = "pendiente",
                dueDateIso = DateTimeHelper.addDaysToIso(todayIso, 5),
                tag = "Borrador inicial",
                progress = 25,
                totalSubtasks = 4,
                completedSubtasks = 1
            ),
            EnfoqueTaskEntity(
                id = "task-examen-algoritmos",
                ownerId = ownerId,
                title = "Examen Final de Algoritmos",
                subtitle = "Facultad Informática",
                category = "examen",
                status = "pendiente",
                dueDateIso = DateTimeHelper.addDaysToIso(todayIso, 9),
                tag = "Práctica + Teoría",
                progress = 25,
                totalSubtasks = 4,
                completedSubtasks = 1
            ),
            EnfoqueTaskEntity(
                id = "task-rutina-diaria",
                ownerId = ownerId,
                title = "Organización Semanal y Entreno",
                subtitle = "Bienestar personal",
                category = "diario",
                status = "en_marcha",
                dueDateIso = todayIso,
                tag = "Hábitos clave",
                progress = 75,
                totalSubtasks = 4,
                completedSubtasks = 3
            ),
            EnfoqueTaskEntity(
                id = "task-rebranding-bodega",
                ownerId = ownerId,
                title = "Presentación de Caso Práctico",
                subtitle = "Seminario Estratégico",
                category = "trabajo",
                status = "terminado",
                dueDateIso = DateTimeHelper.addDaysToIso(todayIso, -2),
                tag = "Entregado",
                progress = 100,
                totalSubtasks = 3,
                completedSubtasks = 3
            ),
            EnfoqueTaskEntity(
                id = "task-gestiones-mes",
                ownerId = ownerId,
                title = "Renovación de Matrícula y Becas",
                subtitle = "Secretaría Académica",
                category = "diario",
                status = "terminado",
                dueDateIso = DateTimeHelper.addDaysToIso(todayIso, -4),
                tag = "Completado",
                progress = 100,
                totalSubtasks = 3,
                completedSubtasks = 3
            )
        )

        val subtasks = listOf(
            EnfoqueSubtaskEntity("sub-arch-1", ownerId, "task-arquitectura-core", "Definición de esquema Room local", true, "", 1),
            EnfoqueSubtaskEntity("sub-arch-2", ownerId, "task-arquitectura-core", "Derivación PBKDF2 y cifrado AES-GCM", true, "", 2),
            EnfoqueSubtaskEntity("sub-arch-3", ownerId, "task-arquitectura-core", "Resolución LWW con tombstones", true, "", 3),
            EnfoqueSubtaskEntity("sub-arch-4", ownerId, "task-arquitectura-core", "Canal SSE y reconexión automática", false, "En curso", 4),
            EnfoqueSubtaskEntity("sub-arch-5", ownerId, "task-arquitectura-core", "Pruebas unitarias y verificación", false, "Mañana", 5),
            EnfoqueSubtaskEntity("sub-est-1", ownerId, "task-examen-mercantil", "Resumir formulario de distribuciones", true, "", 1),
            EnfoqueSubtaskEntity("sub-est-2", ownerId, "task-examen-mercantil", "Resolver 15 problemas de contraste de hipótesis", true, "", 2),
            EnfoqueSubtaskEntity("sub-est-3", ownerId, "task-examen-mercantil", "Simulacro con examen del año pasado", false, "En curso", 3),
            EnfoqueSubtaskEntity("sub-est-4", ownerId, "task-examen-mercantil", "Repaso final de errores frecuentes", false, "Mañana", 4),
            EnfoqueSubtaskEntity("sub-inf-1", ownerId, "task-memoria-tfg", "Definir objetivos y alcance del trabajo", true, "", 1),
            EnfoqueSubtaskEntity("sub-inf-2", ownerId, "task-memoria-tfg", "Diagramas de componentes y flujo de datos", false, "Hoy", 2),
            EnfoqueSubtaskEntity("sub-inf-3", ownerId, "task-memoria-tfg", "Redacción de pruebas y conclusiones", false, "Mañana", 3),
            EnfoqueSubtaskEntity("sub-inf-4", ownerId, "task-memoria-tfg", "Exportar PDF y subir al campus", false, "", 4),
            EnfoqueSubtaskEntity("sub-alg-1", ownerId, "task-examen-algoritmos", "Complejidad asintótica y recurrencias", true, "", 1),
            EnfoqueSubtaskEntity("sub-alg-2", ownerId, "task-examen-algoritmos", "Árboles equilibrados y grafos (Dijkstra / BFS)", false, "En curso", 2),
            EnfoqueSubtaskEntity("sub-alg-3", ownerId, "task-examen-algoritmos", "Programación dinámica y voraces", false, "Mañana", 3),
            EnfoqueSubtaskEntity("sub-alg-4", ownerId, "task-examen-algoritmos", "Practicar ejercicios en papel sin IDE", false, "", 4),
            EnfoqueSubtaskEntity("sub-dia-1", ownerId, "task-rutina-diaria", "Planificar bloque de estudio de la mañana", true, "", 1),
            EnfoqueSubtaskEntity("sub-dia-2", ownerId, "task-rutina-diaria", "Compra semanal y preparar comidas", true, "", 2),
            EnfoqueSubtaskEntity("sub-dia-3", ownerId, "task-rutina-diaria", "Entrenamiento de fuerza (45 min)", true, "", 3),
            EnfoqueSubtaskEntity("sub-dia-4", ownerId, "task-rutina-diaria", "Lectura de 20 páginas antes de dormir", false, "Hoy", 4),
            EnfoqueSubtaskEntity("sub-term-1", ownerId, "task-rebranding-bodega", "Investigación de referencias", true, "", 1),
            EnfoqueSubtaskEntity("sub-term-2", ownerId, "task-rebranding-bodega", "Diseño de diapositivas", true, "", 2),
            EnfoqueSubtaskEntity("sub-term-3", ownerId, "task-rebranding-bodega", "Exposición en clase", true, "", 3),
            EnfoqueSubtaskEntity("sub-gest-1", ownerId, "task-gestiones-mes", "Descargar certificados académicos", true, "", 1),
            EnfoqueSubtaskEntity("sub-gest-2", ownerId, "task-gestiones-mes", "Adjuntar resguardo bancario", true, "", 2),
            EnfoqueSubtaskEntity("sub-gest-3", ownerId, "task-gestiones-mes", "Confirmar registro electrónico", true, "", 3)
        )

        return tasks to subtasks
    }
}
