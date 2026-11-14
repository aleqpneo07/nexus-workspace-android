package dev.quintero.nexus.feature.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.quintero.nexus.feature.focus.EnfoqueUiState
import dev.quintero.nexus.feature.schedule.LiveStatusKind
import dev.quintero.nexus.feature.schedule.UniSubject
import dev.quintero.nexus.feature.schedule.UniversityScheduleRepository
import dev.quintero.nexus.sync.MisCosasSyncUiState
import dev.quintero.nexus.feature.planner.NightPlannerUiState
import dev.quintero.nexus.feature.schedule.UniversityScheduleScreen
import dev.quintero.nexus.core.design.ConflictWarningRed
import dev.quintero.nexus.core.design.ElectricIndigo
import dev.quintero.nexus.core.design.ElectricIndigoBright
import dev.quintero.nexus.core.design.ElectricIndigoContainer
import dev.quintero.nexus.core.design.EnfoqueCoralContainer
import dev.quintero.nexus.core.design.EnfoqueCoralSoft
import dev.quintero.nexus.core.design.MinimalHairline
import dev.quintero.nexus.core.design.MoonlightCyan
import dev.quintero.nexus.core.design.NightBgDeep
import dev.quintero.nexus.core.design.NightBgSurface
import dev.quintero.nexus.core.design.NightCardSurface
import dev.quintero.nexus.core.design.NocturnalAmber
import dev.quintero.nexus.core.design.NocturnalAmberContainer
import dev.quintero.nexus.core.design.SyncSuccessGreen
import dev.quintero.nexus.core.design.TextMoonlight
import dev.quintero.nexus.core.design.TextMutedSlate
import dev.quintero.nexus.core.design.TextStarlight
import dev.quintero.nexus.core.design.WorkoutTealSoft
import dev.quintero.nexus.core.util.DateTimeHelper
import dev.quintero.nexus.feature.workout.WorkoutUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MisCosasModule {
    HUB,
    MI_HORARIO,
    ENTRENO,
    NIGHT_PLANNER,
    ENFOQUE,
    PROGRESO_FISICO,
    NUTRICION
}

@Composable
private fun MinimalHubCloudPill(
    syncState: MisCosasSyncUiState,
    onOpenDialog: () -> Unit,
    onQuickSync: () -> Unit
) {
    val dotColor = when {
        syncState.lastError != null -> ConflictWarningRed
        syncState.isConnected -> SyncSuccessGreen
        else -> TextMutedSlate
    }
    val borderColor = when {
        syncState.lastError != null -> ConflictWarningRed.copy(alpha = 0.45f)
        syncState.isConnected -> SyncSuccessGreen.copy(alpha = 0.35f)
        else -> MinimalHairline
    }
    val label = when {
        syncState.isSyncing -> "Sincronizando"
        syncState.isConnected && syncState.accountIdentifier.isNotBlank() -> syncState.accountIdentifier
        else -> "Móvil ↔ Tablet"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = onOpenDialog,
            shape = RoundedCornerShape(50),
            color = NightCardSurface,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.testTag("hub_sync_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (syncState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = MoonlightCyan
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                Icon(
                    imageVector = if (syncState.isConnected) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                    contentDescription = "Sincronización móvil y tablet",
                    tint = if (syncState.isConnected) SyncSuccessGreen else TextMutedSlate,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (syncState.isConnected) TextMoonlight else TextStarlight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (syncState.isConnected && !syncState.isSyncing) {
            Surface(
                onClick = onQuickSync,
                shape = CircleShape,
                color = MoonlightCyan.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MoonlightCyan.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(26.dp)
                    .testTag("hub_quick_sync_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = "Sincronizar ahora",
                        tint = MoonlightCyan,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MisCosasHubScreen(
    nightUiState: NightPlannerUiState,
    workoutUiState: WorkoutUiState,
    enfoqueUiState: EnfoqueUiState,
    onOpenModule: (MisCosasModule) -> Unit,
    syncState: MisCosasSyncUiState = MisCosasSyncUiState(),
    onConnectCloud: (String, String, (Result<String>) -> Unit) -> Unit = { _, _, cb -> cb(Result.success("")) },
    onSyncNow: ((Result<String>) -> Unit) -> Unit = { cb -> cb(Result.success("")) },
    onDisconnectCloud: () -> Unit = {},
    bodyProgressUiState: dev.quintero.nexus.feature.progress.BodyProgressUiState = dev.quintero.nexus.feature.progress.BodyProgressUiState(),
    nutritionUiState: dev.quintero.nexus.feature.nutrition.NutritionUiState = dev.quintero.nexus.feature.nutrition.NutritionUiState()
) {
    val todayHuman = DateTimeHelper.formatFullDayShortEs(DateTimeHelper.todayIso())
    val uniLive = remember { UniversityScheduleRepository.resolveLiveStatus() }
    var showCloudDialog by remember { mutableStateOf(false) }
    var cloudFeedback by remember { mutableStateOf<String?>(null) }

    val horarioSubtitle = when (uniLive.kind) {
        LiveStatusKind.ACTIVE_NOW -> "En clase · ${uniLive.mainText}"
        LiveStatusKind.NEXT_TODAY -> "Siguiente · ${uniLive.mainText}"
        LiveStatusKind.DAY_FINISHED -> uniLive.subText
        LiveStatusKind.WEEKEND -> uniLive.subText
    }

    val workoutSubtitle = if (workoutUiState.todayCompletedDays.isNotEmpty()) {
        "Hoy completado · Entreno al día"
    } else {
        "Rutina de hoy · ${workoutUiState.currentPlan.label}"
    }

    val nightSubtitle = buildString {
        append("${nightUiState.totalPendingCount} pendientes")
        if (nightUiState.totalOverlapCount > 0) {
            append(" · ${nightUiState.totalOverlapCount} alertas en agenda")
        } else if (nightUiState.oAuthSettings.isConnected) {
            append(" · Calendar conectado")
        } else {
            append(" · Tareas, Agenda y Calendar")
        }
    }

    val enfoqueSubtitle = "${enfoqueUiState.activeTasksCount} proyectos activos · ${enfoqueUiState.priorityTodayTasks.size} para hoy"

    Surface(
        color = NightBgDeep,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = todayHuman.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MinimalHubCloudPill(
                        syncState = syncState,
                        onOpenDialog = { showCloudDialog = true },
                        onQuickSync = {
                            cloudFeedback = null
                            onSyncNow { res ->
                                cloudFeedback = res.fold(
                                    onSuccess = { it },
                                    onFailure = { it.message ?: "Error al sincronizar" }
                                )
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nexus",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = TextMoonlight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Espacio modular offline-first",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                        val syncNotice = syncState.lastError ?: cloudFeedback
                        if (!syncNotice.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = syncNotice,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (syncState.lastError != null) EnfoqueCoralSoft else SyncSuccessGreen,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    MinimalBentoGlyph(modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                MinimalHubModuleCard(
                    title = "Mi horario",
                    subtitle = horarioSubtitle,
                    metaBadge = if (uniLive.kind == LiveStatusKind.ACTIVE_NOW) "AHORA" else null,
                    icon = Icons.Outlined.School,
                    accentColor = NocturnalAmber,
                    bubbleColor = NocturnalAmberContainer,
                    testTag = "hub_card_horario",
                    onClick = { onOpenModule(MisCosasModule.MI_HORARIO) }
                )
            }

            item {
                MinimalHubModuleCard(
                    title = "Entreno",
                    subtitle = workoutSubtitle,
                    metaBadge = if (workoutUiState.todayCompletedDays.isNotEmpty()) "HECHO" else null,
                    icon = Icons.Outlined.FitnessCenter,
                    accentColor = WorkoutTealSoft,
                    bubbleColor = WorkoutTealSoft.copy(alpha = 0.13f),
                    testTag = "hub_card_entreno",
                    onClick = { onOpenModule(MisCosasModule.ENTRENO) }
                )
            }

            item {
                MinimalHubModuleCard(
                    title = "Night Planner",
                    subtitle = nightSubtitle,
                    metaBadge = if (nightUiState.totalPendingCount > 0) "${nightUiState.totalPendingCount}" else null,
                    icon = Icons.Outlined.TaskAlt,
                    accentColor = ElectricIndigoBright,
                    bubbleColor = ElectricIndigoContainer,
                    testTag = "hub_card_night_planner",
                    onClick = { onOpenModule(MisCosasModule.NIGHT_PLANNER) }
                )
            }

            item {
                MinimalHubModuleCard(
                    title = "Enfoque",
                    subtitle = enfoqueSubtitle,
                    metaBadge = if (enfoqueUiState.priorityTodayTasks.isNotEmpty()) "${enfoqueUiState.priorityTodayTasks.size} HOY" else null,
                    icon = Icons.Outlined.DonutLarge,
                    accentColor = EnfoqueCoralSoft,
                    bubbleColor = EnfoqueCoralContainer,
                    testTag = "hub_card_enfoque",
                    onClick = { onOpenModule(MisCosasModule.ENFOQUE) }
                )
            }

            item {
                val latest = bodyProgressUiState.latestValidEntry
                val progressSubtitle = if (latest?.weightKg != null) {
                    val weightText = dev.quintero.nexus.feature.progress.BodyProgressDomainHelper.formatEs2(latest.weightKg)
                    val musclePart = latest.muscleKg?.let { m ->
                        " · Músculo ${dev.quintero.nexus.feature.progress.BodyProgressDomainHelper.formatEs1(m)} kg"
                    }.orEmpty()
                    "$weightText kg$musclePart"
                } else {
                    "Seguimiento corporal y bioimpedancia"
                }
                val progressBadge = bodyProgressUiState.nextWeighIn?.let { next ->
                    "Próx. ${next.nextDisplayDate}"
                }
                MinimalHubModuleCard(
                    title = "Progreso físico",
                    subtitle = progressSubtitle,
                    metaBadge = progressBadge,
                    icon = Icons.Outlined.MonitorWeight,
                    accentColor = dev.quintero.nexus.core.design.SyncSuccessGreen,
                    bubbleColor = dev.quintero.nexus.core.design.SyncSuccessGreen.copy(alpha = 0.14f),
                    testTag = "hub_card_progreso",
                    onClick = { onOpenModule(MisCosasModule.PROGRESO_FISICO) }
                )
            }

            item {
                val consumedKcal = nutritionUiState.todayConsumedKcal
                val goalKcal = nutritionUiState.profile.dailyCalorieGoal.coerceAtLeast(1000)
                val proteinG = nutritionUiState.todayConsumedProteinG
                val remainingKcal = goalKcal - consumedKcal
                val nutritionSubtitle = if (consumedKcal > 0) {
                    "$consumedKcal / $goalKcal kcal · ${proteinG}g prot · ${nutritionUiState.todayMetric.waterMl} ml agua"
                } else {
                    "Escáner QR/EAN · Foto IA · Meta $goalKcal kcal"
                }
                val nutritionBadge = if (remainingKcal >= 0) {
                    "$remainingKcal LIBRES"
                } else {
                    "+${kotlin.math.abs(remainingKcal)} KCAL"
                }
                MinimalHubModuleCard(
                    title = "Nutrición",
                    subtitle = nutritionSubtitle,
                    metaBadge = nutritionBadge,
                    icon = Icons.Outlined.Restaurant,
                    accentColor = MoonlightCyan,
                    bubbleColor = MoonlightCyan.copy(alpha = 0.14f),
                    testTag = "hub_card_nutricion",
                    onClick = { onOpenModule(MisCosasModule.NUTRICION) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showCloudDialog) {
        MisCosasCloudSyncDialog(
            syncState = syncState,
            onDismiss = { showCloudDialog = false },
            onConnect = { id, pin ->
                cloudFeedback = null
                onConnectCloud(id, pin) { res ->
                    res.onSuccess { msg ->
                        cloudFeedback = msg
                        showCloudDialog = false
                    }.onFailure { err ->
                        cloudFeedback = err.message ?: "No se pudo conectar"
                    }
                }
            },
            onSyncNow = {
                onSyncNow { res ->
                    cloudFeedback = res.fold(
                        onSuccess = { it },
                        onFailure = { it.message ?: "Error al sincronizar" }
                    )
                }
            },
            onDisconnect = {
                onDisconnectCloud()
                cloudFeedback = "Sincronización desconectada en este dispositivo"
                showCloudDialog = false
            }
        )
    }
}

@Composable
private fun MisCosasCloudSyncDialog(
    syncState: MisCosasSyncUiState,
    onDismiss: () -> Unit,
    onConnect: (String, String) -> Unit,
    onSyncNow: () -> Unit,
    onDisconnect: () -> Unit
) {
    var identifier by remember(syncState.accountIdentifier) {
        mutableStateOf(syncState.accountIdentifier)
    }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightCardSurface,
        titleContentColor = TextMoonlight,
        textContentColor = TextStarlight,
        title = {
            Text(
                text = "Nube Móvil ↔ Tablet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Introduce el mismo Usuario y PIN en tu móvil y en tu tablet para sincronizar Entreno, Night Planner y Enfoque con cifrado de extremo a extremo (AES-256-GCM).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate
                )
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Usuario o alias") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MoonlightCyan,
                        unfocusedBorderColor = MinimalHairline,
                        focusedTextColor = TextMoonlight,
                        unfocusedTextColor = TextMoonlight,
                        focusedLabelColor = MoonlightCyan,
                        unfocusedLabelColor = TextMutedSlate
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (syncState.isConnected) "Nuevo PIN (opcional para cambiar)" else "PIN o contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MoonlightCyan,
                        unfocusedBorderColor = MinimalHairline,
                        focusedTextColor = TextMoonlight,
                        unfocusedTextColor = TextMoonlight,
                        focusedLabelColor = MoonlightCyan,
                        unfocusedLabelColor = TextMutedSlate
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (syncState.isConnected) {
                    Text(
                        text = "Estado: Conectado como ${syncState.accountIdentifier}" +
                            if (syncState.isLiveConnected) " · Escuchando en vivo" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = SyncSuccessGreen
                    )
                }
            }
        },
        confirmButton = {
            if (syncState.isConnected && password.isBlank()) {
                Button(
                    onClick = {
                        onSyncNow()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoonlightCyan,
                        contentColor = Color(0xFF06131E)
                    )
                ) {
                    Text("Sincronizar ahora", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onConnect(identifier, password) },
                    enabled = identifier.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoonlightCyan,
                        contentColor = Color(0xFF06131E)
                    )
                ) {
                    Text("Conectar y sincronizar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (syncState.isConnected) {
                    TextButton(onClick = onDisconnect) {
                        Text("Desvincular", color = ConflictWarningRed)
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cerrar", color = TextStarlight)
                }
            }
        }
    )
}

@Composable
private fun MinimalHubModuleCard(
    title: String,
    subtitle: String,
    metaBadge: String?,
    icon: ImageVector,
    accentColor: Color,
    bubbleColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = NightCardSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bubbleColor)
                    .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextMoonlight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (metaBadge != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = metaBadge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NightBgDeep.copy(alpha = 0.65f))
                    .border(1.dp, MinimalHairline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMutedSlate,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MinimalBentoGlyph(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NightCardSurface,
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp)
        ) {
            val gap = size.minDimension * 0.16f
            val cell = (size.minDimension - gap) / 2f
            val radius = CornerRadius(cell * 0.28f, cell * 0.28f)
            val stroke = Stroke(width = 1.4.dp.toPx())

            drawRoundRect(
                color = NocturnalAmber,
                topLeft = Offset(0f, 0f),
                size = Size(cell, cell),
                cornerRadius = radius,
                style = stroke
            )
            drawRoundRect(
                color = WorkoutTealSoft,
                topLeft = Offset(cell + gap, 0f),
                size = Size(cell, cell),
                cornerRadius = radius,
                style = stroke
            )
            drawRoundRect(
                color = ElectricIndigo,
                topLeft = Offset(0f, cell + gap),
                size = Size(cell, cell),
                cornerRadius = radius,
                style = stroke
            )
            drawRoundRect(
                color = EnfoqueCoralSoft,
                topLeft = Offset(cell + gap, cell + gap),
                size = Size(cell, cell),
                cornerRadius = radius,
                style = stroke
            )
        }
    }
}

@Composable
fun MiHorarioModuleWrapper(
    onBackToHub: () -> Unit,
    onCreateTaskFromClass: (UniSubject, Boolean) -> Unit
) {
    BackHandler(enabled = true) {
        onBackToHub()
    }

    Scaffold(
        containerColor = NightBgDeep,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
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
                                .background(NocturnalAmberContainer)
                                .border(1.dp, NocturnalAmber.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.School,
                                contentDescription = null,
                                tint = NocturnalAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mi horario",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextMoonlight
                            )
                            Text(
                                text = "Carrera · 6 asignaturas · Acceso rápido +Tarea",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedSlate
                            )
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            UniversityScheduleScreen(onCreateTaskFromClass = onCreateTaskFromClass)
        }
    }
}
