package com.rapsealk.agroundcontrol.data

data class Message(
    val hostname: String,
    val message: String
)

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