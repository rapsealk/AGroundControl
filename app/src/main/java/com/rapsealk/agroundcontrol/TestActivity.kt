package com.rapsealk.agroundcontrol

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.rapsealk.agroundcontrol.data.Agent
import kotlinx.android.synthetic.main.activity_main.*

class TestActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mGoogleMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private val mDroneAgents = arrayOf(Agent("L1", true), Agent("F1"), Agent("F2"))
    private val mDroneMarkers = ArrayList<Marker>()

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
            //Toast.makeText(this@MainActivity, "(${it.latitude}, ${it.longitude}, ${it.altitude}) with acc: ${it.accuracy}", Toast.LENGTH_LONG).show()
            tv_latitude.text = it.latitude.toString()
            tv_longitude.text = it.longitude.toString()
            tv_altitude.text = String.format("%.6f", it.altitude)
            val latlng = LatLng(it.latitude, it.longitude)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 20f))
            /* FIXME: sample marker
            val bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.red_zerg), 128, 128, true)
            val markerOptions = MarkerOptions()
                .position(LatLng(it.latitude, it.longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            val marker = mGoogleMap.addMarker(markerOptions)
            marker.tag = "Drone ID #01"
            droneMarkers[marker.tag as String] = marker
            */
            //val marker = testMarkerWithHeading(latlng)
            for (drone in mDroneAgents) {
                val markerOption = MarkerOptions()

                val marker = mGoogleMap.addMarker(markerOption)
                marker.tag = drone
                mDroneMarkers.add(marker)
            }
        }.addOnFailureListener {
            Toast.makeText(this@TestActivity, "Failed to get last location..", Toast.LENGTH_LONG).show()
            it.printStackTrace()
        }
    }

    /**
     * OnMapReadyCallback
     */
    override fun onMapReady(map: GoogleMap) {
        mGoogleMap = map
        init()
    }
}
