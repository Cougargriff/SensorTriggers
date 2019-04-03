package com.senstrgrs.griffinjohnson.sensortriggers

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.trigger_cell.view.*
import kotlinx.android.synthetic.main.trigger_view.*
import android.support.v7.widget.SimpleItemAnimator



class TriggerView : AppCompatActivity() {

    lateinit var trigger_list: ArrayList<Trigger>

    private lateinit var recyclerView: RecyclerView
    private lateinit var recycler_view_manager: RecyclerView.LayoutManager
    private lateinit var recycler_viewAdapter: RecyclerView.Adapter<*>

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        styling()
        initialize()
        setupRecycler()
    }

    override fun onBackPressed() {
        var intent = Intent()
        intent.putExtra("triggers", trigger_list)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    fun initialize()
    {
        trigger_list = intent.getSerializableExtra("triggers") as ArrayList<Trigger>

        lateinit var userRef : DocumentReference
        userRef = FirebaseFirestore.getInstance().collection("users").document(FirebaseAuth.getInstance().uid.toString())

        var vm = ViewModelProviders.of(this, ViewModelFactory(userRef)).get(ViewModel(userRef)::class.java)
    }

    fun setupRecycler()
    {
        // create classes to manage recycler view
        recyclerView = recycler_view
        recycler_view_manager = LinearLayoutManager(this)
        recycler_viewAdapter = MyListAdapter(trigger_list)

        recyclerView.setHasFixedSize(true)

        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        // Bind delegate and datasource methods to recycler view
        recyclerView.apply {
            layoutManager = recycler_view_manager
            adapter = recycler_viewAdapter
        }
    }

    fun styling()
    {
        setContentView(R.layout.trigger_view)

        trigger_view.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.blueish))
        window.navigationBarColor = ContextCompat.getColor(baseContext, R.color.blueish)
        window.statusBarColor = ContextCompat.getColor(baseContext, R.color.blueish)

        top_base.alpha = 0.5f
    }

}

class MyListAdapter(val myDataset: ArrayList<Trigger>) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    // my cell displays data in one textview. need more if more data
    class ViewHolder(cell_view: LinearLayout) : RecyclerView.ViewHolder(cell_view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        val cell_view = LayoutInflater.from(parent.context).inflate(R.layout.trigger_cell, parent, false) as LinearLayout
        return ViewHolder(cell_view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
    {
        val item = myDataset[position]

        when(item.hr_val)
        {
            -1 -> holder.itemView.hr_view.visibility = View.GONE
            else -> {
                holder.itemView.hr_num.text = item.hr_val.toString()
            }
        }
        holder.itemView.trigger_name.text = item.name
        if(!item.armed)
        {
            holder.itemView.chk_color.alpha = 0.3f
        }

        holder.itemView.title_view.setOnClickListener {
            item.armed = !item.armed
            setArmedColor(holder, item)
        }

        holder.itemView.expander.setOnClickListener {
            onExpand(holder, item)
        }

        setArmedColor(holder, item)
        configureSwitches(holder, item)
    }


    private fun configureSwitches(holder: RecyclerView.ViewHolder, item : Trigger)
    {
        if(item.location)
        {
            holder.itemView.location_switch.isChecked = true
        }

        if(item.weather)
        {
            holder.itemView.weather_switch.isChecked = true
        }

        if(item.hr_context)
        {
            holder.itemView.hr_context_switch.isChecked = true
        }


        holder.itemView.weather_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            when(isChecked)
            {
                true -> item.weather = true
                false -> item.weather = false
            }
        }

        holder.itemView.location_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            when(isChecked)
            {
                true -> item.location = true
                false -> item.location = false
            }
        }

        holder.itemView.hr_context_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            when(isChecked)
            {
                true -> item.hr_context = true
                false -> item.hr_context = false
            }
        }
    }

    private fun onExpand(holder : RecyclerView.ViewHolder, item : Trigger)
    {
        when(holder.itemView.sub_item.visibility)
        {
            View.GONE -> {
                holder.itemView.sub_item.visibility = View.VISIBLE
                when(item.type)
                {
                    "h" -> {}
                    "g" -> { } // TODO add mapfrag to map_frame ...
                }
            }
            View.VISIBLE -> holder.itemView.sub_item.visibility = View.GONE
        }
    }

    private fun setArmedColor(holder : RecyclerView.ViewHolder, item: Trigger)
    {
        when(item.armed)
        {
            true -> holder.itemView.chk_color.alpha = 1f
            false -> holder.itemView.chk_color.alpha = 0.3f
        }
    }


    override fun getItemCount() = myDataset.size

    fun removeAt(position: Int)
    {
        myDataset.removeAt(position)
        notifyItemRemoved(position)
    }

    fun swapItems(fromPosition: Int, toPosition: Int)
    {
        if (fromPosition < toPosition)
        {
            for (i in fromPosition..toPosition - 1)
            {
                myDataset.set(i, myDataset.set(i + 1, myDataset.get(i)))
            }
        }
        else
        {
            for (i in fromPosition..toPosition + 1)
            {
                myDataset.set(i, myDataset.set(i - 1, myDataset.get(i)))
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }
}