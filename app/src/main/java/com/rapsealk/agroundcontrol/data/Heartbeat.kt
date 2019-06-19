package com.rapsealk.agroundcontrol.data

import com.google.android.gms.maps.model.LatLng
import com.rapsealk.agroundcontrol.Haversine

data class Heartbeat(
    override val type: String               = "heartbeat",
    override val timestamp: Double          = System.currentTimeMillis().toDouble() / 1000,
    override val hostname: String           = "android",
    val leader: Boolean                     = false,
    val mission_completed: Boolean          = false,
    val global_position: GlobalPosition     = GlobalPosition(),
    val velocity: Vector                    = Vector.EmptyVector,
    //val waypoints: Array<GlobalPosition>    = Array<GlobalPosition>(),
    val wp_idx: Int                         = 0,
    val mode: String                        = "disarm",
    // TODO
    val collision_avoidance: Vector         = Vector.EmptyVector,
    val velocity_matching: Vector           = Vector.EmptyVector,
    val flocking_center: Vector             = Vector.EmptyVector,
    val formation_control: Vector           = Vector.EmptyVector,
    val steer: Vector                       = Vector.EmptyVector,
    val target: Vector                      = Vector.EmptyVector
) : Message() {
    override fun toString(): String = "{" +
        "\"timestamp\": $timestamp," +
        "\"hostname\": \"$hostname\"," +
        "\"leader\": $leader," +
        "\"mission_completed\": $mission_completed," +
        "\"global_position\": $global_position," +
        "\"velocity\": $velocity," +
        "\"wp_idx\": $wp_idx," +
        "\"mode\": \"$mode\"" +
    "}"
}

data class GlobalPosition(
    val latitude: Double    = 0.0,
    val longitude: Double   = 0.0,
    val altitude: Double    = 0.0
) {
    override fun toString(): String = "{" +
        "\"latitude\": $latitude," +
        "\"longitude\": $longitude," +
        "\"altitude\": $altitude" +
    "}"
}

data class Vector(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0
) {
    companion object {
        fun distnace3D(vector1: Vector, vector2: Vector) = Math.sqrt(
            Math.pow(vector1.x - vector2.x, 2.0)
            + Math.pow(vector1.y - vector2.y, 2.0)
            + Math.pow(vector1.z - vector2.z, 2.0)
        )

        @JvmStatic
        val EmptyVector = Vector()
    }

    fun isEmpty(): Boolean = (x == 0.0 && y == 0.0 && z == 0.0)

    operator fun Vector.plus(other: Vector): Vector {
        this.x += other.x
        this.y += other.y
        this.z += other.z
        return Vector(x, y, z)
    }

    fun toLatLng() = LatLng(x, y)

    fun toLocalized(): Vector {
//        val min_lat = 33.111944
//        val max_lat = 43.011667
//        val min_lon = 124.180833
//        val max_lon = 131.872778
        val center_lat = 38.0
        val center_lon = 127.5

        val center = LatLng(center_lat, center_lon)

        var x = Haversine.getDistance2D(center, LatLng(center_lat, y))
        var y = Haversine.getDistance2D(center, LatLng(x, center_lon))

        if (this.y < center_lon) {
            x *= -1;
        } else if (this.x < center_lat) {
            y *= -1;
        }
        return Vector(x, y, z)
    }

    override fun toString(): String = "{" +
        "\"x\": $x," +
        "\"y\": $y," +
        "\"z\": $z" +
    "}"
}