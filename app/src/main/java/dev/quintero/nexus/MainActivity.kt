package dev.quintero.nexus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ViewTimeline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.quintero.nexus.BuildConfig
import dev.quintero.nexus.feature.focus.EnfoqueModuleScreen
import dev.quintero.nexus.feature.focus.EnfoqueViewModel
import dev.quintero.nexus.feature.hub.MiHorarioModuleWrapper
import dev.quintero.nexus.feature.hub.MisCosasHubScreen
import dev.quintero.nexus.feature.hub.MisCosasModule
import dev.quintero.nexus.sync.MisCosasCloudSyncManager
import dev.quintero.nexus.feature.planner.NightPlannerTab
import dev.quintero.nexus.feature.planner.NightPlannerUiState
import dev.quintero.nexus.feature.planner.NightPlannerViewModel
import dev.quintero.nexus.feature.planner.EditTaskBottomSheet
import dev.quintero.nexus.feature.planner.GoogleCalendarSetupScreen
import dev.quintero.nexus.feature.planner.TasksWorkspaceScreen
import dev.quintero.nexus.feature.planner.TimelineAndConflictsScreen
import dev.quintero.nexus.core.design.ConflictWarningRed
import dev.quintero.nexus.core.design.ElectricIndigo
import dev.quintero.nexus.core.design.ElectricIndigoBright
import dev.quintero.nexus.core.design.ElectricIndigoContainer
import dev.quintero.nexus.core.design.MinimalHairline
import dev.quintero.nexus.core.design.NexusTheme
import dev.quintero.nexus.core.design.NightBgDeep
import dev.quintero.nexus.core.design.NightBgSurface
import dev.quintero.nexus.core.design.NightBorderSubtle
import dev.quintero.nexus.core.design.NightCardSurface
import dev.quintero.nexus.core.design.SyncSuccessGreen
import dev.quintero.nexus.core.design.TextMoonlight
import dev.quintero.nexus.core.design.TextMutedSlate
import dev.quintero.nexus.core.design.TextStarlight
import dev.quintero.nexus.feature.nutrition.NutritionModuleScreen
import dev.quintero.nexus.feature.nutrition.NutritionViewModel
import dev.quintero.nexus.feature.progress.BodyProgressModuleScreen
import dev.quintero.nexus.feature.progress.BodyProgressViewModel
import dev.quintero.nexus.feature.schedule.UniversityScheduleRepository
import dev.quintero.nexus.feature.workout.WorkoutModuleScreen
import dev.quintero.nexus.feature.workout.WorkoutViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: NightPlannerViewModel by viewModels()
    private val workoutViewModel: WorkoutViewModel by viewModels()
    private val enfoqueViewModel: EnfoqueViewModel by viewModels()
    private val bodyProgressViewModel: BodyProgressViewModel by viewModels()
    private val nutritionViewModel: NutritionViewModel by viewModels()
    private val oAuthNavigationTick = MutableStateFlow(0)
    private val syncManager by lazy { MisCosasCloudSyncManager.getInstance(applicationContext) }

    override fun onStart() {
        super.onStart()
        syncManager.onAppForegroundStart()
        workoutViewModel.refreshTodayDateIfNeeded()
        bodyProgressViewModel.refreshTodayDateIfNeeded()
        nutritionViewModel.refreshTodayDateIfNeeded()
    }

    override fun onStop() {
        syncManager.onAppForegroundStop()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        UniversityScheduleRepository.initialize(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkOAuthRedirectIntent(intent)

        setContent {
            NexusTheme {
                val nightState by viewModel.uiState.collectAsStateWithLifecycle()
                val workoutState by workoutViewModel.uiState.collectAsStateWithLifecycle()
                val enfoqueState by enfoqueViewModel.uiState.collectAsStateWithLifecycle()
                val bodyProgressState by bodyProgressViewModel.uiState.collectAsStateWithLifecycle()
                val nutritionState by nutritionViewModel.uiState.collectAsStateWithLifecycle()
                val cloudSyncState by syncManager.syncState.collectAsStateWithLifecycle()
                val oAuthTick by oAuthNavigationTick.collectAsStateWithLifecycle()

                var activeModule by rememberSaveable { mutableStateOf(MisCosasModule.HUB) }

                LaunchedEffect(oAuthTick) {
                    if (oAuthTick > 0) {
                        activeModule = MisCosasModule.NIGHT_PLANNER
                    }
                }

                Crossfade(targetState = activeModule, label = "mis_cosas_nav") { module ->
                    when (module) {
                        MisCosasModule.HUB -> {
                            MisCosasHubScreen(
                                nightUiState = nightState,
                                workoutUiState = workoutState,
                                enfoqueUiState = enfoqueState,
                                onOpenModule = { target -> activeModule = target },
                                syncState = cloudSyncState,
                                onConnectCloud = { id, pin, cb -> syncManager.connectAccount(id, pin, cb) },
                                onSyncNow = { cb -> syncManager.syncAllNow(cb) },
                                onDisconnectCloud = { syncManager.disconnectAccount() },
                                bodyProgressUiState = bodyProgressState,
                                nutritionUiState = nutritionState
                            )
                        }

                        MisCosasModule.MI_HORARIO -> {
                            MiHorarioModuleWrapper(
                                onBackToHub = { activeModule = MisCosasModule.HUB },
                                onCreateTaskFromClass = { subject, isPractice ->
                                    viewModel.prefillTaskFromUniClass(subject, isPractice)
                                    activeModule = MisCosasModule.NIGHT_PLANNER
                                }
                            )
                        }

                        MisCosasModule.ENTRENO -> {
                            WorkoutModuleScreen(
                                viewModel = workoutViewModel,
                                onBackToHub = { activeModule = MisCosasModule.HUB }
                            )
                        }

                        MisCosasModule.NIGHT_PLANNER -> {
                            NightPlannerAppRoot(
                                state = nightState,
                                viewModel = viewModel,
                                onBackToHub = { activeModule = MisCosasModule.HUB }
                            )
                        }

                        MisCosasModule.ENFOQUE -> {
                            EnfoqueModuleScreen(
                                viewModel = enfoqueViewModel,
                                onBackToHub = { activeModule = MisCosasModule.HUB }
                            )
                        }

                        MisCosasModule.PROGRESO_FISICO -> {
                            BodyProgressModuleScreen(
                                viewModel = bodyProgressViewModel,
                                onBackToHub = { activeModule = MisCosasModule.HUB }
                            )
                        }

                        MisCosasModule.NUTRICION -> {
                            NutritionModuleScreen(
                                viewModel = nutritionViewModel,
                                onBackToHub = { activeModule = MisCosasModule.HUB }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkOAuthRedirectIntent(intent)
    }

    private fun checkOAuthRedirectIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val scheme = data.scheme.orEmpty()
        if (scheme == BuildConfig.APPLICATION_ID || scheme == "nexus") {
            viewModel.handleOAuthRedirect(data)
            oAuthNavigationTick.value += 1
        }
    }
}

private data class NavTabSpec(
    val tab: NightPlannerTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val badgeColor: Color = ElectricIndigo,
    val testTag: String
)

@Composable
fun NightPlannerAppRoot(
    state: NightPlannerUiState,
    viewModel: NightPlannerViewModel,
    onBackToHub: () -> Unit = {}
) {
    BackHandler {
        if (state.activeTab != NightPlannerTab.TASKS) {
            viewModel.selectTab(NightPlannerTab.TASKS)
        } else {
            onBackToHub()
        }
    }

    val navItems = listOf(
        NavTabSpec(
            tab = NightPlannerTab.TASKS,
            label = "Tareas",
            selectedIcon = Icons.Filled.TaskAlt,
            unselectedIcon = Icons.Outlined.TaskAlt,
            badgeCount = state.todayPendingCount,
            badgeColor = ElectricIndigo,
            testTag = "nav_tab_tasks"
        ),
        NavTabSpec(
            tab = NightPlannerTab.TIMELINE_CONFLICTS,
            label = "Agenda",
            selectedIcon = Icons.Filled.ViewTimeline,
            unselectedIcon = Icons.Outlined.ViewTimeline,
            badgeCount = state.totalOverlapCount,
            badgeColor = ConflictWarningRed,
            testTag = "nav_tab_timeline"
        ),
        NavTabSpec(
            tab = NightPlannerTab.GOOGLE_CALENDAR_GUIDE,
            label = "Calendar",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth,
            badgeCount = 0,
            badgeColor = SyncSuccessGreen,
            testTag = "nav_tab_calendar"
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBgDeep)
    ) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = NightBgDeep,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                NightPlannerTopBar(
                    isCalendarConnected = state.oAuthSettings.isConnected,
                    onBackToHub = onBackToHub,
                    onNavigateToCalendar = { viewModel.selectTab(NightPlannerTab.GOOGLE_CALENDAR_GUIDE) }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = NightBgSurface,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEach { item ->
                            val isSelected = state.activeTab == item.tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(item.tab) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge(
                                                    containerColor = item.badgeColor,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(item.badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricIndigoBright,
                                    selectedTextColor = TextMoonlight,
                                    indicatorColor = ElectricIndigo.copy(alpha = 0.18f),
                                    unselectedIconColor = TextMutedSlate,
                                    unselectedTextColor = TextMutedSlate
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = NightBgSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navItems.forEach { item ->
                            val isSelected = state.activeTab == item.tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(item.tab) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge(
                                                    containerColor = item.badgeColor,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(item.badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = !state.statusBannerMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        StatusNotificationBanner(
                            message = state.statusBannerMessage.orEmpty(),
                            isError = state.isStatusError,
                            onDismiss = { viewModel.clearStatusBanner() }
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (state.activeTab) {
                            NightPlannerTab.TASKS -> {
                                TasksWorkspaceScreen(
                                    state = state,
                                    onSelectDayFilter = viewModel::selectDayFilter,
                                    onSelectCategoryFilter = viewModel::setCategoryFilter,
                                    onAddTask = viewModel::addTaskForDate,
                                    onPrefillConsumed = viewModel::consumeComposerPrefill,
                                    onToggleComplete = viewModel::toggleTaskCompletion,
                                    onEditTask = viewModel::openEditTask,
                                    onDeleteTask = viewModel::deleteTask,
                                    onConfirmAndSyncActiveDate = viewModel::confirmAndSyncActiveDateTasks,
                                    onRefreshCalendarEvents = {
                                        viewModel.refreshGoogleCalendarEvents(silent = false)
                                    },
                                    onMigratePendingToTomorrow = viewModel::migratePendingTodayToTomorrow,
                                    onNavigateToOAuthSetup = {
                                        viewModel.selectTab(NightPlannerTab.GOOGLE_CALENDAR_GUIDE)
                                    }
                                )
                            }

                            NightPlannerTab.TIMELINE_CONFLICTS -> {
                                TimelineAndConflictsScreen(
                                    state = state,
                                    onSelectDayFilter = viewModel::selectDayFilter,
                                    onToggleIncludeUniClasses = viewModel::toggleIncludeUniInFreeSlots,
                                    onAssignTaskToFreeSlot = viewModel::assignTaskToTimeWindow,
                                    onRefreshCalendarEvents = {
                                        viewModel.refreshGoogleCalendarEvents(silent = false)
                                    }
                                )
                            }

                            NightPlannerTab.GOOGLE_CALENDAR_GUIDE -> {
                                GoogleCalendarSetupScreen(
                                    state = state,
                                    onGeneratePkceAuthUrl = viewModel::generatePkceAuthUrl,
                                    onSaveManualOAuthToken = viewModel::saveManualOAuthToken,
                                    onUpdateClientId = viewModel::updateOAuthClientId,
                                    onUpdateUnscheduledPolicy = viewModel::updateUnscheduledPolicy,
                                    onDisconnect = viewModel::disconnectGoogleCalendar
                                )
                            }
                        }
                    }
                }
            }
        }

        state.editingTask?.let { editing ->
            EditTaskBottomSheet(
                task = editing,
                onDismiss = viewModel::dismissEditTask,
                onSave = viewModel::saveEditedTask
            )
        }
    }
}

@Composable
private fun NightPlannerTopBar(
    isCalendarConnected: Boolean,
    onBackToHub: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    Surface(
        color = NightBgDeep,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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
                            contentDescription = "Volver a Nexus",
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
                        .background(ElectricIndigoContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = "Night Planner",
                        tint = ElectricIndigoBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Night Planner",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                        Text(
                            text = "Tareas · Agenda · Calendar",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    onClick = onNavigateToCalendar,
                    shape = RoundedCornerShape(50),
                    color = if (isCalendarConnected) SyncSuccessGreen.copy(alpha = 0.12f) else NightCardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isCalendarConnected) SyncSuccessGreen.copy(alpha = 0.35f) else MinimalHairline
                    ),
                    modifier = Modifier.testTag("top_calendar_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isCalendarConnected) SyncSuccessGreen else TextMutedSlate)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCalendarConnected) "Calendar Activo" else "Vincular Calendar",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isCalendarConnected) SyncSuccessGreen else TextMutedSlate
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
private fun StatusNotificationBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val accent = if (isError) ConflictWarningRed else SyncSuccessGreen
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("status_notification_banner"),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextMoonlight
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar aviso",
                    tint = TextStarlight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
