package com.senstrgrs.griffinjohnson.sensortriggers

import android.os.Parcelable
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.android.parcel.Parcelize

@Parcelize
class LocationStamp(var epoch : Long, var lat : Double, var long : Double) : Parcelable
{
    // TODO : once a trigger threshold is met, it is turned off.

    fun toAnyMap() : MutableMap<String, Any>
    {
        var toStore = mutableMapOf<String, Any>()
        toStore.put("time", epoch)
        toStore.put("latitude", lat)
        toStore.put("longitude", long)


        return toStore
    }

    companion object
    {
        fun fromSnap(tSnap : DocumentSnapshot) : LocationStamp
        {
            val time = tSnap.get("time") as Long
            val lat = tSnap.get("latitude") as Double
            val long = tSnap.get("longitude") as Double


            return LocationStamp(time, lat, long)
        }


    }
}