package com.autobox.monitor.repository.impl

import android.car.Car
import android.car.VehicleAreaDoor
import android.car.VehicleAreaSeat
import android.car.VehicleAreaWheel
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.Subscription
import android.content.Context
import android.util.Log
import com.autobox.monitor.model.ClimateZone
import com.autobox.monitor.model.DoorPosition
import com.autobox.monitor.model.Gear
import com.autobox.monitor.model.MonitorData
import com.autobox.monitor.model.UxMode
import com.autobox.monitor.repository.MonitorRepository
import com.autobox.monitor.repository.constants.DEFAULT_HVAC_FAN_MAX
import com.autobox.monitor.repository.constants.DEFAULT_HVAC_TEMP_MAX
import com.autobox.monitor.repository.constants.DEFAULT_HVAC_TEMP_MIN
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class MonitorRepositoryImpl @Inject constructor(
    private val context: Context,
) : MonitorRepository {

    private val _data = MutableStateFlow(MonitorData())
    override val data: StateFlow<MonitorData> = _data.asStateFlow()

    private val lock = Any()

    // Guarded by lock
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var uxRestrictionsManager: CarUxRestrictionsManager? = null

    // Read once on connect via INFO_FUEL_CAPACITY to compute fuelPercent from FUEL_LEVEL (mL)
    @Volatile private var fuelCapacityMl = 0f

    // HVAC ranges read from property config on connect; fallback to AAOS typical values
    @Volatile private var hvacTempMin = DEFAULT_HVAC_TEMP_MIN
    @Volatile private var hvacTempMax = DEFAULT_HVAC_TEMP_MAX
    @Volatile private var hvacFanMax = DEFAULT_HVAC_FAN_MAX

    private val lifecycleListener = Car.CarServiceLifecycleListener { car, ready ->
        if (ready) {
            val cpm = car.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            val uxm = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            synchronized(lock) {
                propertyManager = cpm
                uxRestrictionsManager = uxm
            }
            readStaticProperties(cpm)
            registerPropertyCallbacks(cpm)
            uxm?.let { registerUxListener(it) }
            Log.i(TAG, "Car service connected")
        } else {
            Log.w(TAG, "Car service disconnected")
            synchronized(lock) {
                propertyManager = null
                uxRestrictionsManager = null
            }
        }
    }

    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.propertyStatus == CarPropertyValue.STATUS_AVAILABLE) {
                handlePropertyEvent(value)
            }
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w(TAG, "Property error: propId=0x${propId.toString(16)} zone=$zone")
        }
    }

    private val uxRestrictionsListener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
        applyUxRestrictions(restrictions)
    }

    override fun connect() {
        synchronized(lock) {
            if (car != null) return
            car = Car.createCar(
                context,
                /* handler= */ null,
                Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT,
                lifecycleListener,
            )
        }
    }

    override fun disconnect() {
        val carToDisconnect: Car?
        val cpmToUnregister: CarPropertyManager?
        val uxmToUnregister: CarUxRestrictionsManager?
        synchronized(lock) {
            carToDisconnect = car
            cpmToUnregister = propertyManager
            uxmToUnregister = uxRestrictionsManager
            car = null
            propertyManager = null
            uxRestrictionsManager = null
        }
        cpmToUnregister?.let { unregisterPropertyCallbacks(it) }
        uxmToUnregister?.let { unregisterUxListener(it) }
        carToDisconnect?.disconnect()
    }

    private fun readStaticProperties(cpm: CarPropertyManager?) {
        cpm ?: return
        try {
            fuelCapacityMl = cpm.getFloatProperty(VehiclePropertyIds.INFO_FUEL_CAPACITY, /* areaId= */ 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read INFO_FUEL_CAPACITY", e)
        }
        try {
            val tempRange = cpm.getMinMaxSupportedValue<Float>(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                VehicleAreaSeat.SEAT_ROW_1_LEFT,
            )
            hvacTempMin = tempRange.minValue ?: hvacTempMin
            hvacTempMax = tempRange.maxValue ?: hvacTempMax
        } catch (e: Exception) {
            Log.w(TAG, "Could not read HVAC_TEMPERATURE_SET range", e)
        }
        try {
            val fanRange = cpm.getMinMaxSupportedValue<Int>(
                VehiclePropertyIds.HVAC_FAN_SPEED,
                VehicleAreaSeat.SEAT_ROW_1_LEFT,
            )
            hvacFanMax = fanRange.maxValue ?: hvacFanMax
        } catch (e: Exception) {
            Log.w(TAG, "Could not read HVAC_FAN_SPEED range", e)
        }
    }

    private fun registerPropertyCallbacks(cpm: CarPropertyManager?) {
        cpm ?: return
        PROPERTY_SUBSCRIPTIONS.forEach { subscription ->
            try {
                cpm.subscribePropertyEvents(
                    listOf(subscription),
                    /* callbackExecutor= */ null,
                    propertyCallback,
                )
            } catch (e: Exception) {
                Log.e(TAG, "subscribePropertyEvents failed for propId=0x${subscription.propertyId.toString(16)}", e)
            }
        }
    }

    private fun unregisterPropertyCallbacks(cpm: CarPropertyManager) {
        try {
            cpm.unsubscribePropertyEvents(propertyCallback)
        } catch (e: Exception) {
            Log.e(TAG, "unsubscribePropertyEvents failed", e)
        }
    }

    private fun registerUxListener(uxm: CarUxRestrictionsManager) {
        try {
            uxm.currentCarUxRestrictions?.let { applyUxRestrictions(it) }
            uxm.registerListener(uxRestrictionsListener)
        } catch (e: Exception) {
            Log.e(TAG, "registerUxListener failed", e)
        }
    }

    private fun unregisterUxListener(uxm: CarUxRestrictionsManager) {
        try {
            uxm.unregisterListener()
        } catch (e: Exception) {
            Log.e(TAG, "unregisterUxListener failed", e)
        }
    }

    private fun applyUxRestrictions(restrictions: CarUxRestrictions) {
        val mode = if (restrictions.isRequiresDistractionOptimization) UxMode.DRIVING else UxMode.PARKING
        _data.update { it.copy(mode = mode) }
    }

    private fun handlePropertyEvent(value: CarPropertyValue<*>) {
        val v = value.value
        when (value.propertyId) {
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                // m/s → km/h; abs() because negative = reversing
                val speedKmh = abs((v as? Float ?: return) * 3.6f).toInt()
                _data.update { it.copy(speedKmh = speedKmh) }
            }
            VehiclePropertyIds.ENGINE_RPM -> {
                val rpmX1000 = ((v as? Float ?: return) / 1000f).coerceAtLeast(0f)
                _data.update { it.copy(rpmX1000 = rpmX1000) }
            }
            VehiclePropertyIds.GEAR_SELECTION -> {
                val gear = vehicleGearToGear(v as? Int ?: return)
                _data.update { it.copy(gear = gear) }
            }
            VehiclePropertyIds.HVAC_TEMPERATURE_SET -> {
                val temp = v as? Float ?: return
                when (value.areaId) {
                    VehicleAreaSeat.SEAT_ROW_1_LEFT ->
                        _data.update { it.copy(driverClimate = it.driverClimate.copy(targetTemperature = temp)) }
                    VehicleAreaSeat.SEAT_ROW_1_RIGHT ->
                        _data.update { it.copy(passengerClimate = it.passengerClimate.copy(targetTemperature = temp)) }
                }
            }
            VehiclePropertyIds.HVAC_FAN_SPEED -> {
                val level = v as? Int ?: return
                when (value.areaId) {
                    VehicleAreaSeat.SEAT_ROW_1_LEFT ->
                        _data.update { it.copy(driverClimate = it.driverClimate.copy(fanLevel = level)) }
                    VehicleAreaSeat.SEAT_ROW_1_RIGHT ->
                        _data.update { it.copy(passengerClimate = it.passengerClimate.copy(fanLevel = level)) }
                }
            }
            VehiclePropertyIds.DOOR_POS -> {
                // Int: 0 = fully closed, any positive value = open/ajar
                val isOpen = (v as? Int ?: return) > 0
                val doorPos = AREA_TO_DOOR[value.areaId] ?: return
                _data.update { it.copy(doors = it.doors.toMutableMap().apply { put(doorPos, isOpen) }) }
            }
            VehiclePropertyIds.TIRE_PRESSURE -> {
                // kPa → bar (1 bar = 100 kPa)
                val bar = (v as? Float ?: return) / 100f
                val doorPos = WHEEL_TO_DOOR[value.areaId] ?: return
                _data.update { it.copy(tirePressureBar = it.tirePressureBar.toMutableMap().apply { put(doorPos, bar) }) }
            }
            VehiclePropertyIds.ENGINE_OIL_TEMP -> {
                _data.update { it.copy(oilTempC = (v as? Float ?: return).toInt()) }
            }
            VehiclePropertyIds.ENGINE_COOLANT_TEMP -> {
                _data.update { it.copy(coolantTempC = (v as? Float ?: return).toInt()) }
            }
            VehiclePropertyIds.PERF_ODOMETER -> {
                _data.update { it.copy(odometerKm = (v as? Float ?: return).toInt()) }
            }
            VehiclePropertyIds.FUEL_LEVEL -> {
                val ml = v as? Float ?: return
                // Retry capacity read if it failed at connect time
                if (fuelCapacityMl == 0f) {
                    val cpm = synchronized(lock) { propertyManager }
                    if (cpm != null) {
                        try {
                            fuelCapacityMl = cpm.getFloatProperty(VehiclePropertyIds.INFO_FUEL_CAPACITY, 0)
                        } catch (e: Exception) {
                            Log.w(TAG, "INFO_FUEL_CAPACITY retry failed", e)
                        }
                    }
                }
                val capacityMl = fuelCapacityMl
                if (capacityMl > 0f) {
                    val pct = ((ml / capacityMl) * 100f).toInt().coerceIn(0, 100)
                    _data.update { it.copy(fuelPercent = pct, fuelRangeKm = (pct * 6.5f).toInt()) }
                }
            }
        }
    }

    private fun vehicleGearToGear(vehicleGear: Int): Gear = when (vehicleGear) {
        android.car.VehicleGear.GEAR_PARK -> Gear.P
        android.car.VehicleGear.GEAR_REVERSE -> Gear.R
        android.car.VehicleGear.GEAR_NEUTRAL -> Gear.N
        else -> Gear.D
    }

    override fun adjustTemperature(zone: ClimateZone, delta: Float) {
        val cpm = synchronized(lock) { propertyManager } ?: return
        val areaId = hvacAreaId(zone)
        val current = _data.value.run {
            if (zone == ClimateZone.DRIVER) driverClimate.targetTemperature else passengerClimate.targetTemperature
        }
        try {
            cpm.setFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                areaId,
                (current + delta).coerceIn(hvacTempMin, hvacTempMax),
            )
        } catch (e: Exception) {
            Log.e(TAG, "setFloatProperty HVAC_TEMPERATURE_SET failed", e)
        }
    }

    override fun setFanLevel(zone: ClimateZone, level: Int) {
        val cpm = synchronized(lock) { propertyManager } ?: return
        try {
            cpm.setIntProperty(VehiclePropertyIds.HVAC_FAN_SPEED, hvacAreaId(zone), level.coerceIn(0, hvacFanMax))
        } catch (e: Exception) {
            Log.e(TAG, "setIntProperty HVAC_FAN_SPEED failed", e)
        }
    }

    private fun hvacAreaId(zone: ClimateZone) =
        if (zone == ClimateZone.DRIVER) VehicleAreaSeat.SEAT_ROW_1_LEFT else VehicleAreaSeat.SEAT_ROW_1_RIGHT

    companion object {
        private const val TAG = "MonitorRepository"

        private val PROPERTY_SUBSCRIPTIONS: List<Subscription> = listOf(
            Subscription.Builder(VehiclePropertyIds.PERF_VEHICLE_SPEED).setUpdateRateUi().build(),
            Subscription.Builder(VehiclePropertyIds.ENGINE_RPM).setUpdateRateUi().build(),
            Subscription.Builder(VehiclePropertyIds.GEAR_SELECTION).build(),
            Subscription.Builder(VehiclePropertyIds.HVAC_TEMPERATURE_SET).build(),
            Subscription.Builder(VehiclePropertyIds.HVAC_FAN_SPEED).build(),
            Subscription.Builder(VehiclePropertyIds.DOOR_POS).build(),
            Subscription.Builder(VehiclePropertyIds.TIRE_PRESSURE).setUpdateRateNormal().build(),
            Subscription.Builder(VehiclePropertyIds.ENGINE_OIL_TEMP).setUpdateRateNormal().build(),
            Subscription.Builder(VehiclePropertyIds.ENGINE_COOLANT_TEMP).setUpdateRateNormal().build(),
            Subscription.Builder(VehiclePropertyIds.PERF_ODOMETER).setUpdateRateNormal().build(),
            Subscription.Builder(VehiclePropertyIds.FUEL_LEVEL).setUpdateRateNormal().build(),
        )

        private val AREA_TO_DOOR = mapOf(
            VehicleAreaDoor.DOOR_ROW_1_LEFT to DoorPosition.FL,
            VehicleAreaDoor.DOOR_ROW_1_RIGHT to DoorPosition.FR,
            VehicleAreaDoor.DOOR_ROW_2_LEFT to DoorPosition.RL,
            VehicleAreaDoor.DOOR_ROW_2_RIGHT to DoorPosition.RR,
            VehicleAreaDoor.DOOR_REAR to DoorPosition.TRUNK,
        )

        private val WHEEL_TO_DOOR = mapOf(
            VehicleAreaWheel.WHEEL_LEFT_FRONT to DoorPosition.FL,
            VehicleAreaWheel.WHEEL_RIGHT_FRONT to DoorPosition.FR,
            VehicleAreaWheel.WHEEL_LEFT_REAR to DoorPosition.RL,
            VehicleAreaWheel.WHEEL_RIGHT_REAR to DoorPosition.RR,
        )
    }
}
