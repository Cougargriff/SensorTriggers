package com.senstrgrs.griffinjohnson.sensortriggers

import android.animation.ValueAnimator
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.db.chart.animation.Animation
import com.db.chart.model.LineSet
import com.db.chart.model.Point
import com.db.chart.view.LineChartView
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.android.synthetic.main.activity_watch_comms.*
import kotlinx.android.synthetic.main.triggerdialog.*
import kotlinx.android.synthetic.main.triggerdialog.view.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.lang.ClassCastException
import java.security.Permission
import java.security.Permissions
import java.util.*
import java.util.jar.Manifest
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
    private lateinit var fusedLocationClient : FusedLocationProviderClient

    var status = "OFF"
    var mAuth  = FirebaseAuth.getInstance()
    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        styling()
        initialize()

        //checkPermissions(locationUpdates)

        uiUpdaters()
        setButtonListeners()
    }

    fun checkPermissions(cb : (Context) -> Unit)
    {
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            cb(this)
        }
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
            chartUpdater.invoke(it!!, false)
            vm.syncHR(HR_cb)
        })

        vm.getTriggers().observe(this, android.arch.lifecycle.Observer {
            // update ui on triggers change
            vm.syncTriggers()
        })

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
            startActivityForResult(intent, 1)
        }

        cal_fab.setOnClickListener {
//            val datePickerDialog = DatePickerDialog(this)
//            val calendar = Calendar.getInstance()
//            calendar.timeInMillis = System.currentTimeMillis()
//            datePickerDialog.datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
//                    calendar.get(Calendar.DAY_OF_MONTH), { datePicker, year, month, day->
//                textView.text = "Year: "+ year + " Month: "+ (month+1) + " Day: "+day
//            })
//
//            datePickerDialog.show()



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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1 && resultCode == Activity.RESULT_OK) // from trigger list
        {
            val t = data!!.extras.get("triggers") as ArrayList<Trigger>
            vm.setTriggers(t)
        }
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

    fun showTriggerCreationDialog() // TODO dialog class maker?
    {
        var type = "h"
        var v = LayoutInflater.from(this).inflate(R.layout.triggerdialog, null)
        v.geo_radio.setOnClickListener {
            v.lat_long.visibility = View.VISIBLE
            v.hr.visibility = View.GONE
        }

        v.hr_radio.setOnClickListener {
            v.lat_long.visibility = View.GONE
            v.hr.visibility = View.VISIBLE
        }


        var builder = AlertDialog.Builder(this, R.style.MyDialogTheme)
                .setCustomTitle(View.inflate(this, R.layout.custom_title, null))
                .setView(v)
                .setPositiveButton("Create") { dialog, _ ->
                    val d = dialog as Dialog
                    val hr_edit = d.findViewById<EditText>(R.id.hr)
                    val name_edit = d.findViewById<EditText>(R.id.trigger_name)

                    //val radioG = d.findViewById<RadioGroup>(R.id.r_group)

                    when
                    {
                        d.geo_radio.isChecked -> type = "g"
                        d.hr_radio.isChecked -> type = "h"
                    }



                    if(hr_edit.text.toString().compareTo("") == 0)
                    {
                        hr_edit.setText("-1")
                    }

                    if(name_edit.text.length > 0)
                    {
                        vm.addTrigger(Trigger(name_edit.text.toString(), hr_edit.text.toString().toInt() , true, type))

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

//    var seek_cb = object : SeekBar.OnSeekBarChangeListener
//    {
//        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
//
//        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//
//        override fun onStopTrackingTouch(seekBar: SeekBar?)
//        {
//            chartUpdater.invoke(vm.getHRData().value!!, false)
//        }
//    }

    var HR_cb = object : (() -> Unit)
    {
        override fun invoke()
        {
            graphLoad.visibility = View.INVISIBLE
        }
    }
    

    var chartUpdater  = object : ((HashMap<Int, Int>, Boolean) -> Unit)
    {
        override fun invoke(sample: HashMap<Int, Int>, animate : Boolean)
        {
            chartView.reset()
            var ln = LineSet()
            var keys = sample.keys.sorted()

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
                            val hash = p2!![0] as HashMap<Int, Int>
                            vm.addNewHR(hash)
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


