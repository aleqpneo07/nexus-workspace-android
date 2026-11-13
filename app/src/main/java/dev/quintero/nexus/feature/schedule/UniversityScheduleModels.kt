package dev.quintero.nexus.feature.schedule

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.quintero.nexus.core.util.DateTimeHelper
import dev.quintero.nexus.feature.planner.ExternalCalendarEvent
import java.util.Calendar

data class UniBuilding(
    val id: String,
    val displayName: String,
    val color: Color,
    val shortLabel: String = displayName
)

data class UniSubject(
    val id: String,
    val abbr: String,
    val fullName: String,
    val color: Color,
    val theoryTag: Boolean = false,
    val fullTitleInLive: Boolean = false
)

enum class UniDay(
    val calendarDayOfWeek: Int,
    val shortLabel: String,
    val fullLabel: String
) {
    LUNES(Calendar.MONDAY, "Lun", "Lunes"),
    MARTES(Calendar.TUESDAY, "Mar", "Martes"),
    MIERCOLES(Calendar.WEDNESDAY, "Mié", "Miércoles"),
    JUEVES(Calendar.THURSDAY, "Jue", "Jueves"),
    VIERNES(Calendar.FRIDAY, "Vie", "Viernes");

    val timeRangeLabel: String
        get() = UniversityScheduleRepository.dayWindowLabel(this)
}

data class UniClassSession(
    val id: String,
    val day: UniDay,
    val subject: UniSubject,
    val startMinutes: Int,
    val endMinutes: Int,
    val isPractice: Boolean,
    val groupLabel: String? = null,
    val room: String,
    val building: UniBuilding,
    val durationLabel: String
) {
    val startHHmm: String get() = DateTimeHelper.formatMin(startMinutes)
    val endHHmm: String get() = DateTimeHelper.formatMin(endMinutes)

    val liveTitle: String
        get() = when {
            isPractice && groupLabel != null -> "${subject.abbr} (Prácticas $groupLabel)"
            subject.fullTitleInLive -> "${subject.fullName} (${subject.abbr})"
            subject.theoryTag -> "${subject.abbr} (Teoría)"
            else -> subject.abbr
        }

    val liveRoomLabel: String
        get() = if (
            building.shortLabel == building.displayName ||
            room.contains(building.shortLabel, ignoreCase = true)
        ) {
            room
        } else {
            "$room (${building.shortLabel})"
        }
}

sealed interface ScheduleEntry {
    val key: String

    data class DayDivider(
        val day: UniDay,
        val classCount: Int,
        val timeRangeLabel: String
    ) : ScheduleEntry {
        override val key: String = "divider_${day.name}"
    }

    data class DaySummary(
        val day: UniDay,
        val classCount: Int,
        val timeRangeLabel: String
    ) : ScheduleEntry {
        override val key: String = "summary_${day.name}"
    }

    data class ClassCard(
        val session: UniClassSession
    ) : ScheduleEntry {
        override val key: String = "class_${session.id}"
    }

    data class BreakIndicator(
        val id: String,
        val day: UniDay,
        val label: String,
        val timeRange: String
    ) : ScheduleEntry {
        override val key: String = "break_$id"
    }

    data class OverlapNotice(
        val id: String,
        val day: UniDay,
        val message: String
    ) : ScheduleEntry {
        override val key: String = "overlap_$id"
    }
}

enum class LiveStatusKind {
    ACTIVE_NOW,
    NEXT_TODAY,
    WEEKEND,
    DAY_FINISHED
}

data class UniLiveStatus(
    val kind: LiveStatusKind,
    val badgeText: String,
    val mainText: String,
    val subText: String,
    val targetDay: UniDay,
    val highlightedStartMinutes: Int? = null,
    val activeWindowStartMinutes: Int? = null,
    val nextStartMinutes: Int? = null
)

private data class ScheduleJsonBuilding(
    val id: String = "",
    val displayName: String = "",
    val shortLabel: String? = null,
    val colorHex: String = "#5BAED6"
)

private data class ScheduleJsonSubject(
    val id: String = "",
    val abbr: String = "",
    val fullName: String = "",
    val colorHex: String = "#6E7BF5",
    val theoryTag: Boolean = false,
    val fullTitleInLive: Boolean = false
)

private data class ScheduleJsonSession(
    val id: String = "",
    val day: String = "",
    val subjectId: String = "",
    val buildingId: String = "",
    val startMinutes: Int = 0,
    val endMinutes: Int = 0,
    val isPractice: Boolean = false,
    val groupLabel: String? = null,
    val room: String = "",
    val durationLabel: String? = null
)

private data class ScheduleJsonCatalog(
    val courseSubtitle: String = "Horario Académico",
    val buildings: List<ScheduleJsonBuilding> = emptyList(),
    val subjects: List<ScheduleJsonSubject> = emptyList(),
    val sessions: List<ScheduleJsonSession> = emptyList()
)

private data class LoadedScheduleCatalog(
    val courseSubtitle: String,
    val buildings: List<UniBuilding>,
    val subjects: List<UniSubject>,
    val sessions: List<UniClassSession>
)

/**
 * Resolves the academic schedule from local gitignored configuration when bundled
 * (`assets/schedule.json`), falling back cleanly to neutral demo data (`assets/demo_schedule.json`).
 * Dynamically computes breaks and conflicting time intervals across sessions.
 */
object UniversityScheduleRepository {

    private const val PERSONAL_ASSET = "schedule.json"
    private const val DEMO_ASSET = "demo_schedule.json"
    private const val BREAK_GAP_THRESHOLD_MIN = 30

    private val scheduleAdapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(ScheduleJsonCatalog::class.java)

    @Volatile
    private var catalog: LoadedScheduleCatalog = parseScheduleJson(DEFAULT_DEMO_SCHEDULE_JSON)

    @Volatile
    private var initializedFromContext = false

    val courseSubtitle: String
        get() = catalog.courseSubtitle

    val buildings: List<UniBuilding>
        get() = catalog.buildings

    val subjects: List<UniSubject>
        get() = catalog.subjects

    val allSessions: List<UniClassSession>
        get() = catalog.sessions

    fun initialize(context: Context) {
        if (initializedFromContext) return
        synchronized(this) {
            if (initializedFromContext) return
            val json = readAssetOrNull(context, PERSONAL_ASSET)
                ?: readAssetOrNull(context, DEMO_ASSET)
                ?: DEFAULT_DEMO_SCHEDULE_JSON
            catalog = parseScheduleJson(json)
            initializedFromContext = true
        }
    }

    fun loadFromJsonForTest(json: String) {
        catalog = parseScheduleJson(json)
    }

    fun resetToDemoForTest() {
        catalog = parseScheduleJson(DEFAULT_DEMO_SCHEDULE_JSON)
    }

    fun dayFromCalendarDayOfWeek(calendarDayOfWeek: Int): UniDay? =
        UniDay.entries.find { it.calendarDayOfWeek == calendarDayOfWeek }

    fun sessionsForDay(day: UniDay): List<UniClassSession> =
        allSessions.filter { it.day == day }.sortedWith(compareBy({ it.startMinutes }, { it.endMinutes }))

    fun sessionsForDateIso(dateIso: String): List<UniClassSession> {
        val dow = DateTimeHelper.dayOfWeekFromIso(dateIso)
        val uniDay = dayFromCalendarDayOfWeek(dow) ?: return emptyList()
        return sessionsForDay(uniDay)
    }

    fun dayWindowLabel(day: UniDay): String {
        val list = sessionsForDay(day)
        if (list.isEmpty()) return "Sin clases"
        val start = list.minOf { it.startMinutes }
        val end = list.maxOf { it.endMinutes }
        return "${DateTimeHelper.formatMin(start)}–${DateTimeHelper.formatMin(end)}"
    }

    fun toVirtualExternalEvents(dateIso: String): List<ExternalCalendarEvent> =
        sessionsForDateIso(dateIso).map { session ->
            val pracTag = if (session.isPractice) " Prácticas" else ""
            ExternalCalendarEvent(
                id = "uni_class_${session.id}",
                title = "Clase ${session.subject.abbr}$pracTag (${session.room})",
                dateIso = dateIso,
                startTimeHHmm = session.startHHmm,
                endTimeHHmm = session.endHHmm,
                isAllDay = false,
                location = "${session.room} · ${session.building.displayName}",
                isFromNightPlanner = false
            )
        }

    fun entriesForSelection(selectedDay: UniDay?, onlyPractices: Boolean): List<ScheduleEntry> {
        val daysToInclude = if (selectedDay == null) UniDay.entries else listOf(selectedDay)
        val result = mutableListOf<ScheduleEntry>()

        for (day in daysToInclude) {
            val daySessions = sessionsForDay(day).filter { !onlyPractices || it.isPractice }
            if (selectedDay == null) {
                if (onlyPractices && daySessions.isEmpty()) continue
                result.add(ScheduleEntry.DayDivider(day, daySessions.size, day.timeRangeLabel))
            } else {
                result.add(ScheduleEntry.DaySummary(day, daySessions.size, day.timeRangeLabel))
            }

            if (onlyPractices) {
                daySessions.forEach { result.add(ScheduleEntry.ClassCard(it)) }
            } else {
                result.addAll(buildChronologicalDayEntries(day, daySessions))
            }
        }
        return result
    }

    private fun buildChronologicalDayEntries(
        day: UniDay,
        sessions: List<UniClassSession>
    ): List<ScheduleEntry> {
        if (sessions.isEmpty()) return emptyList()
        val entries = mutableListOf<ScheduleEntry>()
        var prevMaxEnd = sessions.first().startMinutes

        for (i in sessions.indices) {
            val current = sessions[i]
            if (i > 0) {
                val overlappingPrev = sessions.subList(0, i).lastOrNull { it.endMinutes > current.startMinutes }
                if (overlappingPrev != null) {
                    val overlapEnd = minOf(overlappingPrev.endMinutes, current.endMinutes)
                    entries.add(
                        ScheduleEntry.OverlapNotice(
                            id = "${day.name.lowercase()}_ov_${overlappingPrev.id}_${current.id}",
                            day = day,
                            message = "Solapamiento ${current.startHHmm}–${DateTimeHelper.formatMin(overlapEnd)}: " +
                                "${overlappingPrev.subject.abbr} (${overlappingPrev.building.shortLabel} ${overlappingPrev.room}) " +
                                "coincide con ${current.subject.abbr} (${current.building.shortLabel} ${current.room})"
                        )
                    )
                } else {
                    val gapMin = current.startMinutes - prevMaxEnd
                    if (gapMin >= BREAK_GAP_THRESHOLD_MIN) {
                        val isLunchWindow = prevMaxEnd in 780..930 || (prevMaxEnd < 870 && current.startMinutes >= 870)
                        val title = if (isLunchWindow) "Descanso / Almuerzo" else "Descanso"
                        entries.add(
                            ScheduleEntry.BreakIndicator(
                                id = "${day.name.lowercase()}_brk_$i",
                                day = day,
                                label = "$title · ${formatDurationLabel(gapMin)}",
                                timeRange = "${DateTimeHelper.formatMin(prevMaxEnd)} → ${current.startHHmm}"
                            )
                        )
                    }
                }
            }
            entries.add(ScheduleEntry.ClassCard(current))
            prevMaxEnd = maxOf(prevMaxEnd, current.endMinutes)
        }
        return entries
    }

    fun resolveLiveStatus(now: Calendar = Calendar.getInstance()): UniLiveStatus {
        val dow = now.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val todayUniDay = dayFromCalendarDayOfWeek(dow)

        if (todayUniDay == null) {
            val mondayFirst = sessionsForDay(UniDay.LUNES).firstOrNull()
            val nextDesc = if (mondayFirst != null) {
                "Lunes ${mondayFirst.startHHmm} · ${mondayFirst.liveTitle} · ${mondayFirst.room}"
            } else {
                "Consulta la vista semanal"
            }
            return UniLiveStatus(
                kind = LiveStatusKind.WEEKEND,
                badgeText = "Fin de Semana",
                mainText = "Sin clases hasta el lunes",
                subText = nextDesc,
                targetDay = UniDay.LUNES
            )
        }

        val todayClasses = sessionsForDay(todayUniDay)
        val activeClass = todayClasses.find { currentMinutes >= it.startMinutes && currentMinutes < it.endMinutes }
        val nextClass = todayClasses.find { it.startMinutes > currentMinutes }

        return when {
            activeClass != null -> {
                val minLeft = activeClass.endMinutes - currentMinutes
                UniLiveStatus(
                    kind = LiveStatusKind.ACTIVE_NOW,
                    badgeText = "EN CURSO AHORA",
                    mainText = activeClass.liveTitle,
                    subText = "${activeClass.liveRoomLabel} · Termina a las ${activeClass.endHHmm} (quedan $minLeft min)",
                    targetDay = todayUniDay,
                    highlightedStartMinutes = activeClass.startMinutes,
                    activeWindowStartMinutes = activeClass.startMinutes
                )
            }

            nextClass != null -> {
                val minUntil = nextClass.startMinutes - currentMinutes
                val hoursUntil = minUntil / 60
                val remMin = minUntil % 60
                val waitStr = if (hoursUntil > 0) "${hoursUntil}h ${remMin}min" else "$minUntil min"
                UniLiveStatus(
                    kind = LiveStatusKind.NEXT_TODAY,
                    badgeText = "SIGUIENTE CLASE",
                    mainText = "${nextClass.liveTitle} a las ${nextClass.startHHmm}",
                    subText = "${nextClass.liveRoomLabel} · Empieza en $waitStr",
                    targetDay = todayUniDay,
                    highlightedStartMinutes = nextClass.startMinutes,
                    nextStartMinutes = nextClass.startMinutes
                )
            }

            else -> {
                val daysOrder = UniDay.entries
                val nextIndex = (daysOrder.indexOf(todayUniDay) + 1) % daysOrder.size
                val nextDay = daysOrder[nextIndex]
                val nextDayFirst = sessionsForDay(nextDay).firstOrNull()
                val nextText = if (nextDayFirst != null) {
                    "Próxima: ${nextDay.fullLabel} ${nextDayFirst.startHHmm} (${nextDayFirst.liveTitle})"
                } else {
                    "Próxima jornada: ${nextDay.fullLabel}"
                }
                UniLiveStatus(
                    kind = LiveStatusKind.DAY_FINISHED,
                    badgeText = "Jornada Finalizada",
                    mainText = "Has completado todas las clases de hoy",
                    subText = nextText,
                    targetDay = todayUniDay
                )
            }
        }
    }

    private fun readAssetOrNull(context: Context, assetName: String): String? = runCatching {
        context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()

    private fun parseScheduleJson(rawJson: String): LoadedScheduleCatalog {
        val parsed = scheduleAdapter.fromJson(rawJson) ?: ScheduleJsonCatalog()
        val subtitle = parsed.courseSubtitle.ifBlank { "Horario Académico" }

        val buildingsList = parsed.buildings.map { b ->
            UniBuilding(
                id = b.id,
                displayName = b.displayName,
                color = parseHexColor(b.colorHex.ifBlank { "#5BAED6" }),
                shortLabel = b.shortLabel?.takeIf { it.isNotBlank() } ?: b.displayName
            )
        }
        val buildingMap = buildingsList.associateBy { it.id }

        val subjectsList = parsed.subjects.map { s ->
            UniSubject(
                id = s.id,
                abbr = s.abbr,
                fullName = s.fullName,
                color = parseHexColor(s.colorHex.ifBlank { "#6E7BF5" }),
                theoryTag = s.theoryTag,
                fullTitleInLive = s.fullTitleInLive
            )
        }
        val subjectMap = subjectsList.associateBy { it.id }

        val sessionsList = parsed.sessions.mapNotNull { item ->
            val day = UniDay.entries.find { it.name == item.day } ?: return@mapNotNull null
            val subject = subjectMap[item.subjectId] ?: return@mapNotNull null
            val building = buildingMap[item.buildingId] ?: return@mapNotNull null
            UniClassSession(
                id = item.id,
                day = day,
                subject = subject,
                startMinutes = item.startMinutes,
                endMinutes = item.endMinutes,
                isPractice = item.isPractice,
                groupLabel = item.groupLabel?.takeIf { it.isNotBlank() },
                room = item.room,
                building = building,
                durationLabel = item.durationLabel?.takeIf { it.isNotBlank() }
                    ?: formatDurationLabel(item.endMinutes - item.startMinutes)
            )
        }

        return LoadedScheduleCatalog(
            courseSubtitle = subtitle,
            buildings = buildingsList,
            subjects = subjectsList,
            sessions = sessionsList
        )
    }

    private fun parseHexColor(hex: String): Color = runCatching {
        val clean = hex.trim().removePrefix("#")
        val argb = when (clean.length) {
            6 -> (0xFF000000L or clean.toLong(16))
            8 -> clean.toLong(16)
            else -> 0xFF5BAED6L
        }
        Color(argb.toInt())
    }.getOrDefault(Color(0xFF5BAED6))

    private fun formatDurationLabel(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}min"
            h > 0 -> "${h}h"
            else -> "${m} min"
        }
    }

    private const val DEFAULT_DEMO_SCHEDULE_JSON = """
    {
      "courseSubtitle": "Horario Académico Demo · Ingeniería Informática",
      "buildings": [
        { "id": "NORTH", "displayName": "Edificio Norte", "shortLabel": "Norte", "colorHex": "#5BAED6" },
        { "id": "CENTRAL", "displayName": "Aulario Central", "shortLabel": "Central", "colorHex": "#38B28B" },
        { "id": "LABS", "displayName": "Laboratorios", "shortLabel": "Labs", "colorHex": "#B5896B" }
      ],
      "subjects": [
        { "id": "SO", "abbr": "SO", "fullName": "Sistemas Operativos", "colorHex": "#5BAED6", "theoryTag": true },
        { "id": "ALG", "abbr": "ALG", "fullName": "Estructuras de Datos y Algoritmia", "colorHex": "#DE6473", "theoryTag": true },
        { "id": "BD", "abbr": "BD", "fullName": "Bases de Datos", "colorHex": "#A78BFA", "theoryTag": true },
        { "id": "RED", "abbr": "RED", "fullName": "Redes de Computadores", "colorHex": "#D4A359", "fullTitleInLive": true },
        { "id": "IS", "abbr": "IS", "fullName": "Ingeniería del Software", "colorHex": "#38B28B", "theoryTag": false }
      ],
      "sessions": [
        { "id": "lun_so_prac", "day": "LUNES", "subjectId": "SO", "startMinutes": 540, "endMinutes": 630, "isPractice": true, "groupLabel": "Grupo A", "room": "Lab 1.2", "buildingId": "LABS" },
        { "id": "lun_alg_teo", "day": "LUNES", "subjectId": "ALG", "startMinutes": 660, "endMinutes": 780, "isPractice": false, "room": "Aula 204", "buildingId": "NORTH" },
        { "id": "lun_bd_teo", "day": "LUNES", "subjectId": "BD", "startMinutes": 900, "endMinutes": 1020, "isPractice": false, "room": "Aula 102", "buildingId": "CENTRAL" },
        { "id": "mar_is_teo", "day": "MARTES", "subjectId": "IS", "startMinutes": 600, "endMinutes": 720, "isPractice": false, "room": "Aula 301", "buildingId": "NORTH" },
        { "id": "mar_bd_prac", "day": "MARTES", "subjectId": "BD", "startMinutes": 750, "endMinutes": 840, "isPractice": true, "groupLabel": "Grupo B", "room": "Lab 2.1", "buildingId": "LABS" },
        { "id": "mie_red_teo", "day": "MIERCOLES", "subjectId": "RED", "startMinutes": 570, "endMinutes": 690, "isPractice": false, "room": "Aula 108", "buildingId": "CENTRAL" },
        { "id": "mie_alg_prac", "day": "MIERCOLES", "subjectId": "ALG", "startMinutes": 840, "endMinutes": 930, "isPractice": true, "groupLabel": "Grupo A", "room": "Lab 1.4", "buildingId": "LABS" },
        { "id": "jue_so_teo", "day": "JUEVES", "subjectId": "SO", "startMinutes": 600, "endMinutes": 720, "isPractice": false, "room": "Aula 204", "buildingId": "NORTH" },
        { "id": "jue_is_prac", "day": "JUEVES", "subjectId": "IS", "startMinutes": 780, "endMinutes": 870, "isPractice": true, "groupLabel": "Grupo A", "room": "Lab 2.3", "buildingId": "LABS" },
        { "id": "vie_red_teo", "day": "VIERNES", "subjectId": "RED", "startMinutes": 600, "endMinutes": 720, "isPractice": false, "room": "Aula 108", "buildingId": "CENTRAL" }
      ]
    }
    """
}
