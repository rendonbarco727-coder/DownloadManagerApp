package com.bmo.downloadmanager.core.network.result

/**
 * Resultado crudo de una llamada de red, antes de traducirse a la
 * semántica de dominio. Vive en core-network y NO debería usarse fuera de
 * la capa data: domain y feature-* trabajan con AppResult (core-common).
 *
 * La distinción importa porque ApiResult conoce conceptos de transporte
 * (código HTTP, body de error crudo) que domain no debería conocer.
 * Es responsabilidad de los Repository en :data traducir ApiResult a
 * AppResult.
 */
sealed class ApiResult<out T> {

    data class Success<out T>(val data: T, val code: Int) : ApiResult<T>()

    /** El servidor respondió, pero con un código de error (4xx/5xx). */
    data class HttpError(
        val code: Int,
        val errorBody: String?
    ) : ApiResult<Nothing>()

    /** No hubo respuesta del servidor: sin conexión, timeout, DNS, host inalcanzable. */
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()

    /** La respuesta llegó pero no se pudo parsear al tipo esperado. */
    data class SerializationError(val cause: Throwable) : ApiResult<Nothing>()

    data class UnknownError(val cause: Throwable) : ApiResult<Nothing>()
}

/**
 * Envuelve una llamada suspend de Retrofit en un ApiResult, capturando
 * las excepciones típicas de OkHttp/Retrofit y clasificándolas. Evita
 * repetir el mismo try/catch en cada método de cada interfaz de servicio.
 */
suspend inline fun <T> safeApiCall(crossinline call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(data = call(), code = 200)
    } catch (e: retrofit2.HttpException) {
        ApiResult.HttpError(code = e.code(), errorBody = e.response()?.errorBody()?.string())
    } catch (e: java.io.IOException) {
        ApiResult.NetworkError(cause = e)
    } catch (e: com.google.gson.JsonParseException) {
        ApiResult.SerializationError(cause = e)
    } catch (e: Exception) {
        ApiResult.UnknownError(cause = e)
    }
}
