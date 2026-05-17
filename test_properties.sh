#!/usr/bin/env bash
# test_properties.sh - VHAL walkthrough for the Monitor system app.
#
# Usage:
#   chmod +x test_properties.sh
#   ./test_properties.sh            # full UI walkthrough
#   ./test_properties.sh check      # only VHAL configs used by this UI
#   ./test_properties.sh drive      # speed, gear, driving mode
#   ./test_properties.sh hvac       # temperature and fan
#   ./test_properties.sh doors      # open/close each door
#   ./test_properties.sh tyres      # tyre pressure
#   ./test_properties.sh engine     # oil and coolant
#   ./test_properties.sh fuel       # fuel level and range
#   ./test_properties.sh odometer   # odometer

set -euo pipefail

CAR="adb shell cmd car_service"

inject()        { $CAR inject-vhal-event "$@"; }
get_prop()      { $CAR get-property-value "$@"; }
inject_silent() { inject "$@" >/dev/null 2>&1 || true; }

readback() {
    local prop="$1"
    if [ "$#" -eq 3 ]; then
        echo "   readback: ${prop} area ${2}"
        get_prop "$prop" "$2"
    else
        echo "   readback: ${prop}"
        get_prop "$prop"
    fi
}

inject_and_read() {
    echo "   inject: $*"
    if ! inject "$@"; then
        echo "   inject failed"
        return 0
    fi
    if ! readback "$@"; then
        echo "   readback failed"
    fi
}

check_prop() {
    local prop="$1"
    echo ""
    echo "-- ${prop} --"
    if $CAR get-carpropertyconfig "$prop"; then
        readback "$prop" || echo "   current value is not available"
    else
        echo "   not configured in this VHAL"
    fi
}

# For properties that require an area ID (per-seat, per-wheel, etc.)
check_prop_area() {
    local prop="$1"
    shift
    local areas=("$@")
    echo ""
    echo "-- ${prop} (areas: ${areas[*]}) --"
    if $CAR get-carpropertyconfig "$prop"; then
        for area in "${areas[@]}"; do
            echo "   area ${area}:"
            get_prop "$prop" "$area" || echo "   area ${area}: not available"
        done
    else
        echo "   not configured in this VHAL"
    fi
}

STEP=0
step() {
    STEP=$((STEP + 1))
    echo ""
    echo "-- Step $STEP: $* --"
}

pause() {
    local secs=${1:-3}
    echo "   waiting ${secs}s..."
    sleep "$secs"
}

header() {
    echo ""
    echo "========================================"
    echo "  $*"
    echo "========================================"
}

section_check() {
    header "CHECK - only VHAL properties used by Monitor UI"

    check_prop PERF_VEHICLE_SPEED
    check_prop ENGINE_RPM
    check_prop GEAR_SELECTION
    check_prop_area HVAC_TEMPERATURE_SET 0x1 0x4            # driver, passenger
    check_prop_area HVAC_FAN_SPEED 0x1 0x4                  # driver, passenger
    check_prop_area DOOR_POS 0x1 0x4 0x10 0x40 0x20000000  # FL FR RL RR TRUNK
    check_prop_area TIRE_PRESSURE 0x1 0x2 0x4 0x8           # FL FR RL RR
    check_prop ENGINE_OIL_TEMP
    check_prop ENGINE_COOLANT_TEMP
    check_prop PERF_ODOMETER
    check_prop FUEL_LEVEL
    check_prop INFO_FUEL_CAPACITY
}

section_drive() {
    header "DRIVE - speed, gear, driving mode"

    step "Park, stopped"
    inject_and_read GEAR_SELECTION 4
    inject_and_read PERF_VEHICLE_SPEED 0.0
    pause 3

    step "Neutral + idle RPM"
    inject_and_read GEAR_SELECTION 1
    inject_and_read ENGINE_RPM 800
    pause 3

    step "Drive gear -- triggers DRIVING mode, INFO tab hidden"
    inject_and_read GEAR_SELECTION 8
    pause 3

    step "Speed 30 km/h (8.33 m/s)"
    inject_and_read PERF_VEHICLE_SPEED 8.33
    inject_and_read ENGINE_RPM 1800
    pause 3

    step "Speed 90 km/h (25 m/s)"
    inject_and_read PERF_VEHICLE_SPEED 25.0
    inject_and_read ENGINE_RPM 2800
    pause 3

    step "Speed 120 km/h (33.3 m/s)"
    inject_and_read PERF_VEHICLE_SPEED 33.3
    inject_and_read ENGINE_RPM 3400
    pause 3

    step "Braking -- speed back to 0"
    inject_and_read PERF_VEHICLE_SPEED 0.0
    inject_and_read ENGINE_RPM 800
    pause 3

    step "Park -- PARKING mode restored, tabs visible"
    inject_and_read GEAR_SELECTION 4
    inject_and_read ENGINE_RPM 0.0
    pause 3

    step "Reverse gear"
    inject_and_read GEAR_SELECTION 2
    pause 3

    step "Back to Park"
    inject_and_read GEAR_SELECTION 4
    pause 2
}

section_hvac() {
    header "HVAC - temperature and fan"

    step "Driver temp: 16 C (minimum)"
    inject_and_read HVAC_TEMPERATURE_SET 0x1 16.0
    pause 3

    step "Driver temp: 22 C (normal)"
    inject_and_read HVAC_TEMPERATURE_SET 0x1 22.0
    pause 3

    step "Driver temp: 30 C (maximum)"
    inject_and_read HVAC_TEMPERATURE_SET 0x1 30.0
    pause 3

    step "Driver temp: 22 C (reset)"
    inject_and_read HVAC_TEMPERATURE_SET 0x1 22.0
    pause 2

    step "Passenger temp: 20 C (normal)"
    inject_and_read HVAC_TEMPERATURE_SET 0x4 20.0
    pause 3

    step "Passenger temp: 28 C (warm)"
    inject_and_read HVAC_TEMPERATURE_SET 0x4 28.0
    pause 3

    step "Passenger temp: 20 C (reset)"
    inject_and_read HVAC_TEMPERATURE_SET 0x4 20.0
    pause 2

    step "Driver fan: max (7) -- all segments filled"
    inject_and_read HVAC_FAN_SPEED 0x1 7
    pause 3

    step "Driver fan: off (0)"
    inject_and_read HVAC_FAN_SPEED 0x1 0
    pause 3

    step "Driver fan: level 3 (reset)"
    inject_and_read HVAC_FAN_SPEED 0x1 3
    pause 2

    step "Passenger fan: max (7)"
    inject_and_read HVAC_FAN_SPEED 0x4 7
    pause 3

    step "Passenger fan: level 2 (reset)"
    inject_and_read HVAC_FAN_SPEED 0x4 2
    pause 2
}

section_doors() {
    header "DOORS - open/close each door"

    step "All doors closed (baseline)"
    for area in 0x1 0x4 0x10 0x40 0x20000000; do
        inject_silent DOOR_POS "$area" 0
    done
    pause 3

    step "Front Left open -- red FL in schema"
    inject_and_read DOOR_POS 0x1 100
    pause 3

    step "Front Right open -- red FR"
    inject_and_read DOOR_POS 0x4 100
    pause 3

    step "Rear Left open -- red RL"
    inject_and_read DOOR_POS 0x10 100
    pause 3

    step "Rear Right open -- red RR"
    inject_and_read DOOR_POS 0x40 100
    pause 3

    step "All passenger doors open at once"
    for area in 0x1 0x4 0x10 0x40; do
        inject_silent DOOR_POS "$area" 100
    done
    pause 3

    step "Trunk open -- amber trunk indicator"
    inject_and_read DOOR_POS 0x20000000 100
    pause 3

    step "Close all doors"
    for area in 0x1 0x4 0x10 0x40 0x20000000; do
        inject_silent DOOR_POS "$area" 0
    done
    pause 2
}

section_tyres() {
    header "TYRE PRESSURE - kPa values (div 100 = bar)"

    step "All normal (230 kPa = 2.3 bar) -- all green"
    inject_silent TIRE_PRESSURE 0x1 230.0
    inject_silent TIRE_PRESSURE 0x2 230.0
    inject_silent TIRE_PRESSURE 0x4 230.0
    inject_silent TIRE_PRESSURE 0x8 230.0
    pause 3

    step "FR low (210 kPa = 2.1 bar) -- amber"
    inject_and_read TIRE_PRESSURE 0x2 210.0
    pause 3

    step "FL critical (160 kPa = 1.6 bar) -- red"
    inject_and_read TIRE_PRESSURE 0x1 160.0
    pause 3

    step "RL low (195 kPa = 1.95 bar) -- amber"
    inject_and_read TIRE_PRESSURE 0x4 195.0
    pause 3

    step "All back to normal"
    inject_silent TIRE_PRESSURE 0x1 230.0
    inject_silent TIRE_PRESSURE 0x2 230.0
    inject_silent TIRE_PRESSURE 0x4 230.0
    inject_silent TIRE_PRESSURE 0x8 230.0
    pause 2
}

section_engine() {
    header "ENGINE - oil temp and coolant"

    step "Cold engine (oil 30 C, coolant 25 C) -- short bars"
    inject_and_read ENGINE_OIL_TEMP 30
    inject_and_read ENGINE_COOLANT_TEMP 25
    pause 3

    step "Warming up (oil 70 C, coolant 75 C)"
    inject_and_read ENGINE_OIL_TEMP 70
    inject_and_read ENGINE_COOLANT_TEMP 75
    pause 3

    step "Normal operating (oil 88 C, coolant 92 C)"
    inject_and_read ENGINE_OIL_TEMP 88
    inject_and_read ENGINE_COOLANT_TEMP 92
    pause 3

    step "Overheating (oil 128 C, coolant 118 C) -- bars near max"
    inject_and_read ENGINE_OIL_TEMP 128
    inject_and_read ENGINE_COOLANT_TEMP 118
    pause 3

    step "Reset to normal"
    inject_and_read ENGINE_OIL_TEMP 88
    inject_and_read ENGINE_COOLANT_TEMP 92
    pause 2
}

section_fuel() {
    header "FUEL - level and range"
    echo "Assumes emulator INFO_FUEL_CAPACITY is 15000 mL."

    step "Full tank (100% = 15000 mL) -- green fuel row"
    inject_and_read FUEL_LEVEL 15000
    pause 3

    step "72% (10800 mL)"
    inject_and_read FUEL_LEVEL 10800
    pause 3

    step "Low -- 25% (3750 mL) -- amber fuel row"
    inject_and_read FUEL_LEVEL 3750
    pause 3

    step "Critical -- 10% (1500 mL) -- red fuel row"
    inject_and_read FUEL_LEVEL 1500
    pause 3

    step "Reset to 72%"
    inject_and_read FUEL_LEVEL 10800
    pause 2
}

section_odometer() {
    header "ODOMETER"

    step "85 432 km"
    inject_and_read PERF_ODOMETER 85432
    pause 3

    step "100 000 km"
    inject_and_read PERF_ODOMETER 100000
    pause 3

    step "Reset to 85 432 km"
    inject_and_read PERF_ODOMETER 85432
    pause 2
}

SECTION="${1:-all}"

case "$SECTION" in
    check)    section_check    ;;
    drive)    section_drive    ;;
    hvac)     section_hvac     ;;
    doors)    section_doors    ;;
    tyres)    section_tyres    ;;
    engine)   section_engine   ;;
    fuel)     section_fuel     ;;
    odometer) section_odometer ;;
    all)
        section_drive
        section_hvac
        section_doors
        section_tyres
        section_engine
        section_fuel
        section_odometer
        ;;
    *)
        echo "Usage: $0 [all|check|drive|hvac|doors|tyres|engine|fuel|odometer]"
        exit 1
        ;;
esac

echo ""
echo "Done - $SECTION walkthrough complete."
