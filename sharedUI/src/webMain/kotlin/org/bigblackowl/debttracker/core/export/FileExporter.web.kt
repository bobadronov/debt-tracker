package org.bigblackowl.debttracker.core.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aakira.napier.Napier

/** Web-експорт через Supabase Edge Function (спек §6, п.8) — Фаза 10. */
private class NoOpFileExporter : FileExporter {
    override suspend fun saveCsv(fileName: String, content: String) {
        Napier.d(tag = "FileExporter") { "saveCsv($fileName): Web-експорт ще не реалізовано (Фаза 10)" }
    }

    override suspend fun savePdf(
        fileName: String,
        title: String,
        description: String,
        headers: List<String>,
        rows: List<ExportRow>,
    ) {
        Napier.d(tag = "FileExporter") { "savePdf($fileName): Web-експорт ще не реалізовано (Фаза 10)" }
    }
}

@Composable
actual fun rememberFileExporter(): FileExporter = remember { NoOpFileExporter() }
