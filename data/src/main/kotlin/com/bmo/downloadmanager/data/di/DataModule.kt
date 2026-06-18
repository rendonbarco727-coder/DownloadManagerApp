package com.bmo.downloadmanager.data.di

import com.bmo.downloadmanager.data.repository.BrowserHistoryRepositoryImpl
import com.bmo.downloadmanager.data.repository.BrowserTabRepositoryImpl
import com.bmo.downloadmanager.data.repository.DownloadRepositoryImpl
import com.bmo.downloadmanager.data.repository.SettingsRepositoryImpl
import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository
import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import com.bmo.downloadmanager.domain.repository.DownloadRepository
import com.bmo.downloadmanager.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindBrowserHistoryRepository(
        impl: BrowserHistoryRepositoryImpl
    ): BrowserHistoryRepository

    @Binds
    @Singleton
    abstract fun bindBrowserTabRepository(
        impl: BrowserTabRepositoryImpl
    ): BrowserTabRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
