package com.autobox.monitor.repository.impl

import com.autobox.monitor.model.ClimatePanelState
import com.autobox.monitor.model.ClimateZone
import com.autobox.monitor.model.Gear
import com.autobox.monitor.model.MonitorData
import com.autobox.monitor.model.UxMode
import com.autobox.monitor.repository.MonitorRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class MockMonitorRepository @Inject constructor(): MonitorRepository {
    private val state = MutableStateFlow(MonitorData())
    override val data: StateFlow<MonitorData> = state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    override fun connect() {
        if (tickerJob != null) {
            return
        }
        tickerJob = scope.launch {
            var phase = 0.0f
            while (isActive) {
                delay(550L)
                phase += 0.12f
                state.update { current ->
                    val base = if (current.mode == UxMode.DRIVING) 85 else 20
                    val speed = (base + (sin(phase.toDouble()) * 55.0 + 55.0)).roundToInt().coerceIn(0, 250)
                    val rpm = (0.8f + speed / 42.0f).coerceIn(0.0f, 8.0f)
                    val drowsiness =
                        (((sin((phase / 1.8f).toDouble()) + 1.0) * 50.0).roundToInt()).coerceIn(0, 100)
                    val fuelDrift = if (current.mode == UxMode.DRIVING && speed > 20) 1 else 0
                    val nextFuel = (current.fuelPercent - fuelDrift).coerceAtLeast(8)
                    val gear = when {
                        current.mode == UxMode.PARKING -> Gear.P
                        speed < 5 -> Gear.N
                        speed < 20 -> Gear.R
                        else -> Gear.D
                    }
                    current.copy(
                        speedKmh = speed,
                        rpmX1000 = rpm,
                        drowsiness = drowsiness,
                        fuelPercent = nextFuel,
                        fuelRangeKm = (nextFuel * 6.5f).roundToInt(),
                        gear = gear,
                        averageSpeedKmh = ((current.averageSpeedKmh + speed) / 2).coerceIn(25, 160),
                        maxSpeedKmh = maxOf(current.maxSpeedKmh, speed),
                    )
                }
            }
        }
    }

    override fun disconnect() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun adjustTemperature(zone: ClimateZone, delta: Float) {
        state.update { current ->
            val min = 16.0f
            val max = 30.0f
            when (zone) {
                ClimateZone.DRIVER -> {
                    val next = (current.driverClimate.targetTemperature + delta).coerceIn(min, max)
                    current.copy(driverClimate = current.driverClimate.copy(targetTemperature = next))
                }

                ClimateZone.PASSENGER -> {
                    val next = (current.passengerClimate.targetTemperature + delta).coerceIn(min, max)
                    current.copy(passengerClimate = current.passengerClimate.copy(targetTemperature = next))
                }
            }
        }
    }

    override fun setFanLevel(zone: ClimateZone, level: Int) {
        val clamped = level.coerceIn(0, 7)
        state.update { current ->
            when (zone) {
                ClimateZone.DRIVER -> current.copy(
                    driverClimate = ClimatePanelState(
                        targetTemperature = current.driverClimate.targetTemperature,
                        fanLevel = clamped,
                    )
                )

                ClimateZone.PASSENGER -> current.copy(
                    passengerClimate = ClimatePanelState(
                        targetTemperature = current.passengerClimate.targetTemperature,
                        fanLevel = clamped,
                    )
                )
            }
        }
    }

}
