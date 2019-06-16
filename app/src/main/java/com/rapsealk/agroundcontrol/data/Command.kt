package com.rapsealk.agroundcontrol.data

open class Command(
    override val type: String               = "command",
    override val timestamp: Double          = System.currentTimeMillis().toDouble() / 1000,
    override val hostname: String           = "android",
    open val target: String,
    open val command: String
) : Message() {
    override fun toString(): String = "{" +
        "\"timestamp\": $timestamp," +
        "\"hostname\": \"$hostname\"," +
        "\"target\": \"$target\"," +
        "\"command\": \"$command\"" +
    "}"
}

data class WaypointCommand(
    override val timestamp: Double          = System.currentTimeMillis().toDouble() / 1000,
    override val hostname: String           = "android",
    override val target: String,
    override val command: String            = "mission_upload",
    val waypoints: List<GlobalPosition>     = ArrayList()
) : Command(timestamp = timestamp, hostname = hostname, target = target, command = command) {
    override fun toString(): String = "{" +
        "\"timestamp\": $timestamp," +
        "\"hostname\": \"$hostname\"," +
        "\"target\": \"$target\"," +
        "\"command\": \"$command\"," +
        "\"waypoints\": [${waypoints.map { "{ \"latitude\": ${it.latitude}, \"longitude\": ${it.longitude}, \"altitude\": ${it.altitude} }"}.joinToString(",")}]" +
    "}"
}

data class LeaderCommand(
    override val timestamp: Double          = System.currentTimeMillis().toDouble() / 1000,
    override val hostname: String           = "android",
    override val target: String,
    override val command: String            = "assign_leader",
    val leader: Boolean                     = true
) : Command(timestamp = timestamp, hostname = hostname, target = target, command = command) {
    override fun toString(): String = "{" +
        "\"timestamp\": $timestamp," +
        "\"hostname\": \"$hostname\"," +
        "\"target\": \"$target\"," +
        "\"command\": \"$command\"," +
        "\"leader\": $leader" +
    "}"
}