package com.bmo.downloadmanager.core.downloader

import com.bmo.downloadmanager.core.network.client.NetworkClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Provee las dependencias propias de core-downloader.
 *
 * El OkHttpClient para descargas reusa NetworkClientProvider.baseClient()
 * en lugar de instanciar un cliente nuevo, para no perder el connection
 * pooling ya configurado ahí (timeouts, interceptors comunes). Si en el
 * futuro algún CDN específico requiere headers por dominio durante la
 * descarga de segmentos, ese cliente se obtiene con
 * NetworkClientProvider.clientForDomain() en el punto de uso, no aquí.
 *
 * No se provee Context aquí: Hilt ya expone @ApplicationContext Context de
 * forma estándar (ver AndroidEntryPointModules), así que DownloadManager y
 * el Service lo piden directamente con ese calificador en su constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object DownloaderModule {

    @Provides
    @Singleton
    fun provideDownloaderOkHttpClient(
        networkClientProvider: NetworkClientProvider,
    ): OkHttpClient = networkClientProvider.baseClient()
}
