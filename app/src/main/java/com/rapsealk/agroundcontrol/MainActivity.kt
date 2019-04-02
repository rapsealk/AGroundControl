package com.rapsealk.agroundcontrol

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mqttUrl = resources.getString(R.string.mqtt_url)
        val mqttClient = MqttUtil.getClient(this, mqttUrl)
    }
}
