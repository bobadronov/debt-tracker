package org.bigblackowl.debttracker.core.auth

import com.sun.net.httpserver.HttpServer
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import kotlin.time.Duration.Companion.minutes

/**
 * Desktop "Continue with Google": supabase-kt opens the system browser at the Supabase authorize
 * URL; Supabase (after Google consent) redirects to a one-shot `http://127.0.0.1:<port>` HTTP
 * listener we run here, which captures the PKCE `code` and hands it to `exchangeCodeForSession`.
 *
 * The `127.0.0.1:*` redirect target is allow-listed in the Supabase dashboard. The listener uses
 * the JDK's built-in [HttpServer] (`jdk.httpserver`) — no extra dependency.
 */
class DesktopGoogleSignInLauncher(
    private val client: SupabaseClient,
) : GoogleSignInLauncher {

    override suspend fun signIn(): GoogleSignInOutcome = withContext(Dispatchers.IO) {
        val callback = CompletableDeferred<Result<String>>()
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange ->
            try {
                val params = (exchange.requestURI.rawQuery ?: "").split("&")
                    .mapNotNull { pair ->
                        val i = pair.indexOf('=')
                        if (i <= 0) null
                        else URLDecoder.decode(pair.substring(0, i), "UTF-8") to
                            URLDecoder.decode(pair.substring(i + 1), "UTF-8")
                    }.toMap()
                val code = params["code"]
                val error = params["error_description"] ?: params["error"]
                // Ignore incidental requests (favicon etc.) that carry neither — only the real
                // OAuth redirect has `code` or `error`.
                if (code == null && error == null) {
                    exchange.sendResponseHeaders(204, -1)
                    exchange.close()
                    return@createContext
                }
                val (status, body) = if (code != null) 200 to RESULT_PAGE_OK else 400 to RESULT_PAGE_ERR
                val bytes = body.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                exchange.sendResponseHeaders(status, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                callback.complete(
                    if (code != null) Result.success(code)
                    else Result.failure(IllegalStateException(error ?: "No authorization code in callback")),
                )
            } catch (e: Exception) {
                callback.complete(Result.failure(e))
            }
        }
        server.start()

        try {
            val redirectUrl = "http://127.0.0.1:${server.address.port}"
            client.auth.signInWith(Google, redirectUrl = redirectUrl)

            val received = withTimeoutOrNull(5.minutes) { callback.await() }
                ?: return@withContext GoogleSignInOutcome.Cancelled

            received.fold(
                onSuccess = { code ->
                    client.auth.exchangeCodeForSession(code)
                    GoogleSignInOutcome.Success
                },
                onFailure = {
                    Napier.w(tag = TAG, throwable = it) { "Google OAuth callback carried no usable code" }
                    GoogleSignInOutcome.Failure(it.message)
                },
            )
        } catch (e: Exception) {
            Napier.w(tag = TAG, throwable = e) { "Desktop Google sign-in failed" }
            GoogleSignInOutcome.Failure(e.message)
        } finally {
            server.stop(0)
        }
    }

    private companion object {
        const val TAG = "DesktopGoogleSignInLauncher"
        const val RESULT_PAGE_OK =
            "<!doctype html><meta charset=utf-8><title>Debt Tracker</title>" +
                "<body style=\"font-family:system-ui;text-align:center;padding-top:3rem\">" +
                "<h2>You're signed in</h2><p>You can close this window and return to Debt Tracker.</p>"
        const val RESULT_PAGE_ERR =
            "<!doctype html><meta charset=utf-8><title>Debt Tracker</title>" +
                "<body style=\"font-family:system-ui;text-align:center;padding-top:3rem\">" +
                "<h2>Sign-in failed</h2><p>You can close this window and try again in Debt Tracker.</p>"
    }
}
