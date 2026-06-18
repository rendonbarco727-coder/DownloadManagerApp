package com.bmo.downloadmanager.core.network.client

import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Punto único para obtener clientes de red configurados.
 *
 * Por qué no es simplemente "un OkHttpClient singleton": los CDNs de
 * descarga frecuentemente exigen un header Referer específico por
 * dominio (mismo patrón ya usado manualmente en otros proyectos del
 * autor con vimeos/digitalsun). Construir un OkHttpClient nuevo por cada
 * descarga sería costoso (pierde connection pooling); este provider
 * resuelve eso entregando un cliente base reusado, más un mecanismo para
 * adjuntar headers específicos de dominio sin perder el pool de
 * conexiones del cliente base.
 *
 * baseClient(): cliente OkHttp genérico, con interceptors comunes
 * (logging, timeouts) ya configurados. Usado por Retrofit para llamadas a
 * APIs normales (JSON).
 *
 * clientForDomain(): cliente derivado de baseClient() vía newBuilder(),
 * con un interceptor que inyecta el header Referer/User-Agent requerido
 * por ese dominio. Usado por el motor de descarga de segmentos cuando el
 * CDN de destino lo exige.
 *
 * retrofit(): instancia de Retrofit ya configurada con baseClient() y el
 * converter de JSON, lista para crear interfaces de servicio con
 * retrofit.create(Service::class.java).
 */
interface NetworkClientProvider {

    fun baseClient(): OkHttpClient

    fun clientForDomain(domainConfig: DomainHeaderConfig): OkHttpClient

    fun retrofit(baseUrl: String): Retrofit
}

/**
 * Configuración de headers requeridos por un dominio/CDN específico.
 * referer es nullable porque no todos los CDNs lo exigen (ver
 * distinción ya documentada entre vimeos.net, que requiere cookies vid=,
 * y otros proveedores que no necesitan Referer en absoluto).
 */
data class DomainHeaderConfig(
    val domain: String,
    val referer: String? = null,
    val userAgent: String? = null,
    val extraHeaders: Map<String, String> = emptyMap()
)
