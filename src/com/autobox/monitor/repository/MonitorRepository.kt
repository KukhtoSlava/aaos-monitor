package com.autobox.monitor.repository

import com.autobox.monitor.model.ClimateZone
import com.autobox.monitor.model.MonitorData
import kotlinx.coroutines.flow.StateFlow

interface MonitorRepository {
    val data: StateFlow<MonitorData>

    fun connect()
    fun disconnect()

    fun adjustTemperature(zone: ClimateZone, delta: Float)
    fun setFanLevel(zone: ClimateZone, level: Int)
}
