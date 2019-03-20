package com.senstrgrs.griffinjohnson.sensortriggers

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.trigger_cell.view.*
import kotlinx.android.synthetic.main.trigger_view.*

class TriggerView : AppCompatActivity() {

    lateinit var trigger_list: ArrayList<Trigger>

    private lateinit var recyclerView: RecyclerView
    private lateinit var recycler_view_manager: RecyclerView.LayoutManager
    private lateinit var recycler_viewAdapter: RecyclerView.Adapter<*>

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        styling()
        trigger_list = intent.getSerializableExtra("triggers") as ArrayList<Trigger>


        setupRecycler()
    }

    fun setupRecycler()
    {
        // create classes to manage recycler view
        recyclerView = recycler_view
        recycler_view_manager = LinearLayoutManager(this)
        recycler_viewAdapter = MyListAdapter(trigger_list)


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
        holder.itemView.setOnClickListener {
            View.OnClickListener {
                // TODO : progress view per lift once you click on item

            }
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