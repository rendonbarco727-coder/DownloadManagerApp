package com.bmo.downloadmanager.feature.media

import com.bmo.downloadmanager.domain.repository.DownloadRepository
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadByIdUseCase
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideGetDownloadsUseCase(repo: DownloadRepository): GetDownloadsUseCase =
        GetDownloadsUseCase(repo)

    @Provides
    @Singleton
    fun provideGetDownloadByIdUseCase(repo: DownloadRepository): GetDownloadByIdUseCase =
        GetDownloadByIdUseCase(repo)
}
