package com.airquality.weatherstation

import com.google.gson.annotations.SerializedName

data class WeatherData(
    @SerializedName("ID") val id: String,
    @SerializedName("updated_on") val timestamp: String,
    @SerializedName("TEMP") val temperature: String,
    @SerializedName("HUMIDITY") val humidity: String,
    @SerializedName("AIRQUALITY") val airQuality: String
)