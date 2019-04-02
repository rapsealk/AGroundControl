package com.rapsealk.agroundcontrol.data

data class Heartbeat(
    val timestamp: Double,
    val hostname: String,
    val leader: Boolean,
    val global_position: GlobalPosition,
    val velocity: Velocity
)

data class GlobalPosition(
    val latitude: Float,
    val longitude: Float,
    val altitude: Float
)

data class Velocity(
    val x: Double,
    val y: Double,
    val z: Double
)