package com.rapsealk.agroundcontrol

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.rapsealk.agroundcontrol.data.GlobalPosition
import com.rapsealk.agroundcontrol.data.Heartbeat
import com.rapsealk.agroundcontrol.data.Message
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MqttUtil(private val context: Context) : MqttCallback {

    private val TAG = MqttUtil::class.java.name

    companion object {
        private val EmptyMessage = "{}"
    }

    object MQTT_TOPIC {
        private val HEARTBEAT       = "heartbeat"
        private val STATUS          = "status"
        private val COMMAND_RESULT  = "command_result"
    }

    private var client: MqttAndroidClient? = null

    public var droneId = ""
        set(value) {
            client?.unsubscribe("command_result/$field")
            field = value
            client?.subscribe("command_result/$value", 2)
        }

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

    public fun assignLeader(droneId: String, leader: Boolean) {
        val message = "{ \"leader\": $leader }"
        publishMessage("assign_leader/$droneId", message)
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

    public fun start(droneIdList: List<String>) {
        droneIdList.forEach { publishMessage("flocking_flight/$it") }
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
                if (heartbeat.hostname == droneId) {
                    notifyLeader(heartbeat.leader)
                    notifyGlobalPosition(heartbeat.global_position)
                }
            }
            /*
            topic.startsWith("mavros/state") -> {
                val mavrosState =
            }*/
            topic.startsWith("command_result") -> {
                val commandResult = gson.fromJson(message.toString(), Message::class.java)
                if (commandResult.hostname == droneId)
                    notifyCommandResult(commandResult.message)
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

    private fun notifyLeader(leader: Boolean) {
        (context as MainActivity).notifyLeader(leader)
    }

    private fun notifyGlobalPosition(globalPosition: GlobalPosition) {
        (context as MainActivity).notifyGlobalPosition(globalPosition)
    }

    private fun notifyMavrosState(mavrosState: String) {
        (context as MainActivity).notifyMavrosState(mavrosState)
    }

    private fun notifyCommandResult(commandResult: String) {
        (context as MainActivity).notifyCommandResult(commandResult)
    }
}