package com.senstrgrs.griffinjohnson.sensortriggers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.lang.Exception
import kotlin.contracts.contract

class LocationTrackService : Service()
{
    private val binder = LocationServiceBinder()
    private val TAG = "LocationTrackService"
    private var mLocationListener: LocationListener? = null
    private var mLocationManager: LocationManager? = null
    private val notificationManager: NotificationManager? = null

    private val LOCATION_INTERVAL = 500
    private val LOCATION_DISTANCE = 10f



    override fun onBind(intent: Intent?): IBinder?
    {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        super.onStartCommand(intent, flags, startId)

        return START_NOT_STICKY
    }

    override fun onCreate()
    {
        startForeground(12345678, getNotification())
        Log.i(TAG, "Tracking Service Created")
    }

    override fun onDestroy()
    {
        super.onDestroy()
        if(mLocationManager != null)
        {
            try
            {
                mLocationManager!!.removeUpdates(mLocationListener)
            }
            catch(e : Exception)
            {
                Log.i(TAG, "fail to remove location listeners, ignore", e)
            }
        }
    }

    private fun initializeLocationManager(context: Context)
    {
        if (mLocationManager == null)
        {
            mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    fun startTracking(context: Context)
    {
        initializeLocationManager(context!!)
        Log.i(TAG, "start Tracking")

        mLocationListener =  object : LocationListener {
            override fun onLocationChanged(location: Location) {

                Log.i(TAG, "LocationChanged: $location")

                val location_upate = Intent("location_update")

                location_upate.putExtra("lat", location.latitude)
                location_upate.putExtra("long", location.longitude)
                location_upate.putExtra("epoch_stamp", System.currentTimeMillis() / 1000)


                context.sendBroadcast(location_upate)
            }

            override fun onProviderDisabled(provider: String?) {
                Log.i("loc tracking", "lost provider")

            }

            override fun onProviderEnabled(provider: String?) {


            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.i("Location Tracking", status.toString())

            }
        }

        try
        {
            mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL.toLong(), LOCATION_DISTANCE, mLocationListener)

        }
        catch (ex: java.lang.SecurityException)
        {
             Log.i(TAG, "fail to request location update, ignore", ex)
        }
        catch (ex: IllegalArgumentException)
        {
             Log.i(TAG, "gps provider does not exist " + ex.message)
        }

    }

    fun stopTracking()
    {
        this.onDestroy()
    }


    private fun getNotification(): Notification
    {

        val channel = NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)

        val builder = Notification.Builder(applicationContext, "channel_01").setAutoCancel(true)

        //TODO custom notification view

        return builder.build()
    }

    class LocationServiceBinder : Binder()
    {
        fun getService() : LocationTrackService
        {
            return LocationTrackService()
        }
    }
}