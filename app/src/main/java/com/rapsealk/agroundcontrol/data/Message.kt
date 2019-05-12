package com.rapsealk.agroundcontrol.data

abstract class Message {
    abstract val type: String
    abstract val hostname: String
    abstract val timestamp: Double
}

data class LogMessage(
    val hostname: String,
    val tag: String,
    val message: String,
    val timestamp: Double
)

data class StateMessage(
    val hostname: String,
    val connected: Boolean,
    val amred: Boolean,
    val guided: Boolean,
    val mode: String
)

data class BatteryMessage(
    val hostname: String,
    val percentage: Float
)