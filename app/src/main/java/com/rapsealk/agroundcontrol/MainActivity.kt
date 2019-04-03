package com.rapsealk.agroundcontrol

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.rapsealk.agroundcontrol.data.GlobalPosition
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener, OnMapReadyCallback {

    private val TAG = MainActivity::class.java.simpleName

    companion object {
        private val REQUEST_PERMISSION_LOCATION = 0x0001
    }

    val droneIdList = arrayListOf("")
    private lateinit var droneIdSpinnerAdapter: ArrayAdapter<String>

    private lateinit var mqttUtil: MqttUtil

    private lateinit var mGoogleMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        droneIdSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, droneIdList)
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

        checkPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    init()
                } else {
                    val snackbar = Snackbar.make(root_view, "권한 획득에 실패했습니다.", Snackbar.LENGTH_INDEFINITE)
                    snackbar.setAction("다시하기") {
                        snackbar.dismiss()
                        checkPermission()
                    }
                    .show()
                }
            }
        }
    }

    private fun checkPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissions.any { ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED }) {
            // request for permission
            ActivityCompat.requestPermissions(this@MainActivity, permissions, REQUEST_PERMISSION_LOCATION)
        } else {
            // permission already granted
            init()
        }
    }

    private fun init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.lastLocation.addOnSuccessListener {
            //Toast.makeText(this@MainActivity, "(${it.latitude}, ${it.longitude}, ${it.altitude}) with acc: ${it.accuracy}", Toast.LENGTH_LONG).show()
            tv_latitude.text = it.latitude.toString()
            tv_longitude.text = it.longitude.toString()
            tv_altitude.text = it.altitude.toString()
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
        }.addOnFailureListener {
            Toast.makeText(this@MainActivity, "Failed to get last location..", Toast.LENGTH_LONG).show()
            it.printStackTrace()
        }
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
