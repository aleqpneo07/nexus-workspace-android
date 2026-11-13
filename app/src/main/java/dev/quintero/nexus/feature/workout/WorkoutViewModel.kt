package dev.quintero.nexus.feature.workout

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.quintero.nexus.core.database.NexusDatabase
import dev.quintero.nexus.sync.MisCosasCloudSyncManager
import dev.quintero.nexus.sync.SyncModule
import dev.quintero.nexus.core.util.DateTimeHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkoutSubTab(val label: String) {
    TRAIN("Entrenar"),
    HISTORY("Historial"),
    BACKUP("Backup")
}

data class WorkoutSetInputUi(
    val weightText: String = "",
    val repsText: String = "",
    val prevWeight: Double? = null,
    val prevReps: Int? = null,
    val prevDateIso: String? = null
) {
    val isCompleted: Boolean
        get() = (repsText.toIntOrNull() ?: 0) > 0

    val prBadge: String?
        get() = WorkoutRoutineCatalog.computePrBadgeLabel(
            prevWeight = prevWeight,
            prevReps = prevReps,
            curWeight = weightText.replace(',', '.').toDoubleOrNull(),
            curReps = repsText.toIntOrNull()
        )
}

data class WorkoutExerciseInputUi(
    val template: WorkoutExerciseTemplate,
    val lastDateIso: String? = null,
    val sets: List<WorkoutSetInputUi> = emptyList()
)

data class WorkoutUiState(
    val activeSubTab: WorkoutSubTab = WorkoutSubTab.TRAIN,
    val selectedDayKey: String = WorkoutRoutineCatalog.todayRecommendedDayKey(),
    val sessionDateIso: String = DateTimeHelper.todayIso(),
    val isManualDate: Boolean = false,
    val sessionNotes: String = "",
    val draftStatusLabel: String = "● Listo para entrenar",
    val isDraftRecovered: Boolean = false,
    val exercises: List<WorkoutExerciseInputUi> = emptyList(),
    val allSessions: List<WorkoutSessionModel> = emptyList(),
    val restTimerExpanded: Boolean = false,
    val restTimerActive: Boolean = false,
    val restTimerRemainingSec: Int = 0,
    val restTimerPulse: Boolean = false,
    val wakeLockEnabled: Boolean = false,
    val techniqueSheetExercise: WorkoutExerciseTemplate? = null,
    val toastMessage: String? = null,
    val isToastPositive: Boolean = true
) {
    val scheduledTodayDayKey: String?
        get() = WorkoutRoutineCatalog.scheduledDayKeyForToday()

    val isSelectedDateToday: Boolean
        get() = sessionDateIso == DateTimeHelper.todayIso()

    val currentPlan: WorkoutDayPlan
        get() = WorkoutRoutineCatalog.PLANS[selectedDayKey] ?: WorkoutRoutineCatalog.PLANS.getValue("Día 1")

    val todayCompletedDays: Set<String>
        get() {
            val today = DateTimeHelper.todayIso()
            return allSessions
                .filter { it.dateIso == today }
                .map { WorkoutRoutineCatalog.resolveCanonicalDayKey(it.dayKey) }
                .toSet()
        }

    val totalSessionsCount: Int get() = allSessions.size

    val monthSessionsCount: Int
        get() {
            val ym = DateTimeHelper.todayIso().take(7)
            return allSessions.count { it.dateIso.startsWith(ym) }
        }

    val yearSessionsCount: Int
        get() {
            val y = DateTimeHelper.todayIso().take(4)
            return allSessions.count { it.dateIso.startsWith(y) }
        }
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NexusDatabase.getInstance(application)
    private val repository = WorkoutRepository(db.workoutDao())
    private val syncManager = MisCosasCloudSyncManager.getInstance(application)

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var hasUnsavedChanges = false
    private var autoSaveJob: Job? = null
    private var restTimerJob: Job? = null
    private var toastJob: Job? = null

    init {
        viewModelScope.launch {
            val initialSessions = repository.getAllSessions()
            _uiState.update { it.copy(allSessions = initialSessions) }
            loadDay(_uiState.value.selectedDayKey, initialSessions)

            repository.sessionsFlow.collect { sessions ->
                _uiState.update { it.copy(allSessions = sessions) }
                if (!hasUnsavedChanges) {
                    loadDay(_uiState.value.selectedDayKey, sessions)
                }
            }
        }
    }

    fun selectSubTab(tab: WorkoutSubTab) {
        _uiState.update { it.copy(activeSubTab = tab) }
    }

    fun selectDay(dayKey: String) {
        if (dayKey == _uiState.value.selectedDayKey) return
        viewModelScope.launch {
            if (hasUnsavedChanges) {
                persistCurrentDraftNow()
            }
            loadDay(dayKey, _uiState.value.allSessions)
        }
    }

    private suspend fun loadDay(dayKey: String, sessions: List<WorkoutSessionModel>) {
        val plan = WorkoutRoutineCatalog.PLANS[dayKey] ?: WorkoutRoutineCatalog.PLANS.getValue("Día 1")
        val todayIso = DateTimeHelper.todayIso()
        val validDraft = repository.getValidTodayDraft(dayKey, todayIso)

        val exerciseUis = plan.exercises.map { template ->
            val prev = repository.findLastExerciseSets(sessions, template.name, dayKey)
            val draftEx = validDraft?.second?.firstOrNull {
                WorkoutRoutineCatalog.normName(it.name) == WorkoutRoutineCatalog.normName(template.name)
            }
            val setCount = maxOf(template.defaultSets, draftEx?.sets?.size ?: 0)
            val sets = (0 until setCount).map { idx ->
                val prevSet = prev.sets.getOrNull(idx) ?: prev.sets.lastOrNull()
                val prevWStr = WorkoutRoutineCatalog.formatWeightCompact(prevSet?.weight)
                val draftSet = draftEx?.sets?.getOrNull(idx)
                val curWeight = draftSet?.weight?.takeIf { it.isNotBlank() } ?: prevWStr
                val curReps = draftSet?.reps ?: ""
                WorkoutSetInputUi(
                    weightText = curWeight,
                    repsText = curReps,
                    prevWeight = prevSet?.weight,
                    prevReps = prevSet?.reps,
                    prevDateIso = prev.dateIso
                )
            }
            WorkoutExerciseInputUi(
                template = template,
                lastDateIso = prev.dateIso,
                sets = sets
            )
        }

        hasUnsavedChanges = validDraft != null
        _uiState.update {
            it.copy(
                selectedDayKey = dayKey,
                sessionDateIso = validDraft?.first?.dateIso ?: todayIso,
                isManualDate = false,
                sessionNotes = validDraft?.first?.notes ?: "",
                draftStatusLabel = if (validDraft != null) "● Borrador recuperado" else "● Listo para entrenar",
                isDraftRecovered = validDraft != null,
                exercises = exerciseUis
            )
        }
    }

    fun refreshTodayDateIfNeeded() {
        val st = _uiState.value
        val today = DateTimeHelper.todayIso()
        if (!hasUnsavedChanges && !st.isManualDate && st.sessionDateIso != today) {
            _uiState.update { it.copy(sessionDateIso = today) }
            viewModelScope.launch {
                loadDay(WorkoutRoutineCatalog.todayRecommendedDayKey(), _uiState.value.allSessions)
            }
        }
    }

    fun onDatePicked(newDateIso: String) {
        val today = DateTimeHelper.todayIso()
        hasUnsavedChanges = true
        _uiState.update {
            it.copy(
                sessionDateIso = newDateIso,
                isManualDate = newDateIso != today
            )
        }
        scheduleAutoDraftSave()
    }

    fun resetToToday() {
        val today = DateTimeHelper.todayIso()
        _uiState.update {
            it.copy(sessionDateIso = today, isManualDate = false)
        }
        if (hasUnsavedChanges) scheduleAutoDraftSave()
        showToast("Fecha fijada a hoy")
    }

    fun updateNotes(notes: String) {
        hasUnsavedChanges = true
        _uiState.update { it.copy(sessionNotes = notes) }
        scheduleAutoDraftSave()
    }

    fun updateSetWeight(exIdx: Int, setIdx: Int, newWeight: String) {
        val sanitized = newWeight.filter { it.isDigit() || it == '.' || it == ',' }
        updateSet(exIdx, setIdx) { it.copy(weightText = sanitized) }
    }

    fun updateSetReps(exIdx: Int, setIdx: Int, newReps: String) {
        val sanitized = newReps.filter { it.isDigit() }
        val wasDone = _uiState.value.exercises.getOrNull(exIdx)?.sets?.getOrNull(setIdx)?.isCompleted == true
        updateSet(exIdx, setIdx) { it.copy(repsText = sanitized) }
        val nowDone = (sanitized.toIntOrNull() ?: 0) > 0
        if (!wasDone && nowDone && !_uiState.value.restTimerActive) {
            triggerTimerPulseHint()
        }
    }

    private fun updateSet(
        exIdx: Int,
        setIdx: Int,
        transform: (WorkoutSetInputUi) -> WorkoutSetInputUi
    ) {
        hasUnsavedChanges = true
        _uiState.update { st ->
            val updatedExercises = st.exercises.mapIndexed { ei, ex ->
                if (ei != exIdx) ex else {
                    val updatedSets = ex.sets.mapIndexed { si, s ->
                        if (si == setIdx) transform(s) else s
                    }
                    ex.copy(sets = updatedSets)
                }
            }
            st.copy(exercises = updatedExercises)
        }
        scheduleAutoDraftSave()
    }

    fun addSet(exIdx: Int) {
        hasUnsavedChanges = true
        _uiState.update { st ->
            val updated = st.exercises.mapIndexed { ei, ex ->
                if (ei != exIdx) ex else {
                    val prev = repository.findLastExerciseSets(st.allSessions, ex.template.name, st.selectedDayKey)
                    val prevSet = prev.sets.getOrNull(ex.sets.size) ?: prev.sets.lastOrNull()
                    val lastCurWeight = ex.sets.lastOrNull()?.weightText
                        ?.takeIf { it.isNotBlank() }
                        ?: WorkoutRoutineCatalog.formatWeightCompact(prevSet?.weight)
                    val newSet = WorkoutSetInputUi(
                        weightText = lastCurWeight,
                        repsText = "",
                        prevWeight = prevSet?.weight,
                        prevReps = prevSet?.reps,
                        prevDateIso = prev.dateIso
                    )
                    ex.copy(sets = ex.sets + newSet)
                }
            }
            st.copy(exercises = updated)
        }
        scheduleAutoDraftSave()
    }

    fun removeSet(exIdx: Int, setIdx: Int) {
        val currentSets = _uiState.value.exercises.getOrNull(exIdx)?.sets ?: return
        if (currentSets.size <= 1) {
            showToast("Mínimo 1 serie por ejercicio", positive = false)
            return
        }
        hasUnsavedChanges = true
        _uiState.update { st ->
            val updated = st.exercises.mapIndexed { ei, ex ->
                if (ei != exIdx) ex else {
                    ex.copy(sets = ex.sets.filterIndexed { i, _ -> i != setIdx })
                }
            }
            st.copy(exercises = updated)
        }
        scheduleAutoDraftSave()
    }

    private fun scheduleAutoDraftSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(400)
            if (hasUnsavedChanges) {
                persistCurrentDraftNow()
            }
        }
    }

    private suspend fun persistCurrentDraftNow() {
        val st = _uiState.value
        val draftExercises = st.exercises.map { ex ->
            WorkoutDraftExerciseDto(
                name = ex.template.name,
                sets = ex.sets.map { WorkoutDraftSetDto(weight = it.weightText, reps = it.repsText) }
            )
        }
        repository.saveDraft(
            dayKey = st.selectedDayKey,
            dateIso = st.sessionDateIso,
            notes = st.sessionNotes,
            exercises = draftExercises
        )
        _uiState.update {
            it.copy(draftStatusLabel = "● Guardado en este móvil")
        }
    }

    fun discardCurrentDraft() {
        viewModelScope.launch {
            val day = _uiState.value.selectedDayKey
            repository.clearDraft(day)
            hasUnsavedChanges = false
            loadDay(day, _uiState.value.allSessions)
            showToast("Sesión reiniciada")
        }
    }

    fun saveSession() {
        val st = _uiState.value
        val loggedExercises = st.exercises.mapNotNull { ex ->
            val completedSets = ex.sets.mapNotNull { s ->
                val reps = s.repsText.toIntOrNull() ?: return@mapNotNull null
                val weight = s.weightText.replace(',', '.').toDoubleOrNull() ?: 0.0
                WorkoutSetDto(weight = weight, reps = reps)
            }
            if (completedSets.isEmpty()) null else {
                WorkoutLoggedExerciseDto(name = ex.template.name, sets = completedSets)
            }
        }

        if (loggedExercises.isEmpty()) {
            showToast("Escribe las repeticiones de las series completadas", positive = false)
            return
        }

        viewModelScope.launch {
            repository.saveSession(
                dateIso = st.sessionDateIso,
                dayKey = st.selectedDayKey,
                dayLabel = st.currentPlan.label,
                notes = st.sessionNotes.trim(),
                exercises = loggedExercises
            )
            hasUnsavedChanges = false
            syncManager.notifyLocalMutation(SyncModule.ENTRENO)
            _uiState.update {
                it.copy(draftStatusLabel = "✅ Sesión guardada")
            }
            showToast("¡Sesión guardada en el historial!")
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            syncManager.markWorkoutSessionDeleted(sessionId)
            showToast("Sesión eliminada")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            val existingIds = _uiState.value.allSessions.map { it.id }
            repository.clearAll()
            syncManager.markAllWorkoutsCleared(existingIds)
            hasUnsavedChanges = false
            loadDay(_uiState.value.selectedDayKey, emptyList())
            showToast("Datos eliminados")
        }
    }

    fun buildExportJson(): String = repository.exportBackupJson(_uiState.value.allSessions)

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            val result = repository.importBackupJson(rawJson)
            if (result.isSuccess) {
                val importedCount = result.getOrThrow()
                val updatedSessions = repository.getAllSessions()
                loadDay(_uiState.value.selectedDayKey, updatedSessions)
                if (importedCount > 0) {
                    syncManager.notifyLocalMutation(SyncModule.ENTRENO)
                }
                showToast(
                    if (importedCount > 0) "$importedCount sesiones importadas y guardadas"
                    else "Todas esas sesiones ya estaban guardadas"
                )
            } else {
                showToast(result.exceptionOrNull()?.message ?: "Archivo no válido", positive = false)
            }
        }
    }

    fun openTechniqueSheet(template: WorkoutExerciseTemplate) {
        _uiState.update { it.copy(techniqueSheetExercise = template) }
    }

    fun closeTechniqueSheet() {
        _uiState.update { it.copy(techniqueSheetExercise = null) }
    }

    fun toggleWakeLock() {
        val next = !_uiState.value.wakeLockEnabled
        _uiState.update { it.copy(wakeLockEnabled = next) }
        showToast(
            if (next) "Pantalla encendida durante el entrenamiento"
            else "Bloqueo automático de pantalla restaurado"
        )
    }

    fun toggleRestTimerExpanded() {
        _uiState.update { it.copy(restTimerExpanded = !it.restTimerExpanded, restTimerPulse = false) }
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _uiState.update {
            it.copy(
                restTimerExpanded = true,
                restTimerActive = true,
                restTimerRemainingSec = seconds,
                restTimerPulse = false
            )
        }
        restTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                if (remaining > 0) {
                    _uiState.update { it.copy(restTimerRemainingSec = remaining) }
                }
            }
            _uiState.update {
                it.copy(
                    restTimerActive = false,
                    restTimerRemainingSec = 0,
                    restTimerPulse = true
                )
            }
            triggerRestCompletedFeedback()
            showToast("¡Tiempo cumplido! A por la siguiente serie")
            delay(4500)
            _uiState.update { it.copy(restTimerPulse = false) }
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _uiState.update {
            it.copy(restTimerActive = false, restTimerRemainingSec = 0, restTimerPulse = false)
        }
    }

    private fun triggerTimerPulseHint() {
        viewModelScope.launch {
            _uiState.update { it.copy(restTimerPulse = true) }
            delay(3500)
            if (!_uiState.value.restTimerActive) {
                _uiState.update { it.copy(restTimerPulse = false) }
            }
        }
    }

    private fun triggerRestCompletedFeedback() {
        val ctx = getApplication<Application>()
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        }
        runCatching {
            val pattern = longArrayOf(0, 220, 100, 220, 100, 350)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(pattern, -1)
                }
            }
        }
    }

    fun showToast(message: String, positive: Boolean = true) {
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            _uiState.update { it.copy(toastMessage = message, isToastPositive = positive) }
            delay(2600)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }
}
