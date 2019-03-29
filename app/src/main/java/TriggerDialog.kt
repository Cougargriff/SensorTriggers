package com.senstrgrs.griffinjohnson.sensortriggers

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.triggerdialog.*
import kotlinx.android.synthetic.main.triggerdialog.view.*
import org.jetbrains.anko.toast

class TriggerDialog : DialogFragment()
{
    companion object
    {
        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LNG = "lng"

        private const val DEFAULT_ZOOM = 15f

        fun newInstance(lat: Double? = null, lng: Double? = null): TriggerDialog
        {
            val dialog = TriggerDialog()
            val args = Bundle().apply{
                lat?.let { putDouble(EXTRA_LAT, it) }
                lng?.let { putDouble(EXTRA_LNG, it) }
            }
            dialog.arguments = args
            return dialog
        }
    }

    lateinit var customView: View
    private var mapFragment: SupportMapFragment? = null
    private var googleMap: GoogleMap? = null
    lateinit var vm : ViewModel

    private val pos_button: Button by lazy {
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog
    {
        // StackOverflowError
        // customView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        customView = activity!!.layoutInflater.inflate(R.layout.triggerdialog, null)

        customView.geo_radio.setOnClickListener {
            customView.geo_options.visibility = View.VISIBLE
            customView.hr.visibility = View.GONE
        }

        customView.hr_radio.setOnClickListener {
            customView.geo_options.visibility = View.GONE
            customView.hr.visibility = View.VISIBLE
        }

        var type = "h"


        val builder = AlertDialog.Builder(context!!)
                .setView(customView)
                .setCustomTitle(View.inflate(context, R.layout.custom_title, null))
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


                    vm.addTrigger(Trigger(name_edit.text.toString(), hr_edit.text.toString().toInt(),
                            true, type, d.weather_chk.isChecked, d.location_chk.isChecked))

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)

        val dialog = builder.create()

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return customView
    }

    override fun onStart()
    {
        super.onStart()
        pos_button.isEnabled = false
        val name_edit = dialog.findViewById<EditText>(R.id.trigger_name)
        name_edit.addTextChangedListener(object : TextWatcher
        {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int)
            {
                when(count)
                {
                    0 -> pos_button.isEnabled = false
                    else -> pos_button.isEnabled = true
                }
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogTheme)

        // if onCreateView didn't return view
        // java.lang.IllegalStateException: Fragment does not have a view
        mapFragment = childFragmentManager.findFragmentByTag("m") as SupportMapFragment?
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction().replace(R.id.mapFrame, mapFragment!!, "map").commit()
        }

        vm = ViewModelProviders.of(activity!!).get(ViewModel::class.java)

        mapFragment?.let { mapFragment ->
            mapFragment.getMapAsync { map ->
                googleMap = map

                map.setOnMapLoadedCallback {
                    val lat = arguments?.getDouble(EXTRA_LAT)
                    val lng = arguments?.getDouble(EXTRA_LNG)

                    if (lat != null && lng != null)
                    {
                        val latLng = LatLng(lat, lng)
                        map.addMarker(MarkerOptions()
                                .position(latLng)
                        )
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))

                    }
                }
            }
        }
    }


}