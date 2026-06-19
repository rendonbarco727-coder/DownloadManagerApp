package com.bmo.downloadmanager.feature.media

import com.bmo.downloadmanager.domain.repository.DownloadRepository
import com.bmo.downloadmanager.domain.usecase.download.GetDownloadByIdUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Solo provee use cases que NO están ya en DownloadsModule.
 * GetDownloadsUseCase la provee DownloadsModule — no duplicar.
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideGetDownloadByIdUseCase(repo: DownloadRepository): GetDownloadByIdUseCase =
        GetDownloadByIdUseCase(repo)
}
