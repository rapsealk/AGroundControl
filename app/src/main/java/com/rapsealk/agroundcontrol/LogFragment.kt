package com.rapsealk.agroundcontrol

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

public class LogFragment : DialogFragment() {

    companion object {
        public val TAG = LogFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_log, container)
        val logRecyclerView = view.findViewById<RecyclerView>(R.id.log_recycler_view)
        logRecyclerView.layoutManager = LinearLayoutManager(context)
        logRecyclerView.adapter = LogAdapter(arrayListOf("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBB"))
        return view
    }

    /*
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        val window = dialog.window
        Log.d(TAG, "window is $window")
        dialog.window?.attributes = layoutParams
    }
    */
}