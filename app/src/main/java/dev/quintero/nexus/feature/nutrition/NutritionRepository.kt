package dev.quintero.nexus.feature.nutrition

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.quintero.nexus.BuildConfig
import dev.quintero.nexus.core.util.DateTimeHelper
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class NutritionRepository(
    context: Context,
    private val dao: NutritionDao
) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("nexus_nutrition_prefs", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(NutritionBackupPayloadDto::class.java)

    val profileFlow: Flow<NutritionProfileEntity?> = dao.observeProfile()
    val allMealLogsFlow: Flow<List<NutritionMealLogEntity>> = dao.observeAllMealLogs()
    val allDailyMetricsFlow: Flow<List<NutritionDailyMetricEntity>> = dao.observeAllDailyMetrics()

    fun getGeminiApiKey(): String {
        val stored = prefs.getString(KEY_GEMINI_API_KEY, null)?.trim().orEmpty()
        return stored.ifEmpty { BuildConfig.DEFAULT_GEMINI_API_KEY.trim() }
    }

    fun saveGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey.trim()).apply()
    }

    suspend fun ensureSeededIfEmpty() {
        val existingProfile = dao.getProfile()
        if (existingProfile == null) {
            dao.upsertProfile(
                NutritionDomainHelper.calculateTargets(
                    weightKg = 78.0,
                    targetWeightKg = 74.0,
                    heightCm = 180,
                    ageYears = 24,
                    activity = NutritionActivityLevel.MODERATE,
                    goal = NutritionGoalType.DEFICIT
                )
            )
        }

        val alreadySeeded = prefs.getBoolean(KEY_INITIAL_SEED_DONE, false)
        if (!alreadySeeded && dao.countMealLogs() == 0) {
            val today = DateTimeHelper.todayIso()
            val starterMeals = listOf(
                NutritionMealLogEntity(
                    id = "seed-meal-breakfast-1",
                    dateIso = today,
                    mealType = MealType.BREAKFAST.name,
                    foodName = "Copos de Avena Suaves Integrales",
                    brandOrSource = "Hacendado · EAN",
                    servingGrams = 60,
                    calories = 223,
                    proteinG = 8,
                    carbsG = 35,
                    fatG = 4,
                    barcode = "4008713700626",
                    nutriScore = "A",
                    updatedAtMillis = 1_000L
                ),
                NutritionMealLogEntity(
                    id = "seed-meal-breakfast-2",
                    dateIso = today,
                    mealType = MealType.BREAKFAST.name,
                    foodName = "Yogur Proteico Natural +Proteínas",
                    brandOrSource = "Hacendado · EAN",
                    servingGrams = 240,
                    calories = 125,
                    proteinG = 24,
                    carbsG = 7,
                    fatG = 0,
                    barcode = "8480000105042",
                    nutriScore = "A",
                    updatedAtMillis = 2_000L
                ),
                NutritionMealLogEntity(
                    id = "seed-meal-lunch-1",
                    dateIso = today,
                    mealType = MealType.LUNCH.name,
                    foodName = "Pechuga de Pollo a la Plancha con Arroz Basmati",
                    brandOrSource = "Gemini IA · Foto",
                    servingGrams = 380,
                    calories = 540,
                    proteinG = 48,
                    carbsG = 60,
                    fatG = 10,
                    ingredientsSummary = "Pechuga de pollo (180g), Arroz basmati (150g), Brócoli (50g)",
                    updatedAtMillis = 3_000L
                )
            )
            dao.upsertMealLogs(starterMeals)
            dao.upsertDailyMetric(
                NutritionDailyMetricEntity(
                    dateIso = today,
                    waterMl = 1500,
                    loggedWeightKg = 78.0,
                    notes = "Día activo",
                    updatedAtMillis = 3_000L
                )
            )
            prefs.edit().putBoolean(KEY_INITIAL_SEED_DONE, true).apply()
        }
    }

    suspend fun getProfileOrDefault(): NutritionProfileEntity {
        return dao.getProfile() ?: NutritionProfileEntity()
    }

    suspend fun getAllMealLogs(): List<NutritionMealLogEntity> = dao.getAllMealLogs()

    suspend fun getAllDailyMetrics(): List<NutritionDailyMetricEntity> = dao.getAllDailyMetrics()

    suspend fun saveProfile(profile: NutritionProfileEntity) {
        dao.upsertProfile(profile.copy(id = "primary", updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun saveMealLog(entry: NutritionMealLogEntity) {
        prefs.edit().putBoolean(KEY_INITIAL_SEED_DONE, true).apply()
        dao.upsertMealLog(entry.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun deleteMealLog(id: String) {
        dao.deleteMealLogById(id)
    }

    suspend fun saveDailyMetric(metric: NutritionDailyMetricEntity) {
        dao.upsertDailyMetric(metric.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    fun exportBackupJson(
        profile: NutritionProfileEntity,
        mealLogs: List<NutritionMealLogEntity>,
        dailyMetrics: List<NutritionDailyMetricEntity>
    ): String {
        val payload = NutritionBackupPayloadDto(
            version = 1,
            exportedAt = Instant.now().toString(),
            profile = profile.toDto(),
            mealLogs = mealLogs.map { it.toDto() },
            dailyMetrics = dailyMetrics.map { it.toDto() }
        )
        return backupAdapter.indent("  ").toJson(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> {
        return runCatching {
            val parsed = backupAdapter.fromJson(rawJson.trim())
                ?: throw IllegalArgumentException("Formato JSON de nutrición inválido")
            val profileEntity = parsed.profile.toEntity()
            val mealEntities = parsed.mealLogs
                .filter { it.id.isNotBlank() && it.dateIso.isNotBlank() }
                .map { it.toEntity() }
            val metricEntities = parsed.dailyMetrics
                .filter { it.dateIso.isNotBlank() }
                .map { it.toEntity() }

            dao.replaceAll(
                profile = profileEntity,
                mealLogs = mealEntities,
                dailyMetrics = metricEntities
            )
            prefs.edit().putBoolean(KEY_INITIAL_SEED_DONE, true).apply()
            mealEntities.size
        }
    }

    companion object {
        private const val KEY_INITIAL_SEED_DONE = "nutrition_initial_seed_v1"
        private const val KEY_GEMINI_API_KEY = "nutrition_gemini_api_key"
    }
}
