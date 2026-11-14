package dev.quintero.nexus.feature.progress

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dev.quintero.nexus.core.util.DateTimeHelper
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import kotlin.math.roundToInt

@Entity(tableName = "body_progress_entries")
data class BodyProgressEntryEntity(
    @PrimaryKey val id: String,
    val dateIso: String,
    val displayDate: String,
    val isBaseEntry: Boolean = false,
    val isPostponed: Boolean = false,
    val weightKg: Double? = null,
    val deltaWeightKg: Double? = null,
    val bmi: Double? = null,
    val bodyFatPct: Double? = null,
    val deltaFatKg: Double? = null,
    val muscleKg: Double? = null,
    val deltaMuscleKg: Double? = null,
    val muscleQuality: Int? = null,
    val bodyType: Int? = null,
    val boneMassKg: Double? = null,
    val visceralFat: Double? = null,
    val bmrKcal: Int? = null,
    val clinicalBmrKcal: Int? = null,
    val metabolicAge: Int? = null,
    val waterPct: Double? = null,
    val postponeShortLabel: String = "",
    val notes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "body_progress_profile")
data class BodyProgressProfileEntity(
    @PrimaryKey val id: String = "primary",
    val athleteName: String = "Atleta",
    val ageYears: Int = 24,
    val heightCm: Int = 180,
    val scaleModel: String = "Bioimpedancia",
    val cycleDays: Int = 14,
    val baselineDateLabel: String = "Inicio",
    val summerRefSummary: String = "",
    val targetDescription: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class BodyProgressProfileDto(
    @Json(name = "athleteName") val athleteName: String = "Atleta",
    @Json(name = "ageYears") val ageYears: Int = 24,
    @Json(name = "heightCm") val heightCm: Int = 180,
    @Json(name = "scaleModel") val scaleModel: String = "Bioimpedancia",
    @Json(name = "cycleDays") val cycleDays: Int = 14,
    @Json(name = "baselineDateLabel") val baselineDateLabel: String = "Inicio",
    @Json(name = "summerRefSummary") val summerRefSummary: String = "",
    @Json(name = "targetDescription") val targetDescription: String = "",
    @Json(name = "updatedAtMillis") val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class BodyProgressEntryDto(
    @Json(name = "id") val id: String,
    @Json(name = "dateIso") val dateIso: String,
    @Json(name = "displayDate") val displayDate: String? = null,
    @Json(name = "isBaseEntry") val isBaseEntry: Boolean = false,
    @Json(name = "isPostponed") val isPostponed: Boolean = false,
    @Json(name = "weightKg") val weightKg: Double? = null,
    @Json(name = "deltaWeightKg") val deltaWeightKg: Double? = null,
    @Json(name = "bmi") val bmi: Double? = null,
    @Json(name = "bodyFatPct") val bodyFatPct: Double? = null,
    @Json(name = "deltaFatKg") val deltaFatKg: Double? = null,
    @Json(name = "muscleKg") val muscleKg: Double? = null,
    @Json(name = "deltaMuscleKg") val deltaMuscleKg: Double? = null,
    @Json(name = "muscleQuality") val muscleQuality: Int? = null,
    @Json(name = "bodyType") val bodyType: Int? = null,
    @Json(name = "boneMassKg") val boneMassKg: Double? = null,
    @Json(name = "visceralFat") val visceralFat: Double? = null,
    @Json(name = "bmrKcal") val bmrKcal: Int? = null,
    @Json(name = "clinicalBmrKcal") val clinicalBmrKcal: Int? = null,
    @Json(name = "metabolicAge") val metabolicAge: Int? = null,
    @Json(name = "waterPct") val waterPct: Double? = null,
    @Json(name = "postponeShortLabel") val postponeShortLabel: String = "",
    @Json(name = "notes") val notes: String = "",
    @Json(name = "updatedAtMillis") val updatedAtMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class BodyProgressPayloadDto(
    @Json(name = "version") val version: Int = 1,
    @Json(name = "exportedAt") val exportedAt: String = "",
    @Json(name = "profile") val profile: BodyProgressProfileDto = BodyProgressProfileDto(),
    @Json(name = "entries") val entries: List<BodyProgressEntryDto> = emptyList()
)

@Dao
interface BodyProgressDao {
    @Query("SELECT * FROM body_progress_entries ORDER BY dateIso ASC")
    fun observeEntries(): Flow<List<BodyProgressEntryEntity>>

    @Query("SELECT * FROM body_progress_entries ORDER BY dateIso ASC")
    suspend fun getAllEntries(): List<BodyProgressEntryEntity>

    @Query("SELECT COUNT(*) FROM body_progress_entries")
    suspend fun countEntries(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: BodyProgressEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<BodyProgressEntryEntity>)

    @Query("DELETE FROM body_progress_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("DELETE FROM body_progress_entries")
    suspend fun deleteAllEntries()

    @Query("SELECT * FROM body_progress_profile WHERE id = 'primary' LIMIT 1")
    fun observeProfile(): Flow<BodyProgressProfileEntity?>

    @Query("SELECT * FROM body_progress_profile WHERE id = 'primary' LIMIT 1")
    suspend fun getProfile(): BodyProgressProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: BodyProgressProfileEntity)

    @Transaction
    suspend fun replaceAll(
        profile: BodyProgressProfileEntity,
        entries: List<BodyProgressEntryEntity>
    ) {
        upsertProfile(profile)
        deleteAllEntries()
        if (entries.isNotEmpty()) upsertEntries(entries)
    }
}

data class NextWeighInScheduleInfo(
    val nextDateIso: String,
    val nextDisplayDate: String,
    val daysRemaining: Int
)

object BodyProgressDomainHelper {

    fun formatShortDisplayDate(dateIso: String): String {
        val parts = dateIso.trim().split("-")
        return if (parts.size == 3) {
            "${parts[2].padStart(2, '0')}/${parts[1].padStart(2, '0')}"
        } else {
            dateIso
        }
    }

    fun calculateBmi(weightKg: Double?, heightCm: Int): Double? {
        if (weightKg == null || weightKg <= 0.0 || heightCm <= 0) return null
        val heightM = heightCm / 100.0
        val raw = weightKg / (heightM * heightM)
        return (raw * 100.0).roundToInt() / 100.0
    }

    fun calculateMifflinStJeorKcal(weightKg: Double?, heightCm: Int, ageYears: Int): Int? {
        if (weightKg == null || weightKg <= 0.0 || heightCm <= 0 || ageYears <= 0) return null
        return (10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears + 5.0).roundToInt()
    }

    fun computeFatKg(weightKg: Double?, bodyFatPct: Double?): Double? {
        if (weightKg == null || bodyFatPct == null || weightKg <= 0.0 || bodyFatPct <= 0.0) return null
        return weightKg * (bodyFatPct / 100.0)
    }

    fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0

    fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0

    fun computeAutoDeltas(
        prevValid: BodyProgressEntryEntity?,
        weightKg: Double?,
        bodyFatPct: Double?,
        muscleKg: Double?
    ): Triple<Double?, Double?, Double?> {
        if (prevValid == null) return Triple(null, null, null)
        val dWeight = if (weightKg != null && prevValid.weightKg != null) {
            round2(weightKg - prevValid.weightKg)
        } else null
        val prevFatKg = computeFatKg(prevValid.weightKg, prevValid.bodyFatPct)
        val currFatKg = computeFatKg(weightKg, bodyFatPct)
        val dFat = if (currFatKg != null && prevFatKg != null) {
            round1(currFatKg - prevFatKg)
        } else null
        val dMuscle = if (muscleKg != null && prevValid.muscleKg != null) {
            round1(muscleKg - prevValid.muscleKg)
        } else null
        return Triple(dWeight, dFat, dMuscle)
    }

    fun computeNextWeighIn(
        entries: List<BodyProgressEntryEntity>,
        cycleDays: Int,
        todayIso: String = DateTimeHelper.todayIso()
    ): NextWeighInScheduleInfo? {
        val lastRecorded = entries
            .filter { !it.isPostponed && it.weightKg != null }
            .maxByOrNull { it.dateIso }
            ?: entries.maxByOrNull { it.dateIso }
            ?: return null
        val safeCycle = cycleDays.coerceIn(7, 60)
        val nextIso = DateTimeHelper.addDaysToIso(lastRecorded.dateIso, safeCycle)
        val diff = DateTimeHelper.daysBetweenIso(todayIso, nextIso)
        return NextWeighInScheduleInfo(
            nextDateIso = nextIso,
            nextDisplayDate = formatShortDisplayDate(nextIso),
            daysRemaining = diff
        )
    }

    fun parseFlexibleDouble(raw: String): Double? =
        raw.trim().replace(',', '.').toDoubleOrNull()

    fun formatEs2(value: Double?): String =
        if (value == null) "—" else String.format(Locale.US, "%.2f", value).replace('.', ',')

    fun formatEs1(value: Double?): String =
        if (value == null) "—" else String.format(Locale.US, "%.1f", value).replace('.', ',')

    fun formatPct1(value: Double?): String =
        if (value == null) "—" else "${formatEs1(value)}%"

    fun formatSigned2(value: Double?): String =
        if (value == null) "—" else {
            val prefix = if (value > 0) "+" else ""
            prefix + String.format(Locale.US, "%.2f", value).replace('.', ',')
        }

    fun formatSigned1(value: Double?): String =
        if (value == null) "—" else {
            val prefix = if (value > 0) "+" else ""
            prefix + String.format(Locale.US, "%.1f", value).replace('.', ',')
        }
}
