package com.senstrgrs.griffinjohnson.sensortriggers

import com.google.common.io.Resources
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

// Dark Sky Data classes used for GSON (json parsing)
data class DarkResponse(var latitude : Double, var longitude : Double, var hourly : Hourly, var currently : Currently)
data class Hourly(var summary : String, var data : List<DataPoint>)
data class Currently(var time : Int, var summary : String, var temperature : Double, var apparentTemperature : Double)
data class DataPoint(var time : Int, var summary : String)

object DarkSky
{
    //https://api.darksky.net/forecast/[key]/[latitude],[longitude],[time]
    private lateinit var endpoint : String

    init
    {
        endpoint = "https://api.darksky.net/forecast/" + Resources.getResource("dark_key")
    }

    fun getWeatherHistory(time : Int, lat : Double, long : Double) : DarkResponse?
    {
        var url = endpoint + "/" + lat.toString() + "," + long.toString() + "," + time

        val client = OkHttpClient()
        var request = Request.Builder()
                .url(url)
                .build()

        val response = client.newCall(request).execute()
        var darkReturn : DarkResponse?
        when(response.isSuccessful)
        {
            true -> {
                darkReturn = Gson().fromJson(response.body().toString(), DarkResponse::class.java)
            }
            false -> {
                darkReturn = null
            }
        }
        return darkReturn
    }
}

