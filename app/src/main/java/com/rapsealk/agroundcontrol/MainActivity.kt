package com.rapsealk.agroundcontrol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.rapsealk.agroundcontrol.data.GlobalPosition
import com.rapsealk.agroundcontrol.data.Heartbeat
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener,
    OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private val TAG = MainActivity::class.java.simpleName

    companion object {
        private const val REQUEST_PERMISSION_LOCATION = 0x0001
    }

    private val droneIdList = arrayListOf("")
    private lateinit var droneIdSpinnerAdapter: ArrayAdapter<String>

    private val flightModeList = arrayOf("flocking", "row", "column")
    private lateinit var flightModeSpinnerAdapter: ArrayAdapter<String>

    private lateinit var mGoogleMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val droneMarkers = HashMap<String, Marker>()

    private lateinit var mSocket: WebSocketIO

    private var droneHostname: String = ""

    private var mCenter = LatLng(0.0, 0.0)
    private val mMarkers = ArrayList<Marker>()
    private var mPolyline: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        droneIdSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, droneIdList)
        droneIdSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        drone_id_spinner.adapter = droneIdSpinnerAdapter
        drone_id_spinner.onItemSelectedListener = this

        flightModeSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, flightModeList)
        flightModeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        flight_mode_spinner.adapter = flightModeSpinnerAdapter

        mSocket = WebSocketIO()
        try {
            mSocket.connect()
        } catch (exception: Exception) {
            Snackbar.make(root_view, "socket.io 연결에 실패했습니다.", Snackbar.LENGTH_INDEFINITE).show()
            Log.d(TAG, "socket.io 연결에 실패했습니다.")
            exception.printStackTrace()
        }
        /*
        val thread = Thread(mSocket)
        thread.isDaemon = true
        thread.start()
        */

        cb_leader.setOnClickListener(this)
        btn_home.setOnClickListener(this)
        btn_mark.setOnClickListener(this)
        btn_reset.setOnClickListener(this)
        btn_upload.setOnClickListener(this)
        btn_ok.setOnClickListener(this)

        btn_arm.setOnClickListener(this)
        btn_disarm.setOnClickListener(this)
        btn_takeoff.setOnClickListener(this)
        btn_land.setOnClickListener(this)
        btn_start.setOnClickListener(this)

        checkPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.disconnect()
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
            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.red_zerg), 128, 128, true)
            val markerOptions = MarkerOptions()
                .position(LatLng(it.latitude, it.longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            val marker = mGoogleMap.addMarker(markerOptions)
            marker.tag = "Drone ID #01"
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
            R.id.cb_leader -> {
                val button = view as CheckBox
                Log.d(TAG, "checked: ${button.isChecked}")
                mSocket.queue(WebSocketIO.EVENT_COMMAND, "{ \"type\": \"command\", \"target\": \"$droneHostname\", \"command\": \"assign_leader\", \"leader\": ${button.isChecked} }")

            }
            R.id.btn_arm    -> { mSocket.queue(WebSocketIO.EVENT_COMMAND, "{ \"type\": \"command\", \"target\": \"$droneHostname\", \"command\": \"arm\" }") }
            R.id.btn_disarm -> { mSocket.queue(WebSocketIO.EVENT_COMMAND, "{ \"type\": \"command\", \"target\": \"$droneHostname\", \"command\": \"disarm\" }") }
            R.id.btn_takeoff -> { mSocket.queue(WebSocketIO.EVENT_COMMAND, "{ \"type\": \"command\", \"target\": \"$droneHostname\", \"command\": \"takeoff\" }") }
            R.id.btn_land -> { mSocket.queue(WebSocketIO.EVENT_COMMAND, "{ \"type\": \"command\", \"target\": \"$droneHostname\", \"command\": \"land\" }") }
            R.id.btn_start -> {
                val flightMode = flight_mode_spinner.selectedItem as String
                Log.d(TAG, "flightMode: $flightMode")
                mSocket.queue(WebSocketIO.EVENT_COMMAND, "{ \"type\": \"command\", \"target\": \"$droneHostname\", \"command\": \"$flightMode\" }")
            }
            R.id.btn_home -> { droneMarkers[droneHostname]?.position?.let { mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(it)) } }

            R.id.btn_mark -> {
                Log.d(TAG, "Mark at (${mCenter.latitude}, ${mCenter.longitude})")
                val markerOptions = MarkerOptions().position(mCenter)
                val marker = mGoogleMap.addMarker(markerOptions)
                mMarkers.add(marker)

                mPolyline?.remove()
                mPolyline = mGoogleMap.addPolyline(PolylineOptions()
                    .addAll(mMarkers.map { it.position })
                    .width(10f)
                    .color(Color.GREEN))
            }
            R.id.btn_reset -> {
                mMarkers.forEach { it.remove() }
                mMarkers.clear()
                mPolyline?.remove()
                mPolyline = null
            }
            R.id.btn_upload -> {
                val mission = mMarkers.map { it.position }
                mission.forEach { Log.d(TAG, "mission: ${it.latitude}, ${it.longitude}")}
                val msgstr = "[" + mission.map { "{ \"latitude\": ${it.latitude}, \"longitude\": ${it.longitude}, \"altitude\": 3 }" }.joinToString(",") + "]"
                mSocket.queue(WebSocketIO.EVENT_COMMAND, "{ \"type\": \"command\", \"target\": \"$droneHostname\", \"command\": \"mission_upload\", \"waypoints\": $msgstr }")
            }
        }
    }
    // View.OnClickListener

    /**
     * AdapterView.OnItemSelectedListener
     */
    override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        Log.d(TAG, "onItemSelected(parent: $parent, view: $view, position: $position, id: $id)")
        droneHostname = parent?.adapter?.getItem(position) as String
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "onNothingSelected(parent: $parent)")
    }
    // AdapterView.OnItemSelectedListener

    /**
     * OnMapReadyCallback
     */
    override fun onMapReady(map: GoogleMap) {
        map.setOnCameraIdleListener {
            val size = (128 * (map.cameraPosition.zoom / 20f)).toInt()
            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.red_zerg), size, size, true)
            droneMarkers.values.forEach { it.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap)) }
        }
        map.setOnCameraIdleListener(this)

        val safeArea = listOf(
            LatLng(37.599227, 126.863522),
            LatLng(37.599356, 126.863067),
            LatLng(37.599523, 126.863155),
            LatLng(37.599394, 126.863610)
        )
        val polygonOption = PolygonOptions()
            .addAll(safeArea)
            .fillColor(Color.GREEN)
            .strokeColor(Color.BLUE)
        map.addPolygon(polygonOption)

        // map.mapType = GoogleMap.MAP_TYPE_SATELLITE

        mGoogleMap = map

        init()
    }
    // OnMapReadyCallback

    /**
     * GoogleMap.OnCameraIdleListener
     */
    override fun onCameraIdle() {
        mCenter = mGoogleMap.projection.visibleRegion.latLngBounds.center
    }
    // GoogleMap.OnCameraIdleListener

    public fun notifyHeartbeat(heartbeat: Heartbeat) {
        val droneId = heartbeat.hostname
        if (!droneIdList.contains(droneId)) {
            droneIdSpinnerAdapter.add(droneId)
            droneIdSpinnerAdapter.notifyDataSetChanged()
        }

        if (!droneMarkers.containsKey(droneId)) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(500)
            }
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
        // Leader flag
        if (heartbeat.hostname == droneHostname)
            notifyLeader(heartbeat.leader)
        notifyGlobalPosition(heartbeat.global_position)
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
            100f == percentage -> { R.drawable.ic_battery_cell_4 }
            75f <= percentage -> { R.drawable.ic_battery_cell_3 }
            50f <= percentage -> { R.drawable.ic_battery_cell_2 }
            25f <= percentage -> { R.drawable.ic_battery_cell_1 }
            10f <= percentage -> { R.drawable.ic_battery_cell_0 }
            else -> { R.drawable.ic_battery_cell_0_red }
        }))
    }
}
