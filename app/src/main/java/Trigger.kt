package com.senstrgrs.griffinjohnson.sensortriggers

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
data class Trigger(var name : String, var hr_val : Int) : Parcelable
{
    // TODO : once a trigger threshold is met, it is turned off.
    // to avoid repeat hits
}