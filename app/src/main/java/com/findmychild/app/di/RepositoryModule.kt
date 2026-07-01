package com.findmychild.app.di

import com.findmychild.app.data.repository.LocationRepositoryImpl
import com.findmychild.app.data.repository.PairingRepositoryImpl
import com.findmychild.app.data.repository.SettingsRepositoryImpl
import com.findmychild.app.data.repository.SyncRepositoryImpl
import com.findmychild.app.domain.repository.LocationRepository
import com.findmychild.app.domain.repository.PairingRepository
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindPairingRepository(impl: PairingRepositoryImpl): PairingRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
