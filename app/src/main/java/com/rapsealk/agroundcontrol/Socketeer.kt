package com.rapsealk.agroundcontrol

import android.util.Log
import com.google.gson.Gson
import com.rapsealk.agroundcontrol.data.Heartbeat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.util.*

class WebSocketIO(hostname: String = "106.10.36.61") {

    companion object {
        private val TAG = WebSocketIO::class.java.simpleName
        private const val EVENT_HEARTBEAT = "heartbeat"

        const val EVENT_COMMAND = "command"
        const val EVENT_ARM     = "arm"
        const val EVENT_DISARM  = "disarm"
        const val EVENT_TAKEOFF = "takeoff"
        const val EVENT_LAND    = "land"
    }

    private val mGson = Gson()
    private val mQueue = LinkedList<Pair<String, String>>()

    private val mSocket: Socket = IO.socket("http://$hostname:3000")

    private val onConnect = Emitter.Listener {
        // publish("connection", "{}")
        Log.d(TAG, "socket is connected to $hostname..")
    }

    private val onDisconnect = Emitter.Listener {
        // publish("disconnect", "{}")
        Log.d(TAG, "socket is disconnected from $hostname..")
    }

    private val onHeartbeat = Emitter.Listener {
        val heartbeat = it.first() as JSONObject
        try {
            val heartbeatMessage = mGson.fromJson(heartbeat.toString(), Heartbeat::class.java)
            Log.d(TAG, "Heartbeat: $heartbeatMessage")
        } catch (exception: JSONException) {
            exception.printStackTrace()
        }
    }

    private var mOutQueueThread: Thread? = null
    private var mHeartbeatThread: Thread? = null

    init {
        mSocket.on(Socket.EVENT_CONNECT, onConnect)
            .on(Socket.EVENT_DISCONNECT, onDisconnect)
            .on(EVENT_HEARTBEAT, onHeartbeat)
    }

    fun connect() {
        try {
            mSocket.connect()
        } catch (exception: Exception) {
            throw exception
        }

        mOutQueueThread = Thread(OutMessageQueue())
        mOutQueueThread?.start()
        Log.d(TAG, "OutQueueThread is alive: ${mOutQueueThread?.isAlive}")
        mHeartbeatThread = Thread(Heartbeater())
        mHeartbeatThread?.start()
        Log.d(TAG, "HeartbeatThread is alive: ${mHeartbeatThread?.isAlive}")
    }

    fun disconnect() {
        mOutQueueThread?.interrupt()
        mHeartbeatThread?.interrupt()

        mSocket.disconnect()

        mSocket.off(Socket.EVENT_CONNECT, onConnect)
            .off(Socket.EVENT_DISCONNECT, onDisconnect)
            .off(EVENT_HEARTBEAT, onHeartbeat)
    }

    fun queue(topic: String, message: String) {
        mQueue.offer(Pair(topic, message))   // add
    }

    fun publish(event: String, message: String) {
        mSocket.emit(event, message)
    }

    /**
     * RunnableImpl
     */
    inner class Heartbeater : Runnable {
        override fun run() {
            while (!Thread.interrupted()) {
                val timestamp = System.currentTimeMillis().toFloat() / 1000
                val message = "{ \"type\": \"heartbeat\", \"hostname\": \"android\", \"timestamp\": $timestamp }"
                queue(EVENT_HEARTBEAT, message)
                Thread.sleep(1000)
            }
        }
    }

    inner class OutMessageQueue : Runnable {
        override fun run() {
            while (Thread.interrupted().not()) {
                if (mQueue.peek() != null) {
                    val message = mQueue.poll()
                    publish(message.first, message.second)
                }
                Thread.sleep(100)
            }
        }
    }
}