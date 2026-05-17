package com.autobox.monitor.ui

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobox.monitor.R
import com.autobox.monitor.model.ClimatePanelState
import com.autobox.monitor.model.ClimateZone
import com.autobox.monitor.model.DoorPosition
import com.autobox.monitor.model.Gear
import com.autobox.monitor.model.MonitorData
import com.autobox.monitor.model.MonitorTab
import com.autobox.monitor.model.ThemeMode
import com.autobox.monitor.model.UxMode
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun MonitorDashboardApp(viewModel: MonitorViewModel) {
    val state by viewModel.uiState.collectAsState()
    val cs = MaterialTheme.colorScheme
    val isDriving = state.data.mode == UxMode.DRIVING

    Surface(modifier = Modifier.fillMaxSize(), color = cs.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            HeaderBar(data = state.data)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val displayTab = if (isDriving) MonitorTab.CAR else state.selectedTab
                when (displayTab) {
                    MonitorTab.CAR -> CarTabContent(data = state.data, viewModel = viewModel)
                    MonitorTab.INFO -> InfoTabContent(
                        data = state.data,
                        themeMode = state.themeMode,
                        onThemeMode = viewModel::setThemeMode,
                    )
                }
            }
            if (!isDriving) {
                BottomTabs(
                    selectedTab = state.selectedTab,
                    onTabSelected = viewModel::onTabSelected,
                )
            }
        }
    }
}

@Composable
private fun HeaderBar(data: MonitorData) {
    val cs = MaterialTheme.colorScheme
    val isDriving = data.mode == UxMode.DRIVING
    val modeColor = if (isDriving) MaterialTheme.warningColor else cs.tertiary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.header_height))
            .background(cs.background)
            .padding(horizontal = dimensionResource(R.dimen.padding_xxl)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.indicator_dot_size))
                    .background(color = cs.tertiary, shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_m)))
            Text(
                stringResource(R.string.header_title),
                color = cs.onSurfaceVariant,
                fontSize = dimensionResource(R.dimen.text_header).value.sp,
                letterSpacing = 2.sp,
            )
        }
        Text(
            text = if (isDriving) stringResource(R.string.mode_driving) else stringResource(R.string.mode_parking),
            color = modeColor,
            fontSize = dimensionResource(R.dimen.text_mode_badge).value.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier
                .background(modeColor.copy(alpha = 0.12f), RoundedCornerShape(dimensionResource(R.dimen.radius_xs)))
                .border(dimensionResource(R.dimen.border_stroke), modeColor.copy(alpha = 0.4f), RoundedCornerShape(dimensionResource(R.dimen.radius_xs)))
                .padding(horizontal = dimensionResource(R.dimen.padding_l), vertical = dimensionResource(R.dimen.padding_xs)),
        )
    }
}

@Composable
private fun BottomTabs(selectedTab: MonitorTab, onTabSelected: (MonitorTab) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.bottom_tab_height))
            .background(cs.background)
            .border(dimensionResource(R.dimen.border_stroke), cs.outlineVariant),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_m)),
    ) {
        val tabs = listOf(MonitorTab.CAR, MonitorTab.INFO)
        tabs.forEach { tab ->
            val selected = selectedTab == tab
            val tabRadius = RoundedCornerShape(dimensionResource(R.dimen.radius_l))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = dimensionResource(R.dimen.padding_s), vertical = dimensionResource(R.dimen.padding_7))
                    .background(
                        color = if (selected) cs.primary.copy(alpha = 0.2f) else cs.surface,
                        shape = tabRadius,
                    )
                    .border(
                        dimensionResource(R.dimen.border_stroke),
                        if (selected) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.45f),
                        tabRadius,
                    )
                    .clickable { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = tab.name,
                    color = if (selected) cs.primary else cs.onSurfaceVariant,
                    fontSize = dimensionResource(R.dimen.text_tab).value.sp,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CarTabContent(data: MonitorData, viewModel: MonitorViewModel) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
    ) {
        ClimateColumn(
            modifier = Modifier.width(dimensionResource(R.dimen.climate_column_width)),
            title = stringResource(R.string.climate_driver),
            state = data.driverClimate,
            onAdjust = { viewModel.adjustTemperature(ClimateZone.DRIVER, it) },
            onFan = { viewModel.setFanLevel(ClimateZone.DRIVER, it) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(dimensionResource(R.dimen.padding_xl)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_l)),
        ) {
            DriveStatsCard(data = data)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_l)),
            ) {
                SectionColumn(modifier = Modifier.weight(1f), title = stringResource(R.string.doors_section_title)) {
                    DoorsPanel(data = data)
                }
                SectionColumn(modifier = Modifier.weight(1f), title = stringResource(R.string.tyres_title)) {
                    TyresCard(data)
                }
                SectionColumn(modifier = Modifier.weight(1f), title = stringResource(R.string.engine_title)) {
                    EngineCard(data)
                }
            }
        }

        ClimateColumn(
            modifier = Modifier.width(dimensionResource(R.dimen.climate_column_width)),
            title = stringResource(R.string.climate_passenger),
            state = data.passengerClimate,
            onAdjust = { viewModel.adjustTemperature(ClimateZone.PASSENGER, it) },
            onFan = { viewModel.setFanLevel(ClimateZone.PASSENGER, it) },
        )
    }
}

@Composable
private fun ClimateColumn(
    modifier: Modifier,
    title: String,
    state: ClimatePanelState,
    onAdjust: (Float) -> Unit,
    onFan: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val segSize = dimensionResource(R.dimen.fan_segment_size)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .border(dimensionResource(R.dimen.border_stroke), cs.outlineVariant)
            .padding(vertical = dimensionResource(R.dimen.padding_xxl), horizontal = dimensionResource(R.dimen.padding_l)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxl)),
    ) {
        Text(title, color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_s).value.sp, letterSpacing = 2.sp)
        Text(String.format(stringResource(R.string.climate_temp_format), state.targetTemperature), color = cs.onSurface, fontSize = dimensionResource(R.dimen.text_temp_large).value.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_l))) {
            CircleButton(text = stringResource(R.string.btn_temp_down)) { onAdjust(-0.5f) }
            CircleButton(text = stringResource(R.string.btn_temp_up)) { onAdjust(0.5f) }
        }
        Text(stringResource(R.string.climate_fan), color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_xs).value.sp, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs))) {
            for (index in 1..7) {
                Box(
                    modifier = Modifier
                        .size(width = segSize, height = segSize)
                        .background(
                            color = if (index <= state.fanLevel) cs.secondary.copy(alpha = 0.45f) else cs.surfaceVariant,
                            shape = RoundedCornerShape(dimensionResource(R.dimen.radius_xxs)),
                        )
                        .clickable { onFan(index) },
                )
            }
        }
    }
}

@Composable
private fun CircleButton(text: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(dimensionResource(R.dimen.circle_button_size))
            .background(cs.surfaceVariant, CircleShape)
            .border(dimensionResource(R.dimen.border_stroke), cs.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = cs.onSurface.copy(alpha = 0.8f), fontSize = dimensionResource(R.dimen.text_btn).value.sp)
    }
}

@Composable
private fun DoorsPanel(data: MonitorData) {
    val cs = MaterialTheme.colorScheme
    val doorRowRadius = RoundedCornerShape(dimensionResource(R.dimen.radius_l))
    val badgeRadius = RoundedCornerShape(dimensionResource(R.dimen.radius_xs))
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_l)), verticalAlignment = Alignment.CenterVertically) {
        CarSchema(data = data)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_s)),
        ) {
            DoorPosition.entries.forEach { door ->
                val isOpen = data.doors[door] == true
                val statusColor = if (isOpen) cs.error else cs.tertiary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cs.surface, doorRowRadius)
                        .border(dimensionResource(R.dimen.border_stroke), cs.outlineVariant, doorRowRadius)
                        .padding(horizontal = dimensionResource(R.dimen.padding_l), vertical = dimensionResource(R.dimen.padding_m)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(door.fullLabel, color = cs.onSurface, fontSize = dimensionResource(R.dimen.text_door_name).value.sp)
                    Text(
                        text = if (isOpen) stringResource(R.string.door_open) else stringResource(R.string.door_closed),
                        color = statusColor,
                        fontSize = dimensionResource(R.dimen.text_badge).value.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.door_badge_width))
                            .background(statusColor.copy(alpha = 0.12f), badgeRadius)
                            .padding(horizontal = dimensionResource(R.dimen.padding_m), vertical = dimensionResource(R.dimen.padding_xxs)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CarSchema(data: MonitorData) {
    val cs = MaterialTheme.colorScheme
    val trunkOpen = data.doors[DoorPosition.TRUNK] == true
    val trunkColor = if (trunkOpen) MaterialTheme.warningColor else cs.tertiary
    val trunkRadius = RoundedCornerShape(dimensionResource(R.dimen.radius_s))
    Box(modifier = Modifier.size(width = dimensionResource(R.dimen.schema_width), height = dimensionResource(R.dimen.schema_height))) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = dimensionResource(R.dimen.schema_body_width), height = dimensionResource(R.dimen.schema_body_height))
                .background(cs.surfaceVariant, RoundedCornerShape(dimensionResource(R.dimen.radius_car_body))),
        )
        val frontOffset = dimensionResource(R.dimen.schema_door_front_offset)
        val rearOffset = dimensionResource(R.dimen.schema_door_rear_offset)
        DoorSchemaRect(modifier = Modifier.align(Alignment.TopStart).padding(top = frontOffset), isOpen = data.doors[DoorPosition.FL] == true)
        DoorSchemaRect(modifier = Modifier.align(Alignment.TopEnd).padding(top = frontOffset), isOpen = data.doors[DoorPosition.FR] == true)
        DoorSchemaRect(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = rearOffset), isOpen = data.doors[DoorPosition.RL] == true)
        DoorSchemaRect(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = rearOffset), isOpen = data.doors[DoorPosition.RR] == true)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = dimensionResource(R.dimen.trunk_width), height = dimensionResource(R.dimen.trunk_height))
                .background(trunkColor.copy(alpha = 0.2f), trunkRadius)
                .border(dimensionResource(R.dimen.border_stroke), trunkColor, trunkRadius),
        )
    }
}

@Composable
private fun DoorSchemaRect(modifier: Modifier, isOpen: Boolean) {
    val cs = MaterialTheme.colorScheme
    val color = if (isOpen) cs.error else cs.tertiary
    val r = RoundedCornerShape(dimensionResource(R.dimen.radius_s))
    Box(
        modifier = modifier
            .size(width = dimensionResource(R.dimen.door_rect_width), height = dimensionResource(R.dimen.door_rect_height))
            .background(color.copy(alpha = 0.14f), r)
            .border(dimensionResource(R.dimen.border_stroke), color, r),
    )
}

@Composable
private fun TyresCard(data: MonitorData) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_m))) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_s))) {
            TyreCell(stringResource(R.string.tyre_fl), data.tirePressureBar[DoorPosition.FL] ?: 0f, Modifier.weight(1f))
            TyreCell(stringResource(R.string.tyre_fr), data.tirePressureBar[DoorPosition.FR] ?: 0f, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_s))) {
            TyreCell(stringResource(R.string.tyre_rl), data.tirePressureBar[DoorPosition.RL] ?: 0f, Modifier.weight(1f))
            TyreCell(stringResource(R.string.tyre_rr), data.tirePressureBar[DoorPosition.RR] ?: 0f, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TyreCell(label: String, value: Float, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val color = when {
        value < 1.9f -> cs.error
        value < 2.2f -> MaterialTheme.warningColor
        else -> cs.tertiary
    }
    Row(
        modifier = modifier
            .background(cs.surfaceVariant, RoundedCornerShape(dimensionResource(R.dimen.radius_m)))
            .padding(dimensionResource(R.dimen.padding_m)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_m)),
    ) {
        Text(label, color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_xs).value.sp)
        Text(String.format(stringResource(R.string.tyre_value_format), value), color = color, fontSize = dimensionResource(R.dimen.text_tyre_value).value.sp, fontWeight = FontWeight.Medium)
        Text(stringResource(R.string.tyre_unit), color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_xs).value.sp)
    }
}

@Composable
private fun EngineCard(data: MonitorData) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_l))) {
        TempBar(label = stringResource(R.string.engine_oil_temp), value = data.oilTempC, color = MaterialTheme.warningColor)
        TempBar(label = stringResource(R.string.engine_coolant), value = data.coolantTempC, color = cs.tertiary)
    }
}

@Composable
private fun TempBar(label: String, value: Int, color: Color) {
    val cs = MaterialTheme.colorScheme
    val barRadius = RoundedCornerShape(dimensionResource(R.dimen.radius_s))
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs))) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_s).value.sp)
            Text(String.format(stringResource(R.string.engine_temp_format), value), color = cs.onSurface.copy(alpha = 0.8f), fontSize = dimensionResource(R.dimen.text_stat_value).value.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.temp_bar_height))
                .background(cs.surfaceVariant, barRadius),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / 130f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color, barRadius),
            )
        }
    }
}

@Composable
private fun DriveStatsCard(data: MonitorData) {
    val cs = MaterialTheme.colorScheme
    val gearColor = when (data.gear) {
        Gear.P -> cs.tertiary
        Gear.R -> MaterialTheme.warningColor
        Gear.N -> cs.onSurface.copy(alpha = 0.8f)
        Gear.D -> cs.secondary
    }
    val fuelColor = when {
        data.fuelPercent < 15 -> cs.error
        data.fuelPercent < 30 -> MaterialTheme.warningColor
        else -> cs.tertiary
    }
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.drive_title), color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_xs).value.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(dimensionResource(R.dimen.spacing_s)))
        DriveStatRow(stringResource(R.string.drive_speed_label), String.format(stringResource(R.string.drive_speed_format), data.speedKmh), cs.onSurface.copy(alpha = 0.8f))
        DriveStatRow(stringResource(R.string.drive_gear_label), data.gear.displayName, gearColor)
        DriveStatRow(stringResource(R.string.drive_rpm_label), String.format(stringResource(R.string.drive_rpm_format), data.rpmX1000), cs.onSurface.copy(alpha = 0.8f))
        DriveStatRow(stringResource(R.string.drive_fuel_label), String.format(stringResource(R.string.drive_fuel_format), data.fuelPercent, data.fuelRangeKm), fuelColor)
    }
}

@Composable
private fun DriveStatRow(label: String, value: String, valueColor: Color) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.padding_5)),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_s).value.sp)
        Text(value, color = valueColor, fontSize = dimensionResource(R.dimen.text_drive_value).value.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ThemeSwitcher(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val chipRadius = RoundedCornerShape(dimensionResource(R.dimen.radius_l))
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_s))) {
        Text(
            text = "THEME",
            color = cs.onSurfaceVariant,
            fontSize = dimensionResource(R.dimen.text_label_xs).value.sp,
            letterSpacing = 2.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_m))) {
            ThemeMode.entries.forEach { mode ->
                val selected = mode == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selected) cs.primary else cs.surface,
                            shape = chipRadius,
                        )
                        .border(
                            width = dimensionResource(R.dimen.border_stroke),
                            color = if (selected) cs.primary else cs.outlineVariant,
                            shape = chipRadius,
                        )
                        .clickable { onSelect(mode) }
                        .padding(vertical = dimensionResource(R.dimen.padding_m)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = mode.label,
                        color = if (selected) cs.onPrimary else cs.onSurfaceVariant,
                        fontSize = dimensionResource(R.dimen.text_tab).value.sp,
                        letterSpacing = 1.2.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTabContent(
    data: MonitorData,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .padding(dimensionResource(R.dimen.padding_xxl))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xl)),
    ) {
        ThemeSwitcher(current = themeMode, onSelect = onThemeMode)
        Text(stringResource(R.string.info_title), color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_xs).value.sp, letterSpacing = 2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_l))) {
            InfoCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.distance_title),
                rows = listOf(
                    stringResource(R.string.distance_odometer) to String.format(stringResource(R.string.distance_km_format), data.odometerKm),
                    stringResource(R.string.distance_trip_a) to String.format(stringResource(R.string.distance_km_format), data.tripAKm),
                    stringResource(R.string.distance_trip_b) to String.format(stringResource(R.string.distance_km_format), data.tripBKm),
                    stringResource(R.string.distance_range) to String.format(stringResource(R.string.distance_km_format), data.fuelRangeKm),
                ),
                valueColor = cs.onSurface.copy(alpha = 0.8f),
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.consumption_title),
                rows = listOf(
                    stringResource(R.string.consumption_average) to String.format(stringResource(R.string.consumption_average_format), data.averageConsumptionL100),
                    stringResource(R.string.consumption_trip_fuel) to String.format(stringResource(R.string.consumption_trip_fuel_format), data.tripConsumptionL),
                    stringResource(R.string.consumption_since_refuel) to String.format(stringResource(R.string.distance_km_format), data.sinceRefuelKm),
                    stringResource(R.string.consumption_fuel) to String.format(stringResource(R.string.consumption_fuel_format), data.fuelPercent),
                ),
                valueColor = MaterialTheme.warningColor,
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.performance_title),
                rows = listOf(
                    stringResource(R.string.performance_avg_speed) to String.format(stringResource(R.string.performance_speed_format), data.averageSpeedKmh),
                    stringResource(R.string.performance_max_speed) to String.format(stringResource(R.string.performance_speed_format), data.maxSpeedKmh),
                    stringResource(R.string.performance_engine_time) to String.format(stringResource(R.string.performance_engine_time_format), data.engineHours),
                    stringResource(R.string.performance_engine_temp) to String.format(stringResource(R.string.performance_engine_temp_format), data.coolantTempC),
                ),
                valueColor = cs.secondary,
            )
        }
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier,
    title: String,
    rows: List<Pair<String, String>>,
    valueColor: Color,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_info_card)),
    ) {
        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_xxl), vertical = dimensionResource(R.dimen.padding_xl))) {
            Text(title, color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_xs).value.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_m)))
            rows.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.padding_m)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(key, color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_s).value.sp)
                    Text(value, color = valueColor, fontSize = dimensionResource(R.dimen.text_row_value).value.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SectionColumn(modifier: Modifier, title: String, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_s)),
    ) {
        Text(title, color = cs.onSurfaceVariant, fontSize = dimensionResource(R.dimen.text_label_xs).value.sp, letterSpacing = 2.sp)
        CardSurface(modifier = Modifier.fillMaxWidth().weight(1f), content = content)
    }
}

@Composable
private fun CardSurface(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_card)),
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.padding_l)), content = content)
    }
}
