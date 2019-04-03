package com.rapsealk.agroundcontrol

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.rapsealk.agroundcontrol.data.GlobalPosition
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener, OnMapReadyCallback {

    private val TAG = MainActivity::class.java.simpleName

    val droneIdList = arrayListOf("")
    private lateinit var droneIdSpinnerAdapter: ArrayAdapter<String>

    private lateinit var mqttUtil: MqttUtil

    private lateinit var mGoogleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        droneIdSpinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, droneIdList)
        droneIdSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drone_id_spinner.adapter = droneIdSpinnerAdapter
        drone_id_spinner.onItemSelectedListener = this

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
        if (mqttUtil.droneId.isEmpty()) return

        when (view.id) {
            R.id.btn_arm        -> { mqttUtil.arm(mqttUtil.droneId) }
            R.id.btn_disarm     -> { mqttUtil.disarm(mqttUtil.droneId) }
            R.id.btn_takeoff    -> { mqttUtil.takeoff(mqttUtil.droneId) }
            R.id.btn_land       -> { mqttUtil.land(mqttUtil.droneId) }
        }
    }
    // View.OnClickListener

    /**
     * AdapterView.OnItemSelectedListener
     */
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(TAG, "onItemSelected(parent: $parent, view: $view, position: $position, id: $id)")
        mqttUtil.droneId = parent?.adapter?.getItem(position) as String
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "onNothingSelected(parent: $parent)")
    }
    // AdapterView.OnItemSelectedListener

    /**
     * OnMapReadyCallback
     */
    override fun onMapReady(map: GoogleMap) {
        mGoogleMap = map

    }
    // OnMapReadyCallback

    public fun notifyDroneId(droneId: String) {
        if (droneIdList.contains(droneId)) return
        droneIdSpinnerAdapter.add(droneId)
        droneIdSpinnerAdapter.notifyDataSetChanged()
    }

    public fun notifyGlobalPosition(globalPosition: GlobalPosition) {
        tv_latitude.text = globalPosition.latitude.toString()
        tv_longitude.text = globalPosition.longitude.toString()
        tv_altitude.text = globalPosition.altitude.toString()
    }
}
