#include "JSON.h"
#include "JSONVar.h"
#include <iostream>

enum WateringMode {
    AUTOMATIC,
    FIXED_FREQ
};

// Define constants
const int MILLISECONDS_MINUTE = 60000;
const int MILLISECONDS_HOUR = 3600000;

#define KEY_WATERING_MODE "mode"
#define KEY_UPDATE_FREQ "u_freq"
#define KEY_WATERING_FREQ "w_freq"
#define KEY_WATERING_TIME "w_time"
#define KEY_ENABLED "enabled"
#define KEY_MAX_TEMP "max_temp"
#define KEY_MOISTURE_THRESHOLD "threshold"

class WaterinoSettings {
  private:
      WateringMode currentWateringMode;
      bool isEnabled;
      int measuringFrequency;
      int wateringFrequency;
      int wateringTime;  // in milliseconds
      int maxWateringTemperature;
      int soilMoistureThreshold;

  public:
      // Constructor with default values
      WaterinoSettings()
          : currentWateringMode(AUTOMATIC),
            isEnabled(true),
            measuringFrequency(MILLISECONDS_MINUTE * 15),
            wateringFrequency(MILLISECONDS_HOUR * 6),
            wateringTime(3500),  // 350ml
            maxWateringTemperature(35),
            soilMoistureThreshold(10)
      {
      }

      String toJsonString() {
        JSONVar result;
        if (getCurrentWateringMode() == AUTOMATIC) {
          result["watering_mode"] = "AUTOMATIC";
        } else {
          result["watering_mode"] = "FIXED_FREQ";
        }
        result["is_enabled"] = getIsEnabled();
        result["update_freq"] = getMeasuringFrequency();
        result["watering_freq"] = getWateringFrequency();
        result["threshold"] = getSoilMoistureThreshold();
        result["watering_time"] = getWateringTime();
        result["max_temperature"] = getMaxWateringTemperature();
        return JSON.stringify(result);
      }

      WateringMode getCurrentWateringMode() const { return currentWateringMode; }
      void setCurrentWateringMode(WateringMode mode) { currentWateringMode = mode; }

      bool getIsEnabled() const { return isEnabled; }
      void setIsEnabled(bool enabled) { isEnabled = enabled; }

      int getMeasuringFrequency() const { return measuringFrequency; }
      void setMeasuringFrequency(int frequency) { measuringFrequency = frequency; }

      int getWateringFrequency() const { return wateringFrequency; }
      void setWateringFrequency(int frequency) { wateringFrequency = frequency; }

      int getWateringTime() const { return wateringTime; }
      void setWateringTime(int time) { wateringTime = time; }

      int getMaxWateringTemperature() const { return maxWateringTemperature; }
      void setMaxWateringTemperature(int temperature) { maxWateringTemperature = temperature; }

      int getSoilMoistureThreshold() const { return soilMoistureThreshold; }
      void setSoilMoistureThreshold(int threshold) { soilMoistureThreshold = threshold; }
};