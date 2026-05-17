package com.autobox.monitor.model

enum class MonitorTab {
    CAR,
    INFO,
}

enum class ThemeMode(val label: String) {
    LIGHT("LIGHT"),
    DARK("DARK"),
    AUTO("AUTO"),
}

enum class UxMode {
    PARKING,
    DRIVING,
}

enum class Gear(val displayName: String) {
    P("P (Park)"),
    R("R (Reverse)"),
    N("N (Neutral)"),
    D("D (Drive)"),
}

enum class ClimateZone {
    DRIVER,
    PASSENGER,
}

enum class DoorPosition(val shortLabel: String, val fullLabel: String) {
    FL("FL", "Front Left"),
    FR("FR", "Front Right"),
    RL("RL", "Rear Left"),
    RR("RR", "Rear Right"),
    TRUNK("TRUNK", "Trunk"),
}

data class ClimatePanelState(
    val targetTemperature: Float,
    val fanLevel: Int,
)

data class MonitorData(
    val mode: UxMode = UxMode.PARKING,
    val speedKmh: Int = 0,
    val rpmX1000: Float = 0f,
    val gear: Gear = Gear.P,
    val speedLimitKmh: Int = 90,
    val drowsiness: Int = 0,
    val fuelPercent: Int = 0,
    val fuelRangeKm: Int = 0,
    val driverClimate: ClimatePanelState = ClimatePanelState(targetTemperature = 22.0f, fanLevel = 0),
    val passengerClimate: ClimatePanelState = ClimatePanelState(targetTemperature = 20.0f, fanLevel = 0),
    val doors: Map<DoorPosition, Boolean> = mapOf(
        DoorPosition.FL to false,
        DoorPosition.FR to false,
        DoorPosition.RL to false,
        DoorPosition.RR to false,
        DoorPosition.TRUNK to false,
    ),
    val tirePressureBar: Map<DoorPosition, Float> = mapOf(
        DoorPosition.FL to 2.3f,
        DoorPosition.FR to 2.3f,
        DoorPosition.RL to 2.3f,
        DoorPosition.RR to 2.3f,
    ),
    val oilTempC: Int = 0,
    val coolantTempC: Int = 0,
    val odometerKm: Int = 0,
    val tripAKm: Int = 236,
    val tripBKm: Int = 58,
    val averageConsumptionL100: Float = 7.2f,
    val tripConsumptionL: Float = 18.6f,
    val sinceRefuelKm: Int = 347,
    val averageSpeedKmh: Int = 63,
    val maxSpeedKmh: Int = 148,
    val engineHours: Int = 1264,
)
