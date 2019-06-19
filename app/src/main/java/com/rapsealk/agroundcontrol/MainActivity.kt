package com.rapsealk.agroundcontrol

import android.Manifest
import android.content.Context
import android.content.Intent
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
import android.support.v4.content.res.ResourcesCompat
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
import com.rapsealk.agroundcontrol.data.*
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
    private val droneHeadings = HashMap<Marker, Polyline>()
    // ==============================================================
    private val collisionAvoidance = HashMap<Marker, Polyline>()
    private val velocityMatching = HashMap<Marker, Polyline>()
    private val flockingCenter = HashMap<Marker, Polyline>()
    private val formationControl = HashMap<Marker, Polyline>()
    private val target = HashMap<Marker, Polyline>()
    private val circles = HashMap<Marker, Circle>()

    private lateinit var mSocket: Socketeer

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

        mSocket = Socketeer(this)
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

        btn_arm.setOnClickListener(this)
        btn_disarm.setOnClickListener(this)
        btn_takeoff.setOnClickListener(this)
        btn_land.setOnClickListener(this)
        btn_start.setOnClickListener(this)

        tv_test.setOnClickListener(this)

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
            tv_latitude.text = it.latitude.toString()
            tv_longitude.text = it.longitude.toString()
            tv_altitude.text = String.format("%.6f", it.altitude)
            val latlng = LatLng(it.latitude, it.longitude)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 20f))
            //val marker = testMarkerWithHeading(latlng)
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
                mSocket.queue(Socketeer.EVENT_COMMAND, LeaderCommand(target = droneHostname, leader = button.isChecked))
            }
            R.id.btn_arm    -> { mSocket.queue(Socketeer.EVENT_COMMAND,  Command(target = droneHostname, command = "arm")) }
            R.id.btn_disarm -> { mSocket.queue(Socketeer.EVENT_COMMAND, Command(target = droneHostname, command = "disarm")) }
            R.id.btn_takeoff -> { mSocket.queue(Socketeer.EVENT_COMMAND, Command(target = droneHostname, command = "takeoff")) }
            R.id.btn_land -> { mSocket.queue(Socketeer.EVENT_COMMAND, Command(target = droneHostname, command = "land")) }
            R.id.btn_start -> {
                val flightMode = flight_mode_spinner.selectedItem as String
                Log.d(TAG, "flightMode: $flightMode")
                mSocket.queue(Socketeer.EVENT_COMMAND, Command(target = droneHostname, command = flightMode))
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
                val waypoints = mMarkers.map { GlobalPosition(it.position.latitude, it.position.longitude, 10.0) }
                mSocket.queue(Socketeer.EVENT_COMMAND, WaypointCommand(target = droneHostname, waypoints = waypoints))
            }
            R.id.tv_test -> {
                val intent = Intent(this, TestActivity::class.java)
                startActivity(intent)
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
        // FIXME: Duplicated
        map.setOnCameraIdleListener {
            val size = (128 * (map.cameraPosition.zoom / 20f)).toInt()
            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.red_zerg), size, size, true)
            droneMarkers.values.forEach { it.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap)) }
        }
        map.setOnCameraIdleListener(this)

        /*
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
        */

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
        val position = heartbeat.global_position

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
            Toast.makeText(this@MainActivity, "new marker @ (${position.latitude}, ${position.longitude})", Toast.LENGTH_LONG).show()
            Log.d(TAG, "new marker @ (${position.latitude}, ${position.longitude})")
            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.red_zerg), 36, 36, true)
            val markerOptions = MarkerOptions()
                .position(LatLng(position.latitude, position.longitude))
                .title("${heartbeat.hostname} (${heartbeat.leader})")
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .anchor(0.5f, 0.5f)
            droneMarkers[droneId] = mGoogleMap.addMarker(markerOptions)
            droneMarkers[droneId]?.tag = droneId
        }
        Log.d(TAG, "Update marker position to (${position.latitude}, ${position.longitude})")
        droneMarkers[droneId]?.position = LatLng(position.latitude, position.longitude)
        // ==========================================================
        val marker = droneMarkers[droneId]!!
        val latlng = LatLng(position.latitude, position.longitude)
        /* # 1 Steering
        val steer = heartbeat.steer
        droneHeadings[marker]?.remove()
        if (!steer.isEmpty()) {
            val polylineOptions = PolylineOptions()
                .add(latlng)
                .add(LatLng(position.latitude + steer.x / 100, position.longitude + steer.y / 100))
                .width(10f)
                .color(ResourcesCompat.getColor(resources, android.R.color.holo_orange_light, null))
            droneHeadings[marker] = mGoogleMap.addPolyline(polylineOptions)
        }
        */
        // # 2 Collision Avoidance
        val colavo = heartbeat.collision_avoidance
        collisionAvoidance[marker]?.remove()
        if (!colavo.isEmpty()) {
            val collisionAvoidancePolylineOptions = PolylineOptions()
                .add(latlng)
                .add(LatLng(latlng.latitude + (colavo.x-latlng.latitude) * 3, latlng.longitude + (colavo.y-latlng.longitude) * 3))
                .width(10f)
                .color(ResourcesCompat.getColor(resources, android.R.color.holo_red_dark, null))
            collisionAvoidance[marker] = mGoogleMap.addPolyline(collisionAvoidancePolylineOptions)
        }
        // # 3 Velocity Matching
        val velmat = heartbeat.velocity_matching
        velocityMatching[marker]?.remove()
        if (!velmat.isEmpty()) {
            val velocityMatchingPolylineOptions = PolylineOptions()
                .add(latlng)
                //.add(LatLng(velmat.x, velmat.y))
                .add(LatLng(latlng.latitude + (velmat.x-latlng.latitude)*3, latlng.longitude+(velmat.y-latlng.longitude)*3))
                .width(10f)
                .color(ResourcesCompat.getColor(resources, android.R.color.holo_green_light, null))
            velocityMatching[marker] = mGoogleMap.addPolyline(velocityMatchingPolylineOptions)
        }
        // # 4 Flocking Center
        val flocen = heartbeat.flocking_center
        flockingCenter[marker]?.remove()
        if (!flocen.isEmpty()) {
            val flockingCenterPolylineOptions = PolylineOptions()
                .add(latlng)
                //.add(LatLng(flocen.x, flocen.y))
                .add(LatLng(latlng.latitude + (flocen.x-latlng.latitude)*3, latlng.longitude+(flocen.y-latlng.longitude)*3))
                .width(10f)
                .color(ResourcesCompat.getColor(resources, android.R.color.holo_blue_light, null))
            flockingCenter[marker] = mGoogleMap.addPolyline(flockingCenterPolylineOptions)
        }
        // # 5 Formation Control
        val forcon = heartbeat.formation_control
        formationControl[marker]?.remove()
        if (!forcon.isEmpty()) {
            val formationControlPolylineOptions = PolylineOptions()
                .add(latlng)
                //.add(LatLng(forcon.x, forcon.y))
                .add(LatLng(latlng.latitude + (forcon.x-latlng.latitude)*3, latlng.longitude+(forcon.y-latlng.longitude)*3))
                .width(10f)
                .color(ResourcesCompat.getColor(resources, android.R.color.holo_purple, null))
            formationControl[marker] = mGoogleMap.addPolyline(formationControlPolylineOptions)
        }
        // # 6 Target
        val _target = heartbeat.target
        target[marker]?.remove()
        if (!_target.isEmpty()) {
            val targetPolylineOptions = PolylineOptions()
                .add(latlng)
                //.add(LatLng(_target.x, _target.y))
                .add(LatLng(latlng.latitude + (_target.x-latlng.latitude)*3, latlng.longitude+(_target.y-latlng.longitude)*3))
                .width(10f)
                .color(ResourcesCompat.getColor(resources, R.color.livingCoral, null))
            target[marker] = mGoogleMap.addPolyline(targetPolylineOptions)
        }
        // # 7 Boundary Circle
        circles[marker]?.remove()
        val circleOptions = CircleOptions()
            .center(latlng)
            .radius(3.0)    // Radius in meters
            .strokeWidth(8f)
            .strokePattern(listOf(Dash(4f)))
            .strokeColor(Color.YELLOW)
            .fillColor(Color.TRANSPARENT)
        circles[marker] = mGoogleMap.addCircle(circleOptions)
        // ==========================================================
        // Leader flag
        if (heartbeat.hostname == droneHostname)
            notifyLeader(heartbeat.leader)
        notifyGlobalPosition(position)
    }

    fun notifyTurnOff(hostname: String) {
        if (droneIdList.contains(hostname)) {
            droneIdSpinnerAdapter.remove(hostname)
            droneIdSpinnerAdapter.notifyDataSetChanged()
        }
        droneMarkers.remove(hostname)?.remove()
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

    private fun testMarkerWithHeading(position: LatLng): Marker {
        val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.red_zerg), 108, 108, true)
        val markerOptions = MarkerOptions()
            .position(position)
            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            .anchor(0.5f, 0.5f)
        val marker = mGoogleMap.addMarker(markerOptions)
        val polylineOptions = PolylineOptions()
            .add(position)
            .add(LatLng(position.latitude + 0.00005, position.longitude + 0.0001))
            .width(10f)
            .color(ResourcesCompat.getColor(resources, android.R.color.holo_orange_light, null))
        val polyline = mGoogleMap.addPolyline(polylineOptions)
        val collisionAvoidancePolylineOptions = PolylineOptions()
            .add(position)
            .add(LatLng(position.latitude + 0.000025, position.longitude + 0.0002))
            .width(10f)
            .color(ResourcesCompat.getColor(resources, android.R.color.holo_red_light, null))
        val collisionAvoidancePolyline = mGoogleMap.addPolyline(collisionAvoidancePolylineOptions)
        val velocityMatchingPolylineOptions = PolylineOptions()
            .add(position)
            .add(LatLng(position.latitude - 0.000025, position.longitude + 0.0002))
            .width(10f)
            .color(ResourcesCompat.getColor(resources, android.R.color.holo_green_light, null))
        val velocityMatchingPolyline = mGoogleMap.addPolyline(velocityMatchingPolylineOptions)
        val flockingCenterPolylineOptions = PolylineOptions()
            .add(position)
            .add(LatLng(position.latitude - 0.000025, position.longitude - 0.0001))
            .width(10f)
            .color(ResourcesCompat.getColor(resources, android.R.color.holo_blue_light, null))
        val flockingCenterPolyline = mGoogleMap.addPolyline(flockingCenterPolylineOptions)
        val formationControlPolylineOptions = PolylineOptions()
            .add(position)
            .add(LatLng(position.latitude + 0.00005, position.longitude - 0.0001))
            .width(10f)
            .color(ResourcesCompat.getColor(resources, android.R.color.holo_purple, null))
        val formationControlPolyline = mGoogleMap.addPolyline(formationControlPolylineOptions)
        droneHeadings[marker] = polyline
        val circleOptions = CircleOptions()
            .center(position)
            .radius(5.0)    // Radius in meters
            .strokeWidth(8f)
            .strokePattern(listOf(Dash(4f)))
            .strokeColor(Color.YELLOW)
            .fillColor(Color.TRANSPARENT)
        mGoogleMap.addCircle(circleOptions)
        return marker
    }
}
