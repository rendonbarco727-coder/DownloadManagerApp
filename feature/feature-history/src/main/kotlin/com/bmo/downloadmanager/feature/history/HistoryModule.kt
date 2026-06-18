package com.bmo.downloadmanager.feature.history

import com.bmo.downloadmanager.domain.repository.BrowserHistoryRepository
import com.bmo.downloadmanager.domain.usecase.browser.ClearAllBrowserHistoryUseCase
import com.bmo.downloadmanager.domain.usecase.browser.DeleteBrowserHistoryEntryUseCase
import com.bmo.downloadmanager.domain.usecase.browser.GetBrowserHistoryUseCase
import com.bmo.downloadmanager.domain.usecase.browser.SearchBrowserHistoryUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object HistoryModule {

    @Provides
    fun provideGetBrowserHistoryUseCase(
        repository: BrowserHistoryRepository,
    ): GetBrowserHistoryUseCase = GetBrowserHistoryUseCase(repository)

    @Provides
    fun provideInsertBrowserHistoryUseCase(
        repository: BrowserHistoryRepository,
    ): com.bmo.downloadmanager.domain.usecase.browser.InsertBrowserHistoryUseCase =
        com.bmo.downloadmanager.domain.usecase.browser.InsertBrowserHistoryUseCase(repository)

    @Provides
    fun provideDeleteBrowserHistoryEntryUseCase(
        repository: BrowserHistoryRepository,
    ): DeleteBrowserHistoryEntryUseCase = DeleteBrowserHistoryEntryUseCase(repository)

    @Provides
    fun provideClearAllBrowserHistoryUseCase(
        repository: BrowserHistoryRepository,
    ): ClearAllBrowserHistoryUseCase = ClearAllBrowserHistoryUseCase(repository)

    @Provides
    fun provideSearchBrowserHistoryUseCase(
        repository: BrowserHistoryRepository,
    ): SearchBrowserHistoryUseCase = SearchBrowserHistoryUseCase(repository)
}
