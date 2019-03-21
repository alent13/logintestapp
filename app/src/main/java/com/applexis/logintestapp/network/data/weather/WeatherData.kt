package com.applexis.logintestapp.network.data.weather

class WeatherData (
    var name: String,
    var code: Int,
    var weather: List<WeatherInfo>,
    var main: MainWeatherInfo,
    var wind: WindInfo,
    var clouds: CloudInfo
)