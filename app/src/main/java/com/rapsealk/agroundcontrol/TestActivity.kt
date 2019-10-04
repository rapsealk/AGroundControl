package com.rapsealk.agroundcontrol

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.rapsealk.agroundcontrol.data.Agent

class TestActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapClickListener {

    private lateinit var mGoogleMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private val mDroneAgents = arrayOf(Agent("L1", true), Agent("F1"), Agent("F2"))
    private val mDroneMarkers = ArrayList<Marker>()
    private val mTargetMarker by lazy { mGoogleMap.addMarker(MarkerOptions().position(LatLng(0.0, 0.0))) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        initMap()
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.lastLocation.addOnSuccessListener {
            val latlng = LatLng(it.latitude, it.longitude)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 20f))

            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.red_zerg), 36, 36, true)
            val offsetX = doubleArrayOf(0.0, 0.00005, -0.00005)
            val offsetY = doubleArrayOf(0.0, 0.0001, -0.0001)
            mDroneAgents.forEachIndexed { i, agent ->
                // TODO: Reactive
                agent.position.x = it.latitude + offsetX[i]
                agent.position.y = it.longitude + offsetY[i]
                val markerOptions = MarkerOptions()
                    .position(agent.position.toLatLng())
                    .rotation(0f)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .anchor(0.5f, 0.5f)
                val marker = mGoogleMap.addMarker(markerOptions)
                marker.tag = agent
                mDroneMarkers.add(marker)
            }

            /*
            val thread = kotlinx.coroutines.Runnable {
                while (true) {
                    val targetPosition = mTargetMarker.position
                    runOnUiThread {

                    }
                    Thread.sleep(1000)
                }
            }
            thread.run()
            */
        }.addOnFailureListener {
            Toast.makeText(this@TestActivity, "Failed to get last location..", Toast.LENGTH_LONG).show()
            it.printStackTrace()
        }
    }

    /**
     * OnMapReadyCallback
     */
    override fun onMapReady(map: GoogleMap) {
        map.setOnCameraIdleListener(this)
        map.setOnMapClickListener(this)
        mGoogleMap = map
        init()
    }

    /**
     * GoogleMap.OnCameraIdleListener
     */
    override fun onCameraIdle() {
        val size = (128 * (mGoogleMap.cameraPosition.zoom / 20f)).toInt()
        val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.red_zerg), size, size, true)
        mDroneMarkers.forEach { it.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap)) }
    }

    /**
     * GoogleMap.OnMapClickListener
     */
    override fun onMapClick(position: LatLng) {
        mTargetMarker.position = position
    }
}
