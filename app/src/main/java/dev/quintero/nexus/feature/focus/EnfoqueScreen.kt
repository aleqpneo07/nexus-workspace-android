package dev.quintero.nexus.feature.focus

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.quintero.nexus.core.design.CategoryProjectsColor
import dev.quintero.nexus.core.design.CategoryUniColor
import dev.quintero.nexus.core.design.ConflictWarningRed
import dev.quintero.nexus.core.design.EnfoqueCoralContainer
import dev.quintero.nexus.core.design.EnfoqueCoralSoft
import dev.quintero.nexus.core.design.MinimalHairline
import dev.quintero.nexus.core.design.MoonlightCyan
import dev.quintero.nexus.core.design.NightBgDeep
import dev.quintero.nexus.core.design.NightBgSurface
import dev.quintero.nexus.core.design.NightBorderSubtle
import dev.quintero.nexus.core.design.NightCardElevated
import dev.quintero.nexus.core.design.NightCardSurface
import dev.quintero.nexus.core.design.NocturnalAmber
import dev.quintero.nexus.core.design.SyncSuccessGreen
import dev.quintero.nexus.core.design.TextMoonlight
import dev.quintero.nexus.core.design.TextMutedSlate
import dev.quintero.nexus.core.design.TextStarlight
import dev.quintero.nexus.core.util.BackupShareHelper
import dev.quintero.nexus.core.util.DateTimeHelper
import dev.quintero.nexus.feature.workout.showAndroidDatePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EnfoqueModuleScreen(
    viewModel: EnfoqueViewModel,
    onBackToHub: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = viewModel.buildBackupJson()
            BackupShareHelper.writeJsonToUri(context, uri, json)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            BackupShareHelper.readTextFromUri(context, uri)
                .onSuccess { viewModel.importBackupJson(it) }
        }
    }

    BackHandler(enabled = true) {
        when {
            uiState.isCloudAndBackupModalOpen -> viewModel.closeCloudBackupModal()
            uiState.isTaskFormOpen -> viewModel.closeTaskForm()
            uiState.selectedTaskId != null -> viewModel.closeTaskInspector()
            else -> onBackToHub()
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(2800L)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = NightBgDeep,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            EnfoqueTopBar(
                activeCount = uiState.activeTasksCount,
                averageProgress = uiState.averageProgress,
                cloudAccount = uiState.cloudAccount,
                isSyncing = uiState.isSyncingCloud,
                onBackToHub = onBackToHub,
                onOpenCloudBackup = viewModel::openCloudBackupModal,
                onOpenNewTask = viewModel::openNewTaskForm
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                EnfoqueFocusSummaryStrip(
                    priorityTasks = uiState.priorityTodayTasks,
                    weeklyDeliveriesCount = uiState.weeklyDeliveriesCount,
                    completedCount = uiState.completedTasksCount,
                    onOpenTask = viewModel::openTaskInspector
                )

                EnfoqueToolbar(
                    searchQuery = uiState.searchQuery,
                    activeFilter = uiState.activeFilter,
                    sortBy = uiState.sortBy,
                    viewMode = uiState.viewMode,
                    onSearchChange = viewModel::setSearchQuery,
                    onFilterSelect = viewModel::setFilter,
                    onSortCycle = {
                        val next = when (uiState.sortBy) {
                            EnfoqueSortBy.FECHA -> EnfoqueSortBy.PROGRESO
                            EnfoqueSortBy.PROGRESO -> EnfoqueSortBy.TITULO
                            EnfoqueSortBy.TITULO -> EnfoqueSortBy.FECHA
                        }
                        viewModel.setSortBy(next)
                    },
                    onViewModeSelect = viewModel::setViewMode
                )

                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.viewMode) {
                        EnfoqueViewMode.TABLERO -> {
                            EnfoqueBoardContent(
                                tasks = uiState.filteredTasks,
                                subtasksByTaskId = uiState.subtasksByTaskId,
                                activeMobileTab = uiState.mobileStatusTab,
                                onSelectMobileTab = viewModel::setMobileStatusTab,
                                onOpenInspector = viewModel::openTaskInspector,
                                onToggleSubtask = viewModel::toggleSubtaskCompleted,
                                onChangeStatus = viewModel::updateTaskStatus,
                                onCreateFirst = viewModel::openNewTaskForm
                            )
                        }

                        EnfoqueViewMode.LISTA -> {
                            EnfoqueListContent(
                                tasks = uiState.filteredTasks,
                                onOpenInspector = viewModel::openTaskInspector,
                                onCycleStatus = { task ->
                                    val next = when (EnfoqueStatus.fromId(task.status)) {
                                        EnfoqueStatus.PENDIENTE -> EnfoqueStatus.EN_MARCHA
                                        EnfoqueStatus.EN_MARCHA -> EnfoqueStatus.TERMINADO
                                        EnfoqueStatus.TERMINADO -> EnfoqueStatus.PENDIENTE
                                    }
                                    viewModel.updateTaskStatus(task.id, next)
                                }
                            )
                        }

                        EnfoqueViewMode.CALENDARIO -> {
                            EnfoqueCalendarContent(
                                monthLabel = uiState.calendarMonthLabel,
                                cells = uiState.calendarCells,
                                selectedIso = uiState.calendarSelectedIso,
                                tasks = uiState.filteredTasks,
                                onPrevMonth = { viewModel.shiftCalendarMonth(-1) },
                                onNextMonth = { viewModel.shiftCalendarMonth(1) },
                                onJumpToday = viewModel::jumpCalendarToday,
                                onSelectDate = viewModel::selectCalendarDate,
                                onOpenInspector = viewModel::openTaskInspector,
                                onCreateForDate = viewModel::openNewTaskForm
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                val msg = uiState.toastMessage.orEmpty()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (uiState.isToastPositive) NightCardElevated else ConflictWarningRed.copy(alpha = 0.22f),
                    border = BorderStroke(
                        1.dp,
                        if (uiState.isToastPositive) EnfoqueCoralSoft.copy(alpha = 0.45f)
                        else ConflictWarningRed.copy(alpha = 0.55f)
                    ),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMoonlight,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    val selectedTask = uiState.selectedTask
    if (selectedTask != null) {
        EnfoqueInspectorSheet(
            task = selectedTask,
            subtasks = uiState.subtasksByTaskId[selectedTask.id].orEmpty(),
            onDismiss = viewModel::closeTaskInspector,
            onEditTask = { viewModel.openEditTaskForm(selectedTask.id) },
            onDeleteTask = { viewModel.deleteTask(selectedTask.id) },
            onStatusChange = { status -> viewModel.updateTaskStatus(selectedTask.id, status) },
            onDueDateChange = { iso -> viewModel.updateTaskDueDate(selectedTask.id, iso) },
            onToggleSubtask = viewModel::toggleSubtaskCompleted,
            onCycleSubtaskSchedule = viewModel::cycleSubtaskSchedule,
            onRenameSubtask = viewModel::updateSubtaskTitle,
            onAddSubtask = { title -> viewModel.addSubtaskToTask(selectedTask.id, title) },
            onDeleteSubtask = viewModel::deleteSubtask
        )
    }

    if (uiState.isTaskFormOpen) {
        val editing = uiState.editingTask
        val editingSubs = if (editing != null) {
            uiState.subtasksByTaskId[editing.id].orEmpty().map {
                EnfoqueFormSubtaskDraft(
                    id = it.id,
                    title = it.title,
                    completed = it.completed,
                    scheduleLabel = it.scheduleLabel
                )
            }
        } else {
            emptyList()
        }
        val initialDue = editing?.dueDateIso ?: if (uiState.viewMode == EnfoqueViewMode.CALENDARIO) {
            uiState.calendarSelectedIso
        } else {
            DateTimeHelper.todayIso()
        }
        EnfoqueTaskFormDialog(
            editingTask = editing,
            initialDueIso = initialDue,
            initialSubtasks = editingSubs,
            onDismiss = viewModel::closeTaskForm,
            onSave = { title, subtitle, category, status, dueIso, tag, subs ->
                viewModel.saveTaskFromForm(
                    existingId = editing?.id,
                    title = title,
                    subtitle = subtitle,
                    category = category,
                    status = status,
                    dueDateIso = dueIso,
                    tag = tag,
                    subtasksDraft = subs
                )
            }
        )
    }

    if (uiState.isCloudAndBackupModalOpen) {
        EnfoqueBackupAndCloudDialog(
            cloudAccount = uiState.cloudAccount,
            isSyncing = uiState.isSyncingCloud,
            tasksCount = uiState.allTasks.size,
            subtasksCount = uiState.allSubtasks.size,
            onDismiss = viewModel::closeCloudBackupModal,
            onExportSaf = {
                val fileName = "enfoque-backup-${DateTimeHelper.todayIso()}.json"
                exportLauncher.launch(fileName)
            },
            onShareFile = {
                val json = viewModel.buildBackupJson()
                val fileName = "enfoque-backup-${DateTimeHelper.todayIso()}.json"
                BackupShareHelper.shareJsonFile(
                    context = context,
                    fileName = fileName,
                    jsonContent = json,
                    chooserTitle = "Compartir copia de Enfoque",
                    subject = "Backup Enfoque ${DateTimeHelper.todayIso()}"
                )
            },
            onImportSaf = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onResetDemo = viewModel::resetDemo,
            onConnectCloud = viewModel::connectCloudAccount,
            onPushCloud = viewModel::syncCloudNow,
            onPullCloud = viewModel::pullCloudNow,
            onDisconnectCloud = viewModel::disconnectCloudAccount
        )
    }
}

@Composable
private fun EnfoqueTopBar(
    activeCount: Int,
    averageProgress: Int,
    cloudAccount: EnfoqueCloudAccount?,
    isSyncing: Boolean,
    onBackToHub: () -> Unit,
    onOpenCloudBackup: () -> Unit,
    onOpenNewTask: () -> Unit
) {
    Surface(
        color = NightBgDeep,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBackToHub,
                    shape = CircleShape,
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a Mis cosas",
                            tint = TextStarlight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EnfoqueCoralContainer)
                        .border(1.dp, EnfoqueCoralSoft.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DonutLarge,
                        contentDescription = null,
                        tint = EnfoqueCoralSoft,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enfoque",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$activeCount activas · $averageProgress% progreso",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = onOpenCloudBackup,
                    shape = CircleShape,
                    color = if (cloudAccount != null) EnfoqueCoralContainer else NightCardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (cloudAccount != null) EnfoqueCoralSoft.copy(alpha = 0.4f) else MinimalHairline
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                strokeWidth = 1.8.dp,
                                color = EnfoqueCoralSoft,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (cloudAccount != null) Icons.Outlined.CloudDone else Icons.Outlined.CloudQueue,
                                contentDescription = "Backup y Nube",
                                tint = if (cloudAccount != null) EnfoqueCoralSoft else TextMutedSlate,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onOpenNewTask,
                    shape = RoundedCornerShape(11.dp),
                    color = EnfoqueCoralSoft.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EnfoqueCoralSoft.copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = EnfoqueCoralSoft,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nueva",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = EnfoqueCoralSoft
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MinimalHairline)
            )
        }
    }
}

@Composable
private fun EnfoqueFocusSummaryStrip(
    priorityTasks: List<EnfoqueTaskEntity>,
    weeklyDeliveriesCount: Int,
    completedCount: Int,
    onOpenTask: (String) -> Unit
) {
    val leadTask = priorityTasks.firstOrNull()
    Surface(
        onClick = { if (leadTask != null) onOpenTask(leadTask.id) },
        shape = RoundedCornerShape(14.dp),
        color = NightCardSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (priorityTasks.isNotEmpty()) EnfoqueCoralSoft else SyncSuccessGreen)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (priorityTasks.isEmpty()) "Todo despejado para hoy" else "Prioridad hoy (${priorityTasks.size})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextMoonlight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (leadTask != null) {
                        "Foco: ${leadTask.title} · $weeklyDeliveriesCount entregas (7d)"
                    } else {
                        "$weeklyDeliveriesCount entregas esta semana · $completedCount completadas"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (leadTask != null) {
                Text(
                    text = "${leadTask.progress}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = EnfoqueCoralSoft
                )
            }
        }
    }
}

@Composable
private fun EnfoqueToolbar(
    searchQuery: String,
    activeFilter: String,
    sortBy: EnfoqueSortBy,
    viewMode: EnfoqueViewMode,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onSortCycle: () -> Unit,
    onViewModeSelect: (EnfoqueViewMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextMutedSlate,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Buscar proyectos, entregas o pasos...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextMoonlight),
                            cursorBrush = SolidColor(EnfoqueCoralSoft),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar búsqueda",
                            tint = TextMutedSlate,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onSearchChange("") }
                        )
                    }
                }
            }

            Surface(
                onClick = onSortCycle,
                shape = RoundedCornerShape(12.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline)
            ) {
                Text(
                    text = "Orden: ${sortBy.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextStarlight,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
                )
            }
        }

        val filters = listOf(
            "todo" to "Todo",
            "trabajo" to "Trabajos",
            "examen" to "Exámenes",
            "diario" to "Día a día",
            "entregas" to "7 días"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            filters.forEach { (id, label) ->
                val selected = activeFilter == id
                Surface(
                    onClick = { onFilterSelect(id) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) EnfoqueCoralContainer else NightCardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) EnfoqueCoralSoft.copy(alpha = 0.45f) else MinimalHairline
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (selected) EnfoqueCoralSoft else TextMutedSlate,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NightCardSurface,
            border = BorderStroke(1.dp, MinimalHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                EnfoqueViewMode.entries.forEach { mode ->
                    val selected = viewMode == mode
                    Surface(
                        onClick = { onViewModeSelect(mode) },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selected) NightCardElevated else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (selected) TextMoonlight else TextMutedSlate
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

internal fun resolveSwipeTargetStatus(current: EnfoqueStatus, dragDp: Float): EnfoqueStatus? {
    val stepDp = 82f
    val farStepDp = 178f
    return when (current) {
        EnfoqueStatus.PENDIENTE -> when {
            dragDp >= farStepDp -> EnfoqueStatus.TERMINADO
            dragDp >= stepDp -> EnfoqueStatus.EN_MARCHA
            else -> null
        }
        EnfoqueStatus.EN_MARCHA -> when {
            dragDp <= -stepDp -> EnfoqueStatus.PENDIENTE
            dragDp >= stepDp -> EnfoqueStatus.TERMINADO
            else -> null
        }
        EnfoqueStatus.TERMINADO -> when {
            dragDp <= -farStepDp -> EnfoqueStatus.PENDIENTE
            dragDp <= -stepDp -> EnfoqueStatus.EN_MARCHA
            else -> null
        }
    }
}

@Composable
private fun EnfoqueBoardContent(
    tasks: List<EnfoqueTaskEntity>,
    subtasksByTaskId: Map<String, List<EnfoqueSubtaskEntity>>,
    activeMobileTab: EnfoqueStatus,
    onSelectMobileTab: (EnfoqueStatus) -> Unit,
    onOpenInspector: (String) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onChangeStatus: (String, EnfoqueStatus) -> Unit,
    onCreateFirst: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var boardOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var columnDropBounds by remember { mutableStateOf<Map<EnfoqueStatus, Rect>>(emptyMap()) }
    var chipDropBounds by remember { mutableStateOf<Map<EnfoqueStatus, Rect>>(emptyMap()) }
    var draggingTask by remember { mutableStateOf<EnfoqueTaskEntity?>(null) }
    var dragFingerInRoot by remember { mutableStateOf(Offset.Zero) }
    var dragCardOffsetFromFinger by remember { mutableStateOf(Offset.Zero) }
    var dragCardWidthPx by remember { mutableIntStateOf(0) }

    fun findHoveredStatus(isWide: Boolean, finger: Offset): EnfoqueStatus? {
        val map = if (isWide) columnDropBounds else chipDropBounds
        return map.entries.firstOrNull { (_, rect) -> rect.contains(finger) }?.key
    }

    fun startDraggingTask(
        task: EnfoqueTaskEntity,
        cardOriginInRoot: Offset,
        fingerInRoot: Offset,
        cardWidthPx: Int
    ) {
        draggingTask = task
        dragFingerInRoot = fingerInRoot
        dragCardOffsetFromFinger = cardOriginInRoot - fingerInRoot
        dragCardWidthPx = cardWidthPx
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun finishDraggingTask(isWide: Boolean) {
        val current = draggingTask
        if (current != null) {
            val dropTarget = findHoveredStatus(isWide, dragFingerInRoot)
            if (dropTarget != null && dropTarget.id != current.status) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onChangeStatus(current.id, dropTarget)
            }
        }
        draggingTask = null
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                boardOriginInRoot = coords.positionInRoot()
            }
    ) {
        val isWide = maxWidth >= 720.dp
        val hoveredStatus = if (draggingTask != null) findHoveredStatus(isWide, dragFingerInRoot) else null

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnfoqueStatus.entries.forEach { status ->
                    val colTasks = tasks.filter { it.status == status.id }
                    val isDropTarget = hoveredStatus == status && draggingTask?.status != status.id
                    val dropAccent = enfoqueStatusColor(status)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isDropTarget) dropAccent.copy(alpha = 0.08f) else Color.Transparent
                            )
                            .border(
                                width = if (isDropTarget) 1.5.dp else 0.dp,
                                color = if (isDropTarget) dropAccent.copy(alpha = 0.65f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = if (isDropTarget) 6.dp else 0.dp, vertical = 2.dp)
                            .onGloballyPositioned { coords ->
                                columnDropBounds = columnDropBounds + (status to coords.boundsInRoot())
                            }
                    ) {
                        EnfoqueColumnHeader(status = status, count = colTasks.size)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(colTasks, key = { it.id }) { task ->
                                val isBeingDragged = draggingTask?.id == task.id
                                EnfoqueTaskCard(
                                    task = task,
                                    subtasks = subtasksByTaskId[task.id].orEmpty(),
                                    isDimmedForDrag = isBeingDragged,
                                    onClick = { onOpenInspector(task.id) },
                                    onToggleSubtask = onToggleSubtask,
                                    onChangeStatus = { target -> onChangeStatus(task.id, target) },
                                    onAdvanceStatus = {
                                        val next = when (status) {
                                            EnfoqueStatus.PENDIENTE -> EnfoqueStatus.EN_MARCHA
                                            EnfoqueStatus.EN_MARCHA -> EnfoqueStatus.TERMINADO
                                            EnfoqueStatus.TERMINADO -> EnfoqueStatus.PENDIENTE
                                        }
                                        onChangeStatus(task.id, next)
                                    },
                                    onDragStart = { cardOrigin, finger, widthPx ->
                                        startDraggingTask(task, cardOrigin, finger, widthPx)
                                    },
                                    onDragMove = { delta ->
                                        dragFingerInRoot += delta
                                    },
                                    onDragEnd = { finishDraggingTask(isWide = true) },
                                    onDragCancel = { draggingTask = null }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EnfoqueStatus.entries.forEach { status ->
                        val count = tasks.count { it.status == status.id }
                        val selected = activeMobileTab == status
                        val isDropHover = hoveredStatus == status && draggingTask?.status != status.id
                        val dotColor = enfoqueStatusColor(status)
                        Surface(
                            onClick = { onSelectMobileTab(status) },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isDropHover -> dotColor.copy(alpha = 0.22f)
                                selected -> NightCardElevated
                                else -> NightCardSurface
                            },
                            border = BorderStroke(
                                width = if (isDropHover) 1.5.dp else 1.dp,
                                color = when {
                                    isDropHover -> dotColor
                                    selected -> dotColor.copy(alpha = 0.55f)
                                    else -> MinimalHairline
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    chipDropBounds = chipDropBounds + (status to coords.boundsInRoot())
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${status.label} ($count)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected || isDropHover) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (selected || isDropHover) TextMoonlight else TextMutedSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                val columnTasks = tasks.filter { it.status == activeMobileTab.id }
                if (columnTasks.isEmpty()) {
                    EnfoqueEmptyState(
                        title = "Sin tareas en ${activeMobileTab.label.lowercase()}",
                        subtitle = "Pulsa «Nueva» para añadir una tarea o desliza una de otra columna.",
                        onCreate = onCreateFirst
                    )
                } else {
                    Text(
                        text = if (draggingTask != null) "Suelta sobre Pendiente, En marcha o Terminado" else "Desliza la tarjeta ← → o arrastra desde ⋮⋮",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (draggingTask != null) MoonlightCyan else TextMutedSlate.copy(alpha = 0.72f),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(columnTasks, key = { it.id }) { task ->
                            val isBeingDragged = draggingTask?.id == task.id
                            EnfoqueTaskCard(
                                task = task,
                                subtasks = subtasksByTaskId[task.id].orEmpty(),
                                isDimmedForDrag = isBeingDragged,
                                onClick = { onOpenInspector(task.id) },
                                onToggleSubtask = onToggleSubtask,
                                onChangeStatus = { target -> onChangeStatus(task.id, target) },
                                onAdvanceStatus = {
                                    val next = when (activeMobileTab) {
                                        EnfoqueStatus.PENDIENTE -> EnfoqueStatus.EN_MARCHA
                                        EnfoqueStatus.EN_MARCHA -> EnfoqueStatus.TERMINADO
                                        EnfoqueStatus.TERMINADO -> EnfoqueStatus.PENDIENTE
                                    }
                                    onChangeStatus(task.id, next)
                                },
                                onDragStart = { cardOrigin, finger, widthPx ->
                                    startDraggingTask(task, cardOrigin, finger, widthPx)
                                },
                                onDragMove = { delta ->
                                    dragFingerInRoot += delta
                                },
                                onDragEnd = { finishDraggingTask(isWide = false) },
                                onDragCancel = { draggingTask = null }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }

        // Floating dragged card preview during cross-column / chip drag & drop
        val activeDrag = draggingTask
        if (activeDrag != null) {
            val relTopLeft = (dragFingerInRoot + dragCardOffsetFromFinger) - boardOriginInRoot
            val cardWidthDp = with(density) {
                dragCardWidthPx.takeIf { it > 0 }?.toDp() ?: 260.dp
            }
            val hoverAccent = hoveredStatus?.let(::enfoqueStatusColor) ?: EnfoqueCoralSoft
            Box(
                modifier = Modifier
                    .zIndex(50f)
                    .offset { IntOffset(relTopLeft.x.roundToInt(), relTopLeft.y.roundToInt()) }
                    .width(cardWidthDp)
                    .graphicsLayer {
                        rotationZ = 1.6f
                        scaleX = 1.02f
                        scaleY = 1.02f
                        shadowElevation = 24f
                        alpha = 0.94f
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NightCardElevated,
                    border = BorderStroke(1.5.dp, hoverAccent)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hoveredStatus != null && hoveredStatus.id != activeDrag.status) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = hoverAccent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "Soltar en ${hoveredStatus.label}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = hoverAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = activeDrag.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextMoonlight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeDrag.completedSubtasks}/${activeDrag.totalSubtasks} · ${activeDrag.progress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedSlate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnfoqueColumnHeader(status: EnfoqueStatus, count: Int) {
    val dotColor = enfoqueStatusColor(status)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextMoonlight
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = TextMutedSlate
        )
    }
}

@Composable
private fun EnfoqueTaskCard(
    task: EnfoqueTaskEntity,
    subtasks: List<EnfoqueSubtaskEntity>,
    isDimmedForDrag: Boolean = false,
    onClick: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onChangeStatus: (EnfoqueStatus) -> Unit,
    onAdvanceStatus: () -> Unit,
    onDragStart: (cardOriginInRoot: Offset, fingerInRoot: Offset, widthPx: Int) -> Unit,
    onDragMove: (delta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val todayIso = DateTimeHelper.todayIso()
    val dateInfo = EnfoqueDomainHelper.formatSpanishDateInfo(task.dueDateIso, todayIso)
    val cat = EnfoqueCategory.fromId(task.category)
    val catColor = enfoqueCategoryColor(cat)
    val statusEnum = EnfoqueStatus.fromId(task.status)
    val isFinished = statusEnum == EnfoqueStatus.TERMINADO
    val progressColor = if (isFinished || task.progress >= 100) SyncSuccessGreen else EnfoqueCoralSoft

    val dateChipColor = when {
        isFinished -> TextMutedSlate
        dateInfo.isOverdue -> ConflictWarningRed
        dateInfo.isToday -> EnfoqueCoralSoft
        dateInfo.isTomorrow -> NocturnalAmber
        else -> TextMutedSlate
    }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val swipeOffsetPx = remember(task.id, task.status) { Animatable(0f) }

    var cardOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var cardWidthPx by remember { mutableIntStateOf(0) }
    var handleOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var lastHapticTarget by remember(task.id, task.status) { mutableStateOf<EnfoqueStatus?>(null) }

    val latestStatus by rememberUpdatedState(statusEnum)
    val latestOnChangeStatus by rememberUpdatedState(onChangeStatus)
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDragMove by rememberUpdatedState(onDragMove)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onDragCancel)

    val currentDragDp = with(density) { swipeOffsetPx.value.toDp().value }
    val pendingSwipeTarget = resolveSwipeTargetStatus(statusEnum, currentDragDp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                cardOriginInRoot = coords.positionInRoot()
                cardWidthPx = coords.size.width
            }
            .pointerInput(task.id, statusEnum) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val curDp = with(density) { swipeOffsetPx.value.toDp().value }
                        val canGoLeft = latestStatus != EnfoqueStatus.PENDIENTE
                        val canGoRight = latestStatus != EnfoqueStatus.TERMINADO
                        val resisted = when {
                            swipeOffsetPx.value + dragAmount < 0f && !canGoLeft -> dragAmount * 0.22f
                            swipeOffsetPx.value + dragAmount > 0f && !canGoRight -> dragAmount * 0.22f
                            else -> dragAmount
                        }
                        val nextPx = (swipeOffsetPx.value + resisted).coerceIn(-680f, 680f)
                        scope.launch { swipeOffsetPx.snapTo(nextPx) }

                        val nextTarget = resolveSwipeTargetStatus(latestStatus, with(density) { nextPx.toDp().value })
                        if (nextTarget != lastHapticTarget) {
                            lastHapticTarget = nextTarget
                            if (nextTarget != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    },
                    onDragEnd = {
                        val finalDp = with(density) { swipeOffsetPx.value.toDp().value }
                        val target = resolveSwipeTargetStatus(latestStatus, finalDp)
                        lastHapticTarget = null
                        if (target != null && target != latestStatus) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            latestOnChangeStatus(target)
                            scope.launch { swipeOffsetPx.snapTo(0f) }
                        } else {
                            scope.launch { swipeOffsetPx.animateTo(0f, spring(stiffness = 500f)) }
                        }
                    },
                    onDragCancel = {
                        lastHapticTarget = null
                        scope.launch { swipeOffsetPx.animateTo(0f, spring(stiffness = 500f)) }
                    }
                )
            }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        latestOnDragStart(
                            cardOriginInRoot,
                            cardOriginInRoot + localOffset,
                            cardWidthPx
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        latestOnDragMove(dragAmount)
                    },
                    onDragEnd = { latestOnDragEnd() },
                    onDragCancel = { latestOnDragCancel() }
                )
            }
    ) {
        if (abs(swipeOffsetPx.value) > 12f) {
            val previewStatus = pendingSwipeTarget ?: when {
                swipeOffsetPx.value > 0f && statusEnum == EnfoqueStatus.PENDIENTE -> EnfoqueStatus.EN_MARCHA
                swipeOffsetPx.value > 0f && statusEnum == EnfoqueStatus.EN_MARCHA -> EnfoqueStatus.TERMINADO
                swipeOffsetPx.value < 0f && statusEnum == EnfoqueStatus.TERMINADO -> EnfoqueStatus.EN_MARCHA
                swipeOffsetPx.value < 0f && statusEnum == EnfoqueStatus.EN_MARCHA -> EnfoqueStatus.PENDIENTE
                else -> null
            }
            if (previewStatus != null) {
                val previewColor = enfoqueStatusColor(previewStatus)
                val isArmed = pendingSwipeTarget != null
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            previewColor.copy(alpha = if (isArmed) 0.22f else 0.08f)
                        )
                        .border(
                            width = 1.dp,
                            color = previewColor.copy(alpha = if (isArmed) 0.65f else 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = if (swipeOffsetPx.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Text(
                        text = if (swipeOffsetPx.value >= 0f) {
                            "${if (isArmed) "Soltar →" else "Desliza →"} ${previewStatus.label}"
                        } else {
                            "${previewStatus.label} ${if (isArmed) "← Soltar" else "← Desliza"}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = previewColor
                    )
                }
            }
        }

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = NightCardSurface,
            border = BorderStroke(
                1.dp,
                if (pendingSwipeTarget != null) enfoqueStatusColor(pendingSwipeTarget).copy(alpha = 0.7f) else MinimalHairline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffsetPx.value.roundToInt(), 0) }
                .graphicsLayer { alpha = if (isDimmedForDrag) 0.28f else 1f }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = catColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateInfo.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = dateChipColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = onAdvanceStatus,
                            shape = CircleShape,
                            color = enfoqueStatusColor(statusEnum).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, enfoqueStatusColor(statusEnum).copy(alpha = 0.45f)),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Cambiar estado",
                                    tint = if (isFinished) SyncSuccessGreen else enfoqueStatusColor(statusEnum),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .onGloballyPositioned { coords ->
                                    handleOriginInRoot = coords.positionInRoot()
                                }
                                .pointerInput(task.id) {
                                    detectDragGestures(
                                        onDragStart = { localOffset ->
                                            latestOnDragStart(
                                                cardOriginInRoot,
                                                handleOriginInRoot + localOffset,
                                                cardWidthPx
                                            )
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            latestOnDragMove(dragAmount)
                                        },
                                        onDragEnd = { latestOnDragEnd() },
                                        onDragCancel = { latestOnDragCancel() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Arrastrar a otra columna",
                                tint = TextMutedSlate.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isFinished) TextMutedSlate else TextMoonlight,
                    textDecoration = if (isFinished) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.subtitle.isNotBlank()) {
                    Text(
                        text = task.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NightBgSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((task.progress.coerceIn(0, 100)) / 100f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(progressColor)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${task.completedSubtasks}/${task.totalSubtasks} · ${task.progress}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = TextStarlight
                )
            }

            val previewSubs = subtasks.filter { !it.completed }.take(2)
            if (previewSubs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    previewSubs.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleSubtask(sub.id) }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                                    .border(1.2.dp, TextMutedSlate, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextStarlight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (sub.scheduleLabel.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                EnfoqueScheduleTagBadge(label = sub.scheduleLabel)
                            }
                        }
                    }
                }
            } else if (task.tag.isNotBlank()) {
                Text(
                    text = "# ${task.tag}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedSlate
                )
            }
        }
    }
}
}

@Composable
private fun EnfoqueListContent(
    tasks: List<EnfoqueTaskEntity>,
    onOpenInspector: (String) -> Unit,
    onCycleStatus: (EnfoqueTaskEntity) -> Unit
) {
    val todayIso = DateTimeHelper.todayIso()
    if (tasks.isEmpty()) {
        EnfoqueEmptyState(
            title = "No hay resultados",
            subtitle = "Prueba con otro filtro o limpia la búsqueda.",
            onCreate = null
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tasks, key = { it.id }) { task ->
            val statusEnum = EnfoqueStatus.fromId(task.status)
            val statusColor = enfoqueStatusColor(statusEnum)
            val dateInfo = EnfoqueDomainHelper.formatSpanishDateInfo(task.dueDateIso, todayIso)
            val cat = EnfoqueCategory.fromId(task.category)
            Surface(
                onClick = { onOpenInspector(task.id) },
                shape = RoundedCornerShape(14.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { onCycleStatus(task) },
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, statusColor),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (statusEnum == EnfoqueStatus.TERMINADO) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SyncSuccessGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                            } else if (statusEnum == EnfoqueStatus.EN_MARCHA) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (statusEnum == EnfoqueStatus.TERMINADO) TextMutedSlate else TextMoonlight,
                            textDecoration = if (statusEnum == EnfoqueStatus.TERMINADO) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${cat.label} · ${dateInfo.shortLabel} (${DateTimeHelper.formatShortDayMonthEs(task.dueDateIso)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "${task.progress}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (statusEnum == EnfoqueStatus.TERMINADO) SyncSuccessGreen else EnfoqueCoralSoft
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun EnfoqueCalendarContent(
    monthLabel: String,
    cells: List<EnfoqueCalendarDayCell>,
    selectedIso: String,
    tasks: List<EnfoqueTaskEntity>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectDate: (String) -> Unit,
    onOpenInspector: (String) -> Unit,
    onCreateForDate: () -> Unit
) {
    val tasksByDate = remember(tasks) { tasks.groupBy { it.dueDateIso } }
    val selectedDayTasks = tasksByDate[selectedIso].orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Mes anterior",
                                tint = TextStarlight
                            )
                        }
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextMoonlight,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = onJumpToday,
                            shape = RoundedCornerShape(8.dp),
                            color = NightCardElevated,
                            border = BorderStroke(1.dp, MinimalHairline)
                        ) {
                            Text(
                                text = "Hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = EnfoqueCoralSoft,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Mes siguiente",
                                tint = TextStarlight
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEach { dow ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dow,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                            }
                        }
                    }

                    cells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { cell ->
                                val dayTasks = tasksByDate[cell.isoDate].orEmpty()
                                val isSelected = cell.isoDate == selectedIso
                                Surface(
                                    onClick = { onSelectDate(cell.isoDate) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isSelected -> EnfoqueCoralContainer
                                        cell.isToday -> NightCardElevated
                                        else -> Color.Transparent
                                    },
                                    border = when {
                                        isSelected -> BorderStroke(1.dp, EnfoqueCoralSoft.copy(alpha = 0.6f))
                                        cell.isToday -> BorderStroke(1.dp, MinimalHairline)
                                        else -> null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(3.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${cell.dayNumber}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (cell.isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = when {
                                                !cell.isCurrentMonth -> TextMutedSlate.copy(alpha = 0.45f)
                                                isSelected || cell.isToday -> EnfoqueCoralSoft
                                                else -> TextStarlight
                                            }
                                        )
                                        if (dayTasks.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                dayTasks.take(3).forEach { t ->
                                                    val c = enfoqueCategoryColor(EnfoqueCategory.fromId(t.category))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(c)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Fecha: ${DateTimeHelper.formatSpanishLongDate(selectedIso)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = TextMoonlight
                )
                TextButton(onClick = onCreateForDate) {
                    Text("+ Añadir aquí", color = EnfoqueCoralSoft)
                }
            }
        }

        if (selectedDayTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sin entregas programadas para este día.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(selectedDayTasks, key = { it.id }) { task ->
                val cat = EnfoqueCategory.fromId(task.category)
                Surface(
                    onClick = { onOpenInspector(task.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(enfoqueCategoryColor(cat))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = TextMoonlight
                            )
                            Text(
                                text = "${cat.label} · ${EnfoqueStatus.fromId(task.status).label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedSlate
                            )
                        }
                        Text(
                            text = "${task.progress}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = EnfoqueCoralSoft
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnfoqueInspectorSheet(
    task: EnfoqueTaskEntity,
    subtasks: List<EnfoqueSubtaskEntity>,
    onDismiss: () -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onStatusChange: (EnfoqueStatus) -> Unit,
    onDueDateChange: (String) -> Unit,
    onToggleSubtask: (String) -> Unit,
    onCycleSubtaskSchedule: (String) -> Unit,
    onRenameSubtask: (String, String) -> Unit,
    onAddSubtask: (String) -> Unit,
    onDeleteSubtask: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var newSubTitle by remember(task.id) { mutableStateOf("") }
    var editingSubId by remember(task.id) { mutableStateOf<String?>(null) }
    var editingSubText by remember(task.id) { mutableStateOf("") }
    var confirmDelete by remember(task.id) { mutableStateOf(false) }

    val cat = EnfoqueCategory.fromId(task.category)
    val catColor = enfoqueCategoryColor(cat)
    val todayIso = DateTimeHelper.todayIso()
    val tomorrowIso = DateTimeHelper.tomorrowIso()
    val currentStatus = EnfoqueStatus.fromId(task.status)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NightBgSurface,
        contentColor = TextMoonlight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = catColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    if (task.tag.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NightCardSurface,
                            border = BorderStroke(1.dp, MinimalHairline)
                        ) {
                            Text(
                                text = task.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextStarlight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditTask, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar tarea",
                            tint = TextStarlight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (confirmDelete) {
                        TextButton(onClick = onDeleteTask) {
                            Text("Confirmar borrar", color = ConflictWarningRed, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar tarea",
                                tint = ConflictWarningRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextMoonlight
                )
                if (task.subtitle.isNotBlank()) {
                    Text(
                        text = task.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                }
            }

            // 3-way Status selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMutedSlate
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EnfoqueStatus.entries.forEach { st ->
                        val selected = currentStatus == st
                        val stColor = enfoqueStatusColor(st)
                        Surface(
                            onClick = { onStatusChange(st) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) stColor.copy(alpha = 0.18f) else NightCardSurface,
                            border = BorderStroke(1.dp, if (selected) stColor else MinimalHairline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = st.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (selected) stColor else TextMutedSlate
                                )
                            }
                        }
                    }
                }
            }

            // Due date row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Fecha límite: ${DateTimeHelper.formatSpanishLongDate(task.dueDateIso)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMutedSlate
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Hoy" to todayIso,
                        "Mañana" to tomorrowIso,
                        "+3 días" to DateTimeHelper.addDaysToIso(todayIso, 3),
                        "+7 días" to DateTimeHelper.addDaysToIso(todayIso, 7)
                    ).forEach { (label, iso) ->
                        val isMatch = task.dueDateIso == iso
                        Surface(
                            onClick = { onDueDateChange(iso) },
                            shape = RoundedCornerShape(9.dp),
                            color = if (isMatch) EnfoqueCoralContainer else NightCardSurface,
                            border = BorderStroke(1.dp, if (isMatch) EnfoqueCoralSoft.copy(alpha = 0.5f) else MinimalHairline)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMatch) EnfoqueCoralSoft else TextStarlight,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            showAndroidDatePicker(context, task.dueDateIso) { picked ->
                                onDueDateChange(picked)
                            }
                        },
                        shape = RoundedCornerShape(9.dp),
                        color = NightCardSurface,
                        border = BorderStroke(1.dp, MinimalHairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = TextStarlight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Elegir",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextStarlight
                            )
                        }
                    }
                }
            }

            // Progress summary card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${task.completedSubtasks} de ${task.totalSubtasks} pasos completados",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextStarlight
                        )
                        Text(
                            text = "${task.progress}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (task.progress >= 100) SyncSuccessGreen else EnfoqueCoralSoft
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NightBgSurface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((task.progress.coerceIn(0, 100)) / 100f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (task.progress >= 100) SyncSuccessGreen else EnfoqueCoralSoft)
                        )
                    }
                }
            }

            // Subtasks list
            Text(
                text = "Subtareas (toca la etiqueta para cambiar En curso / Hoy / Mañana)",
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedSlate
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                subtasks.forEach { sub ->
                    val isEditingThis = editingSubId == sub.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NightCardSurface,
                        border = BorderStroke(1.dp, MinimalHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (sub.completed) SyncSuccessGreen.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        1.4.dp,
                                        if (sub.completed) SyncSuccessGreen else TextMutedSlate,
                                        CircleShape
                                    )
                                    .clickable { onToggleSubtask(sub.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (sub.completed) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SyncSuccessGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            if (isEditingThis) {
                                BasicTextField(
                                    value = editingSubText,
                                    onValueChange = { editingSubText = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = TextMoonlight),
                                    cursorBrush = SolidColor(EnfoqueCoralSoft),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        onRenameSubtask(sub.id, editingSubText)
                                        editingSubId = null
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Guardar", tint = SyncSuccessGreen, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Text(
                                    text = sub.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (sub.completed) TextMutedSlate else TextMoonlight,
                                    textDecoration = if (sub.completed) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            editingSubId = sub.id
                                            editingSubText = sub.title
                                        }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    onClick = { onCycleSubtaskSchedule(sub.id) },
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (sub.scheduleLabel.isNotBlank()) EnfoqueCoralContainer else NightBgSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (sub.scheduleLabel.isNotBlank()) EnfoqueCoralSoft.copy(alpha = 0.35f) else MinimalHairline
                                    )
                                ) {
                                    Text(
                                        text = sub.scheduleLabel.ifBlank { "+ Día" },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (sub.scheduleLabel.isNotBlank()) EnfoqueCoralSoft else TextMutedSlate,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteSubtask(sub.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar paso",
                                        tint = TextMutedSlate,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Add subtask input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                        if (newSubTitle.isEmpty()) {
                            Text(
                                text = "Nuevo paso o subtarea...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedSlate
                            )
                        }
                        BasicTextField(
                            value = newSubTitle,
                            onValueChange = { newSubTitle = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextMoonlight),
                            cursorBrush = SolidColor(EnfoqueCoralSoft),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Surface(
                    onClick = {
                        if (newSubTitle.isNotBlank()) {
                            onAddSubtask(newSubTitle)
                            newSubTitle = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = EnfoqueCoralSoft.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, EnfoqueCoralSoft.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir paso",
                        tint = EnfoqueCoralSoft,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnfoqueTaskFormDialog(
    editingTask: EnfoqueTaskEntity?,
    initialDueIso: String,
    initialSubtasks: List<EnfoqueFormSubtaskDraft>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        subtitle: String,
        category: EnfoqueCategory,
        status: EnfoqueStatus,
        dueIso: String,
        tag: String,
        subtasks: List<EnfoqueFormSubtaskDraft>
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember(editingTask?.id) { mutableStateOf(editingTask?.title.orEmpty()) }
    var subtitle by remember(editingTask?.id) { mutableStateOf(editingTask?.subtitle.orEmpty()) }
    var category by remember(editingTask?.id) {
        mutableStateOf(EnfoqueCategory.fromId(editingTask?.category ?: EnfoqueCategory.TRABAJO.id))
    }
    var status by remember(editingTask?.id) {
        mutableStateOf(EnfoqueStatus.fromId(editingTask?.status ?: EnfoqueStatus.PENDIENTE.id))
    }
    var dueIso by remember(editingTask?.id) { mutableStateOf(initialDueIso) }
    var tag by remember(editingTask?.id) { mutableStateOf(editingTask?.tag.orEmpty()) }
    var newStepText by remember { mutableStateOf("") }
    val draftSubs = remember(editingTask?.id) {
        mutableStateListOf<EnfoqueFormSubtaskDraft>().apply { addAll(initialSubtasks) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NightBgSurface,
            border = BorderStroke(1.dp, NightBorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingTask != null) "Editar proyecto / tarea" else "Nueva tarea",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextMutedSlate)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título *") },
                    singleLine = true,
                    colors = enfoqueTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Asignatura, cliente o contexto") },
                    singleLine = true,
                    colors = enfoqueTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoría", style = MaterialTheme.typography.labelMedium, color = TextMutedSlate)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnfoqueCategory.entries.forEach { cat ->
                        val selected = category == cat
                        val cColor = enfoqueCategoryColor(cat)
                        Surface(
                            onClick = { category = cat },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) cColor.copy(alpha = 0.18f) else NightCardSurface,
                            border = BorderStroke(1.dp, if (selected) cColor else MinimalHairline)
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) cColor else TextStarlight,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Text("Estado inicial", style = MaterialTheme.typography.labelMedium, color = TextMutedSlate)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EnfoqueStatus.entries.forEach { st ->
                        val selected = status == st
                        val stColor = enfoqueStatusColor(st)
                        Surface(
                            onClick = { status = st },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) stColor.copy(alpha = 0.18f) else NightCardSurface,
                            border = BorderStroke(1.dp, if (selected) stColor else MinimalHairline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = st.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) stColor else TextMutedSlate
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Fecha objetivo", style = MaterialTheme.typography.labelMedium, color = TextMutedSlate)
                        Text(
                            text = DateTimeHelper.formatSpanishLongDate(dueIso),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMoonlight
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { dueIso = DateTimeHelper.todayIso() },
                            shape = RoundedCornerShape(8.dp),
                            color = NightCardSurface,
                            border = BorderStroke(1.dp, MinimalHairline)
                        ) {
                            Text("Hoy", style = MaterialTheme.typography.labelSmall, color = TextStarlight, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                        }
                        Surface(
                            onClick = { showAndroidDatePicker(context, dueIso) { dueIso = it } },
                            shape = RoundedCornerShape(8.dp),
                            color = EnfoqueCoralContainer,
                            border = BorderStroke(1.dp, EnfoqueCoralSoft.copy(alpha = 0.45f))
                        ) {
                            Text("Calendario", style = MaterialTheme.typography.labelSmall, color = EnfoqueCoralSoft, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Etiqueta (ej. Temas 1-5, Entrega)") },
                    singleLine = true,
                    colors = enfoqueTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Subtareas / checklist (${draftSubs.size})", style = MaterialTheme.typography.labelMedium, color = TextMutedSlate)
                if (draftSubs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        draftSubs.forEachIndexed { idx, sub ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NightCardSurface,
                                border = BorderStroke(1.dp, MinimalHairline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${sub.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextStarlight,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { draftSubs.removeAt(idx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Quitar", tint = TextMutedSlate, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newStepText,
                        onValueChange = { newStepText = it },
                        placeholder = { Text("Añadir paso...", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        colors = enfoqueTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newStepText.isNotBlank()) {
                                draftSubs.add(EnfoqueFormSubtaskDraft(title = newStepText.trim()))
                                newStepText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EnfoqueCoralContainer, contentColor = EnfoqueCoralSoft),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextMutedSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(title, subtitle, category, status, dueIso, tag, draftSubs.toList())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EnfoqueCoralSoft, contentColor = NightBgDeep)
                    ) {
                        Text(
                            text = if (editingTask != null) "Guardar cambios" else "Crear tarea",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnfoqueBackupAndCloudDialog(
    cloudAccount: EnfoqueCloudAccount?,
    isSyncing: Boolean,
    tasksCount: Int,
    subtasksCount: Int,
    onDismiss: () -> Unit,
    onExportSaf: () -> Unit,
    onShareFile: () -> Unit,
    onImportSaf: () -> Unit,
    onResetDemo: () -> Unit,
    onConnectCloud: (String, String) -> Unit,
    onPushCloud: () -> Unit,
    onPullCloud: () -> Unit,
    onDisconnectCloud: () -> Unit
) {
    var userIdInput by remember { mutableStateOf(cloudAccount?.identifier.orEmpty()) }
    var pinInput by remember { mutableStateOf("") }
    var confirmDemoReset by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NightBgSurface,
            border = BorderStroke(1.dp, NightBorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Backup,
                            contentDescription = null,
                            tint = EnfoqueCoralSoft,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backups y Nube",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextMutedSlate)
                    }
                }

                // Local JSON backup card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Copia JSON ($tasksCount tareas · $subtasksCount pasos)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextMoonlight
                        )
                        Text(
                            text = "Exporta o restaura tus proyectos, estados y subtareas en formato JSON portátil.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExportSaf,
                                border = BorderStroke(1.dp, MinimalHairline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextStarlight)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar", color = TextStarlight, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onShareFile,
                                border = BorderStroke(1.dp, MinimalHairline),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextStarlight)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compartir", color = TextStarlight, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onImportSaf,
                                colors = ButtonDefaults.buttonColors(containerColor = EnfoqueCoralContainer, contentColor = EnfoqueCoralSoft),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Importar .json", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (confirmDemoReset) {
                                        onResetDemo()
                                        confirmDemoReset = false
                                    } else {
                                        confirmDemoReset = true
                                    }
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (confirmDemoReset) ConflictWarningRed else MinimalHairline
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = if (confirmDemoReset) ConflictWarningRed else TextMutedSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (confirmDemoReset) "¿Confirmar?" else "Demo",
                                    color = if (confirmDemoReset) ConflictWarningRed else TextMutedSlate,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // Cloud ntfy.sh sync card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nube privada (ntfy.sh)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextMoonlight
                            )
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = EnfoqueCoralSoft,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (cloudAccount == null) {
                            Text(
                                text = "Usa el mismo usuario y PIN que en la versión web para sincronizar dispositivos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedSlate
                            )
                            OutlinedTextField(
                                value = userIdInput,
                                onValueChange = { userIdInput = it },
                                label = { Text("Usuario o alias") },
                                singleLine = true,
                                colors = enfoqueTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { pinInput = it },
                                label = { Text("PIN o clave de sincronización") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = enfoqueTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { onConnectCloud(userIdInput, pinInput) },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = EnfoqueCoralSoft, contentColor = NightBgDeep),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Conectar y sincronizar", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text(
                                text = "Conectado como ${cloudAccount.identifier}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SyncSuccessGreen
                            )
                            Text(
                                text = "Canal: ${cloudAccount.topicId.take(24)}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedSlate
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onPushCloud,
                                    enabled = !isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = EnfoqueCoralContainer, contentColor = EnfoqueCoralSoft),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Subir", style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = onPullCloud,
                                    enabled = !isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = NightCardElevated, contentColor = TextStarlight),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Descargar", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            TextButton(
                                onClick = onDisconnectCloud,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Desconectar cuenta", color = ConflictWarningRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnfoqueScheduleTagBadge(label: String) {
    val bg = when (label) {
        "En curso" -> EnfoqueCoralContainer
        "Hoy" -> MoonlightCyan.copy(alpha = 0.16f)
        else -> NightCardElevated
    }
    val fg = when (label) {
        "En curso" -> EnfoqueCoralSoft
        "Hoy" -> MoonlightCyan
        else -> TextStarlight
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EnfoqueEmptyState(
    title: String,
    subtitle: String,
    onCreate: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextMoonlight
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedSlate
            )
            if (onCreate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onCreate,
                    border = BorderStroke(1.dp, EnfoqueCoralSoft.copy(alpha = 0.45f))
                ) {
                    Text("+ Crear tarea", color = EnfoqueCoralSoft)
                }
            }
        }
    }
}

@Composable
private fun enfoqueTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EnfoqueCoralSoft,
    unfocusedBorderColor = MinimalHairline,
    focusedLabelColor = EnfoqueCoralSoft,
    unfocusedLabelColor = TextMutedSlate,
    focusedTextColor = TextMoonlight,
    unfocusedTextColor = TextMoonlight,
    cursorColor = EnfoqueCoralSoft
)

private fun enfoqueCategoryColor(cat: EnfoqueCategory): Color = when (cat) {
    EnfoqueCategory.TRABAJO -> EnfoqueCoralSoft
    EnfoqueCategory.EXAMEN -> CategoryUniColor
    EnfoqueCategory.DIARIO -> SyncSuccessGreen
}

private fun enfoqueStatusColor(status: EnfoqueStatus): Color = when (status) {
    EnfoqueStatus.PENDIENTE -> NocturnalAmber
    EnfoqueStatus.EN_MARCHA -> CategoryProjectsColor
    EnfoqueStatus.TERMINADO -> SyncSuccessGreen
}
