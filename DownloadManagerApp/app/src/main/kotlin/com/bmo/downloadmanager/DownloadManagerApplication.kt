package com.bmo.downloadmanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada de la aplicación. La anotación @HiltAndroidApp dispara la
 * generación del componente raíz de Hilt, del cual dependen todos los demás
 * componentes (Activity, ViewModel, Worker, etc.) en el resto de los módulos.
 */
@HiltAndroidApp
class DownloadManagerApplication : Application()
