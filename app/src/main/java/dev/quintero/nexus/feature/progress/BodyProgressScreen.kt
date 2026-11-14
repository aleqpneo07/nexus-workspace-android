package dev.quintero.nexus.feature.progress

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.quintero.nexus.core.design.ConflictWarningRed
import dev.quintero.nexus.core.design.EnfoqueCoralSoft
import dev.quintero.nexus.core.design.JetBrainsMonoFontFamily
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
import java.util.Locale

private data class ProgressColumnDef(
    val id: String,
    val header: String,
    val widthDp: Dp,
    val showInCompact: Boolean = false
)

private val OfficialTableColumns = listOf(
    ProgressColumnDef("weight", "PESO (KG)", 84.dp, showInCompact = true),
    ProgressColumnDef("deltaWeight", "Δ PESO", 76.dp, showInCompact = true),
    ProgressColumnDef("bmi", "IMC", 68.dp, showInCompact = false),
    ProgressColumnDef("fatPct", "% GRASA", 78.dp, showInCompact = true),
    ProgressColumnDef("deltaFat", "Δ GRASA KG", 88.dp, showInCompact = true),
    ProgressColumnDef("muscle", "MÚSCULO (KG)", 96.dp, showInCompact = true),
    ProgressColumnDef("deltaMuscle", "Δ MÚSCULO", 88.dp, showInCompact = true),
    ProgressColumnDef("muscleQuality", "CALIDAD MUSC.", 96.dp, showInCompact = false),
    ProgressColumnDef("bodyType", "TIPO CUERPO", 88.dp, showInCompact = false),
    ProgressColumnDef("boneMass", "M. ÓSEA (KG)", 88.dp, showInCompact = false),
    ProgressColumnDef("visceralFat", "GRASA VISC.", 86.dp, showInCompact = false),
    ProgressColumnDef("bmr", "TMB (KCAL)", 114.dp, showInCompact = true),
    ProgressColumnDef("metabolicAge", "EDAD MET.", 76.dp, showInCompact = false),
    ProgressColumnDef("waterPct", "AGUA (%)", 76.dp, showInCompact = false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyProgressModuleScreen(
    viewModel: BodyProgressViewModel,
    onBackToHub: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshTodayDateIfNeeded()
    }

    var deletePendingId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        when {
            deletePendingId != null -> deletePendingId = null
            state.editor.isOpen -> viewModel.dismissEditor()
            state.selectedEntryForDetail != null -> viewModel.dismissEntryDetail()
            state.isProfileDialogOpen -> viewModel.dismissProfileDialog()
            state.isBackupDialogOpen -> viewModel.dismissBackupDialog()
            else -> onBackToHub()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = viewModel.buildExportJson()
            BackupShareHelper.writeJsonToUri(context, uri, json)
                .onSuccess {
                    viewModel.dismissBackupDialog()
                    viewModel.showToast("Backup descargado")
                }
                .onFailure {
                    viewModel.showToast("No se pudo guardar el backup", positive = false)
                }
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
            BodyProgressTopHeader(
                profile = state.profile,
                onBackToHub = onBackToHub,
                onOpenProfile = viewModel::openProfileDialog,
                onOpenBackup = viewModel::openBackupDialog,
                onAddWeighIn = viewModel::openNewWeighInEditor
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "kpi_strip") {
                    BodyProgressKpiRow(state = state)
                }

                item(key = "next_weigh_in_bar") {
                    NextWeighInScheduleCard(
                        profile = state.profile,
                        nextInfo = state.nextWeighIn,
                        showChart = state.showChart,
                        onToggleChart = viewModel::toggleChartVisible,
                        onAddNow = viewModel::openNewWeighInEditor
                    )
                }

                if (state.showChart) {
                    val validReadings = state.entriesChronological.filter { !it.isPostponed && it.weightKg != null }
                    if (validReadings.size >= 2) {
                        item(key = "trend_chart") {
                            BodyProgressTrendChartCard(
                                entries = validReadings,
                                selectedMetric = state.selectedChartMetric,
                                onSelectMetric = viewModel::selectChartMetric
                            )
                        }
                    }
                }

                item(key = "official_table") {
                    BodyProgressOfficialTableCard(
                        entries = state.displayedEntries,
                        showAllColumns = state.showAllColumns,
                        newestFirst = state.newestFirst,
                        onToggleColumns = viewModel::toggleAllColumns,
                        onToggleOrder = viewModel::toggleOrder,
                        onRowClick = viewModel::openEntryDetail
                    )
                }

                if (state.profile.summerRefSummary.isNotBlank() || state.profile.targetDescription.isNotBlank()) {
                    item(key = "context_footer") {
                        BodyProgressContextCard(profile = state.profile)
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            AnimatedVisibility(
                visible = state.toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (state.toastPositive) SyncSuccessGreen else ConflictWarningRed,
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

    // Detail sheet on row tap
    state.selectedEntryForDetail?.let { entry ->
        BodyProgressEntryDetailSheet(
            entry = entry,
            onDismiss = viewModel::dismissEntryDetail,
            onEdit = { viewModel.openEditEntryEditor(entry) },
            onDelete = { deletePendingId = entry.id }
        )
    }

    // Add / Edit BottomSheet
    if (state.editor.isOpen) {
        BodyProgressEditorSheet(
            state = state,
            prevValidEntry = viewModel.findPreviousValidEntry(
                targetDateIso = state.editor.dateIso,
                excludingEntryId = state.editor.editingEntryId
            ),
            onUpdateEditor = viewModel::updateEditor,
            onPickDate = {
                showAndroidDatePicker(context, state.editor.dateIso) { picked ->
                    viewModel.updateEditor { it.copy(dateIso = picked) }
                }
            },
            onSave = viewModel::saveEditorEntry,
            onRequestDelete = {
                state.editor.editingEntryId?.let { id -> deletePendingId = id }
            },
            onDismiss = viewModel::dismissEditor
        )
    }

    // Athlete profile edit dialog
    if (state.isProfileDialogOpen) {
        BodyProgressProfileDialog(
            profile = state.profile,
            onDismiss = viewModel::dismissProfileDialog,
            onSave = viewModel::saveProfile
        )
    }

    // JSON Backup dialog
    if (state.isBackupDialogOpen) {
        BodyProgressBackupDialog(
            entryCount = state.entriesChronological.size,
            onDismiss = viewModel::dismissBackupDialog,
            onDownloadJson = {
                exportLauncher.launch("progreso-fisico-${DateTimeHelper.todayIso()}.json")
            },
            onShareJson = {
                val json = viewModel.buildExportJson()
                BackupShareHelper.shareJsonFile(
                    context = context,
                    fileName = "progreso-fisico-${DateTimeHelper.todayIso()}.json",
                    jsonContent = json,
                    chooserTitle = "Compartir Progreso físico (JSON)",
                    subject = "Backup Progreso físico · ${state.profile.athleteName}"
                ).onSuccess {
                    viewModel.dismissBackupDialog()
                }.onFailure {
                    viewModel.showToast("No se pudo compartir el archivo", positive = false)
                }
            },
            onImportJson = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }
        )
    }

    // Delete confirmation
    deletePendingId?.let { pendingId ->
        AlertDialog(
            onDismissRequest = { deletePendingId = null },
            containerColor = NightCardSurface,
            title = { Text("Eliminar registro", color = TextMoonlight) },
            text = {
                Text(
                    "¿Eliminar este pesaje de forma permanente? El cambio también se sincronizará entre móvil y tablet.",
                    color = TextStarlight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletePendingId = null
                        viewModel.deleteEntry(pendingId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConflictWarningRed)
                ) {
                    Text("Eliminar", color = TextMoonlight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletePendingId = null }) {
                    Text("Cancelar", color = TextMutedSlate)
                }
            }
        )
    }
}

@Composable
private fun BodyProgressTopHeader(
    profile: BodyProgressProfileEntity,
    onBackToHub: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBackup: () -> Unit,
    onAddWeighIn: () -> Unit
) {
    val heightStr = String.format(Locale.US, "%.2f m", profile.heightCm / 100.0).replace('.', ',')
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
                            text = "Progreso físico",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight,
                            maxLines = 1
                        )
                        Text(
                            text = "${profile.athleteName} · $heightStr · ${profile.scaleModel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenProfile,
                        shape = CircleShape,
                        color = NightCardSurface,
                        border = BorderStroke(1.dp, MinimalHairline)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = "Perfil y referencias",
                            tint = TextStarlight,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(16.dp)
                        )
                    }

                    Surface(
                        onClick = onOpenBackup,
                        shape = CircleShape,
                        color = NightCardSurface,
                        border = BorderStroke(1.dp, MinimalHairline)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = "Backup JSON",
                            tint = TextStarlight,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(16.dp)
                        )
                    }

                    Surface(
                        onClick = onAddWeighIn,
                        shape = RoundedCornerShape(50),
                        color = SyncSuccessGreen,
                        modifier = Modifier.testTag("progress_add_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color(0xFF061510),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Pesaje",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF061510)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyProgressKpiRow(state: BodyProgressUiState) {
    val latest = state.latestValidEntry
    val baseLabel = state.baselineEntry?.displayDate ?: state.profile.baselineDateLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiBentoCard(
            label = "PESO ACTUAL",
            value = latest?.weightKg?.let { "${BodyProgressDomainHelper.formatEs2(it)} kg" } ?: "—",
            deltaText = state.totalDeltaWeightVsBase?.let { "${BodyProgressDomainHelper.formatSigned2(it)} kg vs $baseLabel" }
                ?: "Base: $baseLabel",
            accentColor = MoonlightCyan,
            modifier = Modifier.weight(1f)
        )

        KpiBentoCard(
            label = "MÚSCULO",
            value = latest?.muscleKg?.let { "${BodyProgressDomainHelper.formatEs1(it)} kg" } ?: "—",
            deltaText = state.totalDeltaMuscleVsBase?.let { "${BodyProgressDomainHelper.formatSigned1(it)} kg vs $baseLabel" }
                ?: (latest?.muscleQuality?.let { "Cal. muscular: $it" } ?: "—"),
            accentColor = SyncSuccessGreen,
            modifier = Modifier.weight(1f)
        )

        KpiBentoCard(
            label = "% GRASA",
            value = latest?.bodyFatPct?.let { BodyProgressDomainHelper.formatPct1(it) } ?: "—",
            deltaText = buildString {
                if (latest?.muscleQuality != null) append("Cal. ${latest.muscleQuality}")
                if (latest?.bodyType != null) {
                    if (isNotEmpty()) append(" · ")
                    append("Tipo ${latest.bodyType}")
                }
                if (isEmpty()) {
                    append(state.totalDeltaFatPctVsBase?.let { BodyProgressDomainHelper.formatSigned1(it) + "%" } ?: "—")
                }
            },
            accentColor = NocturnalAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiBentoCard(
    label: String,
    value: String,
    deltaText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NightCardSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextMutedSlate,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = TextMoonlight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = deltaText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NextWeighInScheduleCard(
    profile: BodyProgressProfileEntity,
    nextInfo: NextWeighInScheduleInfo?,
    showChart: Boolean,
    onToggleChart: () -> Unit,
    onAddNow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NightCardSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAddNow)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SyncSuccessGreen.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = SyncSuccessGreen,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (nextInfo != null) {
                            "Próximo pesaje: ${nextInfo.nextDisplayDate} (cada ${profile.cycleDays}d)"
                        } else {
                            "Cadencia configurada: cada ${profile.cycleDays} días"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextMoonlight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val countdownLabel = when {
                        nextInfo == null -> "Pulsa + Pesaje para registrar"
                        nextInfo.daysRemaining == 0 -> "Hoy toca control en báscula"
                        nextInfo.daysRemaining == 1 -> "Mañana toca pesaje"
                        nextInfo.daysRemaining > 1 -> "Faltan ${nextInfo.daysRemaining} días"
                        else -> "Pendiente desde hace ${-nextInfo.daysRemaining} día(s)"
                    }
                    val countdownColor = when {
                        nextInfo == null -> TextMutedSlate
                        nextInfo.daysRemaining == 0 -> SyncSuccessGreen
                        nextInfo.daysRemaining < 0 -> EnfoqueCoralSoft
                        nextInfo.daysRemaining <= 2 -> NocturnalAmber
                        else -> TextMutedSlate
                    }
                    Text(
                        text = countdownLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = countdownColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = onToggleChart,
                shape = RoundedCornerShape(50),
                color = if (showChart) MoonlightCyan.copy(alpha = 0.14f) else NightCardElevated,
                border = BorderStroke(
                    1.dp,
                    if (showChart) MoonlightCyan.copy(alpha = 0.45f) else MinimalHairline
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShowChart,
                        contentDescription = "Mostrar u ocultar gráfica",
                        tint = if (showChart) MoonlightCyan else TextMutedSlate,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Gráfica",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (showChart) MoonlightCyan else TextMutedSlate
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyProgressTrendChartCard(
    entries: List<BodyProgressEntryEntity>,
    selectedMetric: BodyChartMetric,
    onSelectMetric: (BodyChartMetric) -> Unit
) {
    val points = remember(entries, selectedMetric) {
        entries.mapNotNull { entry ->
            val value = when (selectedMetric) {
                BodyChartMetric.WEIGHT -> entry.weightKg
                BodyChartMetric.MUSCLE -> entry.muscleKg
                BodyChartMetric.BODY_FAT -> entry.bodyFatPct
            }
            if (value != null) entry.displayDate to value else null
        }
    }

    val accent = when (selectedMetric) {
        BodyChartMetric.WEIGHT -> MoonlightCyan
        BodyChartMetric.MUSCLE -> SyncSuccessGreen
        BodyChartMetric.BODY_FAT -> NocturnalAmber
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Evolución · ${selectedMetric.label}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextMoonlight
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BodyChartMetric.entries.forEach { metric ->
                        val active = metric == selectedMetric
                        Surface(
                            onClick = { onSelectMetric(metric) },
                            shape = RoundedCornerShape(50),
                            color = if (active) accent.copy(alpha = 0.16f) else NightCardElevated,
                            border = BorderStroke(
                                1.dp,
                                if (active) accent.copy(alpha = 0.5f) else MinimalHairline
                            )
                        ) {
                            Text(
                                text = metric.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.5.sp
                                ),
                                color = if (active) accent else TextMutedSlate,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (points.size >= 2) {
                val minVal = points.minOf { it.second }
                val maxVal = points.maxOf { it.second }
                val latestVal = points.last().second
                val firstVal = points.first().second
                val span = (maxVal - minVal).takeIf { it > 0.001 } ?: 1.0
                val padMin = minVal - span * 0.22
                val padMax = maxVal + span * 0.22

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NightBgSurface)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                        listOf(0.2f, 0.5f, 0.8f).forEach { ratio ->
                            drawLine(
                                color = MinimalHairline,
                                start = Offset(0f, h * ratio),
                                end = Offset(w, h * ratio),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dash
                            )
                        }

                        val coords = points.mapIndexed { idx, pair ->
                            val x = if (points.size == 1) w / 2f else (idx.toFloat() / (points.size - 1)) * w
                            val norm = ((pair.second - padMin) / (padMax - padMin)).toFloat().coerceIn(0f, 1f)
                            val y = h - norm * h
                            Offset(x, y)
                        }

                        val linePath = Path().apply {
                            coords.forEachIndexed { i, pt ->
                                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }

                        val areaPath = Path().apply {
                            addPath(linePath)
                            lineTo(coords.last().x, h)
                            lineTo(coords.first().x, h)
                            close()
                        }

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(accent.copy(alpha = 0.26f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = accent,
                            style = Stroke(
                                width = 2.4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        coords.forEachIndexed { idx, pt ->
                            val isLast = idx == coords.lastIndex
                            drawCircle(
                                color = accent.copy(alpha = if (isLast) 0.28f else 0.16f),
                                radius = if (isLast) 7.dp.toPx() else 5.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = NightBgSurface,
                                radius = 3.2.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = accent,
                                radius = 2.2.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fmt = { v: Double ->
                        if (selectedMetric == BodyChartMetric.WEIGHT) {
                            "${BodyProgressDomainHelper.formatEs2(v)} ${selectedMetric.unit}"
                        } else {
                            "${BodyProgressDomainHelper.formatEs1(v)} ${selectedMetric.unit}"
                        }
                    }
                    Text(
                        text = "${points.first().first}: ${fmt(firstVal)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = TextMutedSlate
                    )
                    Text(
                        text = "${points.last().first}: ${fmt(latestVal)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyProgressOfficialTableCard(
    entries: List<BodyProgressEntryEntity>,
    showAllColumns: Boolean,
    newestFirst: Boolean,
    onToggleColumns: () -> Unit,
    onToggleOrder: () -> Unit,
    onRowClick: (BodyProgressEntryEntity) -> Unit
) {
    val visibleColumns = remember(showAllColumns) {
        if (showAllColumns) OfficialTableColumns else OfficialTableColumns.filter { it.showInCompact }
    }
    val horizontalScrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = NightCardSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_official_table")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card header & table view toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tabla oficial de seguimiento",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Text(
                        text = "${entries.size} registro(s) · Toca una fila para editar o ver notas",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedSlate
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = onToggleOrder,
                        shape = RoundedCornerShape(50),
                        color = NightCardElevated,
                        border = BorderStroke(1.dp, MinimalHairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SwapVert,
                                contentDescription = "Cambiar orden",
                                tint = TextStarlight,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (newestFirst) "Recientes" else "Crono",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = TextStarlight
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleColumns,
                        shape = RoundedCornerShape(50),
                        color = if (showAllColumns) SyncSuccessGreen.copy(alpha = 0.14f) else NightCardElevated,
                        border = BorderStroke(
                            1.dp,
                            if (showAllColumns) SyncSuccessGreen.copy(alpha = 0.45f) else MinimalHairline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TableChart,
                                contentDescription = "Alternar columnas",
                                tint = if (showAllColumns) SyncSuccessGreen else TextStarlight,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (showAllColumns) "15 col." else "Compacta",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (showAllColumns) SyncSuccessGreen else TextStarlight
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MinimalHairline, thickness = 1.dp)

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin pesajes registrados todavía. Pulsa + Pesaje para empezar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Frozen FECHA column + horizontally scrolling 14 metric columns with locked row heights
                val headerHeight = 42.dp
                val rowHeight = 52.dp

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Frozen left column: FECHA
                    Column(
                        modifier = Modifier
                            .width(78.dp)
                            .background(NightBgSurface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(headerHeight)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "FECHA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = TextMutedSlate,
                                maxLines = 1
                            )
                        }
                        HorizontalDivider(color = MinimalHairline, thickness = 1.dp)

                        entries.forEachIndexed { idx, entry ->
                            val rowBg = when {
                                entry.isPostponed -> NocturnalAmber.copy(alpha = 0.08f)
                                idx % 2 == 1 -> NightCardElevated.copy(alpha = 0.42f)
                                else -> NightBgSurface
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                                    .background(rowBg)
                                    .clickable { onRowClick(entry) }
                                    .padding(horizontal = 10.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = entry.displayDate,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = if (entry.isPostponed) NocturnalAmber else TextMoonlight,
                                    maxLines = 1
                                )
                                when {
                                    entry.isBaseEntry -> Text(
                                        text = "BASE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = MoonlightCyan
                                    )
                                    entry.isPostponed -> Text(
                                        text = "APLAZ.",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = NocturnalAmber
                                    )
                                    entry.notes.isNotBlank() -> Text(
                                        text = "nota",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = TextMutedSlate
                                    )
                                }
                            }
                            if (idx < entries.lastIndex) {
                                HorizontalDivider(color = MinimalHairline.copy(alpha = 0.55f), thickness = 0.6.dp)
                            }
                        }
                    }

                    VerticalDivider(color = NightBorderSubtle, thickness = 1.dp)

                    // Scrollable right columns
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Column {
                            // Header row
                            Row(
                                modifier = Modifier
                                    .height(headerHeight)
                                    .background(NightBgSurface),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                visibleColumns.forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .width(col.widthDp)
                                            .fillMaxHeight()
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = col.header,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.4.sp
                                            ),
                                            color = TextMutedSlate,
                                            maxLines = 1,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MinimalHairline, thickness = 1.dp)

                            // Data rows
                            entries.forEachIndexed { idx, entry ->
                                val rowBg = when {
                                    entry.isPostponed -> NocturnalAmber.copy(alpha = 0.08f)
                                    idx % 2 == 1 -> NightCardElevated.copy(alpha = 0.42f)
                                    else -> NightCardSurface
                                }
                                Row(
                                    modifier = Modifier
                                        .height(rowHeight)
                                        .background(rowBg)
                                        .clickable { onRowClick(entry) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    visibleColumns.forEachIndexed { colIdx, col ->
                                        Box(
                                            modifier = Modifier
                                                .width(col.widthDp)
                                                .fillMaxHeight()
                                                .padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            TableCellValue(
                                                columnId = col.id,
                                                columnIndex = colIdx,
                                                entry = entry
                                            )
                                        }
                                    }
                                }
                                if (idx < entries.lastIndex) {
                                    HorizontalDivider(color = MinimalHairline.copy(alpha = 0.55f), thickness = 0.6.dp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MinimalHairline, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NightBgSurface)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "* TMB clínico Mifflin-St Jeor",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextMutedSlate
                    )
                    Text(
                        text = "Desliza la tabla →",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MoonlightCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun TableCellValue(
    columnId: String,
    columnIndex: Int,
    entry: BodyProgressEntryEntity
) {
    if (entry.isPostponed) {
        if (columnIndex == 0) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = NocturnalAmber.copy(alpha = 0.16f)
            ) {
                Text(
                    text = entry.postponeShortLabel.ifBlank { "Aplazado" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = NocturnalAmber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else {
            Text(
                text = "—",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = TextMutedSlate.copy(alpha = 0.55f)
            )
        }
        return
    }

    val mono = MaterialTheme.typography.bodySmall.copy(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 12.sp
    )

    when (columnId) {
        "weight" -> Text(
            text = BodyProgressDomainHelper.formatEs2(entry.weightKg),
            style = mono.copy(fontWeight = FontWeight.Bold),
            color = TextMoonlight,
            maxLines = 1
        )
        "deltaWeight" -> when {
            entry.isBaseEntry -> Surface(
                shape = RoundedCornerShape(6.dp),
                color = MoonlightCyan.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "Base",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = MoonlightCyan,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            entry.deltaWeightKg != null -> Text(
                text = BodyProgressDomainHelper.formatSigned2(entry.deltaWeightKg),
                style = mono.copy(fontWeight = FontWeight.SemiBold),
                color = if (entry.deltaWeightKg < 0) SyncSuccessGreen else if (entry.deltaWeightKg > 0) MoonlightCyan else TextStarlight,
                maxLines = 1
            )
            else -> Text("—", style = mono, color = TextMutedSlate)
        }
        "bmi" -> Text(
            text = BodyProgressDomainHelper.formatEs2(entry.bmi),
            style = mono,
            color = TextStarlight,
            maxLines = 1
        )
        "fatPct" -> Text(
            text = BodyProgressDomainHelper.formatPct1(entry.bodyFatPct),
            style = mono,
            color = TextMoonlight,
            maxLines = 1
        )
        "deltaFat" -> {
            val df = entry.deltaFatKg
            if (df != null) {
                val color = if (df < 0) SyncSuccessGreen else if (df > 0) NocturnalAmber else TextStarlight
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = BodyProgressDomainHelper.formatSigned1(df),
                        style = mono.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text("—", style = mono, color = TextMutedSlate)
            }
        }
        "muscle" -> Text(
            text = BodyProgressDomainHelper.formatEs1(entry.muscleKg),
            style = mono.copy(fontWeight = FontWeight.Bold),
            color = SyncSuccessGreen,
            maxLines = 1
        )
        "deltaMuscle" -> {
            val dm = entry.deltaMuscleKg
            if (dm != null) {
                val color = if (dm > 0) SyncSuccessGreen else if (dm < 0) EnfoqueCoralSoft else TextStarlight
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = BodyProgressDomainHelper.formatSigned1(dm),
                        style = mono.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text("—", style = mono, color = TextMutedSlate)
            }
        }
        "muscleQuality" -> Text(
            text = entry.muscleQuality?.toString() ?: "—",
            style = mono.copy(fontWeight = FontWeight.SemiBold),
            color = TextMoonlight,
            maxLines = 1
        )
        "bodyType" -> Text(
            text = entry.bodyType?.toString() ?: "—",
            style = mono,
            color = TextStarlight,
            maxLines = 1
        )
        "boneMass" -> Text(
            text = BodyProgressDomainHelper.formatEs1(entry.boneMassKg),
            style = mono,
            color = TextStarlight,
            maxLines = 1
        )
        "visceralFat" -> {
            val vfText = entry.visceralFat?.let { v ->
                if (v % 1.0 == 0.0) v.toInt().toString() else BodyProgressDomainHelper.formatEs1(v)
            } ?: "—"
            Text(
                text = vfText,
                style = mono,
                color = TextStarlight,
                maxLines = 1
            )
        }
        "bmr" -> {
            val main = entry.bmrKcal?.toString() ?: "—"
            if (entry.clinicalBmrKcal != null && entry.bmrKcal != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = main,
                        style = mono.copy(fontWeight = FontWeight.SemiBold),
                        color = TextMoonlight,
                        maxLines = 1
                    )
                    Text(
                        text = "(${entry.clinicalBmrKcal}*)",
                        style = mono.copy(fontSize = 10.sp),
                        color = TextMutedSlate,
                        maxLines = 1
                    )
                }
            } else {
                Text(text = main, style = mono, color = TextMoonlight, maxLines = 1)
            }
        }
        "metabolicAge" -> Text(
            text = entry.metabolicAge?.toString() ?: "—",
            style = mono,
            color = TextStarlight,
            maxLines = 1
        )
        "waterPct" -> Text(
            text = BodyProgressDomainHelper.formatPct1(entry.waterPct),
            style = mono,
            color = MoonlightCyan,
            maxLines = 1
        )
    }
}

@Composable
private fun BodyProgressContextCard(profile: BodyProgressProfileEntity) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NightBgSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "REFERENCIAS Y OBJETIVO",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextMutedSlate
            )
            if (profile.summerRefSummary.isNotBlank()) {
                Text(
                    text = profile.summerRefSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextStarlight
                )
            }
            if (profile.targetDescription.isNotBlank()) {
                Text(
                    text = profile.targetDescription,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = SyncSuccessGreen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyProgressEntryDetailSheet(
    entry: BodyProgressEntryEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateTimeHelper.formatHumanDateEs(entry.dateIso),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Text(
                        text = "Columna tabla: ${entry.displayDate}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedSlate
                    )
                }

                when {
                    entry.isBaseEntry -> Surface(
                        shape = RoundedCornerShape(50),
                        color = MoonlightCyan.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "Registro base",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MoonlightCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    entry.isPostponed -> Surface(
                        shape = RoundedCornerShape(50),
                        color = NocturnalAmber.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = entry.postponeShortLabel.ifBlank { "Aplazado" },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = NocturnalAmber,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (!entry.isPostponed) {
                val pairs = listOf(
                    "Peso" to "${BodyProgressDomainHelper.formatEs2(entry.weightKg)} kg",
                    "Δ Peso" to BodyProgressDomainHelper.formatSigned2(entry.deltaWeightKg),
                    "IMC" to BodyProgressDomainHelper.formatEs2(entry.bmi),
                    "% Grasa" to BodyProgressDomainHelper.formatPct1(entry.bodyFatPct),
                    "Δ Grasa kg" to BodyProgressDomainHelper.formatSigned1(entry.deltaFatKg),
                    "Músculo" to "${BodyProgressDomainHelper.formatEs1(entry.muscleKg)} kg",
                    "Δ Músculo" to BodyProgressDomainHelper.formatSigned1(entry.deltaMuscleKg),
                    "Calidad musc." to (entry.muscleQuality?.toString() ?: "—"),
                    "Tipo cuerpo" to (entry.bodyType?.toString() ?: "—"),
                    "Masa ósea" to "${BodyProgressDomainHelper.formatEs1(entry.boneMassKg)} kg",
                    "Grasa visceral" to (entry.visceralFat?.let {
                        if (it % 1.0 == 0.0) it.toInt().toString() else BodyProgressDomainHelper.formatEs1(it)
                    } ?: "—"),
                    "TMB báscula" to (entry.bmrKcal?.let { "$it kcal" } ?: "—"),
                    "TMB clínico*" to (entry.clinicalBmrKcal?.let { "$it kcal" } ?: "—"),
                    "Edad metabólica" to (entry.metabolicAge?.let { "$it años" } ?: "—"),
                    "Agua corporal" to BodyProgressDomainHelper.formatPct1(entry.waterPct)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pairs.chunked(2).forEach { rowPairs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPairs.forEach { (k, v) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NightCardSurface,
                                    border = BorderStroke(1.dp, MinimalHairline),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                        Text(
                                            text = k,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMutedSlate
                                        )
                                        Text(
                                            text = v,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = TextMoonlight
                                        )
                                    }
                                }
                            }
                            if (rowPairs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (entry.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (entry.isPostponed) NocturnalAmber.copy(alpha = 0.4f) else MinimalHairline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "OBSERVACIÓN CLÍNICA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            ),
                            color = if (entry.isPostponed) NocturnalAmber else TextMutedSlate
                        )
                        Text(
                            text = entry.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextStarlight
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.55f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = ConflictWarningRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Eliminar", color = ConflictWarningRed)
                }

                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = SyncSuccessGreen),
                    modifier = Modifier.weight(1.4f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Color(0xFF061510),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar registro", color = Color(0xFF061510), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyProgressEditorSheet(
    state: BodyProgressUiState,
    prevValidEntry: BodyProgressEntryEntity?,
    onUpdateEditor: ((BodyProgressEditorState) -> BodyProgressEditorState) -> Unit,
    onPickDate: () -> Unit,
    onSave: () -> Unit,
    onRequestDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val ed = state.editor
    val profile = state.profile
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val typedWeight = BodyProgressDomainHelper.parseFlexibleDouble(ed.weightInput)
    val typedFatPct = BodyProgressDomainHelper.parseFlexibleDouble(ed.bodyFatPctInput)
    val typedMuscle = BodyProgressDomainHelper.parseFlexibleDouble(ed.muscleInput)
    val previewBmi = BodyProgressDomainHelper.calculateBmi(typedWeight, profile.heightCm)
    val previewClinicalBmr = BodyProgressDomainHelper.calculateMifflinStJeorKcal(
        weightKg = typedWeight,
        heightCm = profile.heightCm,
        ageYears = profile.ageYears
    )
    val (previewDw, previewDf, previewDm) = if (ed.isBaseEntry) {
        Triple(null, null, null)
    } else {
        BodyProgressDomainHelper.computeAutoDeltas(prevValidEntry, typedWeight, typedFatPct, typedMuscle)
    }

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
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sheet Title + Delete if editing
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (ed.editingEntryId == null) "Nuevo pesaje" else "Editar registro",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextMoonlight
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ed.editingEntryId != null) {
                        IconButton(onClick = onRequestDelete) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Eliminar registro",
                                tint = ConflictWarningRed
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = TextMutedSlate
                        )
                    }
                }
            }

            // Date Picker row
            Surface(
                onClick = onPickDate,
                shape = RoundedCornerShape(14.dp),
                color = NightCardSurface,
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MoonlightCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Fecha del pesaje",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedSlate
                            )
                            Text(
                                text = "${DateTimeHelper.formatHumanDateEs(ed.dateIso)} (${BodyProgressDomainHelper.formatShortDisplayDate(ed.dateIso)})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextMoonlight
                            )
                        }
                    }
                    Text(
                        text = "Cambiar",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MoonlightCyan
                    )
                }
            }

            // Postpone / Base toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (ed.isPostponed) NocturnalAmber.copy(alpha = 0.45f) else MinimalHairline
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Aplazado",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (ed.isPostponed) NocturnalAmber else TextStarlight
                        )
                        Switch(
                            checked = ed.isPostponed,
                            onCheckedChange = { checked ->
                                onUpdateEditor { it.copy(isPostponed = checked, isBaseEntry = if (checked) false else it.isBaseEntry) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF1A1200),
                                checkedTrackColor = NocturnalAmber
                            )
                        )
                    }
                }

                if (!ed.isPostponed) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NightCardSurface,
                        border = BorderStroke(
                            1.dp,
                            if (ed.isBaseEntry) MoonlightCyan.copy(alpha = 0.45f) else MinimalHairline
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Base",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (ed.isBaseEntry) MoonlightCyan else TextStarlight
                            )
                            Switch(
                                checked = ed.isBaseEntry,
                                onCheckedChange = { checked ->
                                    onUpdateEditor { it.copy(isBaseEntry = checked) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00141A),
                                    checkedTrackColor = MoonlightCyan
                                )
                            )
                        }
                    }
                }
            }

            if (ed.isPostponed) {
                ProgressInputField(
                    label = "Etiqueta corta en tabla (ej. Incidencia)",
                    value = ed.postponeShortLabel,
                    onValueChange = { v -> onUpdateEditor { it.copy(postponeShortLabel = v) } },
                    keyboardType = KeyboardType.Text
                )
                ProgressInputField(
                    label = "Motivo / nota clínica",
                    value = ed.notesInput,
                    onValueChange = { v -> onUpdateEditor { it.copy(notesInput = v) } },
                    keyboardType = KeyboardType.Text,
                    singleLine = false
                )
            } else {
                // Auto-calculation QoL toggle + Live preview banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-calcular IMC, Δ y TMB*",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextMoonlight
                                )
                                Text(
                                    text = prevValidEntry?.let { "Ref. anterior: ${it.displayDate} (${BodyProgressDomainHelper.formatEs2(it.weightKg)} kg)" }
                                        ?: "Sin pesaje previo para deltas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                            }
                            Switch(
                                checked = ed.autoComputeDerived,
                                onCheckedChange = { on -> onUpdateEditor { it.copy(autoComputeDerived = on) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF061510),
                                    checkedTrackColor = SyncSuccessGreen
                                )
                            )
                        }

                        if (typedWeight != null) {
                            val previewSummary = buildString {
                                append("Vista previa · IMC ${BodyProgressDomainHelper.formatEs2(previewBmi)}")
                                if (previewDw != null) append(" · ΔP ${BodyProgressDomainHelper.formatSigned2(previewDw)}")
                                if (previewDf != null) append(" · ΔG ${BodyProgressDomainHelper.formatSigned1(previewDf)}")
                                if (previewDm != null) append(" · ΔM ${BodyProgressDomainHelper.formatSigned1(previewDm)}")
                                if (previewClinicalBmr != null) append(" · TMB* $previewClinicalBmr")
                            }
                            Text(
                                text = previewSummary,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = SyncSuccessGreen
                            )
                        }
                    }
                }

                // Core bioimpedance measurements (2 columns)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressInputField(
                        label = "Peso (kg) *",
                        value = ed.weightInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(weightInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "% Grasa (%)",
                        value = ed.bodyFatPctInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(bodyFatPctInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressInputField(
                        label = "Músculo (kg)",
                        value = ed.muscleInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(muscleInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "Agua (%)",
                        value = ed.waterPctInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(waterPctInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressInputField(
                        label = "Calidad musc.",
                        value = ed.muscleQualityInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(muscleQualityInput = v) } },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "Tipo cuerpo",
                        value = ed.bodyTypeInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(bodyTypeInput = v) } },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressInputField(
                        label = "Masa ósea (kg)",
                        value = ed.boneMassInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(boneMassInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "Grasa visc.",
                        value = ed.visceralFatInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(visceralFatInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressInputField(
                        label = "TMB báscula (kcal)",
                        value = ed.bmrInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(bmrInput = v) } },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "Edad metabólica",
                        value = ed.metabolicAgeInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(metabolicAgeInput = v) } },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Índices y deltas (déjalos vacíos para auto-rellenar)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedSlate
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressInputField(
                        label = "IMC",
                        placeholder = previewBmi?.let { BodyProgressDomainHelper.formatEs2(it) } ?: "",
                        value = ed.bmiInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(bmiInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "TMB clínico*",
                        placeholder = previewClinicalBmr?.toString() ?: "",
                        value = ed.clinicalBmrInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(clinicalBmrInput = v) } },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressInputField(
                        label = "Δ Peso",
                        placeholder = previewDw?.let { BodyProgressDomainHelper.formatSigned2(it) } ?: "",
                        value = ed.deltaWeightInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(deltaWeightInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "Δ Grasa kg",
                        placeholder = previewDf?.let { BodyProgressDomainHelper.formatSigned1(it) } ?: "",
                        value = ed.deltaFatInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(deltaFatInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "Δ Músculo",
                        placeholder = previewDm?.let { BodyProgressDomainHelper.formatSigned1(it) } ?: "",
                        value = ed.deltaMuscleInput,
                        onValueChange = { v -> onUpdateEditor { it.copy(deltaMuscleInput = v) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                ProgressInputField(
                    label = "Notas / observación (opcional)",
                    value = ed.notesInput,
                    onValueChange = { v -> onUpdateEditor { it.copy(notesInput = v) } },
                    keyboardType = KeyboardType.Text,
                    singleLine = false
                )
            }

            if (ed.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ConflictWarningRed.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = ConflictWarningRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = ed.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = ConflictWarningRed
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = TextStarlight)
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = SyncSuccessGreen),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("progress_save_button")
                ) {
                    Text(
                        text = if (ed.editingEntryId == null) "Guardar pesaje" else "Guardar cambios",
                        color = Color(0xFF061510),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Decimal,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        placeholder = if (placeholder.isNotBlank()) {
            {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = TextMutedSlate.copy(alpha = 0.7f)
                )
            }
        } else null,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SyncSuccessGreen,
            unfocusedBorderColor = NightBorderSubtle,
            focusedLabelColor = SyncSuccessGreen,
            unfocusedLabelColor = TextMutedSlate,
            focusedTextColor = TextMoonlight,
            unfocusedTextColor = TextMoonlight,
            focusedContainerColor = NightCardSurface,
            unfocusedContainerColor = NightCardSurface
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun BodyProgressProfileDialog(
    profile: BodyProgressProfileEntity,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, String, Int, String, String) -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.athleteName) }
    var age by remember(profile) { mutableStateOf(profile.ageYears.toString()) }
    var height by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var scale by remember(profile) { mutableStateOf(profile.scaleModel) }
    var cycle by remember(profile) { mutableStateOf(profile.cycleDays.toString()) }
    var summerRef by remember(profile) { mutableStateOf(profile.summerRefSummary) }
    var targetDesc by remember(profile) { mutableStateOf(profile.targetDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightBgSurface,
        title = {
            Text(
                text = "Perfil y parámetros",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextMoonlight
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressInputField(
                    label = "Nombre",
                    value = name,
                    onValueChange = { name = it },
                    keyboardType = KeyboardType.Text
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressInputField(
                        label = "Edad",
                        value = age,
                        onValueChange = { age = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "Altura (cm)",
                        value = height,
                        onValueChange = { height = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressInputField(
                        label = "Ciclo (días)",
                        value = cycle,
                        onValueChange = { cycle = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
                ProgressInputField(
                    label = "Báscula",
                    value = scale,
                    onValueChange = { scale = it },
                    keyboardType = KeyboardType.Text
                )
                ProgressInputField(
                    label = "Referencia histórica",
                    value = summerRef,
                    onValueChange = { summerRef = it },
                    keyboardType = KeyboardType.Text,
                    singleLine = false
                )
                ProgressInputField(
                    label = "Objetivo",
                    value = targetDesc,
                    onValueChange = { targetDesc = it },
                    keyboardType = KeyboardType.Text,
                    singleLine = false
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        age.toIntOrNull() ?: profile.ageYears,
                        height.toIntOrNull() ?: profile.heightCm,
                        scale,
                        cycle.toIntOrNull() ?: profile.cycleDays,
                        summerRef,
                        targetDesc
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SyncSuccessGreen)
            ) {
                Text("Guardar", color = Color(0xFF061510), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMutedSlate)
            }
        }
    )
}

@Composable
private fun BodyProgressBackupDialog(
    entryCount: Int,
    onDismiss: () -> Unit,
    onDownloadJson: () -> Unit,
    onShareJson: () -> Unit,
    onImportJson: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightBgSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Backup,
                    contentDescription = null,
                    tint = SyncSuccessGreen
                )
                Text(
                    text = "Backup · Progreso físico",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextMoonlight
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tienes $entryCount pesaje(s) guardados localmente (y sincronizados E2EE si la nube está activa). Puedes exportar o restaurar un JSON en cualquier momento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextStarlight
                )

                Button(
                    onClick = onDownloadJson,
                    colors = ButtonDefaults.buttonColors(containerColor = SyncSuccessGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, tint = Color(0xFF061510), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descargar JSON", color = Color(0xFF061510), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShareJson,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = MoonlightCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir archivo JSON", color = TextMoonlight)
                }

                OutlinedButton(
                    onClick = onImportJson,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = NocturnalAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar / fusionar JSON", color = TextMoonlight)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextMutedSlate)
            }
        }
    )
}
