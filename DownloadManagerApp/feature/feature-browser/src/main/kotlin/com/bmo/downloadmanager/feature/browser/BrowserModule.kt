package com.bmo.downloadmanager.feature.browser

import com.bmo.downloadmanager.domain.repository.BrowserTabRepository
import com.bmo.downloadmanager.domain.usecase.tab.DeleteBrowserTabUseCase
import com.bmo.downloadmanager.domain.usecase.tab.GetBrowserTabsUseCase
import com.bmo.downloadmanager.domain.usecase.tab.InsertBrowserTabUseCase
import com.bmo.downloadmanager.domain.usecase.tab.UpdateBrowserTabUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserModule {

    @Provides @Singleton
    fun provideGetBrowserTabsUseCase(repo: BrowserTabRepository): GetBrowserTabsUseCase =
        GetBrowserTabsUseCase(repo)

    @Provides @Singleton
    fun provideInsertBrowserTabUseCase(repo: BrowserTabRepository): InsertBrowserTabUseCase =
        InsertBrowserTabUseCase(repo)

    @Provides @Singleton
    fun provideUpdateBrowserTabUseCase(repo: BrowserTabRepository): UpdateBrowserTabUseCase =
        UpdateBrowserTabUseCase(repo)

    @Provides @Singleton
    fun provideDeleteBrowserTabUseCase(repo: BrowserTabRepository): DeleteBrowserTabUseCase =
        DeleteBrowserTabUseCase(repo)
}
