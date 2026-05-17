package com.autobox.monitor.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autobox.monitor.repository.MonitorRepository
import com.autobox.monitor.ui.MonitorViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorViewModelFactory @Inject constructor(
    private val repository: MonitorRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        check(modelClass.isAssignableFrom(MonitorViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MonitorViewModel(repository) as T
    }
}
