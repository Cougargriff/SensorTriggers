package com.senstrgrs.griffinjohnson.sensortriggers

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.db.chart.animation.Animation
import com.db.chart.model.LineSet
import com.db.chart.model.Point
import com.db.chart.view.LineChartView
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions

import kotlinx.android.synthetic.main.activity_watch_comms.*
import org.jetbrains.anko.toast
import java.lang.ClassCastException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.timerTask


class WatchComms : AppCompatActivity()
{

    lateinit var connectiq : ConnectIQ
    lateinit var available : IQDevice
    lateinit var paired : List<IQDevice>
    lateinit var chartView : LineChartView
    lateinit var app : IQApp
    lateinit var userRef : DocumentReference

    var status = "OFF"
    var HR_DATA = HashMap<Int, Int>()
    var trigger_list = ArrayList<Trigger>()
    var mAuth  = FirebaseAuth.getInstance()
    var db = FirebaseFirestore.getInstance()



    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_comms)
        comms_view.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.blueish))


        window.navigationBarColor = ContextCompat.getColor(baseContext, R.color.blueish)
        window.statusBarColor = ContextCompat.getColor(baseContext, R.color.blueish)

        chartView = findViewById(R.id.chartView)
        graphLoad.isIndeterminate = true


        userRef = db.collection("users").document(mAuth.uid.toString())
        // check if there are any values for today already
        getCurrentDB()
        initialize()

        transmit_button.setOnClickListener {
            when (status)
            {
                "OFF" -> transmit("ON")
                "ON" -> transmit("OFF")
            }
        }

        sync_button.setOnClickListener {
            initialize()
            graphLoad.visibility = View.VISIBLE

            // set timeout for progress bar
            val timer = Timer()
            timer.schedule(timerTask {
                if (graphLoad.visibility == View.VISIBLE)
                    graphLoad.visibility = View.INVISIBLE
                else
                    runOnUiThread {
                        toast("Sync timed out...")
                    }
            }, 5000)


            transmit_string("sync")
        }

        addTrigger.setOnClickListener {
            showTriggerCreationDialog()
        }

        viewTriggers.setOnClickListener {
            val intent = Intent(this, TriggerView::class.java)
            intent.putExtra("triggers", trigger_list)

            ContextCompat.startActivity(this, intent, null)

        }

    }

    // *********************
    // Handling App Behavior
    // *********************

    override fun onStop()
    {
        super.onStop()

    }

    override fun onBackPressed()
    {
        if(connectiq != null)
        {
            connectiq.unregisterForApplicationEvents(available, app) //  de-register from watch
            mAuth.signOut()
        }
        super.onBackPressed()
    }

    override fun onPause()
    {
        super.onPause()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        connectiq.unregisterForApplicationEvents(available, app) //  de-register from watch
        mAuth.signOut()
    }

    // *********************


    fun showTriggerCreationDialog()
    {
        var builder = AlertDialog.Builder(this, android.app.AlertDialog.THEME_TRADITIONAL)


        builder.setView(R.layout.triggerdialog)
                .setPositiveButton("Create Trigger") { dialog, _ ->
                    val d = dialog as Dialog
                    val hr_edit = d.findViewById<EditText>(R.id.hr)
                    val name_edit = d.findViewById<EditText>(R.id.trigger_name)

                    if(name_edit.text.length > 0 && hr_edit.text.length > 0)
                    {
                        trigger_list.add(Trigger(name_edit.text.toString(), hr_edit.text.toString().toInt()))
                        syncTriggers()

                        toast("New Trigger was added to application")

                        dialog.dismiss()
                    }
                    else
                    {
                        showTriggerCreationDialog()
                    }
               }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }
        builder.show()
    }

    fun syncHR()
    {
        var toStore = mutableMapOf<String, Any>()

        for(key in HR_DATA.keys)
        {
            toStore.put(key.toString(), HR_DATA.get(key)!!)
        }

        userRef.collection("hr_data").document(getTimestamp())
            .set(toStore, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("db", "DocumentSnapshot added with ID: ")

                // stop graph progress bar on db callback
                onSyncComplete()
            }
            .addOnFailureListener {
                Log.d("db error", "error adding document")
            }
    }

    fun syncTriggers()
    {

        for(t in trigger_list)
        {
            var toStore = mutableMapOf<String, Any>()
            toStore.put("threshold", t.hr_val)
            userRef.collection("triggers").document(t.name)
                    .set(toStore, SetOptions.merge())
        }

    }

    // gets and sets whatever HR data is in the db for the current day
    fun getCurrentDB()
    {
        userRef.collection("hr_data").document(getTimestamp()).get()
                .addOnCompleteListener {
                    if(it.isSuccessful && it.result!!.exists())
                    {
                        var db_hash = it.result!!.data as HashMap<String, Int>
                        for(key in db_hash.keys)
                        {
                            HR_DATA.put(key.toInt(), db_hash.get(key)!!)
                        }
                        updateChart(HR_DATA, chartView)
                    }
                    graphLoad.visibility = View.INVISIBLE
                }


        userRef.collection("triggers").get()
                .addOnCompleteListener {
                    if(it.isSuccessful && !it.result!!.isEmpty)
                    {
                        var data = it.result
                        getTriggersFromSnap(data!!)
                    }
                }
    }

    fun getTriggersFromSnap(data : QuerySnapshot)
    {
        for(tSnap in data.documents)
        {
            val thresh = tSnap.get("threshold") // todo new way to get properties. what if have >10?
            trigger_list.add(Trigger(tSnap.id, (thresh as Long).toInt()))
        }
    }

    fun onSyncComplete()
    {
        graphLoad.visibility = View.INVISIBLE
    }

    fun getTimestamp() : String
    {
        return DateTimeFormatter
                .ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
    }

    fun triggerFilter(sample : HashMap<Int, Int>)
    {

    }


    fun updateChart(sample : HashMap<Int, Int>, chartView: LineChartView)
    {
        chartView.reset()
        var ln = LineSet()
        var keys = sample.keys.sorted()
        for(key in keys)
        {
            val value = sample.get(key)!!.toFloat()
            var pnt = Point("", value)

            ln.addPoint(pnt)

        }
        ln.setSmooth(true)
        ln.setThickness(4f)
        chartView.addData(ln)


        var anim = Animation()
        anim.setDuration(1200)
        chartView.show(anim)
    }

    fun makeComponentsVisible()
    {
        transmit_button.alpha = 0f
        sync_button.alpha = 0f

        transmit_button.visibility = View.VISIBLE
        sync_button.visibility = View.VISIBLE

        val valueanimator = ValueAnimator.ofFloat(0f, 1f)
        valueanimator.addUpdateListener {
            val value = it.animatedValue as Float

            transmit_button.alpha = value
            sync_button.alpha = value
        }
        valueanimator.duration = 1400L
        valueanimator.start()

    }

    fun transmit(status : String)
    {
        this.status = status
        connectiq.sendMessage(available, app, status, sendMessageCallback())
    }
    fun transmit_string(status : String)
    {
        connectiq.sendMessage(available, app, status, sendMessageCallback())
    }

    fun initialize()
    {
        connectiq = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS)
        connectiq.initialize(this, true, connectListener())
    }

    fun initializeRest()
    {
        if(!checkDevices())
        {
            Log.i("ConnectIQ", "Couldn't find connected device.")
            Toast.makeText(this, "NO DEVICE FOUND", Toast.LENGTH_SHORT)
        }
        else
        {
            Toast.makeText(this, "FOUND AVAILABLE DEVICE", Toast.LENGTH_LONG)
        }

        getAppInstance() // starts listener callback chain

    }

    fun getAppInstance()
    {
        // app is initialized in callback appListener()
        connectiq.getApplicationInfo("a3421fee-d289-106a-538c-b9547ab12095", available, appListener())
    }

    fun checkDevices() : Boolean
    {
        paired = connectiq.knownDevices

        if(paired.size > 0)
        {
            for(device in paired)
            {
                if(connectiq.getDeviceStatus(device) == IQDevice.IQDeviceStatus.CONNECTED)
                {
                    available = device
                    return true
                }
            }
        }
        return false
    }

    // *******************
    // Listener Interfaces
    // *******************
    fun appEventListener() : ConnectIQ.IQApplicationEventListener =
            object : ConnectIQ.IQApplicationEventListener
            {
                override fun onMessageReceived(p0: IQDevice?, p1: IQApp?, p2: MutableList<Any>?, p3: ConnectIQ.IQMessageStatus?)
                {
                    if(p3 == ConnectIQ.IQMessageStatus.SUCCESS)
                    {
                        try
                        {
                            HR_DATA.putAll(p2!![0] as HashMap<Int, Int>)
                            syncHR() // pushes any new hash table info from watch to firestore
                            updateChart(HR_DATA, chartView)
                        }
                        catch (e : ClassCastException)
                        {
                            Log.i("from phone", "tether request from watch")
                        }
                    }
                }
            }

    fun sendMessageCallback() : ConnectIQ.IQSendMessageListener =
            object : ConnectIQ.IQSendMessageListener
            {
                override fun onMessageStatus(p0: IQDevice?, p1: IQApp?, p2: ConnectIQ.IQMessageStatus?) {
                    print("hellow")
                }
            }



    fun appListener() : ConnectIQ.IQApplicationInfoListener =
            object : ConnectIQ.IQApplicationInfoListener
            {
                override fun onApplicationInfoReceived(p0: IQApp?)
                {
                    app = p0!! // getting app instance
                    connectiq.registerForAppEvents(available, app, appEventListener())
                    makeComponentsVisible()
                }

                override fun onApplicationNotInstalled(p0: String?)
                {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }



    fun deviceListener() : ConnectIQ.IQDeviceEventListener =
            object : ConnectIQ.IQDeviceEventListener
            {
                override fun onDeviceStatusChanged(p0: IQDevice?, p1: IQDevice.IQDeviceStatus?)
                {
                    // handle new device status
                    // statuses -> CONNECTED, NOT_CONNECTED, NOT_PAIRED
                }
            }



    fun connectListener() : ConnectIQ.ConnectIQListener =
            object : ConnectIQ.ConnectIQListener
            {
                override fun onInitializeError(p0: ConnectIQ.IQSdkErrorStatus?)
                {
                    // A failure has occurred during initialization.  Inspect
                    // the IQSdkErrorStatus value for more information regarding
                    // the failure.
                    toast("ERROR establishing connection")
                }
                override fun onSdkReady()
                {
                    initializeRest()
                }
                override fun onSdkShutDown()
                {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }


}


