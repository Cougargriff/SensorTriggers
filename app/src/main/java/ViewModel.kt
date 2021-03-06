package com.senstrgrs.griffinjohnson.sensortriggers

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ViewModel(val userRef : DocumentReference) : android.arch.lifecycle.ViewModel()
{
    private val HR_DATA : MutableLiveData<HashMap<Int, Int>> by lazy {
        MutableLiveData<HashMap<Int, Int>>().also{
            loadHR()
        }
    }

    private val triggers : MutableLiveData<ArrayList<Trigger>> by lazy {
        MutableLiveData<ArrayList<Trigger>>().also {
            loadTriggers()
        }
    }

    private val location_history : MutableLiveData<ArrayList<LocationStamp>> by lazy {
        MutableLiveData<ArrayList<LocationStamp>>().also {
            loadLocationStamps()
        }
    }



    private val DATE_FORMAT = "yyyy-MM-dd"

    fun getTriggers() : LiveData<ArrayList<Trigger>>
    {
        return triggers
    }

    fun getHRData() : LiveData<HashMap<Int, Int>>
    {
        return HR_DATA
    }

    fun getLocationHistory() : LiveData<ArrayList<LocationStamp>>
    {
        return location_history
    }


    fun syncHR(cb : () -> Unit)
    {
        var toStore = mutableMapOf<String, Any>()

        for(key in HR_DATA.value!!.keys)
        {
            toStore.put(key.toString(), HR_DATA.value!!.get(key)!!)
        }

        userRef.collection("hr_data").document(getTimestamp())
            .set(toStore, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("db", "DocumentSnapshot added with ID: ")
                // stop graph progress bar on db callback
                cb()
            }
            .addOnFailureListener {
                Log.d("db error", "error adding document")
                cb()
            }
    }

    fun syncTriggers()
    {
        if(triggers.value != null)
        {
            for(t in triggers.value!!)
            {
                userRef.collection("triggers").document(t.name)
                        .set(t.toAnyMap(), SetOptions.merge())
            }
        }
        loadTriggers()

    }

    fun addTrigger(trig : Trigger)
    {
        var new = triggers.value
        if(new == null)
        {
            new = ArrayList<Trigger>().apply {
                add(trig)
            }
        }
        else
        {
            new!!.add(trig)
        }
        triggers.postValue(new)
    }

    fun addNewHR(hash : HashMap<Int, Int>)
    {
        var new = HR_DATA.value
        if(new != null)
        {
            new!!.putAll(hash)
            HR_DATA.postValue(new)
        }
        else
        {
            HR_DATA.value = hash
        }

    }

    fun loadHR()
    {
        userRef.collection("hr_data").document(getTimestamp()).get()
            .addOnCompleteListener {
                if(it.isSuccessful && it.result!!.exists())
                {
                    var db_hash = it.result!!.data as HashMap<String, Int>
                    var hr_map = HashMap<Int, Int>()
                    for(key in db_hash.keys)
                    {
                        hr_map.put(key.toInt(), db_hash.get(key)!!)
                    }
                    HR_DATA.postValue(hr_map)
                }

                if(!it.result!!.exists())
                {
                    HR_DATA.value = HashMap()
                }
            }
    }

    fun loadLocationStamps()
    {
        userRef.collection("location_history").document(getTimestamp())
                .collection("locations").orderBy("time", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener {
                    if(it.isSuccessful && !it.result!!.isEmpty)
                    {
                        var data = it.result
                        val loc_hist = getLocationsFromSnap(data!!)
                        location_history.value = loc_hist
                    }

                    if(it.result!!.isEmpty)
                    {
                        location_history.value = ArrayList()
                    }
                }
    }

    private fun getLocationsFromSnap(data : QuerySnapshot) : ArrayList<LocationStamp>
    {
        var hist = ArrayList<LocationStamp>()

        for(loc in data.documents)
        {
            hist.add(LocationStamp.fromSnap(loc))
        }
        return hist
    }

    fun syncLocationStamps()
    {
        if(location_history.value != null)
        {
            for(l in location_history.value!!)
            {
                userRef.collection("location_history").document(getTimestamp())
                        .collection("locations").document(l.epoch.toString())
                        .set(l.toAnyMap(), SetOptions.merge()).addOnCompleteListener {
                            Log.i("loc stamp sync", it.toString())
                        }
            }
        }
    }

    fun addLocationStamp(stamp : LocationStamp)
    {
        var new = location_history.value
        if(new == null)
        {
            new = ArrayList<LocationStamp>().apply {
                add(stamp)
            }
        }
        else
        {
            new!!.add(stamp)
        }
        location_history.postValue(new)
    }

    fun setTriggers(t : ArrayList<Trigger>)
    {
        triggers.value = t
    }

    fun loadTriggers()
    {
        userRef.collection("triggers").get()
            .addOnCompleteListener {
                if(it.isSuccessful && !it.result!!.isEmpty)
                {
                    var data = it.result
                    val t = getTriggersFromSnap(data!!)
                    triggers.value = t
                }

                if(it.result!!.isEmpty)
                {
                    triggers.value = ArrayList()
                }
            }

    }

    private fun getTriggersFromSnap(data : QuerySnapshot) : ArrayList<Trigger>
    {
        var list = ArrayList<Trigger>()

        for(tSnap in data.documents)
        {
            var t = Trigger.fromSnap(tSnap)
            if(t != null)
            {
                list.add(t)
            }
        }
        return list
    }

    private fun getTimestamp() : String
    {
        return DateTimeFormatter
            .ofPattern(DATE_FORMAT)
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())

    }

}

class ViewModelFactory(val userRef : DocumentReference) : android.arch.lifecycle.ViewModelProvider.Factory
{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return ViewModel(userRef) as T
    }
}

