package com.autobox.monitor.di

import com.autobox.monitor.repository.MonitorRepository
import com.autobox.monitor.repository.impl.MonitorRepositoryImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMonitorRepository(impl: MonitorRepositoryImpl): MonitorRepository
}
