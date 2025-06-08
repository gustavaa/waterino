#include <Preferences.h>

Preferences preferences;

void saveIntSetting(const char* key, int value) {
    preferences.begin("waterino", false); // Namespace "waterino", read-write mode
    preferences.putInt(key, value);
    preferences.end();
    Serial.println(String(key) + " saved: " + String(value));
}

int loadIntSetting(const char* key, int defaultValue) {
    preferences.begin("waterino", true); // Namespace "waterino", read-only mode
    int value = preferences.getInt(key, defaultValue);
    preferences.end();
    Serial.println(String(key) + " loaded: " + String(value));
    return value;
}