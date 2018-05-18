package com.mb.pingmonitor

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.*

//
// RecyclerView Adapter
//

class MyAdapter(private val myDataset: MutableList<String>, private val myStatus: MutableList<Int>, private val newStatus: MutableList<Int>, private val pService: PingService?) :
        RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    inner class ViewHolder(val iv: View) : RecyclerView.ViewHolder(iv) {
        val tv: TextView = iv.findViewById(R.id.textV) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.ViewHolder {

        val iv = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        // set the view's size, margins, paddings and layout parameters

        return ViewHolder(iv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.tv.text = myDataset[position]
        if (myStatus[position] == 0) holder.tv.setBackgroundColor(Color.RED)
        else if (myStatus[position] == 1) holder.tv.setBackgroundColor(Color.GREEN)
        else if (myStatus[position] == 2) holder.tv.setBackgroundColor(Color.WHITE)

    }

    override fun getItemCount() = myDataset.size
/*
    fun removeAt(position: Int) {

        myDataset.removeAt(position)
        myStatus.removeAt(position)
        newStatus.removeAt(position)
        t[position].cancel()
        t.removeAt(position)

        notifyItemRemoved(position)
    }
*/

    fun removeAt(position: Int) {

        notifyItemRemoved(position)

    }

}