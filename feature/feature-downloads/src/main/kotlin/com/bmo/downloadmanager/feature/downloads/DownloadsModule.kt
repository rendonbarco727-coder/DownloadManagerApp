package com.bmo.downloadmanager.feature.downloads

import com.bmo.downloadmanager.domain.repository.DownloadRepository
import com.bmo.downloadmanager.domain.usecase.download.DeleteDownloadUseCase
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadsModule {

    @Provides
    @Singleton
    fun provideGetDownloadsUseCase(repo: DownloadRepository): GetDownloadsUseCase =
        GetDownloadsUseCase(repo)

    @Provides
    @Singleton
    fun provideDeleteDownloadUseCase(repo: DownloadRepository): DeleteDownloadUseCase =
        DeleteDownloadUseCase(repo)
}
