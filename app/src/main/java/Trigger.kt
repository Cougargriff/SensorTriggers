package com.senstrgrs.griffinjohnson.sensortriggers

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
data class Trigger(var name : String, var hr_val : Int) : Parcelable
{
}