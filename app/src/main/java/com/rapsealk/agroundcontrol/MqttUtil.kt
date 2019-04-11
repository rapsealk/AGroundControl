package com.rapsealk.agroundcontrol

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.rapsealk.agroundcontrol.data.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MqttUtil(private val context: Context) : MqttCallback {

    private val TAG = MqttUtil::class.java.name

    companion object {
        private val EmptyMessage = "{}"
    }

    object MQTT_TOPIC {
        const val HEARTBEAT         = "heartbeat"
        const val STATE             = "mavros/state"
        const val BATTERY           = "mavros/battery"
        const val COMMAND_RESULT    = "command_result"
        const val LOG               = "log"

        val TOPICS = arrayOf(STATE, BATTERY, COMMAND_RESULT, LOG)
    }

    private var client: MqttAndroidClient? = null

    public var droneId = ""
        set(value) {
            MQTT_TOPIC.TOPICS.forEach {}
            client?.unsubscribe(MQTT_TOPIC.TOPICS.map { "$it/$field" }.toTypedArray())
            field = value
            client?.subscribe(MQTT_TOPIC.TOPICS.map { "$it/$value" }.toTypedArray(), MQTT_TOPIC.TOPICS.map { 2 }.toIntArray())
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

        if (topic == MQTT_TOPIC.HEARTBEAT) {
            val heartbeat = gson.fromJson(message.toString(), Heartbeat::class.java)
            Log.d(TAG, "Heartbeat(timestamp=${heartbeat.timestamp}")
            notifyHeartbeat(heartbeat)
            if (heartbeat.hostname == droneId) {
                notifyLeader(heartbeat.leader)
                notifyGlobalPosition(heartbeat.global_position)
            }
            return
        }

        when {
            topic.startsWith(MQTT_TOPIC.STATE) -> {
                val mavrosState = gson.fromJson(message.toString(), StateMessage::class.java)

            }
            topic.startsWith(MQTT_TOPIC.COMMAND_RESULT) -> {
                val commandResult = gson.fromJson(message.toString(), Message::class.java)
                if (commandResult.hostname == droneId)
                    notifyCommandResult(commandResult.message)
            }
            topic.startsWith(MQTT_TOPIC.BATTERY) -> {
                val result = gson.fromJson(message.toString(), BatteryMessage::class.java)
                if (result.hostname == droneId)
                    notifyBattery(result.percentage)
            }
            topic.startsWith(MQTT_TOPIC.LOG) -> {
                val log = gson.fromJson(message.toString(), LogMessage::class.java)
                if (log.hostname == droneId)
                    notifyLog(log)
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

    private fun notifyBattery(percentage: Float) {
        (context as MainActivity).notifyBattery(percentage * 100)
    }

    private fun notifyLog(message: LogMessage) {
        (context as MainActivity).notifyLog(message)
    }
}