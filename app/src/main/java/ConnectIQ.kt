package com.senstrgrs.griffinjohnson.sensortriggers

import android.provider.Settings.Global.getString
import android.util.Log
import android.widget.Toast
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import org.jetbrains.anko.toast
import java.lang.ClassCastException
import java.lang.Exception
import java.lang.NullPointerException

class CIQ (context : WatchComms, cb : (hash : HashMap<Int, Int>) -> Unit) {
    private var connectiq: ConnectIQ
    private var comms: WatchComms
    private var onMessageCallback : (hash : HashMap<Int, Int>) -> Unit

    private lateinit var available: IQDevice
    private lateinit var paired: List<IQDevice>
    private lateinit var app: IQApp


    init {
        comms = context
        onMessageCallback = cb


        connectiq = ConnectIQ.getInstance(comms, ConnectIQ.IQConnectType.WIRELESS)
        connectiq.initialize(comms, true, connectListener())

    }


    fun transmit(status: String)
    {
        connectiq.sendMessage(available, app, status, sendMessageCallback())
    }

    fun deRegister()
    {
        connectiq.unregisterForApplicationEvents(available, app) //  de-register from watch
    }


    private fun initializeAfterSDKReady() {
        if (!checkDevices()) {
            Log.i("ConnectIQ", "Couldn't find connected device.")
            Toast.makeText(comms, "NO DEVICE FOUND", Toast.LENGTH_SHORT)
        } else {
            Toast.makeText(comms, "FOUND AVAILABLE DEVICE", Toast.LENGTH_LONG)
        }

        getAppInstance() // starts listener callback chain
    }

    private fun getAppInstance() {
        // app is initialized in callback appListener()

        try{
            connectiq.getApplicationInfo(comms.getString(R.string.watch_appID), available, appListener())

        }
        catch (e : Exception)
        {
            // TODO watch not rdy to connect.
        }
    }

    private fun validateHash(hash : HashMap<Int, Int>) : HashMap<Int, Int>
    {
        var toReturn = HashMap<Int, Int>()

        for(k  in hash.keys)
        {
            try {
                toReturn.put(k, hash.get(k)!!)
            }
            catch (e : NullPointerException)
            {
                Log.i("validating hash", "caught bad value")
            }
        }

        return toReturn
    }

    private fun appEventListener(): ConnectIQ.IQApplicationEventListener =
            object : ConnectIQ.IQApplicationEventListener {
                override fun onMessageReceived(p0: IQDevice?, p1: IQApp?, p2: MutableList<Any>?, p3: ConnectIQ.IQMessageStatus?) {
                    if (p3 == ConnectIQ.IQMessageStatus.SUCCESS) {
                        try {
                            val hash = p2!![0] as HashMap<Int, Int>

                            onMessageCallback(validateHash(hash))

                        } catch (e: ClassCastException) {
                            Log.i("from phone", "tether request from watch")
                        }
                    }
                }
            }

    private fun checkDevices(): Boolean {
        paired = connectiq.knownDevices

        if (paired.size > 0) {
            for (device in paired) {
                if (connectiq.getDeviceStatus(device) == IQDevice.IQDeviceStatus.CONNECTED) {
                    available = device
                    return true
                }
            }
        }
        return false
    }

    private fun sendMessageCallback(): ConnectIQ.IQSendMessageListener =
            object : ConnectIQ.IQSendMessageListener {
                override fun onMessageStatus(p0: IQDevice?, p1: IQApp?, p2: ConnectIQ.IQMessageStatus?) {
                    print("hellow")
                }
            }

    private fun appListener(): ConnectIQ.IQApplicationInfoListener =
            object : ConnectIQ.IQApplicationInfoListener {
                override fun onApplicationInfoReceived(p0: IQApp?) {
                    app = p0!! // getting app instance
                    connectiq.registerForAppEvents(available, app, appEventListener())

                    comms.makeComponentsVisible()
                }

                override fun onApplicationNotInstalled(p0: String?) {
                    //TODO("not implemented")
                }
            }

    private fun deviceListener(): ConnectIQ.IQDeviceEventListener =
            object : ConnectIQ.IQDeviceEventListener {
                override fun onDeviceStatusChanged(p0: IQDevice?, p1: IQDevice.IQDeviceStatus?) {
                    // handle new device status
                    // statuses -> CONNECTED, NOT_CONNECTED, NOT_PAIRED
                }
            }

    private fun connectListener(): ConnectIQ.ConnectIQListener =
            object : ConnectIQ.ConnectIQListener {
                override fun onInitializeError(p0: ConnectIQ.IQSdkErrorStatus?) {
                    // A failure has occurred during initialization.  Inspect
                    // the IQSdkErrorStatus value for more information regarding
                    // the failure.
                    comms.toast("ERROR establishing connection")
                }

                override fun onSdkReady() {
                    initializeAfterSDKReady()
                }

                override fun onSdkShutDown() {
                    // TODO("not implemented")
                }
            }

}