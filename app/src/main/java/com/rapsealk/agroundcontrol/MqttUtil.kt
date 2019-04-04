package com.rapsealk.agroundcontrol

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.rapsealk.agroundcontrol.data.GlobalPosition
import com.rapsealk.agroundcontrol.data.Heartbeat
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MqttUtil(private val context: Context) : MqttCallback {

    private val TAG = MqttUtil::class.java.name

    companion object {
        private val EmptyMessage = "{}"
    }

    private val MQTT_TOPIC = "heartbeat"
    private var client: MqttAndroidClient? = null

    public var droneId = ""

    private val gson = Gson()

    fun getClient(): MqttAndroidClient {
        if (client == null) {
            val clientId = MqttClient.generateClientId()
            val hostname = context.resources.getString(R.string.mqtt_url)
            val mqttUrl = "tcp://$hostname:1883"
            client = MqttAndroidClient(context, mqttUrl, clientId)
            client?.setCallback(this)
        }
        if (!client!!.isConnected)
            connect()

        return client!!
    }

    private fun connect() {
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = true
        mqttConnectOptions.keepAliveInterval = 30

        try {
            client!!.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "onSuccess")
                    client?.subscribe("heartbeat", 1)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d(TAG, "onFailure. Exception when connecting: $exception")
                    Log.w(TAG, exception.message)
                    exception.printStackTrace()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error while connecting to Mqtt broker : $e")
            e.printStackTrace()
        }

    }

    fun publishMessage(topic: String, payload: String = EmptyMessage) {
        try {
            val message = MqttMessage(payload.toByteArray())
            client!!.publish(topic, message)
        } catch (e: Exception) {
            Log.e(TAG, "Error when publishing message : $e")
            e.printStackTrace()
        }
    }

    public fun arm(droneId: String) {
        publishMessage("arm/$droneId")
    }

    public fun disarm(droneId: String) {
        publishMessage("disarm/$droneId")
    }

    public fun takeoff(droneId: String) {
        publishMessage("takeoff/$droneId")
    }

    public fun land(droneId: String) {
        publishMessage("land/$droneId")
    }

    fun close() {
        if (client != null) {
            client!!.unregisterResources()
            client!!.close()
        }
    }

    /**
     * MqttCallback
     */
    override fun messageArrived(topic: String, message: MqttMessage) {
        Log.d(TAG, "messageArrived(topic=$topic, message=$message")

        when {
            topic == "heartbeat" -> {
                val heartbeat = gson.fromJson(message.toString(), Heartbeat::class.java)
                Log.d(TAG, "Heartbeat(timestamp=${heartbeat.timestamp}")
                notifyHeartbeat(heartbeat)
                if (heartbeat.hostname == droneId)
                    notifyGlobalPosition(heartbeat.global_position)
            }
        }
/*
        when {
            topic.startsWith("heartbeat") ->
        }*/

        //Toast.makeText(this, "topic: $topic, message: ${message?.payload}", Toast.LENGTH_SHORT).show()
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d(TAG, "deliveryComplete(token=$token)")
        //Toast.makeText(this, "deliveryComplete(token: $token)", Toast.LENGTH_SHORT).show()
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun connectionLost(cause: Throwable?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    // MqttCallback

    private fun notifyHeartbeat(heartbeat: Heartbeat) {
        (context as MainActivity).notifyHeartbeat(heartbeat)
    }

    private fun notifyGlobalPosition(globalPosition: GlobalPosition) {
        (context as MainActivity).notifyGlobalPosition(globalPosition)
    }
}