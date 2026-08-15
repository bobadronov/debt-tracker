package org.bigblackowl.debttracker.domain.model

import io.ktor.http.Parameters
import io.ktor.http.formUrlEncode
import io.ktor.http.parseQueryString

/** Custom URI scheme (registered as an Android intent-filter / iOS `CFBundleURLTypes`, see
 * AndroidManifest.xml / Info.plist) — this is what makes a third-party QR scanner (Google Lens,
 * the stock camera app) recognize the code as a link and offer to open Debt Tracker with it,
 * instead of just showing inert text. Also lets [decode] cheaply reject a foreign/unrelated QR
 * code (a regular URL, another app's code) via [String.startsWith]. */
private const val DEEP_LINK_PREFIX = "debttracker://contact?"

/**
 * Encodes/decodes the small contact card (full name, phone, email) shared via the QR hub screen,
 * as a `debttracker://contact?name=...&phone=...&email=...` deep link. Query-string encoding
 * (rather than hand-rolled JSON) so [formUrlEncode]/[parseQueryString] handle UTF-8 percent-escaping
 * for free — this app's primary locales (uk/pl) routinely have non-Latin/diacritic names.
 */
object ContactQrPayload {
    fun encode(contact: ScannedContact): String {
        val params = Parameters.build {
            append("name", contact.fullName)
            contact.phone?.let { append("phone", it) }
            contact.email?.let { append("email", it) }
        }
        return DEEP_LINK_PREFIX + params.formUrlEncode()
    }

    /** null for anything that isn't a Debt Tracker contact link (wrong prefix, no/blank name) — never throws. */
    fun decode(raw: String): ScannedContact? {
        if (!raw.startsWith(DEEP_LINK_PREFIX)) return null
        val params = parseQueryString(raw.removePrefix(DEEP_LINK_PREFIX))
        val name = params["name"]?.takeIf(String::isNotBlank) ?: return null
        return ScannedContact(fullName = name, phone = params["phone"], email = params["email"])
    }
}
