package com.senstrgrs.griffinjohnson.sensortriggers

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.*

import android.content.res.Resources
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log

import android.os.IBinder
import android.provider.Settings
import android.view.View
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
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

import kotlinx.android.synthetic.main.activity_watch_comms.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.jetbrains.anko.withAlpha
import java.lang.ClassCastException
import java.lang.Exception
import java.lang.NullPointerException
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timerTask
import kotlin.coroutines.CoroutineContext


class WatchComms : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var connectiq: ConnectIQ
    private lateinit var available: IQDevice
    private lateinit var paired: List<IQDevice>
    private lateinit var chartView: LineChartView
    private lateinit var app: IQApp
    private lateinit var userRef: DocumentReference
    private lateinit var vm: ViewModel
    private lateinit var map: GoogleMap
    private lateinit var darkSky: DarkSky
    private var gpsService: LocationTrackService? = null

    private lateinit var locationBroadcastReceiver: BroadcastReceiver

    private val LOCATION_PERMS = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)


    private val fusedLocation: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    var status = "OFF"
    var mAuth = FirebaseAuth.getInstance()
    var db = FirebaseFirestore.getInstance()
    var perm_granted = false
    var poly_op = PolylineOptions()
            .clickable(false)
            .color(Color.BLUE)

    var circle_ops = ArrayList<CircleOptions>()

    private var isTracking = false


    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap?) {
        var myLocation: LatLng
        checkPermissions {
            perm_granted = it!!

            if (perm_granted) {
                map = p0!!
                map.isMyLocationEnabled = perm_granted
                fusedLocation.lastLocation.addOnSuccessListener {
                    val l = LatLng(it!!.latitude, it!!.longitude)
                    myLocation = l


                    try {
                        // Customise the styling of the base map using a JSON object defined
                        // in a raw resource file.
                        val success = map.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                        this, R.raw.map_style))

                        if (!success) {
                        }
                    } catch (e: Resources.NotFoundException) {
                    }

                    map.addPolyline(poly_op)

                    for(c in circle_ops)
                    {
                        map.addCircle(c)
                    }
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))


                }
            }
        }

    }




    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        styling()
        initialize()
        startBackgroundService()
        uiUpdaters()
        setButtonListeners()


    }

    fun startBackgroundService()
    {
        val intent = Intent(this.application, LocationTrackService::class.java)
        this.application.startService(intent)
        this.application.bindService(intent, serviceConnection(), Context.BIND_AUTO_CREATE)
    }

    private fun serviceConnection() = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (name!!.className.endsWith("LocationTrackService")) {
                gpsService = (service as LocationTrackService.LocationServiceBinder).getService()
                track_btn.visibility = View.VISIBLE

            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            if (name!!.className.equals("LocationTrackService")) {
                gpsService = null
                track_btn.visibility = View.GONE
            }
        }

    }


    fun checkPermissions(cb: ((b: Boolean) -> Unit)) {
        Dexter.withActivity(this)
                .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        cb(false)
                    }

                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                        cb(true)
                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    }
                })
                .check()
    }

    fun styling() {
        setContentView(R.layout.activity_watch_comms)
        comms_view.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.black))
        window.navigationBarColor = ContextCompat.getColor(baseContext, R.color.black)
        window.statusBarColor = ContextCompat.getColor(baseContext, R.color.black)
        chartView = findViewById(R.id.chartView)
        graphLoad.isIndeterminate = true


    }

    fun initialize() {
        userRef = db.collection("users").document(mAuth.uid.toString())
        vm = ViewModelProviders.of(this, ViewModelFactory(userRef)).get(ViewModel(userRef)::class.java)


        //fusedLocation.requestLocationUpdates(LocationRequest(), object : LocationCallback)

        locationBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?)
            {
                val lat = intent!!.extras["lat"] as Double
                val long = intent!!.extras["long"] as Double
                val epoch = intent!!.extras["epoch_stamp"] as Long

                val curr_location_stamp = LocationStamp(epoch, lat, long)

                vm.addLocationStamp(curr_location_stamp)

                curr_lat.text = lat.toString()
                curr_long.text = long.toString()
            }
        }
        registerReceiver(locationBroadcastReceiver, IntentFilter("location_update"))



        (supportFragmentManager.findFragmentById(R.id.mMap) as SupportMapFragment).getMapAsync(this)

        connectiq = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS)
        connectiq.initialize(this, true, connectListener())



    }

    // TODO should migrate map overlay stuff to view model

    private fun updatePolyLines()
    {


        try
        {
            for(loc in vm.getLocationHistory().value!!)
            {
                poly_op.add(LatLng(loc.lat, loc.long))
            }


            doAsync {
                uiThread {
                    map.addPolyline(poly_op).jointType = JointType.ROUND
                }

            }
            
        }
        catch(e : Exception)
        {

        }
    }

    private fun updateGeoFenceViews()
    {
        try
        {
            for(t in vm.getTriggers().value!!)
            {
                if(t.type.compareTo("g") == 0)
                {
                    circle_ops.add(CircleOptions()
                            .center(LatLng(t.lat, t.long))
                            .clickable(true)
                            .radius(100.0)
                            .strokeColor(Color.BLUE)
                            .strokeWidth(3f)
                            .fillColor(ContextCompat.getColor(this, R.color.blueish).withAlpha(50)))
                }
            }

        }
        catch(e : Exception)
        {

        }

    }

    fun uiUpdaters() {
        // HR Data Updater
        vm.getHRData().observe(this, android.arch.lifecycle.Observer {
            // update ui on hr data change
            chartUpdater.invoke(it!!, false)
            vm.syncHR(HR_cb)
        })

        vm.getTriggers().observe(this, android.arch.lifecycle.Observer {
            // update ui on triggers change
            vm.syncTriggers()
            updateGeoFenceViews()

        })

        vm.getLocationHistory().observe(this, android.arch.lifecycle.Observer {
            vm.syncLocationStamps()
            updatePolyLines()
        })

        // TODO observe location updates in firestore


    }

    fun loadingTimeout(duration: Long) {
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

    fun setButtonListeners() {


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



        track_btn.setOnClickListener {
            when (isTracking) {
                true -> {
                    gpsService!!.stopTracking()
                    track_btn.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.black))

                    curr_lat.visibility = View.GONE
                    curr_long.visibility = View.GONE
                }
                false -> {
                    gpsService!!.startTracking(this)

                    track_btn.setBackgroundResource(R.drawable.roundshapebtn)



                    curr_lat.visibility = View.VISIBLE
                    curr_long.visibility = View.VISIBLE
                }
            }
            isTracking = !isTracking

        }
    }

    fun initializeAfterSDKReady() {
        if (!checkDevices()) {
            Log.i("ConnectIQ", "Couldn't find connected device.")
            Toast.makeText(this, "NO DEVICE FOUND", Toast.LENGTH_SHORT)
        } else {
            Toast.makeText(this, "FOUND AVAILABLE DEVICE", Toast.LENGTH_LONG)
        }

        getAppInstance() // starts listener callback chain
    }

    fun getAppInstance() {
        // app is initialized in callback appListener()

        try{
            connectiq.getApplicationInfo(getString(R.string.watch_appID), available, appListener())

        }
        catch (e : Exception)
        {
            // TODO watch not rdy to connect.
        }
    }

    fun checkDevices(): Boolean {
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

    @SuppressLint("MissingPermission")
    fun showTriggerCreationDialog() {
        var lat = 0.0
        var long = 0.0

        fusedLocation.lastLocation.addOnSuccessListener {
            lat = it!!.latitude
            long = it!!.longitude
            val dialog = TriggerDialog.newInstance(lat, long)
            dialog.show(supportFragmentManager, "trigger_dialog")
        }


    }

    fun triggerFilter(sample: HashMap<Int, Int>) {}

    fun makeComponentsVisible() {
        sync_button.alpha = 0f

        sync_button.visibility = View.VISIBLE


        val valueanimator = ValueAnimator.ofFloat(0f, 1f)
        valueanimator.addUpdateListener {
            val value = it.animatedValue as Float

            sync_button.alpha = value
        }
        valueanimator.duration = 1400L
        valueanimator.start()
    }

    fun transmit_temporal(status: String) {
        this.status = status
        connectiq.sendMessage(available, app, status, sendMessageCallback())
    }

    fun transmit_string(status: String) {
        connectiq.sendMessage(available, app, status, sendMessageCallback())
    }

    // *********************
    // Handling App Behavior
    // *********************

    override fun onBackPressed() {
        try {
            connectiq.unregisterForApplicationEvents(available, app) //  de-register from watch
        } catch (e: UninitializedPropertyAccessException) {
        }

        mAuth.signOut()
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()



    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) // from trigger list
        {
            val t = data!!.extras.get("triggers") as ArrayList<Trigger>
            vm.setTriggers(t)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            connectiq.unregisterForApplicationEvents(available, app) //  de-register from watch
        } catch (e: UninitializedPropertyAccessException) {

        } //  de-register from watch
        mAuth.signOut()
    }


    // ******************
    //  Function Objects
    // ******************

    var seek_cb = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            chartUpdater.invoke(vm.getHRData().value!!, false)
        }
    }

    var HR_cb = object : (() -> Unit) {
        override fun invoke() {
            graphLoad.visibility = View.INVISIBLE
        }
    }


    var chartUpdater = object : ((HashMap<Int, Int>, Boolean) -> Unit) {
        override fun invoke(sample: HashMap<Int, Int>, animate: Boolean) {
            chartView.reset()
            var ln = LineSet()
            var keys = sample.keys.sorted()
            var minV  = 1000f
            var maxV = 0f

            var points = ArrayList<Float>()
            if (!keys.isEmpty())
            {
                for (key in keys)
                {


                    val value = sample.get(key)!!.toFloat()


                    if(value > maxV)
                    {
                        maxV = value

                    }
                    if(value < minV)
                    {
                        minV = value

                    }


                    points.add(value)



                }


                for(v in points)
                {
                    if(v == maxV)
                    {
                        ln.addPoint(Point(v.toString(), v).apply {
                            this.strokeColor = Color.RED
                            this.strokeThickness = 8f
                            this.radius = 10f
                        })
                    }
                    else if(v == minV)
                    {
                        ln.addPoint(Point(v.toString(), v).apply {
                            this.strokeColor = Color.BLUE
                            this.strokeThickness = 8f
                            this.radius = 8f
                        })
                    }
                    else
                    {
                        ln.addPoint(Point("", v))
                    }
                }
                ln.setSmooth(true)
                ln.setThickness(4f)

                ln.color = getColor(R.color.white)

                chartView.addData(ln)
                chartView.setClickablePointRadius(10f)

                if (animate) {
                    var anim = Animation()
                    anim.setDuration(1200)
                    chartView.show(anim)
                } else {
                    chartView.show()
                }
            }
        }
    }


    // *******************
    // Listener Interfaces
    // *******************

    fun appEventListener(): ConnectIQ.IQApplicationEventListener =
            object : ConnectIQ.IQApplicationEventListener {
                override fun onMessageReceived(p0: IQDevice?, p1: IQApp?, p2: MutableList<Any>?, p3: ConnectIQ.IQMessageStatus?) {
                    if (p3 == ConnectIQ.IQMessageStatus.SUCCESS) {
                        try {
                            val hash = p2!![0] as HashMap<Int, Int>
                            vm.addNewHR(validateHash(hash))
                        } catch (e: ClassCastException) {
                            Log.i("from phone", "tether request from watch")
                        }
                    }
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

    fun sendMessageCallback(): ConnectIQ.IQSendMessageListener =
            object : ConnectIQ.IQSendMessageListener {
                override fun onMessageStatus(p0: IQDevice?, p1: IQApp?, p2: ConnectIQ.IQMessageStatus?) {
                    print("hellow")
                }
            }

    fun appListener(): ConnectIQ.IQApplicationInfoListener =
            object : ConnectIQ.IQApplicationInfoListener {
                override fun onApplicationInfoReceived(p0: IQApp?) {
                    app = p0!! // getting app instance
                    connectiq.registerForAppEvents(available, app, appEventListener())
                    makeComponentsVisible()
                }

                override fun onApplicationNotInstalled(p0: String?) {
                    //TODO("not implemented")
                }
            }

    fun deviceListener(): ConnectIQ.IQDeviceEventListener =
            object : ConnectIQ.IQDeviceEventListener {
                override fun onDeviceStatusChanged(p0: IQDevice?, p1: IQDevice.IQDeviceStatus?) {
                    // handle new device status
                    // statuses -> CONNECTED, NOT_CONNECTED, NOT_PAIRED
                }
            }

    fun connectListener(): ConnectIQ.ConnectIQListener =
            object : ConnectIQ.ConnectIQListener {
                override fun onInitializeError(p0: ConnectIQ.IQSdkErrorStatus?) {
                    // A failure has occurred during initialization.  Inspect
                    // the IQSdkErrorStatus value for more information regarding
                    // the failure.
                    toast("ERROR establishing connection")
                }

                override fun onSdkReady() {
                    initializeAfterSDKReady()
                }

                override fun onSdkShutDown() {
                    // TODO("not implemented")
                }
            }
}