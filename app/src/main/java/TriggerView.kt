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
        holder.itemView.hr_num.text = item.hr_val.toString()
        holder.itemView.trigger_name.text = item.name



        if(!myDataset[position].armed)
        {
            holder.itemView.chk_color.alpha = 0.3f
        }

        holder.itemView.title_view.setOnClickListener {
                // TODO : progress view per lift once you click on item
                myDataset[position].armed = !myDataset[position].armed

                if(myDataset[position].armed)
                {
                    holder.itemView.chk_color.alpha = 1f
                    holder.itemView.sub_item.visibility = View.VISIBLE
                }
                else
                {
                    holder.itemView.chk_color.alpha = 0.3f
                    holder.itemView.sub_item.visibility = View.GONE
                }
        }


        if(myDataset[position].armed)
        {
            holder.itemView.chk_color.alpha = 1f
            holder.itemView.sub_item.visibility = View.VISIBLE
        }
        else
        {
            holder.itemView.chk_color.alpha = 0.3f
            holder.itemView.sub_item.visibility = View.GONE
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