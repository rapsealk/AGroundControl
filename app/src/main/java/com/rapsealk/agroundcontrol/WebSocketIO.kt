package com.rapsealk.agroundcontrol

import android.util.Log
import com.google.gson.Gson
import com.rapsealk.agroundcontrol.data.Heartbeat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class WebSocketIO(hostname: String = "106.10.36.61") {

    companion object {
        private val TAG = WebSocketIO::class.java.simpleName
        private const val EVENT_HEARTBEAT = "heartbeat"
    }

    private val mGson = Gson()
    private val mQueue = LinkedList<String>()

    private val mSocket: Socket = IO.socket("http://$hostname") // :3000

    private val onConnect = Emitter.Listener {
        // TODO
        mSocket.emit("connection", "{}")
    }

    private val onDisconnect = Emitter.Listener {
        // TODO
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

    init {
        mSocket.on(Socket.EVENT_CONNECT, onConnect)
            .on(Socket.EVENT_DISCONNECT, onDisconnect)
            .on(EVENT_HEARTBEAT, onHeartbeat)
    }

    fun connect() {
        mSocket.connect()
    }

    fun disconnect() {
        mSocket.disconnect()

        mSocket.off(Socket.EVENT_CONNECT, onConnect)
            .off(Socket.EVENT_DISCONNECT, onDisconnect)
            .off(EVENT_HEARTBEAT, onHeartbeat)
    }

    fun queueing() {
        while (Thread.interrupted().not()) {
            if (mQueue.peek() != null) {
                val message = mQueue.poll()
                mSocket.emit()
            }
            Thread.sleep(100)
        }
    }

    fun heartbeat() {

    }

    fun publish(event: String, message: String) {
        mSocket.emit(event, message)
    }
}