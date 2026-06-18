package com.bmo.downloadmanager.core.common.dispatcher

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Conecta la interfaz DispatcherProvider con su implementación de producción
 * para que Hilt pueda inyectarla en cualquier punto del grafo de
 * dependencias (UseCase, Repository, ViewModel, Worker).
 *
 * @Binds en lugar de @Provides porque DefaultDispatcherProvider ya tiene
 * un constructor @Inject sin parámetros adicionales que resolver; @Binds
 * es la forma idiomática y más liviana para ese caso.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DispatcherModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        impl: DefaultDispatcherProvider
    ): DispatcherProvider
}
