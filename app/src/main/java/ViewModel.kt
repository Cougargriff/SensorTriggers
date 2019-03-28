package com.senstrgrs.griffinjohnson.sensortriggers

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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

    fun getTriggers() : LiveData<ArrayList<Trigger>>
    {
        return triggers
    }

    fun getHRData() : LiveData<HashMap<Int, Int>>
    {
        return HR_DATA
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
                var toStore = mutableMapOf<String, Any>()
                toStore.put("threshold", t.hr_val)
                toStore.put("armed", t.armed)
                toStore.put("type", t.type)
                toStore.put("weather", t.weather)
                toStore.put("location", t.location)
                userRef.collection("triggers").document(t.name)
                        .set(toStore, SetOptions.merge())
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
            }
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

            }

    }

    private fun getTriggersFromSnap(data : QuerySnapshot) : ArrayList<Trigger>
    {
        var list = ArrayList<Trigger>()

        for(tSnap in data.documents)
        {
            val thresh = tSnap.get("threshold") // todo new way to get properties. what if have >10? batch get properties regardless of name?
            val triggered = tSnap.get("armed")
            val type = tSnap.get("type")
            val weather = tSnap.get("weather")
            val location = tSnap.get("location")
            list.add(Trigger(tSnap.id, (thresh as Long).toInt(), triggered as Boolean, type as String,
                    weather  as Boolean, location as Boolean))
        }
        return list
    }

    private fun getTimestamp() : String
    {
        return DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
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

