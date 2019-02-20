package com.senstrgrs.griffinjohnson.sensortriggers

import android.animation.ValueAnimator
import android.graphics.Color
import android.opengl.Visibility
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import co.revely.gradient.RevelyGradient
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import kotlinx.android.synthetic.main.activity_login_full.*
import kotlinx.android.synthetic.main.activity_watch_comms.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton


class WatchComms : AppCompatActivity()
{

    lateinit var connectiq : ConnectIQ
    lateinit var available : IQDevice
    lateinit var paired : List<IQDevice>

    lateinit var app : IQApp


    var CONSOLE_STRING = ""
    var status = "OFF"

    var HR_DATA = ArrayList<Int>()



    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_comms)
        comms_view.setBackgroundColor(resources.getColor(R.color.blueish))

        window.navigationBarColor = ContextCompat.getColor(baseContext, R.color.blueish)
        window.statusBarColor = ContextCompat.getColor(baseContext, R.color.blueish)


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
            transmit_test("test")
        }





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
                        HR_DATA.addAll(p2!![0] as List<Int>)
                        CONSOLE_STRING = "Current Heart Rate : " + HR_DATA.toString() + "\n"
                        console.text = CONSOLE_STRING
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


                    console.visibility = View.VISIBLE
                    transmit_button.visibility = View.VISIBLE
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


