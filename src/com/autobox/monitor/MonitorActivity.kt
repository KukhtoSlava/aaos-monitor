package com.autobox.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.autobox.monitor.di.MonitorViewModelFactory
import com.autobox.monitor.ui.MonitorDashboardApp
import com.autobox.monitor.ui.MonitorTheme
import com.autobox.monitor.ui.MonitorViewModel
import javax.inject.Inject

class MonitorActivity : ComponentActivity() {

    @Inject
    lateinit var viewModelFactory: MonitorViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as MonitorApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this, viewModelFactory)[MonitorViewModel::class.java]
        setContent {
            val state by viewModel.uiState.collectAsState()
            MonitorTheme(themeMode = state.themeMode) {
                MonitorDashboardApp(viewModel = viewModel)
            }
        }
    }
}
