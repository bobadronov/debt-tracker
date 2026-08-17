package org.bigblackowl.debttracker.core.qr

import org.bigblackowl.debttracker.core.platform.AppPlatform

/** Camera scanning needs a working QRKit scanner wired up for it — Web has none at all, and
 * Desktop's only wired up for the "show my QR" half (see QrHubScreen's self-review). Shared by
 * every screen that offers a QR-scan entry point (QrHubScreen, AddEditContactForm). */
val QR_SCAN_CAPABLE_PLATFORMS = setOf(AppPlatform.ANDROID, AppPlatform.IOS)
