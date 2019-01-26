package com.senstrgrs.griffinjohnson.sensortriggers

import android.animation.ValueAnimator
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import co.revely.gradient.RevelyGradient
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQDevice
import kotlinx.android.synthetic.main.activity_login_full.*
import kotlinx.android.synthetic.main.activity_watch_comms.*

class WatchComms : AppCompatActivity()
{

    lateinit var connectiq : ConnectIQ
    lateinit var available : IQDevice
    lateinit var paired : List<IQDevice>




    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_comms)

        val valueAnimator = setupGradient(comms_view)




        comms_button.setOnClickListener {
            if(!valueAnimator.isRunning)
                valueAnimator.start()

            initialize()
            Toast.makeText(this,"connection request...", Toast.LENGTH_SHORT)
        }
    }

    fun setupGradient(view : View) : ValueAnimator
    {
        val color1 = Color.parseColor("#00c6ff")
        val color2 = Color.parseColor("#ff72ff")

        val valueAnimator = ValueAnimator.ofFloat(0f, 360f)
        valueAnimator.duration = 15000
        valueAnimator.repeatCount = ValueAnimator.INFINITE
        valueAnimator.interpolator = LinearInterpolator()
        RevelyGradient.sweep()
                .colors(intArrayOf(color1, color2, color1))
                .animate(valueAnimator, { _valueAnimator, _gradientDrawable ->
                    _gradientDrawable.angle = _valueAnimator.animatedValue as Float
                })
                .onBackgroundOf(comms_view)

        return valueAnimator
    }


    fun initialize()
    {
        connectiq = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS)



        connectiq.initialize(this, true, connectListener())

        if(!checkDevices())
        {
            Log.i("ConnectIQ", "Couldn't find connected device.")
        }

        // have available device
        // TODO : get garmin watch app id to register for app events (i.e. onMessageRecieved)



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

    fun deviceListener() : ConnectIQ.IQDeviceEventListener =
            object : ConnectIQ.IQDeviceEventListener {
                override fun onDeviceStatusChanged(p0: IQDevice?, p1: IQDevice.IQDeviceStatus?)
                {
                    // handle new device status
                    // statuses -> CONNECTED, NOT_CONNECTED, NOT_PAIRED


                }
            }

    fun connectListener() : ConnectIQ.ConnectIQListener =
            object : ConnectIQ.ConnectIQListener {
                override fun onInitializeError(p0: ConnectIQ.IQSdkErrorStatus?)
                {
                    // A failure has occurred during initialization.  Inspect
                    // the IQSdkErrorStatus value for more information regarding
                    // the failure.
                }

                override fun onSdkReady()
                {
                    // Do any post initialization setup.
                }

                override fun onSdkShutDown()
                {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

            }
}


