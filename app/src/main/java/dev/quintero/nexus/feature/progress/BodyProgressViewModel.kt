package dev.quintero.nexus.feature.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.quintero.nexus.core.database.NexusDatabase
import dev.quintero.nexus.core.util.DateTimeHelper
import dev.quintero.nexus.sync.MisCosasCloudSyncManager
import dev.quintero.nexus.sync.SyncModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class BodyChartMetric(val label: String, val unit: String) {
    WEIGHT("Peso", "kg"),
    MUSCLE("Músculo", "kg"),
    BODY_FAT("% Grasa", "%")
}

data class BodyProgressEditorState(
    val isOpen: Boolean = false,
    val editingEntryId: String? = null,
    val dateIso: String = DateTimeHelper.todayIso(),
    val isBaseEntry: Boolean = false,
    val isPostponed: Boolean = false,
    val postponeShortLabel: String = "Aplazado",
    val autoComputeDerived: Boolean = true,
    val weightInput: String = "",
    val deltaWeightInput: String = "",
    val bmiInput: String = "",
    val bodyFatPctInput: String = "",
    val deltaFatInput: String = "",
    val muscleInput: String = "",
    val deltaMuscleInput: String = "",
    val muscleQualityInput: String = "",
    val bodyTypeInput: String = "",
    val boneMassInput: String = "",
    val visceralFatInput: String = "",
    val bmrInput: String = "",
    val clinicalBmrInput: String = "",
    val metabolicAgeInput: String = "",
    val waterPctInput: String = "",
    val notesInput: String = "",
    val errorMessage: String? = null
)

data class BodyProgressUiState(
    val isLoading: Boolean = true,
    val profile: BodyProgressProfileEntity = BodyProgressProfileEntity(),
    val entriesChronological: List<BodyProgressEntryEntity> = emptyList(),
    val displayedEntries: List<BodyProgressEntryEntity> = emptyList(),
    val baselineEntry: BodyProgressEntryEntity? = null,
    val latestValidEntry: BodyProgressEntryEntity? = null,
    val totalDeltaWeightVsBase: Double? = null,
    val totalDeltaMuscleVsBase: Double? = null,
    val totalDeltaFatPctVsBase: Double? = null,
    val nextWeighIn: NextWeighInScheduleInfo? = null,
    val validWeighInCount: Int = 0,
    val todayIso: String = DateTimeHelper.todayIso(),
    val newestFirst: Boolean = false,
    val showAllColumns: Boolean = true,
    val showChart: Boolean = true,
    val selectedChartMetric: BodyChartMetric = BodyChartMetric.WEIGHT,
    val selectedEntryForDetail: BodyProgressEntryEntity? = null,
    val editor: BodyProgressEditorState = BodyProgressEditorState(),
    val isProfileDialogOpen: Boolean = false,
    val isBackupDialogOpen: Boolean = false,
    val toastMessage: String? = null,
    val toastPositive: Boolean = true
)

class BodyProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NexusDatabase.getInstance(application)
    private val repository = BodyProgressRepository(application, db.bodyProgressDao())
    private val syncManager = MisCosasCloudSyncManager.getInstance(application)

    private val _uiState = MutableStateFlow(BodyProgressUiState())
    val uiState: StateFlow<BodyProgressUiState> = _uiState.asStateFlow()

    private val viewPrefs = MutableStateFlow(ViewPrefs())
    private var toastJob: Job? = null

    private data class ViewPrefs(
        val newestFirst: Boolean = false,
        val showAllColumns: Boolean = true,
        val showChart: Boolean = true,
        val selectedChartMetric: BodyChartMetric = BodyChartMetric.WEIGHT,
        val todayIso: String = DateTimeHelper.todayIso()
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureSeededIfEmpty()
        }

        viewModelScope.launch {
            combine(
                repository.profileFlow,
                repository.entriesFlow,
                viewPrefs
            ) { profileOrNull, entriesRaw, prefs ->
                val profile = profileOrNull ?: BodyProgressProfileEntity()
                val sortedChronological = entriesRaw.sortedWith(
                    compareBy<BodyProgressEntryEntity> { it.dateIso }.thenBy { it.id }
                )
                val displayed = if (prefs.newestFirst) {
                    sortedChronological.asReversed()
                } else {
                    sortedChronological
                }
                val validEntries = sortedChronological.filter { !it.isPostponed && it.weightKg != null }
                val baseline = validEntries.firstOrNull { it.isBaseEntry } ?: validEntries.firstOrNull()
                val latest = validEntries.lastOrNull()

                val totalDw = if (baseline?.weightKg != null && latest?.weightKg != null && latest.id != baseline.id) {
                    BodyProgressDomainHelper.round2(latest.weightKg - baseline.weightKg)
                } else null
                val totalDm = if (baseline?.muscleKg != null && latest?.muscleKg != null && latest.id != baseline.id) {
                    BodyProgressDomainHelper.round1(latest.muscleKg - baseline.muscleKg)
                } else null
                val totalDf = if (baseline?.bodyFatPct != null && latest?.bodyFatPct != null && latest.id != baseline.id) {
                    BodyProgressDomainHelper.round1(latest.bodyFatPct - baseline.bodyFatPct)
                } else null

                val nextInfo = BodyProgressDomainHelper.computeNextWeighIn(
                    entries = sortedChronological,
                    cycleDays = profile.cycleDays,
                    todayIso = prefs.todayIso
                )

                _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    entriesChronological = sortedChronological,
                    displayedEntries = displayed,
                    baselineEntry = baseline,
                    latestValidEntry = latest,
                    totalDeltaWeightVsBase = totalDw,
                    totalDeltaMuscleVsBase = totalDm,
                    totalDeltaFatPctVsBase = totalDf,
                    nextWeighIn = nextInfo,
                    validWeighInCount = validEntries.size,
                    todayIso = prefs.todayIso,
                    newestFirst = prefs.newestFirst,
                    showAllColumns = prefs.showAllColumns,
                    showChart = prefs.showChart,
                    selectedChartMetric = prefs.selectedChartMetric
                )
            }.collect { updated ->
                _uiState.value = updated
            }
        }
    }

    fun refreshTodayDateIfNeeded() {
        val current = DateTimeHelper.todayIso()
        if (viewPrefs.value.todayIso != current) {
            viewPrefs.update { it.copy(todayIso = current) }
        }
    }

    fun toggleOrder() {
        viewPrefs.update { it.copy(newestFirst = !it.newestFirst) }
    }

    fun toggleAllColumns() {
        viewPrefs.update { it.copy(showAllColumns = !it.showAllColumns) }
    }

    fun toggleChartVisible() {
        viewPrefs.update { it.copy(showChart = !it.showChart) }
    }

    fun selectChartMetric(metric: BodyChartMetric) {
        viewPrefs.update { it.copy(selectedChartMetric = metric) }
    }

    fun openEntryDetail(entry: BodyProgressEntryEntity) {
        _uiState.update { it.copy(selectedEntryForDetail = entry) }
    }

    fun dismissEntryDetail() {
        _uiState.update { it.copy(selectedEntryForDetail = null) }
    }

    fun openNewWeighInEditor() {
        val st = _uiState.value
        val suggestedDate = st.nextWeighIn?.let { next ->
            if (next.daysRemaining in -3..3) next.nextDateIso else DateTimeHelper.todayIso()
        } ?: DateTimeHelper.todayIso()

        val hasBaseline = st.entriesChronological.any { !it.isPostponed }
        val latest = st.latestValidEntry
        _uiState.update {
            it.copy(
                selectedEntryForDetail = null,
                editor = BodyProgressEditorState(
                    isOpen = true,
                    editingEntryId = null,
                    dateIso = suggestedDate,
                    isBaseEntry = !hasBaseline,
                    isPostponed = false,
                    postponeShortLabel = "Aplazado",
                    autoComputeDerived = true,
                    // Pre-hint body type / clinical note context left blank so user writes exact reading cleanly
                    boneMassInput = latest?.boneMassKg?.let { bm -> BodyProgressDomainHelper.formatEs1(bm) }.orEmpty()
                )
            )
        }
    }

    fun openEditEntryEditor(entry: BodyProgressEntryEntity) {
        _uiState.update {
            it.copy(
                selectedEntryForDetail = null,
                editor = BodyProgressEditorState(
                    isOpen = true,
                    editingEntryId = entry.id,
                    dateIso = entry.dateIso,
                    isBaseEntry = entry.isBaseEntry,
                    isPostponed = entry.isPostponed,
                    postponeShortLabel = entry.postponeShortLabel.ifBlank { "Aplazado" },
                    autoComputeDerived = false,
                    weightInput = formatNumForField(entry.weightKg, 2),
                    deltaWeightInput = formatSignedNumForField(entry.deltaWeightKg, 2),
                    bmiInput = formatNumForField(entry.bmi, 2),
                    bodyFatPctInput = formatNumForField(entry.bodyFatPct, 1),
                    deltaFatInput = formatSignedNumForField(entry.deltaFatKg, 1),
                    muscleInput = formatNumForField(entry.muscleKg, 1),
                    deltaMuscleInput = formatSignedNumForField(entry.deltaMuscleKg, 1),
                    muscleQualityInput = entry.muscleQuality?.toString().orEmpty(),
                    bodyTypeInput = entry.bodyType?.toString().orEmpty(),
                    boneMassInput = formatNumForField(entry.boneMassKg, 1),
                    visceralFatInput = formatNumForField(entry.visceralFat, 1),
                    bmrInput = entry.bmrKcal?.toString().orEmpty(),
                    clinicalBmrInput = entry.clinicalBmrKcal?.toString().orEmpty(),
                    metabolicAgeInput = entry.metabolicAge?.toString().orEmpty(),
                    waterPctInput = formatNumForField(entry.waterPct, 1),
                    notesInput = entry.notes
                )
            )
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = BodyProgressEditorState(isOpen = false)) }
    }

    fun updateEditor(transform: (BodyProgressEditorState) -> BodyProgressEditorState) {
        _uiState.update { state ->
            state.copy(editor = transform(state.editor).copy(errorMessage = null))
        }
    }

    /**
     * Resolves the previous valid (non-postponed) entry strictly before [targetDateIso]
     * excluding [excludingEntryId], used for auto-computing incremental deltas.
     */
    fun findPreviousValidEntry(targetDateIso: String, excludingEntryId: String?): BodyProgressEntryEntity? {
        return _uiState.value.entriesChronological
            .filter {
                it.id != excludingEntryId &&
                    !it.isPostponed &&
                    it.weightKg != null &&
                    it.dateIso < targetDateIso
            }
            .maxByOrNull { it.dateIso }
    }

    fun saveEditorEntry() {
        val state = _uiState.value
        val ed = state.editor
        val profile = state.profile
        val cleanDate = ed.dateIso.trim().ifBlank { DateTimeHelper.todayIso() }
        val displayDate = BodyProgressDomainHelper.formatShortDisplayDate(cleanDate)

        val now = System.currentTimeMillis()
        val entryId = ed.editingEntryId ?: "bp-$cleanDate-${UUID.randomUUID().toString().take(8)}"

        val entity = if (ed.isPostponed) {
            BodyProgressEntryEntity(
                id = entryId,
                dateIso = cleanDate,
                displayDate = displayDate,
                isBaseEntry = false,
                isPostponed = true,
                postponeShortLabel = ed.postponeShortLabel.trim().ifBlank { "Aplazado" },
                notes = ed.notesInput.trim(),
                updatedAtMillis = now
            )
        } else {
            val weight = BodyProgressDomainHelper.parseFlexibleDouble(ed.weightInput)
            if (weight == null || weight <= 0.0) {
                _uiState.update {
                    it.copy(editor = ed.copy(errorMessage = "Introduce un peso válido en kg o marca el pesaje como aplazado."))
                }
                return
            }

            val fatPct = BodyProgressDomainHelper.parseFlexibleDouble(ed.bodyFatPctInput)
            val muscle = BodyProgressDomainHelper.parseFlexibleDouble(ed.muscleInput)
            val prevValid = findPreviousValidEntry(cleanDate, ed.editingEntryId)
            val (autoDw, autoDf, autoDm) = if (ed.isBaseEntry) {
                Triple(null, null, null)
            } else {
                BodyProgressDomainHelper.computeAutoDeltas(prevValid, weight, fatPct, muscle)
            }

            val bmi = BodyProgressDomainHelper.parseFlexibleDouble(ed.bmiInput)?.takeIf { !ed.autoComputeDerived }
                ?: BodyProgressDomainHelper.parseFlexibleDouble(ed.bmiInput)
                ?: BodyProgressDomainHelper.calculateBmi(weight, profile.heightCm)

            val deltaW = if (ed.autoComputeDerived) {
                BodyProgressDomainHelper.parseFlexibleDouble(ed.deltaWeightInput) ?: autoDw
            } else {
                BodyProgressDomainHelper.parseFlexibleDouble(ed.deltaWeightInput)
            }
            val deltaF = if (ed.autoComputeDerived) {
                BodyProgressDomainHelper.parseFlexibleDouble(ed.deltaFatInput) ?: autoDf
            } else {
                BodyProgressDomainHelper.parseFlexibleDouble(ed.deltaFatInput)
            }
            val deltaM = if (ed.autoComputeDerived) {
                BodyProgressDomainHelper.parseFlexibleDouble(ed.deltaMuscleInput) ?: autoDm
            } else {
                BodyProgressDomainHelper.parseFlexibleDouble(ed.deltaMuscleInput)
            }

            val bmrVal = ed.bmrInput.trim().toIntOrNull()
            val clinicalBmr = ed.clinicalBmrInput.trim().toIntOrNull()
                ?: BodyProgressDomainHelper.calculateMifflinStJeorKcal(
                    weightKg = weight,
                    heightCm = profile.heightCm,
                    ageYears = profile.ageYears
                )

            BodyProgressEntryEntity(
                id = entryId,
                dateIso = cleanDate,
                displayDate = displayDate,
                isBaseEntry = ed.isBaseEntry,
                isPostponed = false,
                weightKg = BodyProgressDomainHelper.round2(weight),
                deltaWeightKg = deltaW,
                bmi = bmi,
                bodyFatPct = fatPct?.let { BodyProgressDomainHelper.round1(it) },
                deltaFatKg = deltaF,
                muscleKg = muscle?.let { BodyProgressDomainHelper.round1(it) },
                deltaMuscleKg = deltaM,
                muscleQuality = ed.muscleQualityInput.trim().toIntOrNull(),
                bodyType = ed.bodyTypeInput.trim().toIntOrNull(),
                boneMassKg = BodyProgressDomainHelper.parseFlexibleDouble(ed.boneMassInput),
                visceralFat = BodyProgressDomainHelper.parseFlexibleDouble(ed.visceralFatInput),
                bmrKcal = bmrVal,
                clinicalBmrKcal = clinicalBmr,
                metabolicAge = ed.metabolicAgeInput.trim().toIntOrNull(),
                waterPct = BodyProgressDomainHelper.parseFlexibleDouble(ed.waterPctInput),
                postponeShortLabel = "",
                notes = ed.notesInput.trim(),
                updatedAtMillis = now
            )
        }

        val wasEdit = ed.editingEntryId != null
        _uiState.update { it.copy(editor = BodyProgressEditorState(isOpen = false)) }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveEntry(entity)
            syncManager.notifyLocalMutation(SyncModule.PROGRESO_FISICO)
        }
        showToast(if (wasEdit) "Registro actualizado" else "Pesaje guardado")
    }

    fun deleteEntry(entryId: String) {
        _uiState.update {
            it.copy(
                selectedEntryForDetail = null,
                editor = if (it.editor.editingEntryId == entryId) BodyProgressEditorState(isOpen = false) else it.editor
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEntry(entryId)
            syncManager.markBodyProgressDeleted(entryId)
        }
        showToast("Registro eliminado")
    }

    fun openProfileDialog() {
        _uiState.update { it.copy(isProfileDialogOpen = true) }
    }

    fun dismissProfileDialog() {
        _uiState.update { it.copy(isProfileDialogOpen = false) }
    }

    fun saveProfile(
        athleteName: String,
        ageYears: Int,
        heightCm: Int,
        scaleModel: String,
        cycleDays: Int,
        summerRefSummary: String,
        targetDescription: String
    ) {
        val current = _uiState.value.profile
        val updated = current.copy(
            athleteName = athleteName.trim().ifBlank { current.athleteName },
            ageYears = ageYears.coerceIn(12, 110),
            heightCm = heightCm.coerceIn(100, 240),
            scaleModel = scaleModel.trim().ifBlank { current.scaleModel },
            cycleDays = cycleDays.coerceIn(7, 60),
            summerRefSummary = summerRefSummary.trim(),
            targetDescription = targetDescription.trim(),
            updatedAtMillis = System.currentTimeMillis()
        )
        _uiState.update { it.copy(isProfileDialogOpen = false) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProfile(updated)
            syncManager.notifyLocalMutation(SyncModule.PROGRESO_FISICO)
        }
        showToast("Perfil actualizado")
    }

    fun openBackupDialog() {
        _uiState.update { it.copy(isBackupDialogOpen = true) }
    }

    fun dismissBackupDialog() {
        _uiState.update { it.copy(isBackupDialogOpen = false) }
    }

    fun buildExportJson(): String {
        val st = _uiState.value
        return repository.exportBackupJson(st.profile, st.entriesChronological)
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                repository.importBackupJson(rawJson)
            }
            res.onSuccess { count ->
                syncManager.notifyLocalMutation(SyncModule.PROGRESO_FISICO)
                _uiState.update { it.copy(isBackupDialogOpen = false) }
                showToast("Importados $count registros de progreso")
            }.onFailure { err ->
                showToast(err.message ?: "Error al importar JSON", positive = false)
            }
        }
    }

    fun showToast(message: String, positive: Boolean = true) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message, toastPositive = positive) }
        toastJob = viewModelScope.launch {
            delay(2800)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    private fun formatNumForField(value: Double?, decimals: Int): String {
        if (value == null) return ""
        return if (decimals == 2) {
            BodyProgressDomainHelper.formatEs2(value)
        } else {
            BodyProgressDomainHelper.formatEs1(value)
        }
    }

    private fun formatSignedNumForField(value: Double?, decimals: Int): String {
        if (value == null) return ""
        return if (decimals == 2) {
            BodyProgressDomainHelper.formatSigned2(value)
        } else {
            BodyProgressDomainHelper.formatSigned1(value)
        }
    }
}
