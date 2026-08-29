package org.bigblackowl.debttracker.core.security

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException

/**
 * Android [RestoreCredentialClient] over Credential Manager's Restore Credentials API
 * (Block Store-backed, no UI). Works on Android 9+ with Google Play services 24220000+.
 *
 * `context` is the application context — restore-credential create/get/clear never show a prompt,
 * so no Activity is required.
 */
class AndroidRestoreCredentialClient(private val context: Context) : RestoreCredentialClient {

    private val credentialManager = CredentialManager.create(context)

    override val isSupported: Boolean = true

    override suspend fun createRestoreKey(creationOptionsJson: String): String {
        val response = try {
            credentialManager.createCredential(
                context,
                CreateRestoreCredentialRequest(creationOptionsJson, isCloudBackupEnabled = true),
            )
        } catch (e: E2eeUnavailableException) {
            // No Google Backup / screen lock — fall back to a device-local (non-cloud) restore key.
            credentialManager.createCredential(
                context,
                CreateRestoreCredentialRequest(creationOptionsJson, isCloudBackupEnabled = false),
            )
        }
        return (response as CreateRestoreCredentialResponse).responseJson
    }

    override suspend fun getRestoreAssertion(requestOptionsJson: String): String? {
        val request = GetCredentialRequest(listOf(GetRestoreCredentialOption(requestOptionsJson)))
        return try {
            val credential = credentialManager.getCredential(context, request).credential
            (credential as? RestoreCredential)?.authenticationResponseJson
        } catch (e: NoCredentialException) {
            null
        } catch (e: GetCredentialException) {
            null
        }
    }

    override suspend fun clearRestoreKey() {
        credentialManager.clearCredentialState(
            ClearCredentialStateRequest(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL),
        )
    }
}
