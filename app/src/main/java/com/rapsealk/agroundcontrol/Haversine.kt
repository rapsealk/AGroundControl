package com.rapsealk.agroundcontrol

import com.google.android.gms.maps.model.LatLng

class Haversine {

    companion object {
        val RADIUS = 6371.0088
        fun getDistance2D(src: LatLng, dst: LatLng): Double {
            val lat1 = src.latitude
            val lon1 = src.longitude
            val lat2 = dst.latitude
            val lon2 = dst.longitude
            val dlat = Math.toRadians(lat2 - lat1)
            val dlon = Math.toRadians(lon2 - lon1)

            val a = Math.pow(Math.sin(dlat/2), 2.0)
                    + Math.pow(Math.sin(dlon/2), 2.0) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lon1))
            val c = Math.asin(Math.sqrt(a)) * 2

            return RADIUS * c * 1000    // meters
        }
    }
}