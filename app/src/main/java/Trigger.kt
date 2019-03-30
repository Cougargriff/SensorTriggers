package com.senstrgrs.griffinjohnson.sensortriggers

import android.os.Parcelable
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.android.parcel.Parcelize
import java.util.*



@Parcelize
class Trigger(var name : String, var hr_val : Int, var armed : Boolean, var type : String,
              var weather : Boolean, var location : Boolean) : Parcelable
{
    // TODO : once a trigger threshold is met, it is turned off.
    // to avoid repeat hits

    fun toAnyMap() : MutableMap<String, Any>
    {
        var toStore = mutableMapOf<String, Any>()
        toStore.put("threshold", hr_val)
        toStore.put("armed", armed)
        toStore.put("type", type)
        toStore.put("weather", weather)
        toStore.put("location", location)
        return toStore
    }

    companion object
    {
        fun fromSnap(tSnap : DocumentSnapshot) : Trigger
        {
            val thresh = tSnap.get("threshold")
            val triggered = tSnap.get("armed")
            val type = tSnap.get("type")
            val weather = tSnap.get("weather")
            val location = tSnap.get("location")
            return Trigger(tSnap.id, (thresh as Long).toInt(), triggered as Boolean, type as String,
                    weather  as Boolean, location as Boolean)
        }
    }
}