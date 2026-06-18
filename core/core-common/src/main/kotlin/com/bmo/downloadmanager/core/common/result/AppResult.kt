package com.bmo.downloadmanager.core.common.result

/**
 * Resultado de cualquier operación que cruce capas (UseCase -> ViewModel,
 * Repository -> UseCase). Evita propagar excepciones a través de límites
 * de capa y obliga al llamador a manejar explícitamente el caso de error
 * en tiempo de compilación.
 *
 * Distinto de ApiResult<T> (en core-network): ApiResult representa el
 * resultado crudo de una llamada de red, con sus propios tipos de error
 * de transporte HTTP. AppResult es el resultado ya traducido a la
 * semántica del dominio de la app (por ejemplo, un error de red se
 * traduce a AppError.Network antes de llegar a domain). La capa data es
 * responsable de esa traducción.
 */
sealed class AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>()

    data class Error(val error: AppError) : AppResult<Nothing>()

    /** Útil para operaciones en curso cuyo estado se observa vía Flow. */
    object Loading : AppResult<Nothing>()
}

/**
 * Errores de dominio, independientes de su causa técnica original
 * (excepción de red, de base de datos, de I/O de archivos, etc.).
 * Las capas superiores (ViewModel, UI) deciden cómo presentar cada caso
 * sin necesitar conocer detalles de implementación de la capa data.
 */
sealed class AppError {
    data class Network(val cause: Throwable? = null) : AppError()
    data class Database(val cause: Throwable? = null) : AppError()
    data class Storage(val cause: Throwable? = null) : AppError()
    data class NotFound(val identifier: String) : AppError()
    data class Validation(val message: String) : AppError()
    data class Unknown(val cause: Throwable? = null) : AppError()
}

/** Transforma el dato exitoso sin tocar el caso de error. */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
    is AppResult.Loading -> AppResult.Loading
}

/** Ejecuta una acción solo si el resultado fue exitoso; retorna el mismo AppResult para encadenar. */
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

/** Ejecuta una acción solo si el resultado fue un error; retorna el mismo AppResult para encadenar. */
inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(error)
    return this
}
