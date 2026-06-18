package com.bmo.downloadmanager.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Abstrae los CoroutineDispatcher usados en toda la app.
 *
 * Por qué existe: si cada clase llama directamente a Dispatchers.IO o
 * Dispatchers.Default, los tests unitarios no pueden sustituirlos por un
 * TestDispatcher determinista. Inyectando esta interfaz vía Hilt, los tests
 * usan una implementación fake que corre todo en el dispatcher de test,
 * eliminando flakiness por concurrencia real.
 *
 * Uso típico: cualquier UseCase o Repository que lance corrutinas pide
 * DispatcherProvider por constructor en lugar de importar Dispatchers
 * directamente.
 */
interface DispatcherProvider {
    /** Operaciones de I/O bloqueante: red, disco, base de datos. */
    val io: CoroutineDispatcher

    /** Trabajo de CPU intensivo: parsing, cálculos, procesamiento de listas grandes. */
    val default: CoroutineDispatcher

    /** Actualizaciones de UI. Coincide con Dispatchers.Main en producción. */
    val main: CoroutineDispatcher

    /** Variante inmediata de main, sin reencolar si ya se está en el hilo principal. */
    val mainImmediate: CoroutineDispatcher
}
