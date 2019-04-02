package com.rapsealk.agroundcontrol

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private val TAG = MainActivity::class.java.simpleName

    val droneIdList = arrayListOf("droneId01", "droneId02", "droneId03")
    private lateinit var droneIdSpinnerAdapter: ArrayAdapter<String>

    private lateinit var mqttUtil: MqttUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        droneIdSpinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, droneIdList)
        droneIdSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drone_id_spinner.adapter = droneIdSpinnerAdapter
        drone_id_spinner.onItemSelectedListener = this

        //MqttUtil.updateDroneId(this)

        mqttUtil = MqttUtil(this)
        val mqttClient = mqttUtil.getClient()
    }

    /**
     * AdapterView.OnItemSelectedListener
     */
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(TAG, "onItemSelected(parent: $parent, view: $view, position: $position, id: $id)")
        val item = parent?.adapter?.getItem(position) as String
        Log.d(TAG, "onItemSelected(item: $item)")
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "onNothingSelected(parent: $parent)")
    }
    // AdapterView.OnItemSelectedListener

    public fun updateDroneId(droneId: String) {
        if (droneIdList.contains(droneId)) return
        droneIdSpinnerAdapter.add(droneId)
        droneIdSpinnerAdapter.notifyDataSetChanged()
    }
}
