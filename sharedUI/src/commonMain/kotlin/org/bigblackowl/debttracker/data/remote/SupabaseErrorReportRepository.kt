package org.bigblackowl.debttracker.data.remote

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.platform.systemInfo
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.ErrorSeverity
import org.bigblackowl.debttracker.domain.repository.ErrorReportRepository

@Serializable
private data class ReportClientErrorParams(
    @SerialName("p_device_id") val deviceId: String,
    @SerialName("p_platform") val platform: String,
    @SerialName("p_app_version") val appVersion: String,
    @SerialName("p_severity") val severity: String,
    @SerialName("p_tag") val tag: String?,
    @SerialName("p_message") val message: String,
    @SerialName("p_stack_trace") val stackTrace: String?,
    @SerialName("p_system_info") val systemInfo: String?,
)

/**
 * [ErrorReportRepository] backed by the `report_client_error` RPC (migrations 0018/0019) — a plain
 * table insert is deliberately not used: the RPC does server-side rate limiting and length capping,
 * so a crash-looping client (signed in or not) can't flood `client_error_log`, and `user_id` is
 * taken from the caller's JWT server-side rather than a client-supplied field.
 * `AppSettings.deviceSessionId` doubles as the correlation id here — it already exists for
 * `user_sessions` (Active devices) and is neither secret nor privileged, so reusing it avoids
 * minting a second per-install id. [org.bigblackowl.debttracker.core.platform.systemInfo] adds a
 * short OS/device summary (no user-identifying info) so a report is actionable on its own.
 *
 * Every failure is swallowed: this must never throw into whatever call site was already mid-error,
 * and must be silent on a fresh install with no network yet.
 */
class SupabaseErrorReportRepository(
    private val client: SupabaseClient,
    private val appSettings: AppSettings,
) : ErrorReportRepository {

    override suspend fun report(severity: ErrorSeverity, tag: String?, message: String, stackTrace: String?) {
        runCatching {
            client.postgrest.rpc(
                "report_client_error",
                ReportClientErrorParams(
                    deviceId = appSettings.deviceSessionId,
                    platform = currentPlatform.name,
                    appVersion = BuildConfig.APP_VERSION,
                    severity = severity.name,
                    tag = tag,
                    message = message,
                    stackTrace = stackTrace,
                    systemInfo = runCatching { systemInfo() }.getOrNull(),
                ),
            )
        }.onFailure {
            // Napier.v, not .w/.e: this repository is itself what ErrorReportingAntilog's WARNING/
            // ERROR hook forwards into — logging a failure here at WARN+ would try to report the
            // failure to report, looping forever the moment reporting is broken (e.g. offline).
            Napier.v(tag = "SupabaseErrorReportRepository", throwable = it) { "report_client_error failed" }
        }
    }
}
