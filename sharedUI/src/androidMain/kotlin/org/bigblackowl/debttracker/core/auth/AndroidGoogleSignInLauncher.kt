package org.bigblackowl.debttracker.core.auth

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.aakira.napier.Napier
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.platform.AndroidActivityProvider
import org.bigblackowl.debttracker.core.security.sha256
import org.bigblackowl.debttracker.core.security.toHex
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import java.security.SecureRandom

/**
 * Native "Sign in with Google" via Credential Manager: Google mints an ID token which
 * [AuthRepository.signInWithGoogleIdToken] exchanges for a Supabase session.
 *
 * A random [rawNonce] is generated, its SHA-256 (hex) handed to Google via [GetGoogleIdOption],
 * and the raw value passed to Supabase, which re-hashes and compares (so "Skip nonce checks" stays
 * off in the dashboard).
 */
class AndroidGoogleSignInLauncher(
    private val authRepository: AuthRepository,
) : GoogleSignInLauncher {

    override suspend fun signIn(): GoogleSignInOutcome {
        val activity = AndroidActivityProvider.current
            ?: return GoogleSignInOutcome.Failure("No foreground activity to host the Google sign-in sheet")
        val serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
        if (serverClientId.isBlank()) {
            return GoogleSignInOutcome.Failure("GOOGLE_SERVER_CLIENT_ID is not configured")
        }

        val rawNonce = ByteArray(32).also { SecureRandom().nextBytes(it) }.toHex()
        val hashedNonce = sha256(rawNonce.encodeToByteArray()).toHex()

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            // Also surface accounts the user has never used with this app (first-time sign-in).
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val idToken = try {
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                return GoogleSignInOutcome.Failure("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            return GoogleSignInOutcome.Cancelled
        } catch (e: NoCredentialException) {
            return GoogleSignInOutcome.Failure("No Google account is available on this device")
        } catch (e: GetCredentialException) {
            Napier.w(tag = TAG, throwable = e) { "Credential Manager getCredential() failed" }
            return GoogleSignInOutcome.Failure(e.message)
        } catch (e: GoogleIdTokenParsingException) {
            Napier.w(tag = TAG, throwable = e) { "Google ID token parsing failed" }
            return GoogleSignInOutcome.Failure(e.message)
        }

        return authRepository.signInWithGoogleIdToken(idToken, rawNonce).fold(
            onSuccess = { GoogleSignInOutcome.Success },
            onFailure = {
                Napier.w(tag = TAG, throwable = it) { "Supabase signInWith(IDToken) failed" }
                GoogleSignInOutcome.Failure(it.message)
            },
        )
    }

    private companion object {
        const val TAG = "AndroidGoogleSignInLauncher"
    }
}
