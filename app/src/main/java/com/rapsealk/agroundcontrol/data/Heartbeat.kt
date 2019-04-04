package com.rapsealk.agroundcontrol.data

data class Heartbeat(
    val timestamp: Double,
    val hostname: String,
    val leader: Boolean,
    val global_position: GlobalPosition,
    val velocity: Velocity
)

data class GlobalPosition(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double
)

data class Velocity(
    val x: Double,
    val y: Double,
    val z: Double
)