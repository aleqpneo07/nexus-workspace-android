# Nexus Workspace (Android)

Suite modular **offline-first** en Kotlin y Jetpack Compose que unifica en una única aplicación móvil y tablet cuatro espacios de productividad personal: **Horario Académico**, **Entreno**, **Night Planner** y **Enfoque (Kanban)**, con sincronización multi-dispositivo cifrada de extremo a extremo (**AES-256-GCM**) e integración opcional con **Google Calendar (OAuth 2.0 PKCE)**.

---

## Arquitectura del Proyecto

```text
app/src/main/java/dev/quintero/nexus/
├── NexusApplication.kt              # Inicialización temprana de configuración local
├── MainActivity.kt                  # Shell de navegación modular y gestión OAuth PKCE
├── core/
│   ├── crypto/SyncCryptoManager.kt  # Derivación PBKDF2-HMAC-SHA256 + cifrado AES-256-GCM
│   ├── database/NexusDatabase.kt    # Room v3 (NightTask, WorkoutSession/Draft, Enfoque)
│   ├── design/                      # Paleta nocturna Material 3 (`NexusTheme`), tipografía y tokens
│   └── util/                        # Formateo temporal, rangos RFC3339 y soporte Base64URL
├── feature/
│   ├── hub/                         # Dashboard principal, estado en vivo e indicador de nube
│   ├── schedule/                    # Motor dinámico de horarios JSON con descansos y solapamientos
│   ├── workout/                     # Rutina 5 días, historial multi-fecha, alias y backups JSON
│   ├── planner/                     # Captura rápida (#/@/!), timeline horario y Google Calendar API
│   └── focus/                       # Tablero Kanban táctil, lista de entregas, calendario y subtareas
└── sync/
    ├── MisCosasSyncModels.kt        # Motor LWW + tombstones deterministas y derivación de canales
    └── MisCosasCloudSyncManager.kt  # Orquestador SSE/poll sobre HTTPS cifrado con E2EE
```

---

## Seguridad y Privacidad

* **Sincronización Cifrada (E2EE)**:
  * A partir de un identificador de cuenta y un PIN se deriva en local una clave simétrica de 256 bits mediante **PBKDF2-HMAC-SHA256** (31.000 iteraciones, salt determinista con dominio `nexus:e2ee:v1`).
  * Cada paquete enviado entre móvil y tablet se encapsula en **AES-256-GCM** con vector IV aleatorio de 12 bytes y datos autenticados adicionales (**AAD** `v1|syncId|senderDeviceId`).
  * El servidor relay (`ntfy.sh`) únicamente recibe texto cifrado autenticado y el PIN nunca se persiste en disco.
  * Cualquier paquete manipulado, con AAD alterado o cifrado con otro PIN falla de forma segura (`decryptEnvelope` devuelve `null`).
* **Separación de Datos Personales y Repositorio Público**:
  * El código fuente incluye exclusivamente un horario de demostración genérico (`app/src/main/assets/demo_schedule.json`), un esquema documentado (`docs/schedule.example.json`) y credenciales OAuth vacías por defecto.
  * Los ficheros personales (`local-config/assets/schedule.json`, `local.properties`, `.env`, keystores `.jks` y `.apk`) están excluidos explícitamente mediante `.gitignore`.

---

## Configuración Opcional en Local

### 1. Horario personalizado
Para compilar un APK con tu propio horario sin exponerlo en Git:
1. Toma como plantilla [docs/schedule.example.json](docs/schedule.example.json).
2. Guarda tu JSON en `local-config/assets/schedule.json`.
3. Gradle monta `local-config/assets` automáticamente cuando el directorio existe en disco; en su ausencia o en CI usa `demo_schedule.json`.

### 2. Google Calendar OAuth 2.0 (PKCE)
Para inyectar tu **OAuth 2.0 Client ID** sin versionar credenciales:
1. Copia `local.properties.example` a `local.properties`.
2. Configura `sdk.dir` y añade:
   ```properties
   nexus.oauthClientId=TU_CLIENT_ID.apps.googleusercontent.com
   ```
   *(También puedes introducir o modificar el Client ID en tiempo de ejecución dentro de la pestaña Google Calendar de la aplicación).*
3. URI de redirección registrada en la app: `dev.quintero.nexus.app:/oauth2redirect`.

---

## Compilación y Pruebas

Requisitos: **JDK 17** y **Android SDK (Compile/Target SDK 36, Min SDK 24)**.

```bash
# Ejecutar batería de tests unitarios (JVM + Robolectric)
./gradlew testDebugUnitTest --no-daemon

# Generar APK Debug lista para instalar
./gradlew assembleDebug --no-daemon
```

La integración continua en [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml) ejecuta `testDebugUnitTest` y `assembleDebug` sobre la configuración pública de demostración en cada push o pull request a `main`.

---

## Licencia

Distribuido bajo licencia MIT. Consulta [`LICENSE`](LICENSE) para más detalles.
