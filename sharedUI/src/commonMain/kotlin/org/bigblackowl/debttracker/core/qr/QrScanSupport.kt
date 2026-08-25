package org.bigblackowl.debttracker.core.qr

import org.bigblackowl.debttracker.core.platform.AppPlatform

/** Platforms with a working camera-based QRKit scanner. Desktop/Web have none (QRKit's own
 * scanner is either dead-excluded on Desktop or has no js/wasmJs target at all on Web), so they
 * fall back to [ContactQrFilePickerContent] instead — a locally-picked image, decoded without a
 * camera — rather than losing the scan feature outright. Shared by every screen that offers a
 * QR-scan entry point (QrHubScreen, AddEditContactForm) to choose between the two. */
val QR_SCAN_CAPABLE_PLATFORMS = setOf(AppPlatform.ANDROID, AppPlatform.IOS)
