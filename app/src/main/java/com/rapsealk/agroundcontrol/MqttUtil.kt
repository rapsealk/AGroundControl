package com.rapsealk.agroundcontrol

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


object MqttUtil : MqttCallback {

    private val MQTT_TOPIC = "heartbeat"
    private var published: Boolean = false
    private var client: MqttAndroidClient? = null
    private val TAG = MqttUtil::class.java.name


    fun getClient(context: Context, hostname: String): MqttAndroidClient {
        if (client == null) {
            val clientId = MqttClient.generateClientId()
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
                    publishMessage("Harry potter")
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

    fun publishMessage(payload: String) {
        published = false
        try {
            val encodedpayload = payload.toByteArray()
            val message = MqttMessage(encodedpayload)
            client!!.publish(MQTT_TOPIC, message)
            published = true
            Log.i(TAG, "message successfully published : $payload")
        } catch (e: Exception) {
            Log.e(TAG, "Error when publishing message : $e")
            e.printStackTrace()
        }

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
    override fun messageArrived(topic: String?, message: MqttMessage?) {
        Log.d(TAG, "messageArrived(topic=$topic, message=$message")
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
}