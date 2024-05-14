package com.capstone.nongchown.View.Activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.capstone.nongchown.Model.ForegroundService
import com.capstone.nongchown.R
import com.capstone.nongchown.ViewModel.AccidentViewModel

class AccidentActivity : ComponentActivity() {

    val accidentViewModel by viewModels<AccidentViewModel>()

    companion object {
        private var instance: AccidentActivity? = null

        fun getInstance(): AccidentActivity {
            return instance!!
        }
    }

    private var foregroundService: ForegroundService? = null
    var countData = 0


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ForegroundService.LocalBinder
            foregroundService = binder.getService()
            if (countData == 0) {
                foregroundService?.userAccident()
            } else {
                foregroundService?.changeTimer(countData)
            }

        }

        override fun onServiceDisconnected(className: ComponentName) {

        }
    }

    override fun onStart() {
        super.onStart()
        foregroundService?.getTimerCount()?.observe(this) { count ->
            updateTimerText(count)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        val serviceIntent=Intent(this, ForegroundService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        Log.d("test", "accident")
        setContentView(R.layout.accident_notification)

        //foregroundService.


        val intent = intent
        countData = intent?.getIntExtra("timer", 0)!!
        if (countData != 0) {
            if (countData != null) {
                updateTimerText(countData)
            }
        }




        findViewById<Button>(R.id.ok_btn).setOnClickListener {
            Log.d("test", "btnON")
            //accidentViewModel.userSafe()
            foregroundService?.userSafe()
            
            val mainIntent = Intent(this, UserProfileActivity::class.java)
            startActivity(mainIntent)

        }
    }

    public fun updateTimerText(count: Int) {
        findViewById<TextView>(R.id.timer).text = count.toString()
    }
}