package dev.quintero.nexus.feature.nutrition

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.quintero.nexus.core.database.NexusDatabase
import dev.quintero.nexus.core.util.DateTimeHelper
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
import java.util.Locale
import java.util.UUID

enum class NutritionTab(val labelEs: String) {
    TODAY("Diario"),
    SCANNER("Código / QR"),
    AI_PHOTO("Foto IA"),
    GOALS("Metas")
}

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data class Loading(val statusMessage: String) : ScannerUiState
    data class ProductFound(val product: ScannedProductInfo) : ScannerUiState
    data class SearchResults(val query: String, val products: List<ScannedProductInfo>) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

sealed interface PlateAiUiState {
    data object Idle : PlateAiUiState
    data class Analyzing(val bitmap: Bitmap?, val hint: String) : PlateAiUiState
    data class Analyzed(val bitmap: Bitmap?, val result: AiPlateAnalysisResult) : PlateAiUiState
    data class Error(val message: String, val bitmap: Bitmap? = null) : PlateAiUiState
}

data class NutritionUiState(
    val isLoading: Boolean = true,
    val activeTab: NutritionTab = NutritionTab.TODAY,
    val selectedDateIso: String = DateTimeHelper.todayIso(),
    val todayIso: String = DateTimeHelper.todayIso(),
    val defaultTargetMealType: MealType = MealType.LUNCH,
    val profile: NutritionProfileEntity = NutritionProfileEntity(),
    val todayMeals: List<NutritionMealLogEntity> = emptyList(),
    val todayMetric: NutritionDailyMetricEntity = NutritionDailyMetricEntity(dateIso = DateTimeHelper.todayIso()),
    val selectedDateMeals: List<NutritionMealLogEntity> = emptyList(),
    val selectedDateMetric: NutritionDailyMetricEntity = NutritionDailyMetricEntity(dateIso = DateTimeHelper.todayIso()),
    val recentMetrics: List<NutritionDailyMetricEntity> = emptyList(),
    val allMealLogs: List<NutritionMealLogEntity> = emptyList(),
    val allDailyMetrics: List<NutritionDailyMetricEntity> = emptyList(),
    val scannerState: ScannerUiState = ScannerUiState.Idle,
    val plateAiState: PlateAiUiState = PlateAiUiState.Idle,
    val geminiApiKey: String = "",
    val isApiKeyDialogOpen: Boolean = false,
    val isBackupDialogOpen: Boolean = false,
    val manualEntryMealType: MealType? = null,
    val toastMessage: String? = null,
    val toastPositive: Boolean = true
) {
    val todayConsumedKcal: Int
        get() = todayMeals.sumOf { it.calories }

    val todayConsumedProteinG: Int
        get() = todayMeals.sumOf { it.proteinG }

    val selectedConsumedKcal: Int
        get() = selectedDateMeals.sumOf { it.calories }

    val selectedConsumedProteinG: Int
        get() = selectedDateMeals.sumOf { it.proteinG }

    val selectedConsumedCarbsG: Int
        get() = selectedDateMeals.sumOf { it.carbsG }

    val selectedConsumedFatG: Int
        get() = selectedDateMeals.sumOf { it.fatG }
}

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NexusDatabase.getInstance(application)
    private val repository = NutritionRepository(application, db.nutritionDao())
    private val openFoodFactsClient = OpenFoodFactsClient()
    private val plateAiAnalyzer = GeminiPlateAiAnalyzer()

    private val _uiState = MutableStateFlow(
        NutritionUiState(geminiApiKey = repository.getGeminiApiKey())
    )
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    private val selectedDateFlow = MutableStateFlow(DateTimeHelper.todayIso())
    private val todayIsoFlow = MutableStateFlow(DateTimeHelper.todayIso())
    private var toastJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureSeededIfEmpty()
        }

        viewModelScope.launch {
            combine(
                repository.profileFlow,
                repository.allMealLogsFlow,
                repository.allDailyMetricsFlow,
                selectedDateFlow,
                todayIsoFlow
            ) { profileOrNull, mealLogs, dailyMetrics, selectedDate, todayDate ->
                val profile = profileOrNull ?: NutritionProfileEntity()
                val mealsForSelected = mealLogs.filter { it.dateIso == selectedDate }
                val mealsForToday = mealLogs.filter { it.dateIso == todayDate }
                val metricForSelected = dailyMetrics.firstOrNull { it.dateIso == selectedDate }
                    ?: NutritionDailyMetricEntity(
                        dateIso = selectedDate,
                        waterMl = 0,
                        loggedWeightKg = profile.weightKg
                    )
                val metricForToday = dailyMetrics.firstOrNull { it.dateIso == todayDate }
                    ?: NutritionDailyMetricEntity(
                        dateIso = todayDate,
                        waterMl = 0,
                        loggedWeightKg = profile.weightKg
                    )

                _uiState.value.copy(
                    isLoading = false,
                    selectedDateIso = selectedDate,
                    todayIso = todayDate,
                    profile = profile,
                    todayMeals = mealsForToday,
                    todayMetric = metricForToday,
                    selectedDateMeals = mealsForSelected,
                    selectedDateMetric = metricForSelected,
                    recentMetrics = dailyMetrics.take(14),
                    allMealLogs = mealLogs,
                    allDailyMetrics = dailyMetrics
                )
            }.collect { combinedState ->
                _uiState.value = combinedState
            }
        }
    }

    fun refreshTodayDateIfNeeded() {
        val current = DateTimeHelper.todayIso()
        if (todayIsoFlow.value != current) {
            val wasOnToday = selectedDateFlow.value == todayIsoFlow.value
            todayIsoFlow.value = current
            if (wasOnToday) {
                selectedDateFlow.value = current
            }
        }
    }

    fun selectTab(tab: NutritionTab, targetMeal: MealType? = null) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                defaultTargetMealType = targetMeal ?: it.defaultTargetMealType
            )
        }
    }

    fun shiftSelectedDate(daysDelta: Int) {
        val nextDate = DateTimeHelper.addDaysToIso(selectedDateFlow.value, daysDelta)
        selectedDateFlow.value = nextDate
    }

    fun selectToday() {
        val today = DateTimeHelper.todayIso()
        todayIsoFlow.value = today
        selectedDateFlow.value = today
    }

    fun openManualEntryDialog(mealType: MealType) {
        _uiState.update { it.copy(manualEntryMealType = mealType) }
    }

    fun dismissManualEntryDialog() {
        _uiState.update { it.copy(manualEntryMealType = null) }
    }

    fun openApiKeyDialog() {
        _uiState.update {
            it.copy(
                isApiKeyDialogOpen = true,
                geminiApiKey = repository.getGeminiApiKey()
            )
        }
    }

    fun dismissApiKeyDialog() {
        _uiState.update { it.copy(isApiKeyDialogOpen = false) }
    }

    fun saveGeminiApiKey(newKey: String) {
        val cleaned = newKey.trim()
        repository.saveGeminiApiKey(cleaned)
        _uiState.update {
            it.copy(
                geminiApiKey = cleaned,
                isApiKeyDialogOpen = false
            )
        }
        if (cleaned.isNotEmpty()) {
            showToast("API Key de Gemini guardada en este dispositivo")
        } else {
            showToast("Modo estimación inteligente local activado")
        }
    }

    fun openBackupDialog() {
        _uiState.update { it.copy(isBackupDialogOpen = true) }
    }

    fun dismissBackupDialog() {
        _uiState.update { it.copy(isBackupDialogOpen = false) }
    }

    fun buildExportJson(): String {
        val st = _uiState.value
        return repository.exportBackupJson(
            profile = st.profile,
            mealLogs = st.allMealLogs,
            dailyMetrics = st.allDailyMetrics
        )
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.importBackupJson(rawJson)
            withContext(Dispatchers.Main) {
                res.onSuccess { count ->
                    _uiState.update { it.copy(isBackupDialogOpen = false) }
                    showToast("Backup restaurado ($count registros)")
                }.onFailure { err ->
                    showToast(err.message ?: "Error al importar JSON", positive = false)
                }
            }
        }
    }

    fun logFoodItem(
        mealType: MealType,
        foodName: String,
        brandOrSource: String,
        servingGrams: Int,
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        barcode: String = "",
        ingredientsSummary: String = "",
        nutriScore: String = "",
        returnToTodayTab: Boolean = true
    ) {
        val dateIso = selectedDateFlow.value
        val entry = NutritionMealLogEntity(
            id = "meal-$dateIso-${UUID.randomUUID().toString().take(8)}",
            dateIso = dateIso,
            mealType = mealType.name,
            foodName = foodName.trim().ifEmpty { "Alimento" },
            brandOrSource = brandOrSource.trim().ifEmpty { "Manual" },
            servingGrams = servingGrams.coerceIn(1, 5000),
            calories = calories.coerceIn(0, 10000),
            proteinG = proteinG.coerceIn(0, 1000),
            carbsG = carbsG.coerceIn(0, 1000),
            fatG = fatG.coerceIn(0, 1000),
            barcode = barcode.trim(),
            ingredientsSummary = ingredientsSummary.trim(),
            nutriScore = nutriScore.trim(),
            updatedAtMillis = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveMealLog(entry)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        activeTab = if (returnToTodayTab) NutritionTab.TODAY else it.activeTab,
                        manualEntryMealType = null
                    )
                }
                showToast("Añadido a ${mealType.labelEs}: ${entry.foodName} (${entry.calories} kcal)")
            }
        }
    }

    fun deleteMealLog(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMealLog(id)
            withContext(Dispatchers.Main) {
                showToast("Alimento eliminado")
            }
        }
    }

    fun addWaterMl(deltaMl: Int) {
        val st = _uiState.value
        val curr = st.selectedDateMetric
        val newWater = (curr.waterMl + deltaMl).coerceIn(0, 15000)
        val updated = curr.copy(
            waterMl = newWater,
            loggedWeightKg = curr.loggedWeightKg ?: st.profile.weightKg,
            updatedAtMillis = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveDailyMetric(updated)
        }
    }

    fun updateLoggedWeight(newWeightKg: Double) {
        val st = _uiState.value
        val clamped = newWeightKg.coerceIn(30.0, 260.0)
        val curr = st.selectedDateMetric
        val updatedMetric = curr.copy(
            loggedWeightKg = clamped,
            updatedAtMillis = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveDailyMetric(updatedMetric)
            withContext(Dispatchers.Main) {
                showToast("Peso del día registrado: ${String.format(Locale.US, "%.1f", clamped).replace('.', ',')} kg")
            }
        }
    }

    fun recalculateAndSaveProfile(
        weightKg: Double,
        targetWeightKg: Double,
        heightCm: Int,
        ageYears: Int,
        activity: NutritionActivityLevel,
        goal: NutritionGoalType,
        customDailyCalories: Int? = null
    ) {
        val calculated = NutritionDomainHelper.calculateTargets(
            weightKg = weightKg,
            targetWeightKg = targetWeightKg,
            heightCm = heightCm,
            ageYears = ageYears,
            activity = activity,
            goal = goal,
            customDailyCalories = customDailyCalories
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProfile(calculated)
            withContext(Dispatchers.Main) {
                showToast("Metas actualizadas: ${calculated.dailyCalorieGoal} kcal/día (${goal.shortBadgeEs})")
            }
        }
    }

    fun lookupBarcode(rawBarcode: String) {
        val cleaned = rawBarcode.trim()
        if (cleaned.isEmpty()) {
            _uiState.update { it.copy(scannerState = ScannerUiState.Error("Introduce o escanea un código EAN / QR.")) }
            return
        }
        val curr = _uiState.value.scannerState
        if (curr is ScannerUiState.Loading) return
        if (curr is ScannerUiState.ProductFound && curr.product.barcode == cleaned) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(scannerState = ScannerUiState.Loading("Consultando código $cleaned en Open Food Facts..."))
            }
            val result = openFoodFactsClient.fetchProductByBarcode(cleaned)
            result.fold(
                onSuccess = { product ->
                    _uiState.update { it.copy(scannerState = ScannerUiState.ProductFound(product)) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            scannerState = ScannerUiState.Error(
                                error.localizedMessage ?: "No se encontró el código $cleaned"
                            )
                        )
                    }
                }
            )
        }
    }

    fun searchOpenFoodFactsByName(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update {
                it.copy(scannerState = ScannerUiState.Error("Escribe al menos 2 letras para buscar alimentos."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(scannerState = ScannerUiState.Loading("Buscando \"$trimmed\" en Open Food Facts..."))
            }
            val result = openFoodFactsClient.searchProductsByName(trimmed)
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _uiState.update {
                            it.copy(scannerState = ScannerUiState.Error("Sin resultados para \"$trimmed\"."))
                        }
                    } else {
                        _uiState.update {
                            it.copy(scannerState = ScannerUiState.SearchResults(trimmed, list))
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            scannerState = ScannerUiState.Error(
                                error.localizedMessage ?: "Error al buscar en Open Food Facts"
                            )
                        )
                    }
                }
            )
        }
    }

    fun selectProductFromSearch(product: ScannedProductInfo) {
        _uiState.update { it.copy(scannerState = ScannerUiState.ProductFound(product)) }
    }

    fun resetScannerState() {
        _uiState.update { it.copy(scannerState = ScannerUiState.Idle) }
    }

    fun analyzePlatePhoto(bitmap: Bitmap?, userHint: String = "") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(plateAiState = PlateAiUiState.Analyzing(bitmap, userHint))
            }
            val apiKey = repository.getGeminiApiKey()
            val result = plateAiAnalyzer.analyzePlatePhoto(
                bitmap = bitmap,
                userHint = userHint,
                apiKey = apiKey
            )
            result.fold(
                onSuccess = { analysis ->
                    _uiState.update {
                        it.copy(plateAiState = PlateAiUiState.Analyzed(bitmap, analysis))
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            plateAiState = PlateAiUiState.Error(
                                message = error.localizedMessage ?: "Error al analizar el plato con IA",
                                bitmap = bitmap
                            )
                        )
                    }
                }
            )
        }
    }

    fun resetPlateAiState() {
        _uiState.update { it.copy(plateAiState = PlateAiUiState.Idle) }
    }

    fun showToast(message: String, positive: Boolean = true) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message, toastPositive = positive) }
        toastJob = viewModelScope.launch {
            delay(2800L)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }
}
