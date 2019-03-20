package com.senstrgrs.griffinjohnson.sensortriggers

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_watch_comms.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ViewModel(val userRef : DocumentReference) : android.arch.lifecycle.ViewModel()
{
    private val HR_DATA : MutableLiveData<TreeMap<Int, Int>> by lazy {
        MutableLiveData<TreeMap<Int, Int>>().also{
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

    fun getHRData() : LiveData<TreeMap<Int, Int>>
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
        for(t in triggers.value!!)
        {
            var toStore = mutableMapOf<String, Any>()
            toStore.put("threshold", t.hr_val)
            userRef.collection("triggers").document(t.name)
                    .set(toStore, SetOptions.merge())
        }
        loadTriggers()
    }

    fun addTrigger(trig : Trigger)
    {
        var new = triggers.value
        new!!.add(trig)
        triggers.postValue(new)
    }

    fun addNewHR(hash : HashMap<Int, Int>)
    {
        var new = HR_DATA.value
        new!!.putAll(hash)
        HR_DATA.postValue(new)
    }

    fun loadHR()
    {
        userRef.collection("hr_data").document(getTimestamp()).get()
            .addOnCompleteListener {
                if(it.isSuccessful && it.result!!.exists())
                {
                    var db_hash = it.result!!.data as HashMap<String, Int>
                    var hr_map = TreeMap<Int, Int>()
                    for(key in db_hash.keys)
                    {
                        hr_map.put(key.toInt(), db_hash.get(key)!!)
                    }
                    HR_DATA.postValue(hr_map)
                }
            }
    }

    fun loadTriggers()
    {
        userRef.collection("triggers").get()
            .addOnCompleteListener {
                if(it.isSuccessful && !it.result!!.isEmpty)
                {
                    var data = it.result
                    triggers.postValue(getTriggersFromSnap(data!!)!!)
                }
            }
    }

    private fun getTriggersFromSnap(data : QuerySnapshot) : ArrayList<Trigger>
    {
        var list = ArrayList<Trigger>()

        for(tSnap in data.documents)
        {
            val thresh = tSnap.get("threshold") // todo new way to get properties. what if have >10? batch get properties regardless of name?
            list.add(Trigger(tSnap.id, (thresh as Long).toInt()))
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

