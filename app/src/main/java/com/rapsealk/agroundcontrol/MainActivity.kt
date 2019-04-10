package com.rapsealk.agroundcontrol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.rapsealk.agroundcontrol.data.GlobalPosition
import com.rapsealk.agroundcontrol.data.Heartbeat
import com.rapsealk.agroundcontrol.data.LogMessage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener, OnMapReadyCallback {

    private val TAG = MainActivity::class.java.simpleName

    companion object {
        private const val REQUEST_PERMISSION_LOCATION = 0x0001
        private const val REQUEST_MISSION_WAYPOINTS   = 0x1001
    }

    private val droneIdList = arrayListOf("")
    private lateinit var droneIdSpinnerAdapter: ArrayAdapter<String>

    private lateinit var mqttUtil: MqttUtil

    private lateinit var mGoogleMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val droneMarkers = HashMap<String, Marker>()

    private val mLogs = ArrayList<LogMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        droneIdSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, droneIdList)
        droneIdSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drone_id_spinner.adapter = droneIdSpinnerAdapter
        drone_id_spinner.onItemSelectedListener = this

        //MqttUtil.updateDroneId(this)

        mqttUtil = MqttUtil(this)
        val mqttClient = mqttUtil.getClient()

        log_recycler_view.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = LogAdapter(mLogs)
        }

        cb_leader.setOnCheckedChangeListener(this)
        btn_log.setOnClickListener(this)
        btn_ok.setOnClickListener(this)

        btn_arm.setOnClickListener(this)
        btn_disarm.setOnClickListener(this)
        btn_takeoff.setOnClickListener(this)
        btn_land.setOnClickListener(this)
        btn_start.setOnClickListener(this)

        checkPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    initMap()
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
            initMap()
        }
    }

    private fun init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.lastLocation.addOnSuccessListener {
            //Toast.makeText(this@MainActivity, "(${it.latitude}, ${it.longitude}, ${it.altitude}) with acc: ${it.accuracy}", Toast.LENGTH_LONG).show()
            tv_latitude.text = it.latitude.toString()
            tv_longitude.text = it.longitude.toString()
            tv_altitude.text = String.format("%.6f", it.altitude)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 20f))
            // FIXME: sample marker
            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.drone), 128, 128, true)
            val markerOptions = MarkerOptions()
                .position(LatLng(it.latitude, it.longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow_drone))
            var marker = mGoogleMap.addMarker(markerOptions)
            marker.tag = "Drone ID #01"
            droneMarkers[marker.tag as String] = marker
            /*
            Thread {
                while (true) {
                    marker.rotation = (Math.random().toFloat() * 1000) % 360
                    Thread.sleep(3000)
                }
            }.start()
            */
            marker = mGoogleMap.addMarker(MarkerOptions()
                .position(LatLng(it.latitude+0.00004, it.longitude-0.00004))
                .rotation(Math.random().toFloat() * 1000 % 360)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap)))
            marker.tag = "Drone ID #02"
            droneMarkers[marker.tag as String] = marker
            marker = mGoogleMap.addMarker(MarkerOptions()
                .position(LatLng(it.latitude-0.00004, it.longitude-0.00004))
                .rotation(Math.random().toFloat() * 1000 % 360)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap)))
            marker.tag = "Drone ID #03"
            droneMarkers[marker.tag as String] = marker
        }.addOnFailureListener {
            Toast.makeText(this@MainActivity, "Failed to get last location..", Toast.LENGTH_LONG).show()
            it.printStackTrace()
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * View.OnClickListener
     */
    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_log        -> { log_layout.visibility = ConstraintLayout.VISIBLE }
            R.id.btn_ok         -> { log_layout.visibility = ConstraintLayout.GONE }
            R.id.btn_arm        -> { mqttUtil.arm(mqttUtil.droneId) }
            R.id.btn_disarm     -> { mqttUtil.disarm(mqttUtil.droneId) }
            R.id.btn_takeoff    -> { mqttUtil.takeoff(mqttUtil.droneId) }
            R.id.btn_land       -> { mqttUtil.land(mqttUtil.droneId) }
            R.id.btn_start      -> { mqttUtil.start(droneIdList) }
        }
    }
    // View.OnClickListener

    /**
     * CompoundButton.OnCheckedChangeListener
     */
    override fun onCheckedChanged(button: CompoundButton, checked: Boolean) {
        Log.d(TAG, "button.isChecked: ${button.isChecked}")
        Log.d(TAG, "isChecked: $checked")
        mqttUtil.assignLeader(mqttUtil.droneId, button.isChecked)
    }
    // CompoundButton.OnCheckedChangeListener

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
        map.setOnMarkerClickListener(object: GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker): Boolean {
                Log.d(TAG, "OnMarkerClick(marker: $marker)")
                val intent = Intent(this@MainActivity, MissionActivity::class.java)
                    .putExtra("droneId", marker.tag as String)
                    .putExtra("center", marker.position)
                startActivityForResult(intent, REQUEST_MISSION_WAYPOINTS)
                return true
            }
        })
        map.setOnCameraIdleListener {
            val size = (128 * (map.cameraPosition.zoom / 20f)).toInt()
            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.drone), size, size, true)
            droneMarkers.values.forEach { it.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap)) }
        }

        mGoogleMap = map

        init()
    }
    // OnMapReadyCallback

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MISSION_WAYPOINTS -> {
                if (resultCode == RESULT_OK) {
                    val droneId = data?.getStringExtra("droneId")
                    val mission = data?.extras?.getParcelableArray("mission")?.map { it as LatLng }
                    mission?.forEach { Log.d(TAG, "mission: ${it.latitude}, ${it.longitude}")}
                    // TODO: Publish mqtt
                    val msgstr = "{ \"waypoints\": [" + mission?.map { "{ \"latitude\": ${it.latitude}, \"longitude\": ${it.longitude}, \"altitude\": 3 }" }?.joinToString(",") + "] }"
                    mqttUtil.publishMessage("mission_upload/$droneId", msgstr)
                }
            }
        }
    }

    public fun notifyHeartbeat(heartbeat: Heartbeat) {
        val droneId = heartbeat.hostname
        if (!droneIdList.contains(droneId)) {
            droneIdSpinnerAdapter.add(droneId)
            droneIdSpinnerAdapter.notifyDataSetChanged()
        }

        if (!droneMarkers.containsKey(droneId)) {
            Toast.makeText(this@MainActivity, "new marker @ (${heartbeat.global_position.latitude}, ${heartbeat.global_position.longitude})", Toast.LENGTH_LONG).show()
            Log.d(TAG, "new marker @ (${heartbeat.global_position.latitude}, ${heartbeat.global_position.longitude})")
            val markerOptions = MarkerOptions()
                .position(LatLng(heartbeat.global_position.latitude, heartbeat.global_position.longitude))
                .title("${heartbeat.hostname} (${heartbeat.leader})")
            droneMarkers[droneId] = mGoogleMap.addMarker(markerOptions)
            droneMarkers[droneId]?.tag = droneId
        }
        Log.d(TAG, "Update marker position to (${heartbeat.global_position.latitude}, ${heartbeat.global_position.longitude})")
        droneMarkers[droneId]?.position = LatLng(heartbeat.global_position.latitude, heartbeat.global_position.longitude)
    }

    public fun notifyLeader(leader: Boolean) {
        cb_leader.isChecked = leader
    }

    public fun notifyGlobalPosition(globalPosition: GlobalPosition) {
        tv_latitude.text = String.format("%.6f", globalPosition.latitude)
        tv_longitude.text = String.format("%.6f", globalPosition.longitude)
        tv_altitude.text = String.format("%.6f", globalPosition.altitude)
    }

    public fun notifyMavrosState(mavrosState: String) {
        tv_status_message.text = mavrosState
    }

    public fun notifyCommandResult(commandResult: String) {
        tv_command_result_message.text = commandResult
    }

    fun notifyBattery(percentage: Float) {
        tv_battery.text = String.format(resources.getString(R.string.percentage), percentage)
        iv_battery.setImageIcon(Icon.createWithResource(this@MainActivity, when {
            percentage == 100f -> { R.drawable.ic_battery_cell_4 }
            75f <= percentage && percentage < 100f -> { R.drawable.ic_battery_cell_3 }
            50f <= percentage && percentage < 75f -> { R.drawable.ic_battery_cell_2 }
            25f <= percentage && percentage < 50f -> { R.drawable.ic_battery_cell_1 }
            10f <= percentage && percentage < 25f -> { R.drawable.ic_battery_cell_0 }
            else -> { R.drawable.ic_battery_cell_0_red }
        }))
    }

    fun notifyLog(message: LogMessage) {
        mLogs.add(message)
        log_recycler_view.adapter?.notifyDataSetChanged()
    }
}
