package dev.quintero.nexus.feature.workout

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.quintero.nexus.core.design.ConflictWarningRed
import dev.quintero.nexus.core.design.MinimalHairline
import dev.quintero.nexus.core.design.NightBgDeep
import dev.quintero.nexus.core.design.NightBgSurface
import dev.quintero.nexus.core.design.NightBorderSubtle
import dev.quintero.nexus.core.design.NightCardElevated
import dev.quintero.nexus.core.design.NightCardSurface
import dev.quintero.nexus.core.design.NocturnalAmber
import dev.quintero.nexus.core.design.PriorityMediumColor
import dev.quintero.nexus.core.design.SyncSuccessGreen
import dev.quintero.nexus.core.design.TextMoonlight
import dev.quintero.nexus.core.design.TextMutedSlate
import dev.quintero.nexus.core.design.TextStarlight
import dev.quintero.nexus.core.design.WorkoutBlueSoft
import dev.quintero.nexus.core.design.WorkoutTealSoft
import dev.quintero.nexus.core.util.BackupShareHelper
import dev.quintero.nexus.core.util.DateTimeHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutModuleScreen(
    viewModel: WorkoutViewModel,
    onBackToHub: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Keep today's date fresh when returning to module
    LaunchedEffect(Unit) {
        viewModel.refreshTodayDateIfNeeded()
    }

    // Native Android WakeLock via Window flag when "Pantalla" toggle is active
    DisposableEffect(state.wakeLockEnabled) {
        val activity = context.findActivity()
        if (state.wakeLockEnabled) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler {
        if (state.activeSubTab != WorkoutSubTab.TRAIN) {
            viewModel.selectSubTab(WorkoutSubTab.TRAIN)
        } else {
            onBackToHub()
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var deleteSessionIdPending by remember { mutableStateOf<String?>(null) }
    var showClearAllStep1 by remember { mutableStateOf(false) }
    var showClearAllStep2 by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = viewModel.buildExportJson()
            BackupShareHelper.writeJsonToUri(context, uri, json)
                .onSuccess { viewModel.showToast("Backup descargado") }
                .onFailure { viewModel.showToast("No se pudo guardar el backup", positive = false) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            BackupShareHelper.readTextFromUri(context, uri)
                .onSuccess { viewModel.importBackupJson(it) }
                .onFailure { viewModel.showToast("Error al leer el archivo", positive = false) }
        }
    }

    Scaffold(
        containerColor = NightBgDeep,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            WorkoutTopHeader(
                state = state,
                onBackToHub = onBackToHub,
                onToggleRestTimer = viewModel::toggleRestTimerExpanded,
                onToggleWakeLock = viewModel::toggleWakeLock,
                onStartRestSeconds = viewModel::startRestTimer,
                onStopRestTimer = viewModel::stopRestTimer
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NightBgSurface,
                tonalElevation = 6.dp
            ) {
                WorkoutSubTab.entries.forEach { tab ->
                    val selected = state.activeSubTab == tab
                    val icon = when (tab) {
                        WorkoutSubTab.TRAIN -> if (selected) Icons.Filled.FitnessCenter else Icons.Outlined.FitnessCenter
                        WorkoutSubTab.HISTORY -> if (selected) Icons.Filled.History else Icons.Outlined.History
                        WorkoutSubTab.BACKUP -> if (selected) Icons.Filled.Save else Icons.Outlined.Save
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectSubTab(tab) },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WorkoutTealSoft,
                            selectedTextColor = TextMoonlight,
                            indicatorColor = WorkoutTealSoft.copy(alpha = 0.16f),
                            unselectedIconColor = TextMutedSlate,
                            unselectedTextColor = TextMutedSlate
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.activeSubTab) {
                WorkoutSubTab.TRAIN -> WorkoutTrainTab(
                    state = state,
                    onSelectDay = viewModel::selectDay,
                    onPickDate = {
                        showAndroidDatePicker(context, state.sessionDateIso) { picked ->
                            viewModel.onDatePicked(picked)
                        }
                    },
                    onResetToToday = viewModel::resetToToday,
                    onOpenTechnique = viewModel::openTechniqueSheet,
                    onWeightChange = viewModel::updateSetWeight,
                    onRepsChange = viewModel::updateSetReps,
                    onAddSet = viewModel::addSet,
                    onRemoveSet = viewModel::removeSet,
                    onNotesChange = viewModel::updateNotes,
                    onSaveSession = viewModel::saveSession,
                    onRequestDiscard = { showDiscardDialog = true }
                )

                WorkoutSubTab.HISTORY -> WorkoutHistoryTab(
                    state = state,
                    onRequestDeleteSession = { deleteSessionIdPending = it }
                )

                WorkoutSubTab.BACKUP -> WorkoutBackupTab(
                    sessionsCount = state.totalSessionsCount,
                    onExportClick = {
                        if (state.totalSessionsCount == 0) {
                            viewModel.showToast("No hay datos para exportar", positive = false)
                        } else {
                            exportLauncher.launch("entrenamiento_${DateTimeHelper.todayIso()}.json")
                        }
                    },
                    onShareClick = {
                        if (state.totalSessionsCount == 0) {
                            viewModel.showToast("No hay datos para compartir", positive = false)
                        } else {
                            val json = viewModel.buildExportJson()
                            BackupShareHelper.shareJsonFile(
                                context = context,
                                fileName = "entrenamiento_${DateTimeHelper.todayIso()}.json",
                                jsonContent = json,
                                chooserTitle = "Compartir backup de entrenamiento",
                                subject = "Backup de mi diario de entrenamiento"
                            ).onFailure {
                                viewModel.showToast("No se pudo abrir el selector para compartir", positive = false)
                            }
                        }
                    },
                    onImportClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    onRequestClearAll = { showClearAllStep1 = true }
                )
            }

            // Transient Minimal Toast Pill
            AnimatedVisibility(
                visible = state.toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                val bg = if (state.isToastPositive) WorkoutTealSoft else NocturnalAmber
                Surface(
                    shape = RoundedCornerShape(50),
                    color = bg,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = state.toastMessage.orEmpty(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF061510),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // Technique Modal Bottom Sheet
    state.techniqueSheetExercise?.let { ex ->
        ModalBottomSheet(
            onDismissRequest = viewModel::closeTechniqueSheet,
            containerColor = NightBgSurface,
            contentColor = TextMoonlight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = ex.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextMoonlight
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutTargetChip(ex.target)
                    WorkoutRirChip(ex.rir)
                }
                Text(
                    text = ex.tag,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, NocturnalAmber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = ex.cue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextStarlight,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = NightCardSurface,
            title = { Text("Reiniciar sesión", color = TextMoonlight) },
            text = { Text("¿Limpiar los campos escritos hoy y reiniciar esta sesión?", color = TextStarlight) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.discardCurrentDraft()
                }) { Text("Reiniciar", color = ConflictWarningRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancelar", color = TextMutedSlate) }
            }
        )
    }

    deleteSessionIdPending?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteSessionIdPending = null },
            containerColor = NightCardSurface,
            title = { Text("Eliminar sesión", color = TextMoonlight) },
            text = { Text("¿Eliminar esta sesión del historial?", color = TextStarlight) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(id)
                    deleteSessionIdPending = null
                }) { Text("Eliminar", color = ConflictWarningRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteSessionIdPending = null }) { Text("Cancelar", color = TextMutedSlate) }
            }
        )
    }

    if (showClearAllStep1) {
        AlertDialog(
            onDismissRequest = { showClearAllStep1 = false },
            containerColor = NightCardSurface,
            title = { Text("Borrar todos los datos", color = ConflictWarningRed) },
            text = { Text("Se eliminarán todas las sesiones guardadas en este dispositivo. Esta acción no se puede deshacer.", color = TextStarlight) },
            confirmButton = {
                TextButton(onClick = {
                    showClearAllStep1 = false
                    showClearAllStep2 = true
                }) { Text("Continuar", color = ConflictWarningRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllStep1 = false }) { Text("Cancelar", color = TextMutedSlate) }
            }
        )
    }

    if (showClearAllStep2) {
        AlertDialog(
            onDismissRequest = { showClearAllStep2 = false },
            containerColor = NightCardSurface,
            title = { Text("Confirmación final", color = ConflictWarningRed) },
            text = { Text("¿Confirmas borrar definitivamente todo tu historial de entrenamiento?", color = TextStarlight) },
            confirmButton = {
                TextButton(onClick = {
                    showClearAllStep2 = false
                    viewModel.clearAllData()
                }) { Text("Sí, borrar todo", color = ConflictWarningRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllStep2 = false }) { Text("Cancelar", color = TextMutedSlate) }
            }
        )
    }
}

@Composable
private fun WorkoutTopHeader(
    state: WorkoutUiState,
    onBackToHub: () -> Unit,
    onToggleRestTimer: () -> Unit,
    onToggleWakeLock: () -> Unit,
    onStartRestSeconds: (Int) -> Unit,
    onStopRestTimer: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.restTimerPulse) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        color = NightBgSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        onClick = onBackToHub,
                        shape = CircleShape,
                        color = NightCardSurface,
                        border = BorderStroke(1.dp, MinimalHairline)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a Mis cosas",
                            tint = TextStarlight,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Entreno",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight,
                            maxLines = 1
                        )
                        Text(
                            text = DateTimeHelper.formatHumanDateEs(DateTimeHelper.todayIso()),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val timerActiveOrOpen = state.restTimerExpanded || state.restTimerActive
                    Surface(
                        onClick = onToggleRestTimer,
                        shape = RoundedCornerShape(50),
                        color = if (timerActiveOrOpen) WorkoutBlueSoft.copy(alpha = 0.16f) else NightCardSurface,
                        border = BorderStroke(
                            1.dp,
                            if (state.restTimerPulse || state.restTimerActive) WorkoutBlueSoft else MinimalHairline
                        ),
                        modifier = Modifier.scale(pulseScale)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Descanso",
                                tint = if (timerActiveOrOpen) WorkoutBlueSoft else TextMutedSlate,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.restTimerActive) formatSecondsMmSs(state.restTimerRemainingSec) else "Descanso",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (timerActiveOrOpen) WorkoutBlueSoft else TextStarlight
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleWakeLock,
                        shape = RoundedCornerShape(50),
                        color = if (state.wakeLockEnabled) NocturnalAmber.copy(alpha = 0.16f) else NightCardSurface,
                        border = BorderStroke(
                            1.dp,
                            if (state.wakeLockEnabled) NocturnalAmber.copy(alpha = 0.6f) else MinimalHairline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (state.wakeLockEnabled) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                                contentDescription = "Mantener pantalla",
                                tint = if (state.wakeLockEnabled) NocturnalAmber else TextMutedSlate,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.wakeLockEnabled) "Activa" else "Pantalla",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (state.wakeLockEnabled) NocturnalAmber else TextStarlight
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.restTimerExpanded) {
                Surface(
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = formatSecondsMmSs(state.restTimerRemainingSec),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = WorkoutBlueSoft
                            )
                            Text(
                                text = if (state.restTimerActive) "Descansando" else "En reposo",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedSlate
                            )
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(60 to "60s", 90 to "90s", 120 to "2m", 150 to "2.5m", 180 to "3m").forEach { (sec, label) ->
                                Surface(
                                    onClick = { onStartRestSeconds(sec) },
                                    shape = RoundedCornerShape(50),
                                    color = NightBgSurface,
                                    border = BorderStroke(1.dp, NightBorderSubtle)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextMoonlight,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = onStopRestTimer,
                                shape = CircleShape,
                                color = ConflictWarningRed.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Detener temporizador",
                                    tint = ConflictWarningRed,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTrainTab(
    state: WorkoutUiState,
    onSelectDay: (String) -> Unit,
    onPickDate: () -> Unit,
    onResetToToday: () -> Unit,
    onOpenTechnique: (WorkoutExerciseTemplate) -> Unit,
    onWeightChange: (Int, Int, String) -> Unit,
    onRepsChange: (Int, Int, String) -> Unit,
    onAddSet: (Int) -> Unit,
    onRemoveSet: (Int, Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onSaveSession: () -> Unit,
    onRequestDiscard: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Day Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WorkoutRoutineCatalog.DAY_KEYS.forEach { dayKey ->
                    val isSelected = state.selectedDayKey == dayKey
                    val isScheduledToday = state.scheduledTodayDayKey == dayKey
                    val isDoneToday = dayKey in state.todayCompletedDays

                    Surface(
                        onClick = { onSelectDay(dayKey) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) WorkoutBlueSoft else NightCardSurface,
                        border = BorderStroke(1.dp, if (isSelected) WorkoutBlueSoft else MinimalHairline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
                        ) {
                            if (isScheduledToday) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NightBgDeep else NocturnalAmber)
                                )
                            }
                            Text(
                                text = dayKey,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) Color(0xFF07131D) else TextStarlight,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (isDoneToday) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color(0xFF07131D) else WorkoutTealSoft,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Session Info Bar
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.currentPlan.label,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                        Text(
                            text = state.currentPlan.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (state.isDraftRecovered) WorkoutBlueSoft else WorkoutTealSoft).copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = state.draftStatusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (state.isDraftRecovered) WorkoutBlueSoft else WorkoutTealSoft,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        if (!state.isSelectedDateToday) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Guardando en fecha: ${DateTimeHelper.formatShortDayMonthEs(state.sessionDateIso)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WorkoutBlueSoft
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = onPickDate,
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.isSelectedDateToday) NightBgSurface else WorkoutBlueSoft.copy(alpha = 0.14f),
                            border = BorderStroke(
                                1.dp,
                                if (state.isSelectedDateToday) WorkoutBlueSoft.copy(alpha = 0.4f) else WorkoutBlueSoft
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "Cambiar fecha en calendario",
                                    tint = WorkoutBlueSoft,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = DateTimeHelper.formatShortDayMonthEs(state.sessionDateIso),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (state.isSelectedDateToday) TextMoonlight else WorkoutBlueSoft
                                )
                            }
                        }
                        Surface(
                            onClick = onResetToToday,
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.isSelectedDateToday) WorkoutTealSoft.copy(alpha = 0.15f) else NightBgSurface,
                            border = BorderStroke(
                                1.dp,
                                if (state.isSelectedDateToday) WorkoutTealSoft.copy(alpha = 0.5f) else MinimalHairline
                            )
                        ) {
                            Text(
                                text = if (state.isSelectedDateToday) "✓ Hoy" else "Ir a Hoy",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (state.isSelectedDateToday) WorkoutTealSoft else TextMutedSlate,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Exercise Cards
        itemsIndexed(
            items = state.exercises,
            key = { _, ex -> "${state.selectedDayKey}_${ex.template.name}" }
        ) { exIdx, exercise ->
            WorkoutExerciseCard(
                exercise = exercise,
                onOpenTechnique = { onOpenTechnique(exercise.template) },
                onWeightChange = { setIdx, v -> onWeightChange(exIdx, setIdx, v) },
                onRepsChange = { setIdx, v -> onRepsChange(exIdx, setIdx, v) },
                onAddSet = { onAddSet(exIdx) },
                onRemoveSet = { setIdx -> onRemoveSet(exIdx, setIdx) }
            )
        }

        // Notes Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Notas de la sesión",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMutedSlate
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.sessionNotes,
                        onValueChange = onNotesChange,
                        placeholder = {
                            Text(
                                "Energía, sensaciones, fatiga, ajustes...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedSlate
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WorkoutBlueSoft,
                            unfocusedBorderColor = MinimalHairline,
                            focusedContainerColor = NightBgSurface,
                            unfocusedContainerColor = NightBgSurface,
                            focusedTextColor = TextMoonlight,
                            unfocusedTextColor = TextMoonlight
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Save + Reset Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveSession,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkoutTealSoft,
                        contentColor = Color(0xFF04150F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar Sesión",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onRequestDiscard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Limpiar / Empezar de nuevo",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMutedSlate
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutExerciseCard(
    exercise: WorkoutExerciseInputUi,
    onOpenTechnique: () -> Unit,
    onWeightChange: (Int, String) -> Unit,
    onRepsChange: (Int, String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit
) {
    val tpl = exercise.template
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NightCardSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tpl.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Text(
                        text = tpl.tag,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = onOpenTechnique,
                            shape = RoundedCornerShape(6.dp),
                            color = WorkoutBlueSoft.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, WorkoutBlueSoft.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Técnica",
                                    tint = WorkoutBlueSoft,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Técnica",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = WorkoutBlueSoft
                                )
                            }
                        }

                        if (!tpl.superset.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFA78BFA).copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = tpl.superset.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFA78BFA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (!exercise.lastDateIso.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NightBgSurface,
                                border = BorderStroke(1.dp, MinimalHairline)
                            ) {
                                Text(
                                    text = DateTimeHelper.formatShortDayMonthEs(exercise.lastDateIso),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WorkoutTargetChip(tpl.target)
                    WorkoutRirChip(tpl.rir)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sets table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "S",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(30.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KG",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(30.dp))
            }

            HorizontalDivider(color = MinimalHairline, modifier = Modifier.padding(bottom = 8.dp))

            exercise.sets.forEachIndexed { setIdx, setUi ->
                WorkoutSetRow(
                    index = setIdx,
                    setUi = setUi,
                    onWeightChange = { onWeightChange(setIdx, it) },
                    onRepsChange = { onRepsChange(setIdx, it) },
                    onRemove = { onRemoveSet(setIdx) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                onClick = onAddSet,
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = TextMutedSlate,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Añadir serie",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedSlate
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetRow(
    index: Int,
    setUi: WorkoutSetInputUi,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (setUi.isCompleted) WorkoutTealSoft.copy(alpha = 0.05f) else Color.Transparent)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (setUi.isCompleted) WorkoutTealSoft.copy(alpha = 0.18f) else NightBgSurface)
                    .border(
                        width = 1.dp,
                        color = if (setUi.isCompleted) WorkoutTealSoft else MinimalHairline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (setUi.isCompleted) "✓" else "${index + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (setUi.isCompleted) WorkoutTealSoft else TextMutedSlate
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            CompactNumericBox(
                value = setUi.weightText,
                placeholder = WorkoutRoutineCatalog.formatWeightCompact(setUi.prevWeight).ifBlank { "—" },
                textColor = WorkoutBlueSoft,
                keyboardType = KeyboardType.Decimal,
                onValueChange = onWeightChange,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            CompactNumericBox(
                value = setUi.repsText,
                placeholder = setUi.prevReps?.toString() ?: "reps",
                textColor = if (setUi.repsText.isNotBlank()) WorkoutTealSoft else TextMoonlight,
                keyboardType = KeyboardType.Number,
                onValueChange = onRepsChange,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitar serie",
                    tint = TextMutedSlate,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (setUi.prevWeight != null || setUi.prevReps != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, top = 3.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val wLabel = setUi.prevWeight?.let { "${WorkoutRoutineCatalog.formatWeightCompact(it)} kg" } ?: "—"
                val rLabel = setUi.prevReps?.let { "$it reps" } ?: "—"
                val dLabel = setUi.prevDateIso?.let { " · ${DateTimeHelper.formatShortDayMonthEs(it)}" } ?: ""
                Text(
                    text = "Ant: $wLabel × $rLabel$dLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedSlate
                )
                setUi.prBadge?.let { pr ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = WorkoutTealSoft.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, WorkoutTealSoft.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "🔥 $pr",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = WorkoutTealSoft,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactNumericBox(
    value: String,
    placeholder: String,
    textColor: Color,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NightBgSurface)
            .border(1.dp, MinimalHairline, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedSlate.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(WorkoutBlueSoft),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.titleSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WorkoutHistoryTab(
    state: WorkoutUiState,
    onRequestDeleteSession: (String) -> Unit
) {
    var expandedSessionId by remember { mutableStateOf<String?>(null) }

    val groupedByMonth = remember(state.allSessions) {
        state.allSessions.groupBy { it.dateIso.take(7) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutStatCard(
                    value = state.totalSessionsCount.toString(),
                    label = "Total sesiones",
                    modifier = Modifier.weight(1f)
                )
                WorkoutStatCard(
                    value = state.monthSessionsCount.toString(),
                    label = "Este mes",
                    modifier = Modifier.weight(1f)
                )
                WorkoutStatCard(
                    value = state.yearSessionsCount.toString(),
                    label = "Este año",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.allSessions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = TextMutedSlate,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Sin sesiones aún",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextMoonlight
                        )
                        Text(
                            text = "Registra tu primera sesión en la pestaña Entrenar o importa un backup JSON.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            groupedByMonth.forEach { (ym, sessionsInMonth) ->
                item(key = "hdr_$ym") {
                    Text(
                        text = DateTimeHelper.formatMonthYearEs(ym).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextMutedSlate,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(sessionsInMonth, key = { it.id }) { session ->
                    val isOpen = expandedSessionId == session.id
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NightCardSurface,
                        border = BorderStroke(1.dp, MinimalHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSessionId = if (isOpen) null else session.id
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = DateTimeHelper.formatFullDayShortEs(session.dateIso),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextMoonlight
                                    )
                                    Text(
                                        text = "${session.dayLabel} · ${session.totalSets} series",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedSlate
                                    )
                                }
                                Icon(
                                    imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextMutedSlate
                                )
                            }

                            AnimatedVisibility(visible = isOpen) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(color = MinimalHairline)
                                    session.exercises.forEach { ex ->
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = ex.name,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = WorkoutBlueSoft
                                            )
                                            ex.sets.forEachIndexed { idx, s ->
                                                val wTxt = s.weight?.let { "${WorkoutRoutineCatalog.formatWeightCompact(it)} kg" } ?: "—"
                                                val rTxt = s.reps?.let { "$it reps" } ?: "—"
                                                Text(
                                                    text = "S${idx + 1} · $wTxt × $rTxt",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextStarlight
                                                )
                                            }
                                        }
                                    }
                                    if (session.notes.isNotBlank()) {
                                        Text(
                                            text = "“${session.notes}”",
                                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                            color = TextMutedSlate
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onRequestDeleteSession(session.id) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = ConflictWarningRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Eliminar sesión", color = ConflictWarningRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NightCardSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = WorkoutBlueSoft
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedSlate
            )
        }
    }
}

@Composable
private fun WorkoutBackupTab(
    sessionsCount: Int,
    onExportClick: () -> Unit,
    onShareClick: () -> Unit,
    onImportClick: () -> Unit,
    onRequestClearAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Exportar y Guardar Datos ($sessionsCount sesiones)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Text(
                        text = "Guarda o envía una copia JSON portátil de todas tus sesiones registradas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                    Button(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorkoutBlueSoft,
                            contentColor = Color(0xFF06131E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Descargar backup JSON", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onShareClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MinimalHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = TextMoonlight, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir por Telegram / WhatsApp", color = TextMoonlight)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Importar Datos",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Text(
                        text = "Restaura o fusiona sesiones desde un archivo de backup JSON sin perder tu historial actual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                    OutlinedButton(
                        onClick = onImportClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MinimalHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = WorkoutTealSoft, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Seleccionar archivo backup (.json)", color = WorkoutTealSoft)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Zona de peligro",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ConflictWarningRed
                    )
                    Text(
                        text = "Elimina permanentemente todos los entrenamientos y borradores guardados en este dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate
                    )
                    OutlinedButton(
                        onClick = onRequestClearAll,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Borrar todos los datos", color = ConflictWarningRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTargetChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = WorkoutBlueSoft.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = WorkoutBlueSoft,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun WorkoutRirChip(rir: String) {
    val color = when {
        rir.contains("0-1") || rir.contains("1-2") -> PriorityMediumColor
        rir.contains("0") -> ConflictWarningRed
        else -> NocturnalAmber
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = "🎯 $rir",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

private fun formatSecondsMmSs(totalSeconds: Int): String {
    val m = (totalSeconds / 60).coerceAtLeast(0)
    val s = (totalSeconds % 60).coerceAtLeast(0)
    return "%02d:%02d".format(m, s)
}

internal fun showAndroidDatePicker(
    context: Context,
    currentIso: String,
    onPicked: (String) -> Unit
) {
    val parts = currentIso.split("-")
    val cal = Calendar.getInstance()
    val initY = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
    val initM = (parts.getOrNull(1)?.toIntOrNull()?.minus(1)) ?: cal.get(Calendar.MONTH)
    val initD = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
    DatePickerDialog(context, { _, y, m, d ->
        onPicked("%04d-%02d-%02d".format(y, m + 1, d))
    }, initY, initM, initD).show()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
