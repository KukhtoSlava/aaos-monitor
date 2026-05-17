package com.autobox.monitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autobox.monitor.model.ClimateZone
import com.autobox.monitor.model.MonitorData
import com.autobox.monitor.model.MonitorTab
import com.autobox.monitor.model.ThemeMode
import com.autobox.monitor.repository.MonitorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MonitorViewModel(
    private val repository: MonitorRepository,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(MonitorTab.CAR)
    private val themeMode = MutableStateFlow(ThemeMode.DARK)

    val uiState: StateFlow<MonitorUiState> = combine(
        repository.data,
        selectedTab,
        themeMode,
    ) { data, tab, theme ->
        MonitorUiState(data = data, selectedTab = tab, themeMode = theme)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MonitorUiState(MonitorData(), MonitorTab.CAR, ThemeMode.DARK),
    )

    init {
        repository.connect()
    }

    fun onTabSelected(tab: MonitorTab) {
        selectedTab.value = tab
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    fun adjustTemperature(zone: ClimateZone, delta: Float) {
        repository.adjustTemperature(zone, delta)
    }

    fun setFanLevel(zone: ClimateZone, level: Int) {
        repository.setFanLevel(zone, level)
    }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }
}

data class MonitorUiState(
    val data: MonitorData,
    val selectedTab: MonitorTab,
    val themeMode: ThemeMode,
)
