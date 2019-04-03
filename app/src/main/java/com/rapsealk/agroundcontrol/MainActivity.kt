package com.rapsealk.agroundcontrol

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener {

    private val TAG = MainActivity::class.java.simpleName

    val droneIdList = arrayListOf("")
    private lateinit var droneIdSpinnerAdapter: ArrayAdapter<String>

    private lateinit var mqttUtil: MqttUtil
    private var droneId: String = ""

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

        btn_arm.setOnClickListener(this)
        btn_disarm.setOnClickListener(this)
        btn_takeoff.setOnClickListener(this)
        btn_land.setOnClickListener(this)
    }

    /**
     * View.OnClickListener
     */
    override fun onClick(view: View) {
        if (droneId.isEmpty()) return

        when (view.id) {
            R.id.btn_arm -> { mqttUtil.arm(droneId) }
            R.id.btn_disarm -> { mqttUtil.disarm(droneId) }
            R.id.btn_takeoff -> { mqttUtil.takeoff(droneId) }
            R.id.btn_land -> { mqttUtil.land(droneId) }
        }
    }
    // View.OnClickListener

    /**
     * AdapterView.OnItemSelectedListener
     */
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(TAG, "onItemSelected(parent: $parent, view: $view, position: $position, id: $id)")
        droneId = parent?.adapter?.getItem(position) as String
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
