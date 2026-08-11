package org.bigblackowl.debttracker.core.platform

import platform.UIKit.UIDevice

// Since iOS 16, UIDevice.name returns a generic model name (e.g. "iPhone") instead of the
// user-assigned one unless the app holds the paired device-name entitlement — acceptable here,
// this is only used to tell devices apart in a list, not to identify the user.
actual fun deviceDisplayName(): String = UIDevice.currentDevice.name
