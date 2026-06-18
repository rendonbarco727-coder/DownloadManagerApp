package com.bmo.downloadmanager.core.network.di

import com.bmo.downloadmanager.core.network.client.DefaultNetworkClientProvider
import com.bmo.downloadmanager.core.network.client.NetworkClientProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNetworkClientProvider(
        impl: DefaultNetworkClientProvider
    ): NetworkClientProvider
}
