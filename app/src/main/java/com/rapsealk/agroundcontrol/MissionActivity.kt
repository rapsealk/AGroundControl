package com.rapsealk.agroundcontrol

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_mission.*

class MissionActivity : AppCompatActivity(), View.OnClickListener, OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private val TAG = MissionActivity::class.java.simpleName

    private lateinit var mGoogleMap: GoogleMap

    private var droneId: String = ""
    private var center: LatLng = LatLng(0.0, 0.0)
    private val mMarkers = ArrayList<Marker>()
    private var mMarker: Marker? = null
    private var mPolyline: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mission)

        droneId = intent.getStringExtra("droneId")
        center = intent.extras?.getParcelable("center") as LatLng
        Log.d(TAG, "Drone ID: $droneId")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btn_add.setOnClickListener(this)
        btn_remove.setOnClickListener(this)
        btn_allocate.setOnClickListener(this)
        btn_cancel.setOnClickListener(this)
    }

    /**
     * OnMapReadyCallback
     */
    override fun onMapReady(map: GoogleMap) {
        map.setOnCameraIdleListener(this@MissionActivity)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 16f))
        mGoogleMap = map

        btn_add.isEnabled = true
    }
    // OnMapReadyCallback

    /**
     * GoogleMap.OnCameraIdleListener
     */
    override fun onCameraIdle() {
        center = mGoogleMap.projection.visibleRegion.latLngBounds.center
    }
    // GoogleMap.OnCameraIdleListener

    /**
     * View.OnClickListener
     */
    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_add -> {
                val markerOptions = MarkerOptions()
                    .position(center)
                val marker = mGoogleMap.addMarker(markerOptions)
                mMarkers.add(marker)
                drawPolyline()
            }
            R.id.btn_remove -> {

            }
            R.id.btn_allocate -> {
                val intent = Intent()
                intent.putExtra("droneId", droneId)
                intent.putExtra("mission", mMarkers.map { it.position }.toTypedArray())
                setResult(RESULT_OK, intent)
                finish()
            }
            R.id.btn_cancel -> {
                finish()
            }
        }
    }
    // View.OnClickListener

    private fun drawPolyline() {
        mPolyline?.remove()
        mPolyline = mGoogleMap.addPolyline(PolylineOptions()
            .addAll(mMarkers.map { it.position })
            .width(10f)
            .color(Color.GREEN))
    }
}
