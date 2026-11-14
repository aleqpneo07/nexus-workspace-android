package dev.quintero.nexus.feature.progress

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BodyProgressRepository(
    private val context: Context,
    private val dao: BodyProgressDao
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(BodyProgressPayloadDto::class.java).indent("  ")

    val entriesFlow: Flow<List<BodyProgressEntryEntity>> = dao.observeEntries()
    val profileFlow: Flow<BodyProgressProfileEntity?> = dao.observeProfile()

    suspend fun ensureSeededIfEmpty() {
        val hasEntries = dao.countEntries() > 0
        val hasProfile = dao.getProfile() != null
        if (hasEntries && hasProfile) return

        val seedJson = readAssetOrNull(PERSONAL_SEED_ASSET)
            ?: readAssetOrNull(DEMO_SEED_ASSET)
            ?: return

        val parsed = runCatching { payloadAdapter.fromJson(seedJson) }.getOrNull() ?: return
        if (!hasProfile) {
            dao.upsertProfile(parsed.profile.toEntity())
        }
        if (!hasEntries && parsed.entries.isNotEmpty()) {
            dao.upsertEntries(parsed.entries.map { it.toEntity() })
        }
    }

    suspend fun getAllEntries(): List<BodyProgressEntryEntity> = dao.getAllEntries()

    suspend fun getProfileOrDefault(): BodyProgressProfileEntity =
        dao.getProfile() ?: BodyProgressProfileEntity()

    suspend fun saveEntry(entry: BodyProgressEntryEntity) {
        dao.upsertEntry(entry)
    }

    suspend fun deleteEntry(id: String) {
        dao.deleteEntryById(id)
    }

    suspend fun updateProfile(profile: BodyProgressProfileEntity) {
        dao.upsertProfile(profile)
    }

    suspend fun replaceAllFromSync(
        profile: BodyProgressProfileEntity,
        entries: List<BodyProgressEntryEntity>
    ) {
        dao.replaceAll(profile, entries)
    }

    fun exportBackupJson(
        profile: BodyProgressProfileEntity,
        entries: List<BodyProgressEntryEntity>
    ): String {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val payload = BodyProgressPayloadDto(
            version = 1,
            exportedAt = nowIso,
            profile = profile.toDto(),
            entries = entries.sortedBy { it.dateIso }.map { it.toDto() }
        )
        return payloadAdapter.toJson(payload)
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> = runCatching {
        val cleaned = rawJson.trim().removePrefix("\uFEFF").trim()
        val parsed = payloadAdapter.fromJson(cleaned)
            ?: error("El archivo JSON no corresponde a un backup de Progreso físico.")
        val entities = parsed.entries.map { it.toEntity() }
        if (parsed.profile.athleteName.isNotBlank()) {
            dao.upsertProfile(parsed.profile.toEntity())
        }
        if (entities.isNotEmpty()) {
            dao.upsertEntries(entities)
        }
        entities.size
    }

    private fun readAssetOrNull(assetName: String): String? = runCatching {
        context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()

    companion object {
        private const val PERSONAL_SEED_ASSET = "body_progress.json"
        private const val DEMO_SEED_ASSET = "demo_body_progress.json"
    }
}

fun BodyProgressProfileEntity.toDto(): BodyProgressProfileDto = BodyProgressProfileDto(
    athleteName = athleteName,
    ageYears = ageYears,
    heightCm = heightCm,
    scaleModel = scaleModel,
    cycleDays = cycleDays,
    baselineDateLabel = baselineDateLabel,
    summerRefSummary = summerRefSummary,
    targetDescription = targetDescription,
    updatedAtMillis = updatedAtMillis
)

fun BodyProgressProfileDto.toEntity(): BodyProgressProfileEntity = BodyProgressProfileEntity(
    id = "primary",
    athleteName = athleteName,
    ageYears = ageYears,
    heightCm = heightCm,
    scaleModel = scaleModel,
    cycleDays = cycleDays,
    baselineDateLabel = baselineDateLabel,
    summerRefSummary = summerRefSummary,
    targetDescription = targetDescription,
    updatedAtMillis = updatedAtMillis
)

fun BodyProgressEntryEntity.toDto(): BodyProgressEntryDto = BodyProgressEntryDto(
    id = id,
    dateIso = dateIso,
    displayDate = displayDate,
    isBaseEntry = isBaseEntry,
    isPostponed = isPostponed,
    weightKg = weightKg,
    deltaWeightKg = deltaWeightKg,
    bmi = bmi,
    bodyFatPct = bodyFatPct,
    deltaFatKg = deltaFatKg,
    muscleKg = muscleKg,
    deltaMuscleKg = deltaMuscleKg,
    muscleQuality = muscleQuality,
    bodyType = bodyType,
    boneMassKg = boneMassKg,
    visceralFat = visceralFat,
    bmrKcal = bmrKcal,
    clinicalBmrKcal = clinicalBmrKcal,
    metabolicAge = metabolicAge,
    waterPct = waterPct,
    postponeShortLabel = postponeShortLabel,
    notes = notes,
    updatedAtMillis = updatedAtMillis
)

fun BodyProgressEntryDto.toEntity(): BodyProgressEntryEntity = BodyProgressEntryEntity(
    id = id,
    dateIso = dateIso,
    displayDate = displayDate?.takeIf { it.isNotBlank() }
        ?: BodyProgressDomainHelper.formatShortDisplayDate(dateIso),
    isBaseEntry = isBaseEntry,
    isPostponed = isPostponed,
    weightKg = weightKg,
    deltaWeightKg = deltaWeightKg,
    bmi = bmi,
    bodyFatPct = bodyFatPct,
    deltaFatKg = deltaFatKg,
    muscleKg = muscleKg,
    deltaMuscleKg = deltaMuscleKg,
    muscleQuality = muscleQuality,
    bodyType = bodyType,
    boneMassKg = boneMassKg,
    visceralFat = visceralFat,
    bmrKcal = bmrKcal,
    clinicalBmrKcal = clinicalBmrKcal,
    metabolicAge = metabolicAge,
    waterPct = waterPct,
    postponeShortLabel = postponeShortLabel,
    notes = notes,
    updatedAtMillis = updatedAtMillis
)
