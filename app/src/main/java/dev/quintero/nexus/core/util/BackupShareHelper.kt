package dev.quintero.nexus.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Shared Android file IO and ShareSheet helper used by Entreno and Enfoque JSON backup flows.
 */
object BackupShareHelper {

    fun writeJsonToUri(context: Context, uri: Uri, jsonContent: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(jsonContent.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: error("No se pudo abrir el archivo de destino.")
    }

    fun readTextFromUri(context: Context, uri: Uri): Result<String> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("No se pudo leer el archivo seleccionado.")
    }

    fun shareJsonFile(
        context: Context,
        fileName: String,
        jsonContent: String,
        chooserTitle: String,
        subject: String
    ): Result<Unit> = runCatching {
        val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(backupsDir, fileName)
        file.writeText(jsonContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
