package dev.quintero.nexus.feature.nutrition

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

enum class MealType(val labelEs: String, val targetPct: Float, val iconEmoji: String) {
    BREAKFAST("Desayuno", 0.25f, "🌅"),
    LUNCH("Almuerzo", 0.35f, "🍽️"),
    DINNER("Cena", 0.25f, "🌙"),
    SNACK("Snacks", 0.15f, "⚡");

    companion object {
        fun fromName(value: String): MealType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LUNCH
    }
}

enum class NutritionActivityLevel(
    val labelEs: String,
    val descriptionEs: String,
    val multiplier: Double
) {
    SEDENTARY("Sedentario", "Oficina / estudio, poco movimiento diario", 1.2),
    LIGHT("Ligero", "Entrenamiento 1-3 días/semana", 1.375),
    MODERATE("Moderado", "Entreno de fuerza 3-5 días/semana", 1.55),
    ACTIVE("Activo", "Entrenamiento intenso 5-6 días/semana", 1.725),
    ATHLETE("Atleta", "Doble sesión o alto gasto físico diario", 1.9);

    companion object {
        fun fromName(value: String): NutritionActivityLevel =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MODERATE
    }
}

enum class NutritionGoalType(
    val labelEs: String,
    val shortBadgeEs: String,
    val calorieDelta: Int
) {
    DEFICIT("Definición (-400 kcal)", "Definición", -400),
    MAINTAIN("Mantenimiento", "Mantenimiento", 0),
    SURPLUS("Volumen (+300 kcal)", "Volumen", 300);

    companion object {
        fun fromName(value: String): NutritionGoalType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFICIT
    }
}

@Entity(tableName = "nutrition_profile")
data class NutritionProfileEntity(
    @PrimaryKey val id: String = "primary",
    val weightKg: Double = 78.0,
    val targetWeightKg: Double = 74.0,
    val heightCm: Int = 180,
    val ageYears: Int = 24,
    val activityLevel: String = NutritionActivityLevel.MODERATE.name,
    val goalType: String = NutritionGoalType.DEFICIT.name,
    val dailyCalorieGoal: Int = 2250,
    val proteinGoalG: Int = 160,
    val carbsGoalG: Int = 235,
    val fatGoalG: Int = 65,
    val waterGoalMl: Int = 2750,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "nutrition_meal_logs")
data class NutritionMealLogEntity(
    @PrimaryKey val id: String,
    val dateIso: String,
    val mealType: String = MealType.LUNCH.name,
    val foodName: String,
    val brandOrSource: String = "Manual",
    val servingGrams: Int = 100,
    val calories: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val barcode: String = "",
    val ingredientsSummary: String = "",
    val nutriScore: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "nutrition_daily_metrics")
data class NutritionDailyMetricEntity(
    @PrimaryKey val dateIso: String,
    val waterMl: Int = 0,
    val loggedWeightKg: Double? = null,
    val notes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class NutritionProfileDto(
    @Json(name = "weightKg") val weightKg: Double = 78.0,
    @Json(name = "targetWeightKg") val targetWeightKg: Double = 74.0,
    @Json(name = "heightCm") val heightCm: Int = 180,
    @Json(name = "ageYears") val ageYears: Int = 24,
    @Json(name = "activityLevel") val activityLevel: String = NutritionActivityLevel.MODERATE.name,
    @Json(name = "goalType") val goalType: String = NutritionGoalType.DEFICIT.name,
    @Json(name = "dailyCalorieGoal") val dailyCalorieGoal: Int = 2250,
    @Json(name = "proteinGoalG") val proteinGoalG: Int = 160,
    @Json(name = "carbsGoalG") val carbsGoalG: Int = 235,
    @Json(name = "fatGoalG") val fatGoalG: Int = 65,
    @Json(name = "waterGoalMl") val waterGoalMl: Int = 2750,
    @Json(name = "updatedAtMillis") val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class NutritionMealLogDto(
    @Json(name = "id") val id: String,
    @Json(name = "dateIso") val dateIso: String,
    @Json(name = "mealType") val mealType: String = MealType.LUNCH.name,
    @Json(name = "foodName") val foodName: String,
    @Json(name = "brandOrSource") val brandOrSource: String = "Manual",
    @Json(name = "servingGrams") val servingGrams: Int = 100,
    @Json(name = "calories") val calories: Int = 0,
    @Json(name = "proteinG") val proteinG: Int = 0,
    @Json(name = "carbsG") val carbsG: Int = 0,
    @Json(name = "fatG") val fatG: Int = 0,
    @Json(name = "barcode") val barcode: String = "",
    @Json(name = "ingredientsSummary") val ingredientsSummary: String = "",
    @Json(name = "nutriScore") val nutriScore: String = "",
    @Json(name = "updatedAtMillis") val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class NutritionDailyMetricDto(
    @Json(name = "dateIso") val dateIso: String,
    @Json(name = "waterMl") val waterMl: Int = 0,
    @Json(name = "loggedWeightKg") val loggedWeightKg: Double? = null,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "updatedAtMillis") val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class NutritionBackupPayloadDto(
    @Json(name = "version") val version: Int = 1,
    @Json(name = "exportedAt") val exportedAt: String = "",
    @Json(name = "profile") val profile: NutritionProfileDto = NutritionProfileDto(),
    @Json(name = "mealLogs") val mealLogs: List<NutritionMealLogDto> = emptyList(),
    @Json(name = "dailyMetrics") val dailyMetrics: List<NutritionDailyMetricDto> = emptyList()
)

data class ScannedProductInfo(
    val barcode: String,
    val name: String,
    val brand: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Int,
    val carbsPer100g: Int,
    val fatPer100g: Int,
    val servingGramsDefault: Int = 100,
    val nutriScore: String? = null,
    val imageUrl: String? = null
)

data class AiPlateAnalysisResult(
    val dishName: String,
    val estimatedTotalGrams: Int,
    val totalCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val detectedIngredients: List<String>,
    val confidenceNote: String,
    val usedLiveGeminiApi: Boolean = true
)

fun NutritionProfileEntity.toDto(): NutritionProfileDto = NutritionProfileDto(
    weightKg = weightKg,
    targetWeightKg = targetWeightKg,
    heightCm = heightCm,
    ageYears = ageYears,
    activityLevel = activityLevel,
    goalType = goalType,
    dailyCalorieGoal = dailyCalorieGoal,
    proteinGoalG = proteinGoalG,
    carbsGoalG = carbsGoalG,
    fatGoalG = fatGoalG,
    waterGoalMl = waterGoalMl,
    updatedAtMillis = updatedAtMillis
)

fun NutritionProfileDto.toEntity(): NutritionProfileEntity = NutritionProfileEntity(
    id = "primary",
    weightKg = weightKg,
    targetWeightKg = targetWeightKg,
    heightCm = heightCm,
    ageYears = ageYears,
    activityLevel = activityLevel,
    goalType = goalType,
    dailyCalorieGoal = dailyCalorieGoal,
    proteinGoalG = proteinGoalG,
    carbsGoalG = carbsGoalG,
    fatGoalG = fatGoalG,
    waterGoalMl = waterGoalMl,
    updatedAtMillis = if (updatedAtMillis > 0L) updatedAtMillis else System.currentTimeMillis()
)

fun NutritionMealLogEntity.toDto(): NutritionMealLogDto = NutritionMealLogDto(
    id = id,
    dateIso = dateIso,
    mealType = mealType,
    foodName = foodName,
    brandOrSource = brandOrSource,
    servingGrams = servingGrams,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    barcode = barcode,
    ingredientsSummary = ingredientsSummary,
    nutriScore = nutriScore,
    updatedAtMillis = updatedAtMillis
)

fun NutritionMealLogDto.toEntity(): NutritionMealLogEntity = NutritionMealLogEntity(
    id = id,
    dateIso = dateIso,
    mealType = mealType,
    foodName = foodName,
    brandOrSource = brandOrSource,
    servingGrams = servingGrams,
    calories = calories,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    barcode = barcode,
    ingredientsSummary = ingredientsSummary,
    nutriScore = nutriScore,
    updatedAtMillis = if (updatedAtMillis > 0L) updatedAtMillis else System.currentTimeMillis()
)

fun NutritionDailyMetricEntity.toDto(): NutritionDailyMetricDto = NutritionDailyMetricDto(
    dateIso = dateIso,
    waterMl = waterMl,
    loggedWeightKg = loggedWeightKg,
    notes = notes,
    updatedAtMillis = updatedAtMillis
)

fun NutritionDailyMetricDto.toEntity(): NutritionDailyMetricEntity = NutritionDailyMetricEntity(
    dateIso = dateIso,
    waterMl = waterMl,
    loggedWeightKg = loggedWeightKg,
    notes = notes,
    updatedAtMillis = if (updatedAtMillis > 0L) updatedAtMillis else System.currentTimeMillis()
)

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition_profile WHERE id = 'primary' LIMIT 1")
    fun observeProfile(): Flow<NutritionProfileEntity?>

    @Query("SELECT * FROM nutrition_profile WHERE id = 'primary' LIMIT 1")
    suspend fun getProfile(): NutritionProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: NutritionProfileEntity)

    @Query("SELECT * FROM nutrition_meal_logs ORDER BY dateIso DESC, updatedAtMillis ASC")
    fun observeAllMealLogs(): Flow<List<NutritionMealLogEntity>>

    @Query("SELECT * FROM nutrition_meal_logs WHERE dateIso = :dateIso ORDER BY updatedAtMillis ASC")
    fun observeMealLogsForDate(dateIso: String): Flow<List<NutritionMealLogEntity>>

    @Query("SELECT * FROM nutrition_meal_logs ORDER BY dateIso DESC, updatedAtMillis ASC")
    suspend fun getAllMealLogs(): List<NutritionMealLogEntity>

    @Query("SELECT COUNT(*) FROM nutrition_meal_logs")
    suspend fun countMealLogs(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealLog(entry: NutritionMealLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealLogs(entries: List<NutritionMealLogEntity>)

    @Query("DELETE FROM nutrition_meal_logs WHERE id = :id")
    suspend fun deleteMealLogById(id: String)

    @Query("DELETE FROM nutrition_meal_logs")
    suspend fun deleteAllMealLogs()

    @Query("SELECT * FROM nutrition_daily_metrics ORDER BY dateIso DESC")
    fun observeAllDailyMetrics(): Flow<List<NutritionDailyMetricEntity>>

    @Query("SELECT * FROM nutrition_daily_metrics WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getDailyMetricForDate(dateIso: String): NutritionDailyMetricEntity?

    @Query("SELECT * FROM nutrition_daily_metrics ORDER BY dateIso DESC")
    suspend fun getAllDailyMetrics(): List<NutritionDailyMetricEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyMetric(metric: NutritionDailyMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyMetrics(metrics: List<NutritionDailyMetricEntity>)

    @Query("DELETE FROM nutrition_daily_metrics")
    suspend fun deleteAllDailyMetrics()

    @Transaction
    suspend fun replaceAll(
        profile: NutritionProfileEntity,
        mealLogs: List<NutritionMealLogEntity>,
        dailyMetrics: List<NutritionDailyMetricEntity>
    ) {
        upsertProfile(profile)
        deleteAllMealLogs()
        if (mealLogs.isNotEmpty()) upsertMealLogs(mealLogs)
        deleteAllDailyMetrics()
        if (dailyMetrics.isNotEmpty()) upsertDailyMetrics(dailyMetrics)
    }
}

object NutritionDomainHelper {

    fun calculateTargets(
        weightKg: Double,
        targetWeightKg: Double,
        heightCm: Int,
        ageYears: Int,
        activity: NutritionActivityLevel,
        goal: NutritionGoalType
    ): NutritionProfileEntity {
        val safeWeight = weightKg.coerceIn(35.0, 260.0)
        val safeTargetWeight = targetWeightKg.coerceIn(35.0, 260.0)
        val safeHeight = heightCm.coerceIn(120, 240)
        val safeAge = ageYears.coerceIn(14, 100)

        // Mifflin-St Jeor base BMR
        val bmr = (10.0 * safeWeight) + (6.25 * safeHeight.toDouble()) - (5.0 * safeAge.toDouble()) + 5.0
        val tdee = bmr * activity.multiplier
        val targetCalories = (tdee + goal.calorieDelta).roundToInt().coerceIn(1200, 6000)
        val proteinG = (safeWeight * 2.0).roundToInt().coerceIn(60, 350)
        val fatG = ((targetCalories * 0.27) / 9.0).roundToInt().coerceIn(35, 200)
        val remainingKcal = (targetCalories - (proteinG * 4) - (fatG * 9)).coerceAtLeast(200)
        val carbsG = (remainingKcal / 4.0).roundToInt().coerceIn(50, 800)
        val waterMl = (safeWeight * 35.0).roundToInt().coerceIn(1500, 5000)

        return NutritionProfileEntity(
            id = "primary",
            weightKg = safeWeight,
            targetWeightKg = safeTargetWeight,
            heightCm = safeHeight,
            ageYears = safeAge,
            activityLevel = activity.name,
            goalType = goal.name,
            dailyCalorieGoal = targetCalories,
            proteinGoalG = proteinG,
            carbsGoalG = carbsG,
            fatGoalG = fatG,
            waterGoalMl = waterMl,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    fun scaleProductForGrams(
        product: ScannedProductInfo,
        grams: Int
    ): Quadruple<Int, Int, Int, Int> {
        val safeGrams = grams.coerceIn(1, 5000)
        val factor = safeGrams / 100.0
        val kcal = (product.caloriesPer100g * factor).roundToInt().coerceAtLeast(0)
        val pro = (product.proteinPer100g * factor).roundToInt().coerceAtLeast(0)
        val carb = (product.carbsPer100g * factor).roundToInt().coerceAtLeast(0)
        val fat = (product.fatPer100g * factor).roundToInt().coerceAtLeast(0)
        return Quadruple(kcal, pro, carb, fat)
    }
}

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
