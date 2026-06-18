package com.bmo.downloadmanager.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Implementación de producción de DispatcherProvider. Delega directamente
 * a los Dispatchers estándar de kotlinx.coroutines.
 *
 * Esta es la única clase del proyecto que debería importar
 * kotlinx.coroutines.Dispatchers directamente. Todo lo demás depende de
 * DispatcherProvider.
 */
class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate
}
