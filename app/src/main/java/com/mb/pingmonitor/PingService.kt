package com.mb.pingmonitor

import android.app.Service
import android.content.Intent
import android.os.*
import java.io.IOException
import java.util.*
import kotlin.concurrent.fixedRateTimer


class PingService : Service() {
    // Binder given to clients
    private val mBinder = LocalBinder()
    var vibratorService: Vibrator? = null

    var myDataset = mutableListOf<String>()
    val myStatus = mutableListOf<Int>()
    val newStatus = mutableListOf<Int>()
    val t = mutableListOf<Timer>()
    var alarm = false

    fun setNewAlarm(new: Boolean) {

        alarm = new

    }

    fun getStatus() : MutableList<Int> {

        return myStatus

    }

    fun getAlarmStatus() : Boolean {

        return alarm

    }

    fun checkStatus(old: MutableList<Int>, new: MutableList<Int>) {

        for (i in 0..newStatus.size - 1) {

            if (new[i] == 0 && old[i] == 1) {
                if (!alarm) {
                    alarm = true
                    vibrate()
                }
            }

            if (new[i] != old[i]) myStatus[i] = new[i]

        }

    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        fun getService() : PingService {
            return this@PingService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()

        val mainT = fixedRateTimer("MainTask", true, initialDelay = 0, period = 1000) {

            checkStatus(myStatus, newStatus)
            println("Timer: ${myStatus.size} ${newStatus.size} ${t.size}")

        }
    }

    fun updateData(newData: MutableList<String>) {

        println("Updating data 1: ${myStatus.size} ${newStatus.size} ${t.size}")
        stopPingTimers()
        println("Updating data 2: ${myStatus.size} ${newStatus.size} ${t.size}")

        //for (i in 0..newData.size - 1) myDataset[i] = newData[i]
        myDataset = newData

        if (myDataset.size > myStatus.size) for (i in 1..myDataset.size - myStatus.size) myStatus.add(2)
        if (myDataset.size > newStatus.size) for (i in 1..myDataset.size - newStatus.size) newStatus.add(2)

        if (myStatus.size > myDataset.size) for (i in 1..myStatus.size - myDataset.size) myStatus.removeAt(myStatus.size - 1)
        if (newStatus.size > myDataset.size) for (i in 1..newStatus.size - myDataset.size) newStatus.removeAt(newStatus.size - 1)

        println("Updating data 3: ${myStatus.size} ${newStatus.size} ${t.size}")
        startPingTimers()
        println("Updating data 4: ${myStatus.size} ${newStatus.size} ${t.size}")

    }

    fun vibrate() {

        val pattern = longArrayOf(0, 1000, 1000)

        vibratorService = (this.applicationContext.getSystemService(Service.VIBRATOR_SERVICE)) as Vibrator

        if (Build.VERSION.SDK_INT >= 26) {
            vibratorService!!.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibratorService!!.vibrate(pattern, 0);
        }

    }


    //
    // add a Timer
    //

    fun addTimer() {

        println("addTimer: ${myStatus.size} ${newStatus.size} ${t.size}")
        val i = t.size

        t.add(i, fixedRateTimer("PingTask $i", true, initialDelay = 0, period = 10000) {
            newStatus[i] = Ping(myDataset[i])

            if (newStatus[i] == 0 && myStatus[i] == 1) {
                if (!alarm) {
                    alarm = true
                    vibrate()
                }
            }

            if (myStatus[i] != newStatus[i]) myStatus[i] = newStatus[i]

        })

    }

    //
    // Start Ping Timers
    //
    fun startPingTimers() {

        for (i in myDataset.indices) addTimer()

    }

    //
    // Stop Ping Timers
    //
    fun stopPingTimers() {

        while (!t.isEmpty()) {

            println("stopTimer: ${myStatus.size} ${newStatus.size} ${t.size}")

            t[0].cancel()
            t.removeAt(0)

        }

    }

    fun cancelVibrate() {

        vibratorService = (this.applicationContext.getSystemService(Service.VIBRATOR_SERVICE)) as Vibrator
        vibratorService!!.cancel()


    }
}


//
// Ping Function
//

fun Ping(host: String): Int {

    val runtime = Runtime.getRuntime()

    try {
        val ipProcess = runtime.exec("/system/bin/ping -c 1 -W 1 $host")
        val exitValue = ipProcess.waitFor()
        println("Executing: /system/bin/ping -c 1 -W 1 $host $exitValue")

        if (exitValue == 0) return 1
        else return 0
    } catch (e: IOException) {
        e.printStackTrace()
        return 0
    } catch (e: InterruptedException) {
        e.printStackTrace()
        return 0

    }

}

