package com.rapsealk.agroundcontrol.data

data class Heartbeat(
    val timestamp: Double,
    val hostname: String,
    val leader: Boolean                     = false,
    val mission_completed: Boolean          = false,
    val global_position: GlobalPosition     = GlobalPosition(),
    val velocity: Velocity                  = Velocity(),
    //val waypoints: Array<GlobalPosition>    = Array<GlobalPosition>(),
    val wp_idx: Int                         = 0,
    val mode: String                        = "disarm"
)

data class GlobalPosition(
    val latitude: Double    = 0.0,
    val longitude: Double   = 0.0,
    val altitude: Double    = 0.0
)

data class Velocity(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
)