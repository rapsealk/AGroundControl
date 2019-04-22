package com.rapsealk.agroundcontrol

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_socket.*
import java.io.*
import java.net.Socket

class SocketActivity : AppCompatActivity() {

    private lateinit var mSocket: Socket
    private lateinit var mSocketReader: BufferedReader
    private lateinit var mSocketWriter: BufferedWriter

    private lateinit var mHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socket)

        mHandler = Handler()

        Thread {
            try {
                mSocket = Socket("106.10.36.61", 8080)
                mSocketReader = BufferedReader(InputStreamReader(mSocket.getInputStream()))
                mSocketWriter = BufferedWriter(OutputStreamWriter(mSocket.getOutputStream()))
            } catch (e: IOException) {
                Snackbar.make(root_view, "Failed to connect.", Snackbar.LENGTH_INDEFINITE).apply {
                    setAction("OK") { this.dismiss() }
                }
            }

            val out = PrintWriter(mSocketWriter, true)
            out.println("{ \"type\": \"heartbeat\", \"id\": \"android\", \"timestamp\": ${System.currentTimeMillis().toFloat() / 1000} }")

            val buf = CharArray(1024)
            while (true) {

                if (mSocketReader.read(buf, 0, 1024) > 0) {
                    Log.d("Heartbeat", String(buf))
                    //val message = mSocketReader.readLine()
                    mHandler.post {
                        tv_message.text = String(buf)
                    }
                }
                Thread.sleep(100)
            }
        }.start()
    }

    override fun onStop() {
        super.onStop()
        try {
            mSocket.close()
        } catch (e: IOException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }
}
