package com.bmo.downloadmanager.feature.settings

import com.bmo.downloadmanager.domain.repository.SettingsRepository
import com.bmo.downloadmanager.domain.usecase.settings.DeleteSettingUseCase
import com.bmo.downloadmanager.domain.usecase.settings.GetAllSettingsUseCase
import com.bmo.downloadmanager.domain.usecase.settings.GetSettingUseCase
import com.bmo.downloadmanager.domain.usecase.settings.SetSettingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    fun provideGetAllSettingsUseCase(
        repository: SettingsRepository,
    ): GetAllSettingsUseCase = GetAllSettingsUseCase(repository)

    @Provides
    fun provideGetSettingUseCase(
        repository: SettingsRepository,
    ): GetSettingUseCase = GetSettingUseCase(repository)

    @Provides
    fun provideSetSettingUseCase(
        repository: SettingsRepository,
    ): SetSettingUseCase = SetSettingUseCase(repository)

    @Provides
    fun provideDeleteSettingUseCase(
        repository: SettingsRepository,
    ): DeleteSettingUseCase = DeleteSettingUseCase(repository)
}
