package se.aaro.waterino.wateringdata

import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject

class SharedPreferencesRepository @Inject constructor(
    private val preferences: SharedPreferences
) {
    companion object {
        private const val PUSH_NOTIFICATIONS_ENABLED = "ENABLE_NOTIFICATION"
        private const val PLANT_DATA_FIREBASE_TOPIC = "plantData"
    }

    var pushNotificationsEnabled: Boolean
        get() = preferences.getBoolean(PUSH_NOTIFICATIONS_ENABLED, true)
        set(enabled) {
            preferences.edit().putBoolean(PUSH_NOTIFICATIONS_ENABLED, enabled).apply()
            if (enabled) FirebaseMessaging.getInstance().subscribeToTopic(PLANT_DATA_FIREBASE_TOPIC)
            else FirebaseMessaging.getInstance().unsubscribeFromTopic(PLANT_DATA_FIREBASE_TOPIC)
        }
}
