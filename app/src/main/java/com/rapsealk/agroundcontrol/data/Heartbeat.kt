package com.rapsealk.agroundcontrol.data

data class Heartbeat(
    override val type: String               = "heartbeat",
    override val timestamp: Double          = System.currentTimeMillis().toDouble() / 1000,
    override val hostname: String           = "android",
    val leader: Boolean                     = false,
    val mission_completed: Boolean          = false,
    val global_position: GlobalPosition     = GlobalPosition(),
    val velocity: Velocity                  = Velocity(),
    //val waypoints: Array<GlobalPosition>    = Array<GlobalPosition>(),
    val wp_idx: Int                         = 0,
    val mode: String                        = "disarm"
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

data class Velocity(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    override fun toString(): String = "{" +
        "\"x\": $x," +
        "\"y\": $y," +
        "\"z\": $z" +
    "}"
}