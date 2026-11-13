package dev.quintero.nexus.feature.focus

import android.app.Application
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class EnfoqueFormSubtaskDraft(
    val id: String? = null,
    val title: String,
    val completed: Boolean = false,
    val scheduleLabel: String = ""
)

data class EnfoqueUiState(
    val allTasks: List<EnfoqueTaskEntity> = emptyList(),
    val allSubtasks: List<EnfoqueSubtaskEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: String = "todo",
    val sortBy: EnfoqueSortBy = EnfoqueSortBy.FECHA,
    val viewMode: EnfoqueViewMode = EnfoqueViewMode.TABLERO,
    val mobileStatusTab: EnfoqueStatus = EnfoqueStatus.PENDIENTE,
    val selectedTaskId: String? = null,
    val isTaskFormOpen: Boolean = false,
    val editingTaskId: String? = null,
    val isCloudAndBackupModalOpen: Boolean = false,
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val calendarMonth0: Int = Calendar.getInstance().get(Calendar.MONTH),
    val calendarSelectedIso: String = DateTimeHelper.todayIso(),
    val cloudAccount: EnfoqueCloudAccount? = null,
    val isSyncingCloud: Boolean = false,
    val toastMessage: String? = null,
    val isToastPositive: Boolean = true
) {
    val subtasksByTaskId: Map<String, List<EnfoqueSubtaskEntity>> by lazy {
        allSubtasks
            .sortedWith(compareBy<EnfoqueSubtaskEntity> { it.sortOrder }.thenBy { it.title })
            .groupBy { it.taskId }
    }

    val filteredTasks: List<EnfoqueTaskEntity> by lazy {
        val todayIso = DateTimeHelper.todayIso()
        val cleanQuery = searchQuery.trim().lowercase()

        val filtered = allTasks.filter { task ->
            val categoryMatch = when (activeFilter) {
                "trabajo", "examen", "diario" -> task.category == activeFilter
                "entregas" -> {
                    val info = EnfoqueDomainHelper.formatSpanishDateInfo(task.dueDateIso, todayIso)
                    task.status != EnfoqueStatus.TERMINADO.id && (info.isThisWeek || info.isOverdue)
                }
                else -> true
            }
            if (!categoryMatch) return@filter false

            if (cleanQuery.isBlank()) return@filter true
            val children = subtasksByTaskId[task.id].orEmpty()
            task.title.lowercase().contains(cleanQuery) ||
                task.subtitle.lowercase().contains(cleanQuery) ||
                task.tag.lowercase().contains(cleanQuery) ||
                children.any { it.title.lowercase().contains(cleanQuery) }
        }

        when (sortBy) {
            EnfoqueSortBy.FECHA -> filtered.sortedWith(
                compareBy<EnfoqueTaskEntity> { it.dueDateIso }.thenBy { it.title.lowercase() }
            )
            EnfoqueSortBy.PROGRESO -> filtered.sortedWith(
                compareByDescending<EnfoqueTaskEntity> { it.progress }.thenBy { it.dueDateIso }
            )
            EnfoqueSortBy.TITULO -> filtered.sortedWith(
                compareBy<EnfoqueTaskEntity> { it.title.lowercase() }.thenBy { it.dueDateIso }
            )
        }
    }

    val selectedTask: EnfoqueTaskEntity?
        get() = allTasks.find { it.id == selectedTaskId }

    val editingTask: EnfoqueTaskEntity?
        get() = allTasks.find { it.id == editingTaskId }

    val priorityTodayTasks: List<EnfoqueTaskEntity> by lazy {
        val todayIso = DateTimeHelper.todayIso()
        allTasks.filter { task ->
            if (task.status == EnfoqueStatus.TERMINADO.id) return@filter false
            val dateInfo = EnfoqueDomainHelper.formatSpanishDateInfo(task.dueDateIso, todayIso)
            val hasTodaySubtask = subtasksByTaskId[task.id].orEmpty().any { sub ->
                !sub.completed && (sub.scheduleLabel == "En curso" || sub.scheduleLabel == "Hoy")
            }
            dateInfo.isToday || dateInfo.isOverdue || hasTodaySubtask
        }.sortedBy { it.dueDateIso }
    }

    val weeklyDeliveriesCount: Int by lazy {
        val todayIso = DateTimeHelper.todayIso()
        allTasks.count { task ->
            if (task.status == EnfoqueStatus.TERMINADO.id) return@count false
            val diff = DateTimeHelper.daysBetweenIso(todayIso, task.dueDateIso)
            diff in 0..7
        }
    }

    val activeTasksCount: Int
        get() = allTasks.count { it.status != EnfoqueStatus.TERMINADO.id }

    val completedTasksCount: Int
        get() = allTasks.count { it.status == EnfoqueStatus.TERMINADO.id }

    val averageProgress: Int
        get() = if (allTasks.isEmpty()) 0 else (allTasks.sumOf { it.progress } / allTasks.size)

    val calendarMonthLabel: String
        get() = DateTimeHelper.formatMonthYearEs("%04d-%02d".format(calendarYear, calendarMonth0 + 1))

    val calendarCells: List<EnfoqueCalendarDayCell>
        get() = EnfoqueDomainHelper.getCalendarMonthCells(
            year = calendarYear,
            monthIndex0 = calendarMonth0,
            todayIso = DateTimeHelper.todayIso()
        )
}

class EnfoqueViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NexusDatabase.getInstance(application)
    private val repository = EnfoqueRepository(application, database.enfoqueDao())
    private val syncManager = MisCosasCloudSyncManager.getInstance(application)

    private val _uiState = MutableStateFlow(EnfoqueUiState())
    val uiState: StateFlow<EnfoqueUiState> = _uiState.asStateFlow()

    private var autoPushJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeededIfEmpty()
        }
        viewModelScope.launch {
            combine(
                repository.tasksFlow,
                repository.subtasksFlow,
                repository.cloudAccountFlow
            ) { tasks, subtasks, account ->
                Triple(tasks, subtasks, account)
            }.collect { (tasks, subtasks, account) ->
                _uiState.update { state ->
                    val validSelected = if (tasks.any { it.id == state.selectedTaskId }) state.selectedTaskId else null
                    val validEditing = if (tasks.any { it.id == state.editingTaskId }) state.editingTaskId else null
                    state.copy(
                        allTasks = tasks,
                        allSubtasks = subtasks,
                        cloudAccount = account,
                        selectedTaskId = validSelected,
                        editingTaskId = validEditing
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun setSortBy(sortBy: EnfoqueSortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
    }

    fun setViewMode(mode: EnfoqueViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setMobileStatusTab(status: EnfoqueStatus) {
        _uiState.update { it.copy(mobileStatusTab = status) }
    }

    fun openTaskInspector(taskId: String) {
        _uiState.update { it.copy(selectedTaskId = taskId) }
    }

    fun closeTaskInspector() {
        _uiState.update { it.copy(selectedTaskId = null) }
    }

    fun openNewTaskForm() {
        _uiState.update { it.copy(isTaskFormOpen = true, editingTaskId = null) }
    }

    fun openEditTaskForm(taskId: String) {
        _uiState.update { it.copy(isTaskFormOpen = true, editingTaskId = taskId) }
    }

    fun closeTaskForm() {
        _uiState.update { it.copy(isTaskFormOpen = false, editingTaskId = null) }
    }

    fun openCloudBackupModal() {
        _uiState.update { it.copy(isCloudAndBackupModalOpen = true) }
    }

    fun closeCloudBackupModal() {
        _uiState.update { it.copy(isCloudAndBackupModalOpen = false) }
    }

    fun selectCalendarDate(isoDate: String) {
        _uiState.update { it.copy(calendarSelectedIso = isoDate) }
    }

    fun shiftCalendarMonth(deltaMonths: Int) {
        _uiState.update { state ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, state.calendarYear)
                set(Calendar.MONTH, state.calendarMonth0)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, deltaMonths)
            }
            state.copy(
                calendarYear = cal.get(Calendar.YEAR),
                calendarMonth0 = cal.get(Calendar.MONTH)
            )
        }
    }

    fun jumpCalendarToday() {
        val cal = Calendar.getInstance()
        _uiState.update {
            it.copy(
                calendarYear = cal.get(Calendar.YEAR),
                calendarMonth0 = cal.get(Calendar.MONTH),
                calendarSelectedIso = DateTimeHelper.todayIso()
            )
        }
    }

    fun saveTaskFromForm(
        existingId: String?,
        title: String,
        subtitle: String,
        category: EnfoqueCategory,
        status: EnfoqueStatus,
        dueDateIso: String,
        tag: String,
        subtasksDraft: List<EnfoqueFormSubtaskDraft>
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            showToast("El título es obligatorio", positive = false)
            return
        }

        val taskId = existingId ?: EnfoqueDomainHelper.makeSafeId("task")
        val ownerId = _uiState.value.allTasks.find { it.id == existingId }?.ownerId
            ?: _uiState.value.cloudAccount?.identifier
            ?: "demo-user"

        val normalizedSubs = subtasksDraft
            .filter { it.title.trim().isNotEmpty() }
            .take(50)
            .mapIndexed { idx, draft ->
                EnfoqueSubtaskEntity(
                    id = draft.id ?: EnfoqueDomainHelper.makeSafeId("sub"),
                    ownerId = ownerId,
                    taskId = taskId,
                    title = draft.title.trim(),
                    completed = draft.completed,
                    scheduleLabel = draft.scheduleLabel,
                    sortOrder = idx + 1
                )
            }

        val stats = EnfoqueDomainHelper.calculateTaskProgress(normalizedSubs, status.id)
        val task = EnfoqueTaskEntity(
            id = taskId,
            ownerId = ownerId,
            title = cleanTitle,
            subtitle = subtitle.trim(),
            category = category.id,
            status = status.id,
            dueDateIso = dueDateIso.ifBlank { DateTimeHelper.todayIso() },
            tag = tag.trim().ifBlank { category.label },
            progress = stats.progress,
            totalSubtasks = stats.totalSubtasks,
            completedSubtasks = stats.completedSubtasks
        )

        viewModelScope.launch {
            repository.upsertTaskAndSubtasks(task, normalizedSubs)
            _uiState.update {
                it.copy(
                    isTaskFormOpen = false,
                    editingTaskId = null,
                    selectedTaskId = if (existingId != null) existingId else it.selectedTaskId,
                    mobileStatusTab = status
                )
            }
            showToast(if (existingId != null) "Tarea actualizada" else "Tarea creada")
            scheduleCloudPush()
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: EnfoqueStatus) {
        val state = _uiState.value
        val existing = state.allTasks.find { it.id == taskId } ?: return
        val children = state.subtasksByTaskId[taskId].orEmpty()
        val stats = EnfoqueDomainHelper.calculateTaskProgress(children, newStatus.id)
        val updated = existing.copy(
            status = newStatus.id,
            progress = stats.progress,
            totalSubtasks = stats.totalSubtasks,
            completedSubtasks = stats.completedSubtasks
        )
        viewModelScope.launch {
            repository.upsertTaskAndSubtasks(updated)
            scheduleCloudPush()
        }
    }

    fun updateTaskDueDate(taskId: String, newDueDateIso: String) {
        val existing = _uiState.value.allTasks.find { it.id == taskId } ?: return
        viewModelScope.launch {
            repository.upsertTaskAndSubtasks(existing.copy(dueDateIso = newDueDateIso))
            showToast("Fecha límite actualizada")
            scheduleCloudPush()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            _uiState.update {
                it.copy(
                    selectedTaskId = if (it.selectedTaskId == taskId) null else it.selectedTaskId,
                    editingTaskId = if (it.editingTaskId == taskId) null else it.editingTaskId
                )
            }
            showToast("Tarea eliminada")
            scheduleCloudPush()
        }
    }

    fun toggleSubtaskCompleted(subtaskId: String) {
        val state = _uiState.value
        val sub = state.allSubtasks.find { it.id == subtaskId } ?: return
        val parent = state.allTasks.find { it.id == sub.taskId } ?: return
        val updatedSub = sub.copy(completed = !sub.completed)
        val siblingList = state.subtasksByTaskId[parent.id].orEmpty().map {
            if (it.id == subtaskId) updatedSub else it
        }
        val stats = EnfoqueDomainHelper.calculateTaskProgress(siblingList, parent.status)
        val resolvedStatus = when {
            stats.totalSubtasks > 0 && stats.completedSubtasks == stats.totalSubtasks ->
                EnfoqueStatus.TERMINADO.id
            stats.totalSubtasks > 0 && stats.completedSubtasks > 0 && parent.status == EnfoqueStatus.PENDIENTE.id ->
                EnfoqueStatus.EN_MARCHA.id
            stats.totalSubtasks > 0 && stats.completedSubtasks < stats.totalSubtasks && parent.status == EnfoqueStatus.TERMINADO.id ->
                EnfoqueStatus.EN_MARCHA.id
            else -> parent.status
        }
        val updatedParent = parent.copy(
            status = resolvedStatus,
            progress = stats.progress,
            totalSubtasks = stats.totalSubtasks,
            completedSubtasks = stats.completedSubtasks
        )
        viewModelScope.launch {
            repository.upsertSubtask(updatedSub)
            repository.upsertTaskAndSubtasks(updatedParent)
            scheduleCloudPush()
        }
    }

    fun cycleSubtaskSchedule(subtaskId: String) {
        val sub = _uiState.value.allSubtasks.find { it.id == subtaskId } ?: return
        val nextLabel = EnfoqueDomainHelper.cycleScheduleLabel(sub.scheduleLabel)
        viewModelScope.launch {
            repository.upsertSubtask(sub.copy(scheduleLabel = nextLabel))
            scheduleCloudPush()
        }
    }

    fun updateSubtaskTitle(subtaskId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        val sub = _uiState.value.allSubtasks.find { it.id == subtaskId } ?: return
        if (sub.title == clean) return
        viewModelScope.launch {
            repository.upsertSubtask(sub.copy(title = clean))
            scheduleCloudPush()
        }
    }

    fun addSubtaskToTask(taskId: String, title: String, scheduleLabel: String = "") {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val state = _uiState.value
        val parent = state.allTasks.find { it.id == taskId } ?: return
        val existingSubs = state.subtasksByTaskId[taskId].orEmpty()
        if (existingSubs.size >= 50) {
            showToast("Máximo 50 subtareas por tarea", positive = false)
            return
        }
        val nextOrder = (existingSubs.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val newSub = EnfoqueSubtaskEntity(
            id = EnfoqueDomainHelper.makeSafeId("sub"),
            ownerId = parent.ownerId,
            taskId = taskId,
            title = clean,
            completed = false,
            scheduleLabel = scheduleLabel,
            sortOrder = nextOrder
        )
        val updatedSubs = existingSubs + newSub
        val stats = EnfoqueDomainHelper.calculateTaskProgress(updatedSubs, parent.status)
        val updatedParent = parent.copy(
            progress = stats.progress,
            totalSubtasks = stats.totalSubtasks,
            completedSubtasks = stats.completedSubtasks
        )
        viewModelScope.launch {
            repository.upsertSubtask(newSub)
            repository.upsertTaskAndSubtasks(updatedParent)
            scheduleCloudPush()
        }
    }

    fun deleteSubtask(subtaskId: String) {
        val state = _uiState.value
        val sub = state.allSubtasks.find { it.id == subtaskId } ?: return
        val parent = state.allTasks.find { it.id == sub.taskId }
        val remainingSubs = state.subtasksByTaskId[sub.taskId].orEmpty().filterNot { it.id == subtaskId }
        viewModelScope.launch {
            repository.deleteSubtask(subtaskId)
            if (parent != null) {
                val stats = EnfoqueDomainHelper.calculateTaskProgress(remainingSubs, parent.status)
                repository.upsertTaskAndSubtasks(
                    parent.copy(
                        progress = stats.progress,
                        totalSubtasks = stats.totalSubtasks,
                        completedSubtasks = stats.completedSubtasks
                    )
                )
            }
            scheduleCloudPush()
        }
    }

    fun buildBackupJson(): String {
        val state = _uiState.value
        return repository.exportBackupJson(state.allTasks, state.allSubtasks)
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            repository.importBackupJson(rawJson)
                .onSuccess { (taskCount, subCount) ->
                    showToast("Backup restaurado ($taskCount tareas · $subCount pasos)")
                    scheduleCloudPush()
                }
                .onFailure {
                    showToast("Error al importar backup JSON", positive = false)
                }
        }
    }

    fun resetDemo() {
        viewModelScope.launch {
            repository.resetDemo(DateTimeHelper.todayIso())
            _uiState.update { it.copy(selectedTaskId = null) }
            showToast("Espacio demo restaurado")
            scheduleCloudPush()
        }
    }

    fun connectCloudAccount(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            showToast("Introduce usuario y PIN de sincronización", positive = false)
            return
        }
        _uiState.update { it.copy(isSyncingCloud = true) }
        syncManager.connectAccount(identifier, password) { res ->
            _uiState.update { it.copy(isSyncingCloud = false) }
            res.onSuccess { msg -> showToast(msg, positive = true) }
                .onFailure { showToast("Cuenta vinculada (sin red para descarga)", positive = true) }
        }
    }

    fun syncCloudNow() {
        val account = _uiState.value.cloudAccount
        if (account == null && !syncManager.syncState.value.isConnected) return
        if (syncManager.syncState.value.isConnected) {
            _uiState.update { it.copy(isSyncingCloud = true) }
            syncManager.syncAllNow { res ->
                _uiState.update { it.copy(isSyncingCloud = false) }
                res.onSuccess { msg -> showToast(msg, positive = true) }
                    .onFailure { err -> showToast(err.message ?: "Error al sincronizar", positive = false) }
            }
        } else if (account != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSyncingCloud = true) }
                val pushed = repository.pushCloudSnapshot(account)
                _uiState.update { it.copy(isSyncingCloud = false) }
                showToast(
                    if (pushed) "Subido a la nube (${account.identifier})"
                    else "No se pudo subir a ntfy.sh",
                    positive = pushed
                )
            }
        }
    }

    fun pullCloudNow() {
        val account = _uiState.value.cloudAccount
        if (account == null && !syncManager.syncState.value.isConnected) return
        if (syncManager.syncState.value.isConnected) {
            syncCloudNow()
        } else if (account != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSyncingCloud = true) }
                repository.pullCloudSnapshot(account)
                    .onSuccess { changed ->
                        _uiState.update { it.copy(isSyncingCloud = false) }
                        showToast(
                            if (changed) "Cambios remotos aplicados"
                            else "Ya estás al día con la nube"
                        )
                    }
                    .onFailure {
                        _uiState.update { it.copy(isSyncingCloud = false) }
                        showToast("Error al consultar la nube", positive = false)
                    }
            }
        }
    }

    fun disconnectCloudAccount() {
        syncManager.disconnectAccount()
        showToast("Cuenta desconectada de este dispositivo")
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun scheduleCloudPush() {
        if (syncManager.syncState.value.isConnected) {
            syncManager.notifyLocalMutation(SyncModule.ENFOQUE)
            return
        }
        val account = _uiState.value.cloudAccount ?: return
        autoPushJob?.cancel()
        autoPushJob = viewModelScope.launch {
            delay(1200L)
            repository.pushCloudSnapshot(account)
        }
    }

    private fun showToast(message: String, positive: Boolean = true) {
        _uiState.update { it.copy(toastMessage = message, isToastPositive = positive) }
    }
}
