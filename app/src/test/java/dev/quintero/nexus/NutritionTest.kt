package dev.quintero.nexus

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import dev.quintero.nexus.core.database.NexusDatabase
import dev.quintero.nexus.feature.nutrition.GeminiPlateAiAnalyzer
import dev.quintero.nexus.feature.nutrition.MealType
import dev.quintero.nexus.feature.nutrition.NutritionActivityLevel
import dev.quintero.nexus.feature.nutrition.NutritionDailyMetricEntity
import dev.quintero.nexus.feature.nutrition.NutritionDomainHelper
import dev.quintero.nexus.feature.nutrition.NutritionGoalType
import dev.quintero.nexus.feature.nutrition.NutritionMealLogEntity
import dev.quintero.nexus.feature.nutrition.NutritionRepository
import dev.quintero.nexus.feature.nutrition.OpenFoodFactsClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NutritionTest {

    private lateinit var context: Context
    private lateinit var db: NexusDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun domainHelper_calculatesMifflinTargetsAndScalesProductGrams() {
        val profile = NutritionDomainHelper.calculateTargets(
            weightKg = 80.0,
            targetWeightKg = 75.0,
            heightCm = 180,
            ageYears = 25,
            activity = NutritionActivityLevel.MODERATE,
            goal = NutritionGoalType.DEFICIT
        )
        // BMR = 800 + 1125 - 125 + 5 = 1805
        // TDEE = 1805 * 1.55 = 2798 -> Deficit (-400) = 2398 kcal
        assertEquals(2398, profile.dailyCalorieGoal)
        assertEquals(168, profile.proteinGoalG)
        assertEquals(3040, profile.waterGoalMl)

        val surplusProfile = NutritionDomainHelper.calculateTargets(
            weightKg = 78.0,
            targetWeightKg = 84.0,
            heightCm = 180,
            ageYears = 24,
            activity = NutritionActivityLevel.ACTIVE,
            goal = NutritionGoalType.BULK_HIGH,
            customDailyCalories = 3150
        )
        assertEquals(3150, surplusProfile.dailyCalorieGoal)
        assertTrue(surplusProfile.carbsGoalG >= 380)

        val offClient = OpenFoodFactsClient()
        val oats = offClient.offlineSpanishCatalog.first { it.barcode == "4008713700626" }
        val (kcal, pro, carb, fat) = NutritionDomainHelper.scaleProductForGrams(oats, 50)
        assertEquals(186, kcal)
        assertEquals(7, pro)
        assertEquals(30, carb)
        assertEquals(4, fat)
    }

    @Test
    fun openFoodFactsClient_parsesApiJsonAndSearchesOfflineCatalog() = runBlocking {
        val client = OpenFoodFactsClient()
        val sampleJson = JSONObject(
            """
            {
              "product_name_es": "Queso Fresco Batido 0%",
              "brands": "Hacendado",
              "nutriscore_grade": "a",
              "serving_quantity": 125,
              "nutriments": {
                "energy-kcal_100g": 46.0,
                "proteins_100g": 8.1,
                "carbohydrates_100g": 3.5,
                "fat_100g": 0.1
              }
            }
            """.trimIndent()
        )
        val parsed = client.parseProductJson("8480000105000", sampleJson)
        assertEquals("Queso Fresco Batido 0%", parsed.name)
        assertEquals("Hacendado", parsed.brand)
        assertEquals("A", parsed.nutriScore)
        assertEquals(46, parsed.caloriesPer100g)
        assertEquals(8, parsed.proteinPer100g)
        assertEquals(125, parsed.servingGramsDefault)

        val searchRes = client.searchProductsByName("avena")
        assertTrue(searchRes.isSuccess)
        assertTrue(searchRes.getOrNull().orEmpty().any { it.name.contains("Avena", ignoreCase = true) })
    }

    @Test
    fun geminiAnalyzer_parsesStructuredJsonAndSupportsSmartFallback() = runBlocking {
        val analyzer = GeminiPlateAiAnalyzer()
        val rawGeminiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"dishName\":\"Pechuga de Pollo con Batata Asada\",\"estimatedTotalGrams\":360,\"totalCalories\":510,\"proteinGrams\":45,\"carbsGrams\":58,\"fatGrams\":9,\"detectedIngredients\":[\"Pollo a la plancha (180g)\",\"Batata asada (180g)\"],\"confidenceNote\":\"Alto en proteína magra\"}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = analyzer.parseGeminiGenerateContentResponse(rawGeminiResponse)
        assertNotNull(parsed)
        assertEquals("Pechuga de Pollo con Batata Asada", parsed?.dishName)
        assertEquals(510, parsed?.totalCalories)
        assertEquals(45, parsed?.proteinGrams)
        assertEquals(2, parsed?.detectedIngredients?.size)

        val fallback = analyzer.analyzePlatePhoto(
            bitmap = null,
            userHint = "Poke de salmón, quinoa y aguacate",
            apiKey = ""
        )
        assertTrue(fallback.isSuccess)
        assertEquals(615, fallback.getOrNull()?.totalCalories)

        // Test OCR parsing of a back-of-package Nutrition Table
        val sampleBackLabelOcr = """
            INFORMACIÓN NUTRICIONAL por 100 g
            Valor energético 1335 kJ / 316 kcal
            Grasas 7,5 g
            de las cuales saturadas 1,1 g
            Hidratos de carbono 52,0 g
            de los cuales azúcares 2,4 g
            Proteínas 8,6 g
            Sal 1,2 g
        """.trimIndent()
        val labelResult = analyzer.parseNutritionTableFromOcrText(
            rawOcrText = sampleBackLabelOcr,
            userHint = "Tortitas de trigo"
        )
        assertNotNull(labelResult)
        assertEquals(316, labelResult?.totalCalories)
        assertEquals(9, labelResult?.proteinGrams)
        assertEquals(52, labelResult?.carbsGrams)
        assertEquals(8, labelResult?.fatGrams)

        // Test OCR identification of the Almendras 0% Sal Añadida 200g front package
        val almondsFrontOcr = "0% SAL AÑADIDA ADICIONADO 200g SIN GLUTEN"
        val almondsResult = analyzer.analyzeBitmapWithOcrAndOpenFoodFacts(
            ocrText = almondsFrontOcr,
            userHint = ""
        )
        assertNotNull(almondsResult)
        assertTrue(almondsResult?.dishName?.contains("Almendras", ignoreCase = true) == true)
        assertEquals(179, almondsResult?.totalCalories) // 30g serving of 598 kcal/100g almonds = 179 kcal

        // Test EAN extraction from QR URLs and Tortitas barcode 8480012029107
        val offClient = OpenFoodFactsClient()
        assertEquals(
            "8480012029107",
            offClient.extractEanFromRawCode("https://www.grupoifa.es/producto/8480012029107?qr=1")
        )
        val tortitasRes = offClient.fetchProductByBarcode("8480012029107")
        assertTrue(tortitasRes.isSuccess)
        assertTrue(tortitasRes.getOrNull()?.name?.contains("Tortitas", ignoreCase = true) == true)
    }

    @Test
    fun repository_seedsPersistsAndRoundtripsBackupJson() = runBlocking {
        val repo = NutritionRepository(context, db.nutritionDao())
        repo.ensureSeededIfEmpty()

        val initialMeals = repo.getAllMealLogs()
        val initialProfile = repo.getProfileOrDefault()
        assertTrue("Expected seeded meals", initialMeals.size >= 2)
        assertTrue(initialProfile.dailyCalorieGoal >= 1200)

        val customMeal = NutritionMealLogEntity(
            id = "meal-custom-1",
            dateIso = "2026-11-14",
            mealType = MealType.DINNER.name,
            foodName = "Salmón al horno con espárragos",
            brandOrSource = "Gemini IA",
            servingGrams = 310,
            calories = 490,
            proteinG = 42,
            carbsG = 12,
            fatG = 28,
            nutriScore = "A"
        )
        repo.saveMealLog(customMeal)
        repo.saveDailyMetric(
            NutritionDailyMetricEntity(
                dateIso = "2026-11-14",
                waterMl = 2250,
                loggedWeightKg = 77.6
            )
        )

        val allMealsAfter = repo.getAllMealLogs()
        val allMetricsAfter = repo.getAllDailyMetrics()
        assertTrue(allMealsAfter.any { it.id == "meal-custom-1" })

        val json = repo.exportBackupJson(initialProfile, allMealsAfter, allMetricsAfter)
        db.nutritionDao().deleteAllMealLogs()
        assertTrue(repo.getAllMealLogs().isEmpty())

        val importRes = repo.importBackupJson(json)
        assertTrue(importRes.isSuccess)
        assertEquals(allMealsAfter.size, importRes.getOrNull())
        assertTrue(repo.getAllMealLogs().any { it.id == "meal-custom-1" && it.calories == 490 })
    }

    @Test
    fun migration4To5_matchesCompiledRoomSchema() {
        val roomColsProfile = readTableColumns(db.openHelper.writableDatabase, "nutrition_profile")
        val roomColsMeals = readTableColumns(db.openHelper.writableDatabase, "nutrition_meal_logs")
        val roomColsMetrics = readTableColumns(db.openHelper.writableDatabase, "nutrition_daily_metrics")

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(sqDb: SupportSQLiteDatabase) {
                        sqDb.execSQL("CREATE TABLE IF NOT EXISTS night_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL)")
                    }

                    override fun onUpgrade(sqDb: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.use {
            val rawDb = it.writableDatabase
            NexusDatabase.MIGRATION_4_5.migrate(rawDb)

            assertEquals(roomColsProfile, readTableColumns(rawDb, "nutrition_profile"))
            assertEquals(roomColsMeals, readTableColumns(rawDb, "nutrition_meal_logs"))
            assertEquals(roomColsMetrics, readTableColumns(rawDb, "nutrition_daily_metrics"))
        }
    }

    private fun readTableColumns(db: SupportSQLiteDatabase, tableName: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            val typeIdx = cursor.getColumnIndex("type")
            val notNullIdx = cursor.getColumnIndex("notnull")
            val pkIdx = cursor.getColumnIndex("pk")
            while (cursor.moveToNext()) {
                val colName = cursor.getString(nameIdx)
                val colSpec = "${cursor.getString(typeIdx)}|nn=${cursor.getInt(notNullIdx)}|pk=${cursor.getInt(pkIdx)}"
                map[colName] = colSpec
            }
        }
        return map
    }
}
