package com.applexis.logintestapp.network

import com.applexis.logintestapp.network.data.weather.Lang
import com.applexis.logintestapp.network.data.weather.Units
import com.applexis.logintestapp.network.data.weather.WeatherData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherAPI {
    @GET("weather")
    fun weatherByCoordinates(@Query("lat") latitude: Double,
                             @Query("lon") longitude: Double,
                             @Query("appid") appid: String,
                             @Query("units") units: String = Units.metric,
                             @Query("lang") lang: String = Lang.Russian
    ): Call<WeatherData>
}