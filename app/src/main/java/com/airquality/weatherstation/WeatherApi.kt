package com.airquality.weatherstation

import retrofit2.http.GET

interface WeatherApi {
    @GET("data")
    suspend fun getWeatherData(): WeatherData

    @GET("data")
    suspend fun getAllWeatherData(): List<WeatherData>
}