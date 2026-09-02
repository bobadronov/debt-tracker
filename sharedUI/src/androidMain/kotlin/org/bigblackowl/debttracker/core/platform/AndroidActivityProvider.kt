package org.bigblackowl.debttracker.core.platform

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Holds a [WeakReference] to the current (single) [Activity] so a `commonMain`-driven flow that
 * needs a real Activity context — Credential Manager's "Sign in with Google" bottom sheet — can
 * reach it. `AppActivity` (`launchMode="singleInstance"`, so there is only ever one) sets it in
 * `onCreate` and clears it in `onDestroy`.
 *
 * Application context is enough for the existing `AndroidRestoreCredentialClient` (no UI), but the
 * Google sign-in sheet must be hosted by an Activity.
 */
object AndroidActivityProvider {
    private var ref: WeakReference<Activity>? = null

    val current: Activity? get() = ref?.get()

    fun set(activity: Activity) {
        ref = WeakReference(activity)
    }

    fun clear(activity: Activity) {
        if (ref?.get() === activity) ref = null
    }
}
