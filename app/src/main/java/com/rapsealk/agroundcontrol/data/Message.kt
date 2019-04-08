package com.rapsealk.agroundcontrol.data

data class Message(
    val hostname: String,
    val message: String
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