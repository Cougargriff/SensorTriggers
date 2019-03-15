package com.senstrgrs.griffinjohnson.sensortriggers

import android.animation.ValueAnimator
import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
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

import kotlinx.android.synthetic.main.activity_watch_comms.*
import org.jetbrains.anko.toast
import java.lang.ClassCastException
import java.util.*
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
    lateinit var vm : ViewModel

    var status = "OFF"
    var mAuth  = FirebaseAuth.getInstance()
    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        styling()
        initialize()
        uiUpdaters()
        setButtonListeners()
    }

    fun styling()
    {
        setContentView(R.layout.activity_watch_comms)
        comms_view.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.blueish))
        window.navigationBarColor = ContextCompat.getColor(baseContext, R.color.blueish)
        window.statusBarColor = ContextCompat.getColor(baseContext, R.color.blueish)
        chartView = findViewById(R.id.chartView)
        graphLoad.isIndeterminate = true
    }

    fun initialize()
    {
        userRef = db.collection("users").document(mAuth.uid.toString())
        vm = ViewModelProviders.of(this, ViewModelFactory(userRef)).get(ViewModel(userRef)::class.java)

        loadingTimeout(5000)

        connectiq = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS)
        connectiq.initialize(this, true, connectListener())
    }

    fun uiUpdaters()
    {
        // HR Data Updater
        vm.getHRData().observe(this, android.arch.lifecycle.Observer {
            // update ui on hr data change
            chartUpdater.invoke(it!!, true)
        })

        vm.getTriggers().observe(this, android.arch.lifecycle.Observer {
            // update ui on triggers change
        })

        seekBar.progress = 0
        seekBar.setOnSeekBarChangeListener(seek_cb)
    }

    fun loadingTimeout(duration: Long)
    {
        val timer = Timer()
        timer.schedule(timerTask {
            if (graphLoad.visibility == View.VISIBLE) // TODO better check possible?
                graphLoad.visibility = View.INVISIBLE
            else
                runOnUiThread {
                    toast("Sync timed out...")
                }
        }, duration)
    }

    fun setButtonListeners()
    {
        transmit_button.setOnClickListener {
            when (status)
            {
                "OFF" -> transmit("ON")
                "ON" -> transmit("OFF")
            }
        }

        sync_button.setOnClickListener {
            graphLoad.visibility = View.VISIBLE

            // set timeout for progress bar
            loadingTimeout(5000)
            transmit_string("sync")
        }

        addTrigger.setOnClickListener {
            showTriggerCreationDialog()
        }

        viewTriggers.setOnClickListener {
            val intent = Intent(this, TriggerView::class.java)
            intent.putExtra("triggers", vm.getTriggers().value)
            ContextCompat.startActivity(this, intent, null)
        }
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

    // *********************
    // Handling App Behavior
    // *********************
    override fun onBackPressed()
    {
        try
        {
            connectiq.unregisterForApplicationEvents(available, app) //  de-register from watch
        }
        catch (e : UninitializedPropertyAccessException) {}

        mAuth.signOut()
        super.onBackPressed()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        try
        {
            connectiq.unregisterForApplicationEvents(available, app) //  de-register from watch

        }
        catch (e : UninitializedPropertyAccessException)
        {

        } //  de-register from watch
        mAuth.signOut()
    }

    // *********************

    fun showTriggerCreationDialog()
    {
        var builder = AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setCustomTitle(View.inflate(this, R.layout.custom_title, null))


        builder.setView(R.layout.triggerdialog)
                .setPositiveButton("Create") { dialog, _ ->
                    val d = dialog as Dialog
                    val hr_edit = d.findViewById<EditText>(R.id.hr)
                    val name_edit = d.findViewById<EditText>(R.id.trigger_name)

                    if(name_edit.text.length > 0 && hr_edit.text.length > 0)
                    {
                        vm.addTrigger(Trigger(name_edit.text.toString(), hr_edit.text.toString().toInt()))
                        vm.syncTriggers()

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



    fun triggerFilter(sample : HashMap<Int, Int>)
    {

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


    // ******************
    //  Function Objects
    // ******************

    var seek_cb = object : SeekBar.OnSeekBarChangeListener
    {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

        override fun onStopTrackingTouch(seekBar: SeekBar?)
        {
            chartUpdater.invoke(vm.getHRData().value!!, false)
        }
    }

    var HR_cb = object : (() -> Unit)
    {
        override fun invoke()
        {
            graphLoad.visibility = View.INVISIBLE
        }
    }



    var chartUpdater  = object : ((TreeMap<Int, Int>, Boolean) -> Unit)
    {
        override fun invoke(sample: TreeMap<Int, Int>, animate : Boolean)
        {
            chartView.reset()
            var ln = LineSet()
            var keys = sample.keys.sorted()
            val scalar = (seekBar.progress.toFloat() / 1000f).toInt()

            keys = keys.subList(0, (keys.size - 1) - (scalar * keys.size) )

            if(!keys.isEmpty())
            {
                for(key in keys)
                {
                    val value = sample.get(key)!!.toFloat()
                    var pnt = Point("", value)

                    ln.addPoint(pnt)

                }
                ln.setSmooth(true)
                ln.setThickness(4f)
                chartView.addData(ln)
                chartView.setClickablePointRadius(10f)


                if(animate)
                {
                    var anim = Animation()
                    anim.setDuration(1200)
                    chartView.show(anim)
                }
                else
                {
                    chartView.show()
                }
            }
        }
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
                            vm.addNewHR(p2!![0] as HashMap<Int, Int>)
                            vm.syncHR(HR_cb) // pushes any new hash table info from watch to firestore
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
                    //TODO("not implemented")
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
                   // TODO("not implemented")
                }
            }
}


