package dev.quintero.nexus.feature.nutrition

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.quintero.nexus.core.design.ConflictWarningRed
import dev.quintero.nexus.core.design.ElectricIndigoBright
import dev.quintero.nexus.core.design.EnfoqueCoralSoft
import dev.quintero.nexus.core.design.MinimalHairline
import dev.quintero.nexus.core.design.MoonlightCyan
import dev.quintero.nexus.core.design.NightBgDeep
import dev.quintero.nexus.core.design.NightCardElevated
import dev.quintero.nexus.core.design.NightCardSurface
import dev.quintero.nexus.core.design.NocturnalAmber
import dev.quintero.nexus.core.design.NocturnalAmberContainer
import dev.quintero.nexus.core.design.SyncSuccessGreen
import dev.quintero.nexus.core.design.TextMoonlight
import dev.quintero.nexus.core.design.TextMutedSlate
import dev.quintero.nexus.core.design.TextStarlight
import dev.quintero.nexus.core.util.BackupShareHelper
import dev.quintero.nexus.core.util.DateTimeHelper
import java.util.EnumMap
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt

private data class SampleBarcodePreset(val label: String, val ean: String)

private val sampleSpanishBarcodes = listOf(
    SampleBarcodePreset("Yogur +Proteínas", "8480000105042"),
    SampleBarcodePreset("Copos de Avena", "4008713700626"),
    SampleBarcodePreset("Pavo 92%", "8480000591029"),
    SampleBarcodePreset("Atún al Natural", "8480000160126"),
    SampleBarcodePreset("Gazpacho Alvalle", "8410179100050"),
    SampleBarcodePreset("Leche Pascual", "8410066000050"),
    SampleBarcodePreset("Crema Cacahuete", "8480000174826"),
    SampleBarcodePreset("Coca-Cola Zero", "5449000131805")
)

@Composable
fun NutritionModuleScreen(
    viewModel: NutritionViewModel,
    onBackToHub: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshTodayDateIfNeeded()
    }

    var showWeightDialog by remember { mutableStateOf(false) }

    BackHandler {
        when {
            state.manualEntryMealType != null -> viewModel.dismissManualEntryDialog()
            state.isApiKeyDialogOpen -> viewModel.dismissApiKeyDialog()
            state.isBackupDialogOpen -> viewModel.dismissBackupDialog()
            showWeightDialog -> showWeightDialog = false
            state.activeTab != NutritionTab.TODAY -> viewModel.selectTab(NutritionTab.TODAY)
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
                    viewModel.showToast("Backup nutricional exportado")
                }
                .onFailure {
                    viewModel.showToast("No se pudo exportar el backup", positive = false)
                }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            BackupShareHelper.readTextFromUri(context, uri)
                .onSuccess { viewModel.importBackupJson(it) }
                .onFailure { viewModel.showToast("Error al leer el archivo JSON", positive = false) }
        }
    }

    Scaffold(
        containerColor = NightBgDeep,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            NutritionTopBar(
                activeTab = state.activeTab,
                hasGeminiKey = state.geminiApiKey.isNotBlank(),
                onBackToHub = onBackToHub,
                onSelectTab = { viewModel.selectTab(it) },
                onOpenApiKeyDialog = viewModel::openApiKeyDialog,
                onOpenBackupDialog = viewModel::openBackupDialog
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.activeTab) {
                NutritionTab.TODAY -> {
                    NutritionDailyDashboardContent(
                        state = state,
                        onPreviousDay = { viewModel.shiftSelectedDate(-1) },
                        onNextDay = { viewModel.shiftSelectedDate(1) },
                        onTodayClick = viewModel::selectToday,
                        onOpenScanner = { meal -> viewModel.selectTab(NutritionTab.SCANNER, meal) },
                        onOpenAiPhoto = { meal -> viewModel.selectTab(NutritionTab.AI_PHOTO, meal) },
                        onOpenManualAdd = { meal -> viewModel.openManualEntryDialog(meal) },
                        onDeleteMealLog = viewModel::deleteMealLog,
                        onAddWaterMl = viewModel::addWaterMl,
                        onEditWeightClick = { showWeightDialog = true }
                    )
                }

                NutritionTab.SCANNER -> {
                    NutritionBarcodeScannerContent(
                        scannerState = state.scannerState,
                        defaultMealType = state.defaultTargetMealType,
                        onLookupBarcode = viewModel::lookupBarcode,
                        onSearchByName = viewModel::searchOpenFoodFactsByName,
                        onSelectProduct = viewModel::selectProductFromSearch,
                        onResetScanner = viewModel::resetScannerState,
                        onLogProduct = { mealType, product, grams, kcal, pro, carb, fat ->
                            viewModel.logFoodItem(
                                mealType = mealType,
                                foodName = product.name,
                                brandOrSource = "${product.brand} · EAN",
                                servingGrams = grams,
                                calories = kcal,
                                proteinG = pro,
                                carbsG = carb,
                                fatG = fat,
                                barcode = product.barcode,
                                nutriScore = product.nutriScore.orEmpty(),
                                returnToTodayTab = true
                            )
                        },
                        onBackToToday = { viewModel.selectTab(NutritionTab.TODAY) }
                    )
                }

                NutritionTab.AI_PHOTO -> {
                    NutritionPlateAiContent(
                        plateAiState = state.plateAiState,
                        defaultMealType = state.defaultTargetMealType,
                        hasGeminiKey = state.geminiApiKey.isNotBlank(),
                        onOpenApiKeyDialog = viewModel::openApiKeyDialog,
                        onAnalyzePhoto = viewModel::analyzePlatePhoto,
                        onResetAiState = viewModel::resetPlateAiState,
                        onLogAnalyzedPlate = { mealType, dishName, grams, kcal, pro, carb, fat, ingredients ->
                            viewModel.logFoodItem(
                                mealType = mealType,
                                foodName = dishName,
                                brandOrSource = "Gemini IA · Foto",
                                servingGrams = grams,
                                calories = kcal,
                                proteinG = pro,
                                carbsG = carb,
                                fatG = fat,
                                ingredientsSummary = ingredients,
                                returnToTodayTab = true
                            )
                        },
                        onBackToToday = { viewModel.selectTab(NutritionTab.TODAY) }
                    )
                }

                NutritionTab.GOALS -> {
                    NutritionGoalsAndHistoryContent(
                        profile = state.profile,
                        recentMetrics = state.recentMetrics,
                        onSaveGoals = viewModel::recalculateAndSaveProfile
                    )
                }
            }

            // Toast notification banner
            AnimatedVisibility(
                visible = state.toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCardElevated,
                    border = BorderStroke(
                        1.dp,
                        if (state.toastPositive) SyncSuccessGreen.copy(alpha = 0.55f)
                        else ConflictWarningRed.copy(alpha = 0.55f)
                    )
                ) {
                    Text(
                        text = state.toastMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (state.toastPositive) SyncSuccessGreen else ConflictWarningRed,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    if (state.manualEntryMealType != null) {
        ManualFoodEntryDialog(
            initialMealType = state.manualEntryMealType!!,
            onDismiss = viewModel::dismissManualEntryDialog,
            onConfirm = { meal, name, grams, kcal, pro, carb, fat ->
                viewModel.logFoodItem(
                    mealType = meal,
                    foodName = name,
                    brandOrSource = "Manual",
                    servingGrams = grams,
                    calories = kcal,
                    proteinG = pro,
                    carbsG = carb,
                    fatG = fat,
                    returnToTodayTab = true
                )
            }
        )
    }

    if (showWeightDialog) {
        WeightUpdateDialog(
            currentWeight = state.selectedDateMetric.loggedWeightKg ?: state.profile.weightKg,
            onDismiss = { showWeightDialog = false },
            onConfirm = { newWeight ->
                viewModel.updateLoggedWeight(newWeight)
                showWeightDialog = false
            }
        )
    }

    if (state.isApiKeyDialogOpen) {
        GeminiApiKeyDialog(
            currentKey = state.geminiApiKey,
            onDismiss = viewModel::dismissApiKeyDialog,
            onSave = viewModel::saveGeminiApiKey
        )
    }

    if (state.isBackupDialogOpen) {
        NutritionBackupDialog(
            mealCount = state.allMealLogs.size,
            onDismiss = viewModel::dismissBackupDialog,
            onExportClick = {
                exportLauncher.launch("nexus_nutricion_${DateTimeHelper.todayIso()}.json")
            },
            onImportClick = {
                importLauncher.launch(arrayOf("application/json", "text/*"))
            }
        )
    }
}

@Composable
private fun NutritionTopBar(
    activeTab: NutritionTab,
    hasGeminiKey: Boolean,
    onBackToHub: () -> Unit,
    onSelectTab: (NutritionTab) -> Unit,
    onOpenApiKeyDialog: () -> Unit,
    onOpenBackupDialog: () -> Unit
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBackToHub,
                    shape = CircleShape,
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("nutrition_back_to_hub")
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
                        .background(NocturnalAmberContainer)
                        .border(1.dp, NocturnalAmber.copy(alpha = 0.32f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = NocturnalAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nutrición",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Text(
                        text = "Escáner QR/EAN · Visión IA · Macros",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = onOpenApiKeyDialog,
                    shape = CircleShape,
                    color = NightCardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (hasGeminiKey) SyncSuccessGreen.copy(alpha = 0.45f) else MinimalHairline
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("nutrition_api_key_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = "Configurar Gemini API Key",
                            tint = if (hasGeminiKey) SyncSuccessGreen else NocturnalAmber,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onOpenBackupDialog,
                    shape = CircleShape,
                    color = NightCardSurface,
                    border = BorderStroke(1.dp, MinimalHairline),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("nutrition_backup_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Backup,
                            contentDescription = "Backup JSON",
                            tint = TextStarlight,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Module Sub-navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NutritionTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    Surface(
                        onClick = { onSelectTab(tab) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) NocturnalAmber.copy(alpha = 0.18f) else NightCardSurface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) NocturnalAmber.copy(alpha = 0.55f) else MinimalHairline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nutrition_tab_${tab.name.lowercase(Locale.ROOT)}")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.labelEs,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) NocturnalAmber else TextStarlight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
private fun NutritionDailyDashboardContent(
    state: NutritionUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    onOpenScanner: (MealType) -> Unit,
    onOpenAiPhoto: (MealType) -> Unit,
    onOpenManualAdd: (MealType) -> Unit,
    onDeleteMealLog: (String) -> Unit,
    onAddWaterMl: (Int) -> Unit,
    onEditWeightClick: () -> Unit
) {
    val totalCalories = state.selectedConsumedKcal
    val totalProtein = state.selectedConsumedProteinG
    val totalCarbs = state.selectedConsumedCarbsG
    val totalFat = state.selectedConsumedFatG
    val goalCalories = state.profile.dailyCalorieGoal.coerceAtLeast(1000)
    val remainingCalories = goalCalories - totalCalories
    val isToday = state.selectedDateIso == state.todayIso
    val dateHumanLabel = if (isToday) {
        "Hoy · ${DateTimeHelper.formatShortDayMonthEs(state.selectedDateIso)}"
    } else {
        DateTimeHelper.formatFullDayShortEs(state.selectedDateIso)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Date Navigation Pill
        item {
            Surface(
                color = NightCardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousDay,
                        modifier = Modifier.testTag("nutrition_prev_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Día anterior",
                            tint = TextStarlight
                        )
                    }
                    TextButton(onClick = onTodayClick) {
                        Text(
                            text = dateHumanLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextMoonlight
                        )
                    }
                    IconButton(
                        onClick = onNextDay,
                        modifier = Modifier.testTag("nutrition_next_day")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Día siguiente",
                            tint = TextStarlight
                        )
                    }
                }
            }
        }

        // Calorie Ring & Macros Hero Card
        item {
            val activeGoalType = NutritionGoalType.fromName(state.profile.goalType)
            val activeActivity = NutritionActivityLevel.fromName(state.profile.activityLevel)
            val maintenanceTdee = NutritionDomainHelper.estimateMaintenanceTdee(
                weightKg = state.profile.weightKg,
                heightCm = state.profile.heightCm,
                ageYears = state.profile.ageYears,
                activity = activeActivity
            )
            val surplusOrDeficitDelta = goalCalories - maintenanceTdee
            CalorieAndMacrosCard(
                consumedCalories = totalCalories,
                goalCalories = goalCalories,
                remainingCalories = remainingCalories,
                goalBadgeText = activeGoalType.shortBadgeEs,
                surplusDeltaKcal = surplusOrDeficitDelta,
                maintenanceTdee = maintenanceTdee,
                proteinG = totalProtein,
                proteinGoalG = state.profile.proteinGoalG,
                carbsG = totalCarbs,
                carbsGoalG = state.profile.carbsGoalG,
                fatG = totalFat,
                fatGoalG = state.profile.fatGoalG
            )
        }

        // Fast Action Buttons (Scan Barcode/QR + AI Photo + Manual)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenScanner(MealType.LUNCH) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SyncSuccessGreen,
                        contentColor = Color(0xFF061610)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("nutrition_quick_scan_btn")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Código / QR", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onOpenAiPhoto(MealType.LUNCH) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NocturnalAmber,
                        contentColor = Color(0xFF1A1205)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("nutrition_quick_ai_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Foto con IA", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Hydration & Weight Row
        item {
            HydrationAndWeightRow(
                waterMl = state.selectedDateMetric.waterMl,
                waterGoalMl = state.profile.waterGoalMl,
                loggedWeightKg = state.selectedDateMetric.loggedWeightKg ?: state.profile.weightKg,
                targetWeightKg = state.profile.targetWeightKg,
                onAddWaterMl = onAddWaterMl,
                onEditWeightClick = onEditWeightClick
            )
        }

        // 4 Meal Slots (Desayuno, Almuerzo, Cena, Snacks)
        items(MealType.entries.size) { idx ->
            val mealType = MealType.entries[idx]
            val logsForMeal = state.selectedDateMeals.filter {
                it.mealType.equals(mealType.name, ignoreCase = true)
            }
            MealSectionCard(
                mealType = mealType,
                logs = logsForMeal,
                dailyCalorieGoal = goalCalories,
                onScanForMeal = { onOpenScanner(mealType) },
                onAiPhotoForMeal = { onOpenAiPhoto(mealType) },
                onManualAddForMeal = { onOpenManualAdd(mealType) },
                onDeleteLog = onDeleteMealLog
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CalorieAndMacrosCard(
    consumedCalories: Int,
    goalCalories: Int,
    remainingCalories: Int,
    goalBadgeText: String,
    surplusDeltaKcal: Int,
    maintenanceTdee: Int,
    proteinG: Int,
    proteinGoalG: Int,
    carbsG: Int,
    carbsGoalG: Int,
    fatG: Int,
    fatGoalG: Int
) {
    val progressTarget = (consumedCalories.toFloat() / goalCalories.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(),
        label = "nexusCalorieRing"
    )

    Surface(
        color = NightCardSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val deltaLabel = when {
                surplusDeltaKcal > 0 -> "SUPERÁVIT CALÓRICO (+$surplusDeltaKcal kcal sobre $maintenanceTdee kcal)"
                surplusDeltaKcal < 0 -> "DÉFICIT CALÓRICO ($surplusDeltaKcal kcal sobre $maintenanceTdee kcal)"
                else -> "MANTENIMIENTO ($maintenanceTdee kcal)"
            }
            Surface(
                color = if (surplusDeltaKcal > 0) NocturnalAmber.copy(alpha = 0.16f) else NightBgDeep,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (surplusDeltaKcal > 0) NocturnalAmber.copy(alpha = 0.45f) else MinimalHairline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 $deltaLabel",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (surplusDeltaKcal > 0) NocturnalAmber else SyncSuccessGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = goalBadgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextMoonlight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$consumedCalories",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Text(
                        text = "CONSUMIDAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedSlate
                    )
                }

                Box(
                    modifier = Modifier.size(136.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringColor = if (remainingCalories >= 0) NocturnalAmber else SyncSuccessGreen
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        drawArc(
                            color = MinimalHairline,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = ringColor,
                            startAngle = 135f,
                            sweepAngle = 270f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${abs(remainingCalories)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (remainingCalories >= 0) TextMoonlight else SyncSuccessGreen
                        )
                        Text(
                            text = if (remainingCalories >= 0) "POR CONSUMIR" else "META LOGRADA",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedSlate
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$goalCalories",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SyncSuccessGreen
                    )
                    Text(
                        text = "META DÍA",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroProgressBox(
                    modifier = Modifier.weight(1f),
                    label = "Proteínas",
                    currentG = proteinG,
                    goalG = proteinGoalG,
                    color = SyncSuccessGreen
                )
                MacroProgressBox(
                    modifier = Modifier.weight(1f),
                    label = "Carbos",
                    currentG = carbsG,
                    goalG = carbsGoalG,
                    color = MoonlightCyan
                )
                MacroProgressBox(
                    modifier = Modifier.weight(1f),
                    label = "Grasas",
                    currentG = fatG,
                    goalG = fatGoalG,
                    color = NocturnalAmber
                )
            }
        }
    }
}

@Composable
private fun MacroProgressBox(
    modifier: Modifier = Modifier,
    label: String,
    currentG: Int,
    goalG: Int,
    color: Color
) {
    val safeGoal = goalG.coerceAtLeast(1)
    val ratio = (currentG.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NightBgDeep)
            .border(1.dp, MinimalHairline, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMutedSlate
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "${currentG}g / ${safeGoal}g",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextMoonlight
        )
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = MinimalHairline,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
private fun HydrationAndWeightRow(
    waterMl: Int,
    waterGoalMl: Int,
    loggedWeightKg: Double,
    targetWeightKg: Double,
    onAddWaterMl: (Int) -> Unit,
    onEditWeightClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = NightCardSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MinimalHairline),
            modifier = Modifier.weight(1.25f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalDrink,
                        contentDescription = null,
                        tint = MoonlightCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hidratación",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextMoonlight
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$waterMl / $waterGoalMl ml",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MoonlightCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                val waterRatio = (waterMl.toFloat() / waterGoalMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { waterRatio },
                    color = MoonlightCyan,
                    trackColor = MinimalHairline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onAddWaterMl(-250) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("nutrition_water_minus")
                    ) {
                        Text("-250", style = MaterialTheme.typography.labelSmall, color = TextStarlight)
                    }
                    Button(
                        onClick = { onAddWaterMl(250) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoonlightCyan,
                            contentColor = Color(0xFF06131E)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("nutrition_water_plus")
                    ) {
                        Text("+250 ml", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Surface(
            color = NightCardSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MinimalHairline),
            modifier = Modifier.weight(0.95f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = SyncSuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Peso",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextMoonlight
                        )
                    }
                    IconButton(
                        onClick = onEditWeightClick,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar peso",
                            tint = TextMutedSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format(Locale.US, "%.1f", loggedWeightKg).replace('.', ',')} kg",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SyncSuccessGreen
                )
                Text(
                    text = "Meta: ${String.format(Locale.US, "%.1f", targetWeightKg).replace('.', ',')} kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedSlate
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onEditWeightClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Text("Anotar peso", style = MaterialTheme.typography.labelSmall, color = TextStarlight)
                }
            }
        }
    }
}

@Composable
private fun MealSectionCard(
    mealType: MealType,
    logs: List<NutritionMealLogEntity>,
    dailyCalorieGoal: Int,
    onScanForMeal: () -> Unit,
    onAiPhotoForMeal: () -> Unit,
    onManualAddForMeal: () -> Unit,
    onDeleteLog: (String) -> Unit
) {
    val mealKcal = logs.sumOf { it.calories }
    val recommendedKcal = (dailyCalorieGoal * mealType.targetPct).roundToInt()

    Surface(
        color = NightCardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MinimalHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${mealType.iconEmoji} ${mealType.labelEs}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Text(
                        text = "$mealKcal / $recommendedKcal kcal recomendadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mealKcal > 0) NocturnalAmber else TextMutedSlate
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onScanForMeal,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("meal_scan_${mealType.name.lowercase(Locale.ROOT)}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear código para ${mealType.labelEs}",
                            tint = SyncSuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onAiPhotoForMeal,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("meal_ai_${mealType.name.lowercase(Locale.ROOT)}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Foto IA para ${mealType.labelEs}",
                            tint = NocturnalAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onManualAddForMeal,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("meal_manual_${mealType.name.lowercase(Locale.ROOT)}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir manual a ${mealType.labelEs}",
                            tint = TextStarlight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (logs.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sin alimentos registrados. Usa el lector QR/EAN, haz una foto con IA o pulsa +.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    logs.forEach { log ->
                        val isAiLog = log.brandOrSource.contains("IA", ignoreCase = true)
                        val isBarcodeLog = log.barcode.isNotBlank() || log.brandOrSource.contains("EAN", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(NightBgDeep)
                                .border(1.dp, MinimalHairline, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Columna 1: Icono de origen alineado
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isAiLog -> NocturnalAmber.copy(alpha = 0.16f)
                                            isBarcodeLog -> SyncSuccessGreen.copy(alpha = 0.16f)
                                            else -> NightCardSurface
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isAiLog -> NocturnalAmber.copy(alpha = 0.4f)
                                            isBarcodeLog -> SyncSuccessGreen.copy(alpha = 0.4f)
                                            else -> MinimalHairline
                                        },
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isAiLog -> Icons.Default.AutoAwesome
                                        isBarcodeLog -> Icons.Default.QrCodeScanner
                                        else -> Icons.Default.Restaurant
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isAiLog -> NocturnalAmber
                                        isBarcodeLog -> SyncSuccessGreen
                                        else -> TextStarlight
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Columna 2: Nombre + Metadatos + Macros segmentados
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.foodName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextMoonlight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${log.brandOrSource} · ${log.servingGrams} g",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedSlate,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (log.nutriScore.isNotBlank()) {
                                        NutriScoreBadge(grade = log.nutriScore)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "P: ${log.proteinG}g",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = SyncSuccessGreen
                                    )
                                    Text(
                                        text = "C: ${log.carbsG}g",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MoonlightCyan
                                    )
                                    Text(
                                        text = "G: ${log.fatG}g",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = NocturnalAmber
                                    )
                                }
                                if (log.ingredientsSummary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Contexto IA: ${log.ingredientsSummary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedSlate,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Columna 3: Calorías y borrado alineados a la derecha
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${log.calories}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SyncSuccessGreen
                                )
                                Text(
                                    text = "kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onDeleteLog(log.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Eliminar ${log.foodName}",
                                    tint = TextMutedSlate,
                                    modifier = Modifier.size(17.dp)
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
private fun NutritionBarcodeScannerContent(
    scannerState: ScannerUiState,
    defaultMealType: MealType,
    onLookupBarcode: (String) -> Unit,
    onSearchByName: (String) -> Unit,
    onSelectProduct: (ScannedProductInfo) -> Unit,
    onResetScanner: () -> Unit,
    onLogProduct: (
        mealType: MealType,
        product: ScannedProductInfo,
        grams: Int,
        kcal: Int,
        protein: Int,
        carbs: Int,
        fat: Int
    ) -> Unit,
    onBackToToday: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraActive by remember { mutableStateOf(hasCameraPermission) }
    var manualBarcodeInput by remember { mutableStateOf("") }
    var textSearchQuery by remember { mutableStateOf("") }
    var selectedMealType by remember(defaultMealType) { mutableStateOf(defaultMealType) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        isCameraActive = granted
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Camera & Barcode Reader Card
        item {
            Surface(
                color = NightCardSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = SyncSuccessGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Lector de Código de Barras y QR",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextMoonlight
                                )
                                Text(
                                    text = "Open Food Facts API + Catálogo Español Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SyncSuccessGreen
                                )
                            }
                        }
                        if (hasCameraPermission) {
                            OutlinedButton(
                                onClick = { isCameraActive = !isCameraActive },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = if (isCameraActive) "Pausar" else "Activar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextStarlight
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (hasCameraPermission && isCameraActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, SyncSuccessGreen, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraBarcodePreview(
                                onBarcodeDetected = { code ->
                                    manualBarcodeInput = code
                                    onLookupBarcode(code)
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(90.dp)
                                    .border(
                                        width = 2.dp,
                                        color = NocturnalAmber.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            )
                            Surface(
                                color = NightBgDeep.copy(alpha = 0.82f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Apunta al código EAN-13 o QR del envase",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SyncSuccessGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(NightBgDeep)
                                .border(1.dp, MinimalHairline, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!hasCameraPermission) "Escaneo directo con cámara" else "Cámara en pausa",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextMoonlight
                                )
                                Text(
                                    text = "Activa la cámara o selecciona un código debajo.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    if (!hasCameraPermission) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        isCameraActive = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SyncSuccessGreen,
                                    contentColor = Color(0xFF061610)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cámara", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcodeInput,
                            onValueChange = { manualBarcodeInput = it },
                            label = { Text("Código EAN-13 / QR") },
                            placeholder = { Text("Ej: 8480000105042") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { onLookupBarcode(manualBarcodeInput) }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nutrition_barcode_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onLookupBarcode(manualBarcodeInput) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SyncSuccessGreen,
                                contentColor = Color(0xFF061610)
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("nutrition_lookup_barcode_btn")
                        ) {
                            Text("Consultar", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Acceso rápido a productos habituales:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleSpanishBarcodes.forEach { sample ->
                            FilterChip(
                                selected = manualBarcodeInput == sample.ean,
                                onClick = {
                                    manualBarcodeInput = sample.ean
                                    onLookupBarcode(sample.ean)
                                },
                                label = { Text(sample.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NocturnalAmber.copy(alpha = 0.22f),
                                    selectedLabelColor = NocturnalAmber
                                )
                            )
                        }
                    }
                }
            }
        }

        // Search by Name Card
        item {
            Surface(
                color = NightCardSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Buscador por Nombre (Supermercados)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextMoonlight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textSearchQuery,
                            onValueChange = { textSearchQuery = it },
                            label = { Text("Buscar alimento o marca") },
                            placeholder = { Text("Ej: yogur, avena, pavo, atún...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { onSearchByName(textSearchQuery) }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nutrition_search_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSearchByName(textSearchQuery) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NocturnalAmber,
                                contentColor = Color(0xFF1A1205)
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("nutrition_search_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                }
            }
        }

        // Dynamic Result Card
        item {
            when (scannerState) {
                is ScannerUiState.Idle -> Unit

                is ScannerUiState.Loading -> {
                    Surface(
                        color = NightCardSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MinimalHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = SyncSuccessGreen,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = scannerState.statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMoonlight
                            )
                        }
                    }
                }

                is ScannerUiState.Error -> {
                    Surface(
                        color = ConflictWarningRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = scannerState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ConflictWarningRed
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = onResetScanner) {
                                Text("Limpiar", color = TextStarlight)
                            }
                        }
                    }
                }

                is ScannerUiState.SearchResults -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Resultados encontrados (${scannerState.products.size}):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SyncSuccessGreen
                        )
                        scannerState.products.forEach { item ->
                            Surface(
                                color = NightCardSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MinimalHairline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectProduct(item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextMoonlight
                                        )
                                        Text(
                                            text = "${item.brand} · ${item.caloriesPer100g} kcal/100g (P:${item.proteinPer100g}g C:${item.carbsPer100g}g G:${item.fatPer100g}g)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMutedSlate
                                        )
                                    }
                                    if (item.nutriScore != null) {
                                        NutriScoreBadge(grade = item.nutriScore)
                                    }
                                }
                            }
                        }
                    }
                }

                is ScannerUiState.ProductFound -> {
                    val product = scannerState.product
                    var gramsSlider by remember(product.barcode) {
                        mutableFloatStateOf(product.servingGramsDefault.toFloat().coerceIn(10f, 600f))
                    }
                    val grams = gramsSlider.roundToInt().coerceAtLeast(1)
                    val (calcKcal, calcProtein, calcCarbs, calcFat) =
                        NutritionDomainHelper.scaleProductForGrams(product, grams)

                    Surface(
                        color = NightCardSurface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.5.dp, SyncSuccessGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PRODUCTO IDENTIFICADO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = SyncSuccessGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextMoonlight
                                    )
                                    Text(
                                        text = "${product.brand} · Código: ${product.barcode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedSlate
                                    )
                                }
                                if (product.nutriScore != null) {
                                    NutriScoreBadge(grade = product.nutriScore)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Porción a registrar:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextStarlight
                                )
                                Text(
                                    text = "$grams g",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = NocturnalAmber
                                )
                            }
                            Slider(
                                value = gramsSlider,
                                onValueChange = { gramsSlider = it },
                                valueRange = 10f..600f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NocturnalAmber,
                                    activeTrackColor = NocturnalAmber
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(30, 60, 100, 150, 250).forEach { preset ->
                                    FilterChip(
                                        selected = grams == preset,
                                        onClick = { gramsSlider = preset.toFloat() },
                                        label = { Text("${preset}g") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NightBgDeep)
                                    .border(1.dp, MinimalHairline, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                MacroStatPill("KCAL", "$calcKcal", NocturnalAmber)
                                MacroStatPill("PROTEÍNA", "${calcProtein}g", SyncSuccessGreen)
                                MacroStatPill("CARBOS", "${calcCarbs}g", MoonlightCyan)
                                MacroStatPill("GRASAS", "${calcFat}g", EnfoqueCoralSoft)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Guardar en comida:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextStarlight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MealType.entries.forEach { meal ->
                                    FilterChip(
                                        selected = selectedMealType == meal,
                                        onClick = { selectedMealType = meal },
                                        label = { Text("${meal.iconEmoji} ${meal.labelEs}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NocturnalAmber.copy(alpha = 0.25f),
                                            selectedLabelColor = NocturnalAmber
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    onLogProduct(
                                        selectedMealType,
                                        product,
                                        grams,
                                        calcKcal,
                                        calcProtein,
                                        calcCarbs,
                                        calcFat
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SyncSuccessGreen,
                                    contentColor = Color(0xFF061610)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("confirm_log_scanned_product")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Registrar en ${selectedMealType.labelEs} ($calcKcal kcal)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutritionPlateAiContent(
    plateAiState: PlateAiUiState,
    defaultMealType: MealType,
    hasGeminiKey: Boolean,
    onOpenApiKeyDialog: () -> Unit,
    onAnalyzePhoto: (Bitmap?, String) -> Unit,
    onResetAiState: () -> Unit,
    onLogAnalyzedPlate: (
        mealType: MealType,
        dishName: String,
        grams: Int,
        kcal: Int,
        protein: Int,
        carbs: Int,
        fat: Int,
        ingredientsSummary: String
    ) -> Unit,
    onBackToToday: () -> Unit
) {
    val context = LocalContext.current
    var userHint by remember { mutableStateOf("") }
    var useDescriptionContext by remember { mutableStateOf(true) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedMealType by remember(defaultMealType) { mutableStateOf(defaultMealType) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            val effectiveHint = if (useDescriptionContext) userHint else ""
            onAnalyzePhoto(bitmap, effectiveHint)
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bmp != null) {
                    capturedBitmap = bmp
                    val effectiveHint = if (useDescriptionContext) userHint else ""
                    onAnalyzePhoto(bmp, effectiveHint)
                }
            } catch (_: Exception) {
                // Ignore decode failure
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = NightCardSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reconocimiento Visual de Platos con IA",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextMoonlight
                            )
                            Text(
                                text = if (hasGeminiKey) {
                                    "Gemini 2.5 Flash Multimodal conectado (API Key activa)"
                                } else {
                                    "Modo Estimación Inteligente · Pulsa Configurar API para usar tu clave de AI Studio"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasGeminiKey) SyncSuccessGreen else NocturnalAmber
                            )
                        }
                        TextButton(onClick = onOpenApiKeyDialog) {
                            Text(
                                text = if (hasGeminiKey) "API Key ✓" else "Configurar API",
                                color = if (hasGeminiKey) SyncSuccessGreen else NocturnalAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selector de modo: 1) Solo Foto vs 2) Foto + Descripción
                    Text(
                        text = "Modo de análisis IA:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextStarlight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !useDescriptionContext,
                            onClick = { useDescriptionContext = false },
                            label = { Text("📷 Solo Foto") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SyncSuccessGreen.copy(alpha = 0.22f),
                                selectedLabelColor = SyncSuccessGreen
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = useDescriptionContext,
                            onClick = { useDescriptionContext = true },
                            label = { Text("📝 Foto + Descripción") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NocturnalAmber.copy(alpha = 0.25f),
                                selectedLabelColor = NocturnalAmber
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (useDescriptionContext) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userHint,
                            onValueChange = { userHint = it },
                            label = { Text("Contexto para la IA (cantidades, cocinado, aceite...)") },
                            placeholder = { Text("Ej: Son 2 pechugas de pollo a la plancha con poco aceite y arroz") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_plate_hint_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { takePictureLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NocturnalAmber,
                                contentColor = Color(0xFF1A1205)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("take_plate_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (useDescriptionContext) "Foto + Contexto" else "Hacer Foto",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_gallery_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galería", color = TextStarlight)
                        }
                    }

                    if (useDescriptionContext && userHint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onAnalyzePhoto(capturedBitmap, userHint) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NocturnalAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NocturnalAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recalcular con esta descripción",
                                color = NocturnalAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ejemplos rápidos (con contexto de preparación):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sampleDishes = listOf(
                            "2 Pechugas de pollo a la plancha con arroz",
                            "Pollo con arroz basmati y brócoli",
                            "Poke de salmón, quinoa y aguacate",
                            "Bowl de avena, proteína y plátano",
                            "Tortilla de 3 huevos con tostada integral",
                            "Pasta integral boloñesa magra"
                        )
                        sampleDishes.forEach { dish ->
                            FilterChip(
                                selected = userHint == dish,
                                onClick = {
                                    userHint = dish
                                    useDescriptionContext = true
                                    onAnalyzePhoto(capturedBitmap, dish)
                                },
                                label = { Text(dish) }
                            )
                        }
                    }
                }
            }
        }

        item {
            when (plateAiState) {
                is PlateAiUiState.Idle -> Unit

                is PlateAiUiState.Analyzing -> {
                    Surface(
                        color = NightCardSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, NocturnalAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (plateAiState.bitmap != null) {
                                Image(
                                    bitmap = plateAiState.bitmap.asImageBitmap(),
                                    contentDescription = "Analizando plato",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            CircularProgressIndicator(color = NocturnalAmber)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Identificando ingredientes, pesos en gramos y macronutrientes...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NocturnalAmber
                            )
                        }
                    }
                }

                is PlateAiUiState.Error -> {
                    Surface(
                        color = ConflictWarningRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ConflictWarningRed.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = plateAiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ConflictWarningRed
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAnalyzePhoto(plateAiState.bitmap, userHint) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NocturnalAmber,
                                        contentColor = Color(0xFF1A1205)
                                    )
                                ) {
                                    Text("Reintentar")
                                }
                                OutlinedButton(onClick = onResetAiState) {
                                    Text("Limpiar", color = TextStarlight)
                                }
                            }
                        }
                    }
                }

                is PlateAiUiState.Analyzed -> {
                    val result = plateAiState.result
                    var dishNameInput by remember(result) { mutableStateOf(result.dishName) }
                    var gramsInput by remember(result) { mutableStateOf(result.estimatedTotalGrams.toString()) }
                    var kcalInput by remember(result) { mutableStateOf(result.totalCalories.toString()) }
                    var proteinInput by remember(result) { mutableStateOf(result.proteinGrams.toString()) }
                    var carbsInput by remember(result) { mutableStateOf(result.carbsGrams.toString()) }
                    var fatInput by remember(result) { mutableStateOf(result.fatGrams.toString()) }

                    Surface(
                        color = NightCardSurface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.5.dp, NocturnalAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NocturnalAmber
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (result.usedLiveGeminiApi) {
                                        "ANÁLISIS GEMINI FLASH MULTIMODAL"
                                    } else {
                                        "DESGLOSE NUTRICIONAL ESTIMADO"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NocturnalAmber
                                )
                            }

                            if (plateAiState.bitmap != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Image(
                                    bitmap = plateAiState.bitmap.asImageBitmap(),
                                    contentDescription = "Foto analizada",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = dishNameInput,
                                onValueChange = { dishNameInput = it },
                                label = { Text("Nombre del plato") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Ingredientes identificados:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextStarlight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.detectedIngredients.forEach { item ->
                                    Surface(
                                        color = NightBgDeep,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MinimalHairline)
                                    ) {
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SyncSuccessGreen,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = result.confidenceNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedSlate
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = gramsInput,
                                    onValueChange = { gramsInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Gramos") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = kcalInput,
                                    onValueChange = { kcalInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Kcal") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = proteinInput,
                                    onValueChange = { proteinInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Prot (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = carbsInput,
                                    onValueChange = { carbsInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Carbos (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = fatInput,
                                    onValueChange = { fatInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Grasas (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MealType.entries.forEach { meal ->
                                    FilterChip(
                                        selected = selectedMealType == meal,
                                        onClick = { selectedMealType = meal },
                                        label = { Text("${meal.iconEmoji} ${meal.labelEs}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NocturnalAmber.copy(alpha = 0.25f),
                                            selectedLabelColor = NocturnalAmber
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    onLogAnalyzedPlate(
                                        selectedMealType,
                                        dishNameInput.ifBlank { "Plato IA" },
                                        gramsInput.toIntOrNull() ?: result.estimatedTotalGrams,
                                        kcalInput.toIntOrNull() ?: result.totalCalories,
                                        proteinInput.toIntOrNull() ?: result.proteinGrams,
                                        carbsInput.toIntOrNull() ?: result.carbsGrams,
                                        fatInput.toIntOrNull() ?: result.fatGrams,
                                        result.detectedIngredients.joinToString(", ")
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SyncSuccessGreen,
                                    contentColor = Color(0xFF061610)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("confirm_log_ai_plate")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Guardar Plato en ${selectedMealType.labelEs}",
                                    fontWeight = FontWeight.Bold
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
private fun NutritionGoalsAndHistoryContent(
    profile: NutritionProfileEntity,
    recentMetrics: List<NutritionDailyMetricEntity>,
    onSaveGoals: (
        weightKg: Double,
        targetWeightKg: Double,
        heightCm: Int,
        ageYears: Int,
        activity: NutritionActivityLevel,
        goal: NutritionGoalType,
        customDailyCalories: Int?
    ) -> Unit
) {
    var weightText by remember(profile.weightKg) {
        mutableStateOf(String.format(Locale.US, "%.1f", profile.weightKg))
    }
    var targetWeightText by remember(profile.targetWeightKg) {
        mutableStateOf(String.format(Locale.US, "%.1f", profile.targetWeightKg))
    }
    var heightText by remember(profile.heightCm) {
        mutableStateOf(profile.heightCm.toString())
    }
    var ageText by remember(profile.ageYears) {
        mutableStateOf(profile.ageYears.toString())
    }
    var selectedActivity by remember(profile.activityLevel) {
        mutableStateOf(NutritionActivityLevel.fromName(profile.activityLevel))
    }
    var selectedGoal by remember(profile.goalType) {
        mutableStateOf(NutritionGoalType.fromName(profile.goalType))
    }
    var customKcalText by remember(profile.dailyCalorieGoal) {
        mutableStateOf(profile.dailyCalorieGoal.toString())
    }

    val currentWeightVal = weightText.replace(',', '.').toDoubleOrNull() ?: profile.weightKg
    val currentHeightVal = heightText.toIntOrNull() ?: profile.heightCm
    val currentAgeVal = ageText.toIntOrNull() ?: profile.ageYears
    val maintenanceTdee = remember(currentWeightVal, currentHeightVal, currentAgeVal, selectedActivity) {
        NutritionDomainHelper.estimateMaintenanceTdee(
            weightKg = currentWeightVal,
            heightCm = currentHeightVal,
            ageYears = currentAgeVal,
            activity = selectedActivity
        )
    }

    val previewProfile = remember(
        weightText,
        targetWeightText,
        heightText,
        ageText,
        selectedActivity,
        selectedGoal,
        customKcalText
    ) {
        val customParsed = customKcalText.trim().toIntOrNull()?.takeIf { it in 1200..6500 }
        NutritionDomainHelper.calculateTargets(
            weightKg = currentWeightVal,
            targetWeightKg = targetWeightText.replace(',', '.').toDoubleOrNull() ?: profile.targetWeightKg,
            heightCm = currentHeightVal,
            ageYears = currentAgeVal,
            activity = selectedActivity,
            goal = selectedGoal,
            customDailyCalories = customParsed
        )
    }

    val activeSurplusDelta = previewProfile.dailyCalorieGoal - maintenanceTdee

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = NightCardSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = NocturnalAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Metas y Superávit Calórico (Mifflin-St Jeor)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextMoonlight
                            )
                            Text(
                                text = "Calcula tu gasto de mantenimiento + superávit o fija tus calorías estipuladas",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Peso actual (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetWeightText,
                            onValueChange = { targetWeightText = it },
                            label = { Text("Peso meta (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Altura (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Edad (años)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Nivel de actividad:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextStarlight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NutritionActivityLevel.entries.forEach { level ->
                            FilterChip(
                                selected = selectedActivity == level,
                                onClick = {
                                    selectedActivity = level
                                    val newTdee = NutritionDomainHelper.estimateMaintenanceTdee(
                                        weightKg = currentWeightVal,
                                        heightCm = currentHeightVal,
                                        ageYears = currentAgeVal,
                                        activity = level
                                    )
                                    customKcalText = (newTdee + selectedGoal.calorieDelta).toString()
                                },
                                label = { Text(level.labelEs) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SyncSuccessGreen.copy(alpha = 0.22f),
                                    selectedLabelColor = SyncSuccessGreen
                                )
                            )
                        }
                    }
                    Text(
                        text = selectedActivity.descriptionEs,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Fase / Objetivo (Superávit, Mantenimiento o Definición):",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextStarlight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NutritionGoalType.entries.forEach { goal ->
                            FilterChip(
                                selected = selectedGoal == goal,
                                onClick = {
                                    selectedGoal = goal
                                    customKcalText = (maintenanceTdee + goal.calorieDelta).toString()
                                },
                                label = { Text(goal.labelEs) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NocturnalAmber.copy(alpha = 0.25f),
                                    selectedLabelColor = NocturnalAmber
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customKcalText,
                        onValueChange = { customKcalText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Calorías diarias estipuladas (ej: 3150 kcal en Superávit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_calorie_goal_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Resumen de Mantenimiento vs Superávit
                    Surface(
                        color = NightBgDeep,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MinimalHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mantenimiento (TDEE): $maintenanceTdee kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedSlate
                                )
                                Text(
                                    text = if (activeSurplusDelta >= 0) {
                                        "Superávit aplicado: +$activeSurplusDelta kcal/día (Volumen)"
                                    } else {
                                        "Déficit aplicado: $activeSurplusDelta kcal/día (Definición)"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (activeSurplusDelta >= 0) NocturnalAmber else SyncSuccessGreen
                                )
                            }
                            Text(
                                text = "${previewProfile.dailyCalorieGoal} kcal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SyncSuccessGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NightBgDeep)
                            .border(1.dp, MinimalHairline, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MacroStatPill("META KCAL", "${previewProfile.dailyCalorieGoal}", NocturnalAmber)
                        MacroStatPill("PROTEÍNA", "${previewProfile.proteinGoalG}g", SyncSuccessGreen)
                        MacroStatPill("CARBOS", "${previewProfile.carbsGoalG}g", MoonlightCyan)
                        MacroStatPill("GRASAS", "${previewProfile.fatGoalG}g", EnfoqueCoralSoft)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onSaveGoals(
                                currentWeightVal,
                                targetWeightText.replace(',', '.').toDoubleOrNull() ?: profile.targetWeightKg,
                                currentHeightVal,
                                currentAgeVal,
                                selectedActivity,
                                selectedGoal,
                                customKcalText.trim().toIntOrNull()?.takeIf { it in 1200..6500 }
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocturnalAmber,
                            contentColor = Color(0xFF1A1205)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_nutrition_goals_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Objetivos en Nexus", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Surface(
                color = NightCardSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MinimalHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = SyncSuccessGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Historial de Hidratación y Peso Diario",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextMoonlight
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (recentMetrics.isEmpty()) {
                        Text(
                            text = "Registra agua o peso en la pestaña Diario para ver tu evolución.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedSlate
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentMetrics.forEach { metric ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NightBgDeep)
                                        .border(1.dp, MinimalHairline, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = metric.dateIso,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextMoonlight
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        if (metric.loggedWeightKg != null) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", metric.loggedWeightKg).replace('.', ',')} kg",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = SyncSuccessGreen
                                            )
                                        }
                                        Text(
                                            text = "${metric.waterMl} ml agua",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MoonlightCyan
                                        )
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
fun NutriScoreBadge(grade: String) {
    val bgColor = when (grade.uppercase(Locale.ROOT)) {
        "A" -> Color(0xFF038141)
        "B" -> Color(0xFF85BB2F)
        "C" -> Color(0xFFFECB02)
        "D" -> Color(0xFFEE8100)
        else -> Color(0xFFE63E11)
    }
    val textColor = if (grade.uppercase(Locale.ROOT) == "C") Color.Black else Color.White
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "Nutri-Score ${grade.uppercase(Locale.ROOT)}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MacroStatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMutedSlate
        )
    }
}

@Composable
private fun ManualFoodEntryDialog(
    initialMealType: MealType,
    onDismiss: () -> Unit,
    onConfirm: (MealType, String, Int, Int, Int, Int, Int) -> Unit
) {
    var selectedMeal by remember { mutableStateOf(initialMealType) }
    var foodName by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("150") }
    var calories by remember { mutableStateOf("250") }
    var protein by remember { mutableStateOf("20") }
    var carbs by remember { mutableStateOf("25") }
    var fat by remember { mutableStateOf("8") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightCardSurface,
        titleContentColor = TextMoonlight,
        textContentColor = TextStarlight,
        title = { Text("Añadir Alimento Manual", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = selectedMeal == meal,
                            onClick = { selectedMeal = meal },
                            label = { Text(meal.labelEs) }
                        )
                    }
                }
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Alimento (ej: Tortilla francesa)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grams,
                        onValueChange = { grams = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Gramos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Kcal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Prot (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Carbos (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Grasa (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedMeal,
                        foodName.ifBlank { "Alimento manual" },
                        grams.toIntOrNull() ?: 100,
                        calories.toIntOrNull() ?: 0,
                        protein.toIntOrNull() ?: 0,
                        carbs.toIntOrNull() ?: 0,
                        fat.toIntOrNull() ?: 0
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SyncSuccessGreen,
                    contentColor = Color(0xFF061610)
                )
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextStarlight)
            }
        }
    )
}

@Composable
private fun WeightUpdateDialog(
    currentWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var weightInput by remember {
        mutableStateOf(String.format(Locale.US, "%.1f", currentWeight))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightCardSurface,
        titleContentColor = TextMoonlight,
        textContentColor = TextStarlight,
        title = { Text("Registrar Peso del Día", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text("Peso corporal (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = weightInput.replace(',', '.').toDoubleOrNull()
                    if (parsed != null && parsed > 25.0) {
                        onConfirm(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SyncSuccessGreen,
                    contentColor = Color(0xFF061610)
                )
            ) {
                Text("Actualizar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextStarlight)
            }
        }
    )
}

@Composable
private fun GeminiApiKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var keyInput by remember(currentKey) { mutableStateOf(currentKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightCardSurface,
        titleContentColor = TextMoonlight,
        textContentColor = TextStarlight,
        title = { Text("Google AI Studio (Gemini API Key)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Introduce tu API Key gratuita de Google AI Studio (aistudio.google.com) para analizar fotografías reales con Gemini 2.5 Flash Multimodal. Se guarda únicamente en tu dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Gemini API Key (AIza...)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NocturnalAmber,
                        unfocusedBorderColor = MinimalHairline,
                        focusedTextColor = TextMoonlight,
                        unfocusedTextColor = TextMoonlight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyInput) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NocturnalAmber,
                    contentColor = Color(0xFF1A1205)
                )
            ) {
                Text("Guardar clave", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextStarlight)
            }
        }
    )
}

@Composable
private fun NutritionBackupDialog(
    mealCount: Int,
    onDismiss: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightCardSurface,
        titleContentColor = TextMoonlight,
        textContentColor = TextStarlight,
        title = { Text("Backup JSON de Nutrición", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Exporta o restaura tus registros de comidas ($mealCount guardados), métricas de agua y perfil de macros en formato JSON.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSlate
                )
                Button(
                    onClick = onExportClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SyncSuccessGreen,
                        contentColor = Color(0xFF061610)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exportar JSON", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Importar JSON", color = TextStarlight)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextStarlight)
            }
        }
    )
}

@Composable
private fun CameraBarcodePreview(
    onBarcodeDetected: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val reader = remember {
        MultiFormatReader().apply {
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
            hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128
            )
            setHints(hints)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                decodeBarcodeFromImageProxy(imageProxy, reader) { detectedCode ->
                                    previewView.post {
                                        onBarcodeDetected(detectedCode)
                                    }
                                }
                            }
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("NutritionScanner", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun decodeBarcodeFromImageProxy(
    imageProxy: ImageProxy,
    reader: MultiFormatReader,
    onFound: (String) -> Unit
) {
    try {
        val plane = imageProxy.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        val width = imageProxy.width
        val height = imageProxy.height
        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result = reader.decodeWithState(bitmap)
        val text = result.text
        if (!text.isNullOrBlank()) {
            onFound(text)
        }
    } catch (_: Exception) {
        // Frame did not contain a barcode
    } finally {
        reader.reset()
        imageProxy.close()
    }
}
