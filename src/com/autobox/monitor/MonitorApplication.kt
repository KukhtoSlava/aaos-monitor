package com.autobox.monitor

import android.app.Application
import com.autobox.monitor.di.AppComponent
import com.autobox.monitor.di.DaggerAppComponent

class MonitorApplication : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(applicationContext)
    }
}
