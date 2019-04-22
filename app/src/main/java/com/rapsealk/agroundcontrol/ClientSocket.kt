package com.rapsealk.agroundcontrol

import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.google.gson.Gson
import com.rapsealk.agroundcontrol.data.BasicMessage
import com.rapsealk.agroundcontrol.data.Heartbeat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.Socket

class ClientSocket(private val context: Context,
                   private val handler: Handler,
                   private val hostname: String = "106.10.36.61",
                   private val port: Int        = 8080) : Thread() {

    private lateinit var mSocket: Socket
    private lateinit var mSocketReader: BufferedReader
    private lateinit var mSocketWriter: BufferedWriter

    private val mQueue = ArrayList<String>()
    private val mGson = Gson()

    private lateinit var mHeartbeatThread: Thread
    private lateinit var mReceiverThread: Thread
    private lateinit var mPublisherThread: Thread

    override fun start() {
        try {
            mSocket = Socket(hostname, port)
            mSocketReader = BufferedReader(InputStreamReader(mSocket.getInputStream()))
            mSocketWriter = BufferedWriter(OutputStreamWriter(mSocket.getOutputStream()))
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to connect socket..", Toast.LENGTH_LONG).show()
        }

        val out = PrintWriter(mSocketWriter, true)

        val localHostname = mSocket.localAddress.hostAddress
        mHeartbeatThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                out.println("{ \"type\": \"heartbeat\", \"id\": \"$localHostname\", \"timestamp\": ${System.currentTimeMillis().toFloat() / 1000} }")
                Thread.sleep(1000)
            }
        }
        mHeartbeatThread.isDaemon = true
        mHeartbeatThread.start()

        val buff = CharArray(1024)
        mReceiverThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                // FIXME: readLine()
                if (mSocketReader.read(buff, 0, 1024) > 0) {
                    val received = String(buff)
                    val message = mGson.fromJson(received, BasicMessage::class.java)
                    when (message.type) {
                        "heartbeat" -> {
                            val heartbeat = mGson.fromJson(received, Heartbeat::class.java)
                        }
                    }
                    handler.post {
                        (context as MainActivity).tv_status_message.text = received
                    }
                }
                Thread.sleep(100)
            }
        }
        mReceiverThread.isDaemon = true
        mReceiverThread.start()

        mPublisherThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                mQueue.getOrNull(0)?.let { out.println(it) }
                Thread.sleep(100)
            }
        }
        mPublisherThread.start()

        mPublisherThread.join()
        mReceiverThread.join()
        mHeartbeatThread.join()

        try {
            mSocket.close()
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to close socket..", Toast.LENGTH_SHORT).show()
        }
    }

    override fun interrupt() {
        super.interrupt()
        mPublisherThread.interrupt()
        mReceiverThread.interrupt()
        mHeartbeatThread.interrupt()
    }

    fun queueMessage(message: String) {
        mQueue.add(message)
    }
}