package com.mb.pingmonitor

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.view.*
import kotlin.concurrent.fixedRateTimer
import android.content.Intent
import android.content.ComponentName
import android.os.IBinder
import android.content.ServiceConnection

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    var pService: PingService? = null
    var mBound = false

    val myDataset = mutableListOf<String>()
    val myStatus = mutableListOf<Int>()
    private var newStatus = mutableListOf<Int>()
    var alarm = false
/*
    var myDataset = mutableListOf<String>("1.1.1.1")
    var myStatus = mutableListOf<Int>(1)
    private var newStatus = mutableListOf<Int>(1)
*/
    //
    // add a Host
    //

    fun addHost(host: String) {

        myDataset.add(host)
        myStatus.add(2)
        newStatus.add(2)

        pService?.updateData(myDataset)

        this@MainActivity.runOnUiThread {
            viewAdapter.notifyDataSetChanged()
        }

        saveConfig()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //
        // Set up the RecyclerView
        //

        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(myDataset, myStatus, newStatus, pService)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }

        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                myDataset.removeAt(viewHolder.adapterPosition)
                myStatus.removeAt(viewHolder.adapterPosition)
                newStatus.removeAt(viewHolder.adapterPosition)

                val adapter = recyclerView.adapter as MyAdapter
                adapter.removeAt(viewHolder.adapterPosition)

                pService?.updateData(myDataset)

            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        //
        // Flowing Action Button
        //

        fab.setOnClickListener {
            if (editText.getVisibility() == View.VISIBLE) {
                editText.setVisibility(View.INVISIBLE);
                addButton.visibility = View.INVISIBLE
                fab.visibility = View.VISIBLE
            } else {
                editText.setVisibility(View.VISIBLE);
                addButton.visibility = View.VISIBLE
                fab.visibility = View.INVISIBLE
            }
        }

        //
        // addButton
        //

        addButton.setOnClickListener {

            editText.setVisibility(View.INVISIBLE);
            addButton.visibility = View.INVISIBLE
            fab.visibility = View.VISIBLE

            val newHost = editText.text.toString()
            addHost(newHost)
            editText.setText("")

        }

        //
        // Mute Button
        //

        fab_mute.setOnClickListener {

            pService?.cancelVibrate()
            fab_mute.visibility = View.INVISIBLE
            alarm = false
            pService?.setNewAlarm(false)

        }

    }

    //
    // Load Prefs
    //
    fun loadConfig() {

        val myPref = getSharedPreferences("PingMonitorData", Context.MODE_PRIVATE)

        var prefHosts = mutableSetOf<String>()
        if(myPref != null) prefHosts =  myPref.getStringSet("Hosts", myDataset.toSet())
        for (i in 0..prefHosts.size - 1) addHost(prefHosts.toMutableList()[i])

        myDataset.sort()

    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        val intent = Intent(this, PingService::class.java)

        val PingRunnable = Runnable {

            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)

        }

        val PingThread = Thread(PingRunnable)
        PingThread.start()

        val mainT = fixedRateTimer("MainTask", true, initialDelay = 0, period = 1000) {

            if (pService?.getStatus() != null) {

                myStatus.removeAll(myStatus.indices)  // clean list
                myStatus.addAll(newStatus)  // copy

                val tmp = pService?.getStatus() as MutableList<Int>
                newStatus.removeAll(newStatus.indices)  // clean list
                newStatus.addAll(tmp)  // copy

                alarm = pService!!.getAlarmStatus()

                if (alarm) {

                    this@MainActivity.runOnUiThread {
                        fab_mute.visibility = View.VISIBLE
                    }

                }

            }

            this@MainActivity.runOnUiThread {
                viewAdapter.notifyDataSetChanged()
            }

            println("Main Task: ${myStatus.size}")

        }

    }
/*
    override fun onStop() {
        super.onStop()
        unbindService(mConnection)
        mBound = false
    }
*/
    /** Defines callbacks for service binding, passed to bindService()  */
    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as PingService.LocalBinder
            pService = binder.getService()
            mBound = true

            println("Loading config")
            loadConfig()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    private fun saveConfig() {
        val myPref = getSharedPreferences("PingMonitorData", Context.MODE_PRIVATE)
        val prefEditor = myPref.edit()
        prefEditor.putStringSet("Hosts", myDataset.toSet())
        prefEditor.apply()
    }

    override fun onResume() {
        super.onResume()

        pService?.cancelVibrate()
        fab_mute.visibility = View.INVISIBLE
        alarm = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}

