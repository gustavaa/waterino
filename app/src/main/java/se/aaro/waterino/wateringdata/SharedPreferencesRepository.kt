package se.aaro.waterino.wateringdata

import android.content.SharedPreferences
import javax.inject.Inject

class SharedPreferencesRepository @Inject constructor(
    private val preferences: SharedPreferences
) {
    companion object {
        private const val PUSH_NOTIFICATIONS_ENABLED = "ENABLE_NOTIFICATION"
    }

    var pushNotificationsEnabled: Boolean
        get() = preferences.getBoolean(PUSH_NOTIFICATIONS_ENABLED, true)
        set(enabled) = preferences.edit().putBoolean(PUSH_NOTIFICATIONS_ENABLED, enabled).apply()
}
