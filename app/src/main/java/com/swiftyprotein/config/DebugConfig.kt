package com.swiftyprotein.config

import com.swiftyprotein.BuildConfig

object DebugConfig {
    /**
     * Master Debug Flag.
     *
     * Set to `true` to enable debug features and bypasses (e.g. "Enter without biometrics" button,
     * skipping email verification, auto-filling credentials) even on a physical external device.
     *
     * Options:
     * - `true`: Always force debug bypass ON (even on physical devices).
     * - `BuildConfig.DEBUG`: Automatically ON in Debug builds and OFF in Release builds.
     * - `false`: Always OFF.
     */
    val ENABLE_DEBUG_BYPASS: Boolean = BuildConfig.DEBUG
}
