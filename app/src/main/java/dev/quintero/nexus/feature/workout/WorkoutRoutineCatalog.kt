package dev.quintero.nexus.feature.workout

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 5-day strength & hypertrophy split catalog with biomechanical notes and RIR targets.
 */
data class WorkoutExerciseTemplate(
    val name: String,
    val target: String,
    val rir: String,
    val tag: String,
    val defaultSets: Int,
    val cue: String,
    val superset: String? = null
)

data class WorkoutDayPlan(
    val dayKey: String,
    val label: String,
    val subtitle: String,
    val biomechanicalNote: String,
    val exercises: List<WorkoutExerciseTemplate>
)

object WorkoutRoutineCatalog {

    val DAY_KEYS = listOf("Día 1", "Día 2", "Día 3", "Día 4", "Día 5")

    val OLD_DAY_MAP: Map<String, String> = mapOf(
        "Empuje" to "Día 1",
        "Tirón" to "Día 2",
        "Pierna" to "Día 3",
        "Pecho+Espalda" to "Día 4",
        "Brazo+Hombro" to "Día 5",
        "Lunes" to "Día 1",
        "Martes" to "Día 2",
        "Miércoles" to "Día 3",
        "Jueves" to "Día 4",
        "Viernes" to "Día 5",
        "Dia 1" to "Día 1",
        "Dia 2" to "Día 2",
        "Dia 3" to "Día 3",
        "Dia 4" to "Día 4",
        "Dia 5" to "Día 5"
    )

    val EXERCISE_ALIASES: Map<String, List<String>> = mapOf(
        "press inclinado en multipower smith" to listOf("press inclinado mancuernas", "press inclinado multipower", "press inclinado barra", "press inclinado en multipower"),
        "press plano en maquina guiada" to listOf("press plano barra", "press plano mancuernas", "press banca", "press plano en maquina guiada o barra"),
        "peck deck en contractora" to listOf("peck deck", "biserie fst7 peck deck en contractora", "aperturas en contractora"),
        "elevaciones laterales maquina sentado" to listOf("elevaciones laterales maquina", "elevaciones laterales"),
        "elevaciones laterales en polea unilateral" to listOf("elevaciones laterales polea", "elevaciones laterales en polea"),
        "press katana unilateral en polea" to listOf("press katana unilateral", "press katana"),
        "extension triceps en polea barra en v" to listOf("extension cuerda", "extension triceps polea", "extension triceps barra en v"),
        "jalon al pecho agarre neutro" to listOf("jalon agarre neutro", "jalon al pecho"),
        "remo en t agarre ancho con apoyo en pecho" to listOf("remo en t agarre ancho", "remo en t", "remo en t con apoyo"),
        "remo en t agarre ancho con apoyo" to listOf("remo en t agarre ancho", "remo en t", "remo en t con apoyo en pecho"),
        "remo unilateral en polea baja con straps" to listOf("remo unilateral en polea baja", "remo unilateral polea", "remo unilateral polea baja"),
        "face pull en polea con cuerda" to listOf("face pull en polea alta con cuerda", "face pull polea", "face pull"),
        "face pull o pajaros en maquina" to listOf("face pull", "pajaros en maquina"),
        "pajaros en maquina reverse peck deck" to listOf("pajaros en maquina", "reverse peck deck", "pajaros maquina"),
        "curl predicador en banco scott" to listOf("curl predicador"),
        "curl con brazos por detras en polea bayesian" to listOf("curl brazos por detras", "bayesian curl", "curl con brazos por detras en polea"),
        "curl con brazos por detras en polea" to listOf("curl brazos por detras", "bayesian curl", "curl con brazos por detras en polea bayesian"),
        "curl con barra z de pie" to listOf("curl con barra z o recta de pie", "curl con barra", "curl barra z"),
        "curl femoral en maquina sentado priorizado" to listOf("curl femoral en maquina tumbadosentado", "curl femoral sentado", "curl femoral en maquina", "curl femoral"),
        "curl femoral en maquina tumbadosentado" to listOf("curl femoral en maquina sentado priorizado", "curl femoral sentado", "curl femoral en maquina", "curl femoral"),
        "sentadilla hack hack squat" to listOf("hack squat", "sentadilla hack"),
        "peso muerto rumano rdl con straps" to listOf("peso muerto rumano rdl", "peso muerto rumano", "peso muerto", "rdl"),
        "peso muerto rumano rdl" to listOf("peso muerto rumano rdl con straps", "peso muerto rumano", "peso muerto", "rdl"),
        "extension de cuadriceps en maquina" to listOf("extension de cuadriceps", "extension cuadriceps"),
        "maquina de aductores" to listOf("aductores", "maquina aductores"),
        "extension de gemelos prensade pie" to listOf("extension gemelos", "gemelos prensa", "extension de gemelos"),
        "pull over en polea alta cuerdabarra" to listOf("biserie fst7 pull over en polea alta", "pull over polea alta", "pull over"),
        "crunch en polea alta" to listOf("crunch polea", "crunch abdominal polea"),
        "elevaciones de piernas colgado" to listOf("elevaciones piernas", "elevaciones colgado")
    )

    val PLANS: Map<String, WorkoutDayPlan> = linkedMapOf(
        "Día 1" to WorkoutDayPlan(
            dayKey = "Día 1",
            label = "Día 1",
            subtitle = "Empuje: Pecho Pesado + Deltoides Lateral + Tríceps",
            biomechanicalNote = "ORDEN BIOMECÁNICO: Intercalar el Peck Deck entre el Press Inclinado y el Press Plano permite que los tríceps y deltoides anterior descansen 5-6 minutos, garantizando que en el press plano el fallo muscular ocurra en el pecho y no en los brazos fatigados.",
            exercises = listOf(
                WorkoutExerciseTemplate(
                    name = "Press Inclinado en Multipower (Smith)",
                    target = "3 × 6 – 8",
                    rir = "RIR 1",
                    tag = "Pecho Superior Pesado",
                    defaultSets = 3,
                    cue = "Banco a 30º. Baja en 3 s controlado a la altura de la clavícula. Máxima sobrecarga progresiva sin la inestabilidad de mancuernas."
                ),
                WorkoutExerciseTemplate(
                    name = "Peck Deck en Contractora",
                    target = "3 × 10 – 12",
                    rir = "RIR 0",
                    tag = "Pectoral Aislado (Aducción)",
                    defaultSets = 3,
                    cue = "Pausa de 2 s en estiramiento profundo atrás y 1 s apretando adelante. Cero fatiga en tríceps. En la 3ª serie: drop-set opcional bajando un 30% del peso."
                ),
                WorkoutExerciseTemplate(
                    name = "Press Plano en Máquina Guiada",
                    target = "2 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Pecho Medio & Esternal",
                    defaultSets = 2,
                    cue = "Agarre convergente o a la altura media del esternón. Pectoral ya pre-activado: busca sentir cada fibra sin sobrecargar hombros ni codos."
                ),
                WorkoutExerciseTemplate(
                    name = "Elevaciones Laterales (Máquina Sentado)",
                    target = "3 × 10 – 15",
                    rir = "RIR 0",
                    tag = "Deltoides Lateral",
                    defaultSets = 3,
                    cue = "Espalda pegada al respaldo. Empuja hacia las paredes (hacia afuera, no hacia arriba). En la última serie: 3-5 repeticiones parciales al fallo."
                ),
                WorkoutExerciseTemplate(
                    name = "Press Katana Unilateral en Polea",
                    target = "2 × 10 – 12",
                    rir = "RIR 1",
                    tag = "Tríceps Cabeza Larga",
                    defaultSets = 2,
                    cue = "Polea a la altura del hombro, brazo en plano escapular (~30º respecto al torso). Máximo estiramiento de la cabeza larga con codo estable."
                ),
                WorkoutExerciseTemplate(
                    name = "Extensión Tríceps en Polea (Barra en V)",
                    target = "2 × 10 – 12",
                    rir = "RIR 0",
                    tag = "Tríceps Lateral & Medial",
                    defaultSets = 2,
                    cue = "Codos pegados a las costillas. Empuja con el talón de la palma y bloquea 1 segundo abajo en contracción pico."
                )
            )
        ),
        "Día 2" to WorkoutDayPlan(
            dayKey = "Día 2",
            label = "Día 2",
            subtitle = "Tirón: Espalda Completa + Deltoides Posterior + Bíceps + Abdomen",
            biomechanicalNote = "CLAVE BIOMECÁNICA: El agarre neutro en jalones y remos viaja en el plano sagital con codos pegados, dando máxima palanca al dorsal ancho sin pinzamiento de hombro. El Pull Over se retira de este día para evitar fatiga residual antes del Día 4.",
            exercises = listOf(
                WorkoutExerciseTemplate(
                    name = "Jalón al Pecho Agarre Neutro",
                    target = "3 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Dorsal Ancho (Amplitud)",
                    defaultSets = 3,
                    cue = "Deprime escápulas antes de tirar. Conduce los codos directos hacia los bolsillos traseros. Amplitud pura sin balanceos."
                ),
                WorkoutExerciseTemplate(
                    name = "Remo en T Agarre Ancho (con Apoyo en Pecho)",
                    target = "3 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Espalda Alta & Trapecio Medio",
                    defaultSets = 3,
                    cue = "Pecho pegado al soporte acolchado (descarga lumbar total). Codos abiertos a 45-60º. Aprieta escápulas 1 s atrás para máxima densidad."
                ),
                WorkoutExerciseTemplate(
                    name = "Remo Unilateral en Polea Baja (con Straps)",
                    target = "2 × 10 – 12",
                    rir = "RIR 0-1",
                    tag = "Dorsal Inferior (Ilíaco)",
                    defaultSets = 2,
                    cue = "Usa straps para aislar el dorsal del antebrazo. Tira hacia la cadera con el brazo rozando el torso. Cero torsión de columna."
                ),
                WorkoutExerciseTemplate(
                    name = "Face Pull en Polea con Cuerda",
                    target = "3 × 12 – 15",
                    rir = "RIR 0",
                    tag = "Deltoides Posterior & Rotadores",
                    defaultSets = 3,
                    cue = "Polea a la altura de la frente. Tira separando los extremos de la cuerda hacia las sienes con rotación externa final."
                ),
                WorkoutExerciseTemplate(
                    name = "Curl Predicador en Banco Scott",
                    target = "2 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Bíceps Cabeza Corta",
                    defaultSets = 2,
                    cue = "Axilas bien encajadas en el borde superior. Controla la bajada en 3 s hasta estirar casi por completo el codo."
                ),
                WorkoutExerciseTemplate(
                    name = "Curl con Brazos por Detrás en Polea (Bayesian)",
                    target = "2 × 10 – 12",
                    rir = "RIR 0",
                    tag = "Bíceps Cabeza Larga",
                    defaultSets = 2,
                    cue = "Un paso por delante de la polea. Brazos extendidos atrás: tensión hipertrófica máxima en la posición de estiramiento del bíceps."
                ),
                WorkoutExerciseTemplate(
                    name = "Crunch en Polea Alta",
                    target = "3 × 10 – 12",
                    rir = "RIR 0-1",
                    tag = "Recto Abdominal",
                    defaultSets = 3,
                    cue = "De rodillas frente a la polea con cuerda. Flexiona la columna enrollando el pecho hacia la pelvis (no flexiones la cadera)."
                ),
                WorkoutExerciseTemplate(
                    name = "Elevaciones de Piernas Colgado",
                    target = "2 × 12 – 15",
                    rir = "RIR 0-1",
                    tag = "Abdomen Inferior & Core",
                    defaultSets = 2,
                    cue = "Cuélgate de la barra o en paralelas. Sube las rodillas/piernas curvando la pelvis hacia arriba para activar el abdomen y no los flexores de cadera."
                )
            )
        ),
        "Día 3" to WorkoutDayPlan(
            dayKey = "Día 3",
            label = "Día 3",
            subtitle = "Pierna: Cuádriceps + Isquios + Aductores + Gemelos",
            biomechanicalNote = "ESTRATEGIA CIENTÍFICA: El Curl Femoral va primero para calentar y lubricar las rodillas antes de la Sentadilla Hack pesada. El Peso Muerto Rumano (RDL) se limita estrictamente a 2 series con straps para no agotar el sistema nervioso ni sobrecargar la espalda baja.",
            exercises = listOf(
                WorkoutExerciseTemplate(
                    name = "Curl Femoral en Máquina (Sentado priorizado)",
                    target = "3 × 10 – 12",
                    rir = "RIR 1",
                    tag = "Isquios (Pre-activación)",
                    defaultSets = 3,
                    cue = "Pre-activación y protección articular. En máquina sentada los isquios se estiran desde la cadera logrando un 20-30% más de hipertrofia que tumbado."
                ),
                WorkoutExerciseTemplate(
                    name = "Sentadilla Hack (Hack Squat)",
                    target = "3 × 6 – 10",
                    rir = "RIR 1",
                    tag = "Cuádriceps (Constructor Rey)",
                    defaultSets = 3,
                    cue = "1 Top Set pesada a 6-8 reps + 2 Back-off a 8-10 reps. Pies a anchura de hombros, rompe el paralelo manteniendo los talones soldados a la plataforma."
                ),
                WorkoutExerciseTemplate(
                    name = "Peso Muerto Rumano (RDL) con Straps",
                    target = "2 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Cadena Posterior & Isquios",
                    defaultSets = 2,
                    cue = "Con straps obligatorios. Desplaza la cadera hacia atrás como si quisieras tocar la pared, rodillas semiflexionadas fijas y columna 100% neutra."
                ),
                WorkoutExerciseTemplate(
                    name = "Extensión de Cuádriceps en Máquina",
                    target = "3 × 10 – 12",
                    rir = "RIR 0",
                    tag = "Cuádriceps Aislado (Recto Femoral)",
                    defaultSets = 3,
                    cue = "Pausa de 1 segundo arriba en máxima contracción. En la 3ª serie: realiza 3-5 repeticiones parciales en estiramiento al fallo."
                ),
                WorkoutExerciseTemplate(
                    name = "Máquina de Aductores",
                    target = "2 × 10 – 15",
                    rir = "RIR 0-1",
                    tag = "Aductores (Densidad de Muslo)",
                    defaultSets = 2,
                    cue = "Apertura amplia controlada, aguanta 1-2 s en estiramiento máximo. Da volumen y plenitud visual al muslo interno."
                ),
                WorkoutExerciseTemplate(
                    name = "Extensión de Gemelos (Prensa/De pie)",
                    target = "3 × 10 – 12",
                    rir = "RIR 0",
                    tag = "Gemelo (Gastrocnemio)",
                    defaultSets = 3,
                    cue = "Rodillas totalmente bloqueadas/rectas + PAUSA DE 2 SEGUNDOS abajo en máximo estiramiento para disipar la elasticidad del tendón de Aquiles."
                )
            )
        ),
        "Día 4" to WorkoutDayPlan(
            dayKey = "Día 4",
            label = "Día 4",
            subtitle = "Super-Torso: Pecho + Espalda (Básicos Pesados + Superserie Final)",
            biomechanicalNote = "ORGANIZACIÓN DE LA SESIÓN: Los primeros 4 compuestos pesados se realizan en series individuales independientes (con descansos completos de 2 a 2.5 min) para mover el máximo peso con técnica perfecta. Al final, el aislamiento de pecho y dorsal se combina en una superserie ágil y no extenuante.",
            exercises = listOf(
                WorkoutExerciseTemplate(
                    name = "Press Inclinado en Multipower (Smith)",
                    target = "3 × 6 – 8",
                    rir = "RIR 1",
                    tag = "Pecho Superior Pesado",
                    defaultSets = 3,
                    cue = "Banco a 30º. Continuidad de sobrecarga progresiva directa con el Día 1. Descansa 2.5 min entre series para batir marcas."
                ),
                WorkoutExerciseTemplate(
                    name = "Remo en T Agarre Ancho (con Apoyo)",
                    target = "3 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Espalda Densidad & Trapecio",
                    defaultSets = 3,
                    cue = "Realizado de forma independiente (no en superserie). Tracción horizontal pesada al esternón con retracción escapular estricta."
                ),
                WorkoutExerciseTemplate(
                    name = "Press Plano en Máquina Guiada",
                    target = "2 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Pectoral Medio & Esternal",
                    defaultSets = 2,
                    cue = "Empuje esternal estricto. Excelente estabilidad para exprimir el pectoral medio sin balanceos ni fatiga de estabilizadores."
                ),
                WorkoutExerciseTemplate(
                    name = "Jalón al Pecho Agarre Neutro",
                    target = "2 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Dorsal Ancho (Amplitud)",
                    defaultSets = 2,
                    cue = "Codos al plano sagital rozando el costado hacia los bolsillos. Tracción vertical limpia a RIR 1."
                ),
                WorkoutExerciseTemplate(
                    name = "Peck Deck en Contractora",
                    target = "2 × 10 – 12",
                    rir = "RIR 0",
                    tag = "Superserie Torso (Pecho)",
                    defaultSets = 2,
                    cue = "Haces 1 serie de Peck Deck al fallo, descansas 60 segundos y pasas al Pull Over. Congestión torácica profunda.",
                    superset = "Superserie"
                ),
                WorkoutExerciseTemplate(
                    name = "Pull Over en Polea Alta (Cuerda/Barra)",
                    target = "2 × 12 – 15",
                    rir = "RIR 0",
                    tag = "Superserie Torso (Dorsal)",
                    defaultSets = 2,
                    cue = "Haces 1 serie de Pull Over al fallo, descansas 60 segundos y vuelves al Peck Deck. Remate perfecto de dorsal y serratos.",
                    superset = "Superserie"
                )
            )
        ),
        "Día 5" to WorkoutDayPlan(
            dayKey = "Día 5",
            label = "Día 5",
            subtitle = "Brazo + Hombro 3D + Abdomen (Aislamiento y Superseries Antagonistas)",
            biomechanicalNote = "DETALLE 3D & BRAZO: Los deltoides laterales y posteriores se atacan primero con aislamiento fresco. Luego, bíceps y tríceps se trabajan en superseries antagonistas (Curl Barra Z + Press Katana; y Curl Bayesian + Extensión Cuerda), rematando con el bloque de abdomen.",
            exercises = listOf(
                WorkoutExerciseTemplate(
                    name = "Elevaciones Laterales en Polea (Unilateral)",
                    target = "3 × 12 – 15",
                    rir = "RIR 0",
                    tag = "Deltoides Lateral",
                    defaultSets = 3,
                    cue = "Polea a la altura de la cadera o rodilla. Cable por detrás o en diagonal. Tensión constante en el vientre lateral desde el inicio."
                ),
                WorkoutExerciseTemplate(
                    name = "Elevaciones Laterales en Máquina Sentado",
                    target = "2 × 10 – 12",
                    rir = "RIR 0",
                    tag = "Deltoides Lateral (Drop-Set)",
                    defaultSets = 2,
                    cue = "Máxima estabilidad. En la 2ª serie aplica una Drop-Set (baja un 30% del peso inmediatamente al fallo y saca 4-6 reps extra)."
                ),
                WorkoutExerciseTemplate(
                    name = "Pájaros en Máquina (Reverse Peck Deck)",
                    target = "3 × 12 – 15",
                    rir = "RIR 0",
                    tag = "Deltoides Posterior",
                    defaultSets = 3,
                    cue = "Agarre neutro, pecho en el respaldo. Conduce los codos hacia afuera en un arco amplio para redondear el hombro 3D."
                ),
                WorkoutExerciseTemplate(
                    name = "Curl con Barra Z de Pie",
                    target = "3 × 8 – 10",
                    rir = "RIR 1",
                    tag = "Bíceps (Masa Básica)",
                    defaultSets = 3,
                    cue = "Constructor de masa. Codos pegados a los costados, muñecas alineadas en la barra Z. Haces la serie, descansas 60 s y pasas a Katana.",
                    superset = "Superserie A"
                ),
                WorkoutExerciseTemplate(
                    name = "Press Katana Unilateral en Polea",
                    target = "3 × 10 – 12",
                    rir = "RIR 1",
                    tag = "Tríceps (Cabeza Larga)",
                    defaultSets = 3,
                    cue = "Brazo en plano escapular (~30º). Estiramiento extremo de la cabeza larga. Haces la serie, descansas 60 s y vuelves a Curl Barra Z.",
                    superset = "Superserie A"
                ),
                WorkoutExerciseTemplate(
                    name = "Curl con Brazos por Detrás en Polea (Bayesian)",
                    target = "2 × 10 – 12",
                    rir = "RIR 0",
                    tag = "Bíceps (Cabeza Larga)",
                    defaultSets = 2,
                    cue = "Paso al frente, brazo retrasado. Tensión máxima en estiramiento. Haces la serie, descansas 60 s y pasas a Extensión Cuerda.",
                    superset = "Superserie B"
                ),
                WorkoutExerciseTemplate(
                    name = "Extensión de Tríceps en Cuerda",
                    target = "2 × 12 – 15",
                    rir = "RIR 0",
                    tag = "Tríceps (Bombeo Final)",
                    defaultSets = 2,
                    cue = "Abre la cuerda al final del recorrido bloqueando 1 segundo. Haces la serie, descansas 60 s y vuelves a Bayesian.",
                    superset = "Superserie B"
                ),
                WorkoutExerciseTemplate(
                    name = "Crunch en Polea Alta",
                    target = "3 × 10 – 12",
                    rir = "RIR 0-1",
                    tag = "Recto Abdominal",
                    defaultSets = 3,
                    cue = "Flexión pura de columna vertebral enrollando el pecho hacia el ombligo. Máxima contracción."
                ),
                WorkoutExerciseTemplate(
                    name = "Elevaciones de Piernas Colgado",
                    target = "2 × 12 – 15",
                    rir = "RIR 0-1",
                    tag = "Abdomen Inferior & Core",
                    defaultSets = 2,
                    cue = "Colgado en barra o paralelas. Eleva rodillas curvando la pelvis hacia arriba para enfocar el core."
                )
            )
        )
    )

    fun todayRecommendedDayKey(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Día 1"
            Calendar.TUESDAY -> "Día 2"
            Calendar.WEDNESDAY -> "Día 3"
            Calendar.THURSDAY -> "Día 4"
            Calendar.FRIDAY -> "Día 5"
            else -> "Día 1"
        }
    }

    fun scheduledDayKeyForToday(): String? {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Día 1"
            Calendar.TUESDAY -> "Día 2"
            Calendar.WEDNESDAY -> "Día 3"
            Calendar.THURSDAY -> "Día 4"
            Calendar.FRIDAY -> "Día 5"
            else -> null
        }
    }

    fun normName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(raw.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val noAccents = decomposed.replace(Regex("[\\u0300-\\u036f]"), "")
        return noAccents
            .replace(Regex("[^a-z0-9_\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun resolveCanonicalDayKey(rawDayKey: String): String = OLD_DAY_MAP[rawDayKey] ?: rawDayKey

    fun computePrBadgeLabel(
        prevWeight: Double?,
        prevReps: Int?,
        curWeight: Double?,
        curReps: Int?
    ): String? {
        if (prevWeight == null || prevReps == null || curWeight == null || curReps == null || curReps <= 0) {
            return null
        }
        if (curWeight > prevWeight) {
            val diff = ((curWeight - prevWeight) * 10.0).roundToInt() / 10.0
            val formatted = if (diff % 1.0 == 0.0) diff.toInt().toString() else diff.toString()
            return "+$formatted kg"
        }
        if (curWeight == prevWeight && curReps > prevReps) {
            val repDiff = curReps - prevReps
            return "+$repDiff ${if (repDiff == 1) "rep" else "reps"}"
        }
        return null
    }

    fun formatWeightCompact(weight: Double?): String {
        if (weight == null) return ""
        return if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
    }
}
