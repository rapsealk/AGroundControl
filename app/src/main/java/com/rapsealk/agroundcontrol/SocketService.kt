package com.rapsealk.agroundcontrol

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class SocketService(private val context: Context) : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}