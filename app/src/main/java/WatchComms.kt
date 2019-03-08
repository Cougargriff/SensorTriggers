package com.senstrgrs.griffinjohnson.sensortriggers

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.Toast
import co.revely.gradient.RevelyGradient
import com.db.chart.model.LineSet
import com.db.chart.view.LineChartView
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseUserMetadata
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.android.synthetic.main.activity_login_full.*

import kotlinx.android.synthetic.main.activity_watch_comms.*
import kotlinx.android.synthetic.main.triggerdialog.*
import org.jetbrains.anko.toast
import java.util.*
import java.util.zip.Inflater
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class WatchComms : AppCompatActivity()
{

    lateinit var connectiq : ConnectIQ
    lateinit var available : IQDevice
    lateinit var paired : List<IQDevice>

    lateinit var app : IQApp


    var CONSOLE_STRING = ""
    var status = "OFF"

    var HR_DATA = HashMap<Int, Int>()

    var trigger_list = ArrayList<Trigger>()

    lateinit var chartView : LineChartView

    var mAuth  = FirebaseAuth.getInstance()
    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_comms)
        comms_view.setBackgroundColor(resources.getColor(R.color.blueish))


        window.navigationBarColor = ContextCompat.getColor(baseContext, R.color.blueish)
        window.statusBarColor = ContextCompat.getColor(baseContext, R.color.blueish)

        chartView = findViewById<LineChartView>(R.id.chartView)


        comms_button.setOnClickListener {

            initialize()
        }

        transmit_button.setOnClickListener {
            when (status)
            {
                "OFF" -> transmit("ON")
                "ON" -> transmit("OFF")
            }
        }

        test_button.setOnClickListener {


            transmit_test("sync")
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

    fun showTriggerCreationDialog()
    {
        var builder = AlertDialog.Builder(this, android.app.AlertDialog.THEME_TRADITIONAL)


        builder.setView(R.layout.triggerdialog)
                .setPositiveButton("Create Trigger") { dialog, which ->
                    val d = dialog as Dialog
                    // TODO - get hr info and name
                    val hr_edit = d.findViewById<EditText>(R.id.hr)
                    val name_edit = d.findViewById<EditText>(R.id.trigger_name)
                    trigger_list.add(Trigger(name_edit.text.toString(), hr_edit.text.toString().toInt()))

                    toast("New Trigger was added to application")

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }

        builder.show()
    }

    fun syncDB()
    {
        // check for trigger info?? TODO

        var toStore = mutableMapOf<String, Any>()

        for(key in HR_DATA.keys.sorted())
        {
            toStore.put(key.toString(), HR_DATA.get(key)!!)
        }

        db.collection("users").document(mAuth.uid.toString())
                .update(toStore)
                .addOnSuccessListener {
                    Log.d("db", "DocumentSnapshot added with ID: ")
                }
                .addOnFailureListener {
                    Log.d("db error", "error adding document")
                }
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
            ln.addPoint("", sample.get(key)!!.toFloat())
        }

        ln.setSmooth(true)
        ln.setThickness(4f)
        chartView.addData(ln)
        chartView.show()

    }

    fun transmit(status : String)
    {
        this.status = status
        connectiq.sendMessage(available, app, status, sendMessageCallback())
    }
    fun transmit_test(status : String)
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
            Toast.makeText(this, "NO DEVICE FOUND", Toast.LENGTH_SHORT);
        }
        else
        {
            Toast.makeText(this, "FOUND AVAILABLE DEVICE", Toast.LENGTH_LONG);
        }

        getAppInstance()

    }

    fun getAppInstance()
    {
        // app is initialized in callback appListener()
        connectiq.getApplicationInfo("a3421fee-d289-106a-538c-b9547ab12095", available, appListener())
    }



    fun checkDevices() : Boolean
    {
        paired = connectiq.knownDevices

        if(paired != null && paired.size > 0)
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



    // Listener Interfaces
    fun appEventListener() : ConnectIQ.IQApplicationEventListener =
            object : ConnectIQ.IQApplicationEventListener
            {
                override fun onMessageReceived(p0: IQDevice?, p1: IQApp?, p2: MutableList<Any>?, p3: ConnectIQ.IQMessageStatus?)
                {
                    if(p3 == ConnectIQ.IQMessageStatus.SUCCESS)
                    {
                        var hash_return = p2!![0] as HashMap<Int, Int>
                        var keys = hash_return.keys
                        HR_DATA.putAll(hash_return)
                        CONSOLE_STRING = "Current Heart Rate : " + HR_DATA.toString() + "\n"
                        console.text = CONSOLE_STRING
                        updateChart(HR_DATA, chartView)

                        syncDB() // pushes any new hash table info from watch to firestore

                    }
                }
            }

    fun sendMessageCallback() : ConnectIQ.IQSendMessageListener =
            object : ConnectIQ.IQSendMessageListener
            {
                override fun onMessageStatus(p0: IQDevice?, p1: IQApp?, p2: ConnectIQ.IQMessageStatus?) {

                }
            }



    fun appListener() : ConnectIQ.IQApplicationInfoListener =
            object : ConnectIQ.IQApplicationInfoListener
            {
                override fun onApplicationInfoReceived(p0: IQApp?)
                {
                    app = p0!!

                    //connectiq.openApplication(available, app, openListener())


                    connectiq.registerForAppEvents(available, app, appEventListener())

                    console_label.visibility = View.VISIBLE
                    console.visibility = View.VISIBLE
                    transmit_button.visibility = View.VISIBLE
                    chartBase.visibility = View.VISIBLE
                    chartView.visibility = View.VISIBLE
                    test_button.visibility = View.VISIBLE

                }

                override fun onApplicationNotInstalled(p0: String?)
                {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }

    fun openListener() : ConnectIQ.IQOpenApplicationListener =
            object : ConnectIQ.IQOpenApplicationListener
            {
                override fun onOpenApplicationResponse(p0: IQDevice?, p1: IQApp?, p2: ConnectIQ.IQOpenApplicationStatus?)
                {
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


