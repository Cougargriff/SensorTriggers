package com.senstrgrs.griffinjohnson.sensortriggers

import android.app.Dialog
import android.os.Parcelable
import com.google.firebase.firestore.DocumentSnapshot
import com.google.type.LatLng
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import kotlinx.android.synthetic.main.triggerdialog.*
import java.util.*



@Parcelize
class Trigger(var name : String, var hr_val : Int, var armed : Boolean, var type : String,
              var weather : Boolean, var location : Boolean, var lat : Double, var long : Double,
              var hr_context : Boolean, var time_triggered : Int = -1) : Parcelable
{
    // TODO : once a trigger threshold is met, it is turned off.

    fun toAnyMap() : MutableMap<String, Any>
    {
        var toStore = mutableMapOf<String, Any>()
        toStore.put("threshold", hr_val)
        toStore.put("armed", armed)
        toStore.put("type", type)
        toStore.put("weather", weather)
        toStore.put("location", location)
        toStore.put("latitude", lat)
        toStore.put("longitude", long)
        toStore.put("hr_context", hr_context)
        toStore.put("time_triggered", time_triggered)

        return toStore
    }

    companion object
    {
        fun fromSnap(tSnap : DocumentSnapshot) : Trigger
        {
            val thresh = (tSnap.get("threshold") as Long).toInt()
            val triggered = tSnap.get("armed") as Boolean
            val type = tSnap.get("type") as String
            val weather = tSnap.get("weather") as Boolean
            val location = tSnap.get("location") as Boolean
            val lat = tSnap.get("latitude") as Double
            val long = tSnap.get("longitude") as Double
            val hr_context = tSnap.get("hr_context") as Boolean
            val time = (tSnap.get("time_triggered") as Long).toInt()

            return Trigger(tSnap.id, thresh, triggered, type, weather, location, lat, long, hr_context, time)
        }


    }
}