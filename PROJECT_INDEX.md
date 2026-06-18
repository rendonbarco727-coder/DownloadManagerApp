# PROJECT_INDEX.md
## Gestor de Descargas Avanzado con Navegador Integrado (Android)

> Este archivo es el registro maestro del proyecto. Se actualiza después de cada módulo entregado. Antes de generar nuevo código, este archivo debe revisarse para evitar duplicación, referencias rotas o incoherencias arquitectónicas.

**Última actualización:** Paso 1 — Planificación inicial (módulo 0, sin código de implementación)
**Estado general:** Arquitectura y stack definidos, archivos Gradle raíz creados. Pendiente confirmación para iniciar Módulo 1.

---

## 1. Stack tecnológico fijado

| Componente | Versión | Estado |
|---|---|---|
| Kotlin | 2.2.0 | Estable |
| AGP | 9.2.0 | Estable, con Kotlin built-in habilitado |
| Gradle | 8.11+ | Estable |
| Compose BOM | 2026.05.01 | Estable |
| Material 3 | 1.4.0 (vía BOM) | Estable |
| Hilt | androidx.hilt 1.3.0 + dagger.hilt 2.57.1 | Estable |
| Room | 2.8.4 | Estable (no androidx.room3, en alpha) |
| OkHttp | 5.1.0 | Estable |
| Retrofit | 3.0.0 | Estable (suspend nativo) |
| Coroutines | 1.11.0 | Estable |
| WorkManager | 2.10.1 | Estable |
| DataStore | 1.2.1 | Estable |
| FFmpeg | com.antonkarpenko:ffmpeg-kit-https:2.1.0 | Fork comunitario activo, LGPL |
| KSP | 2.2.0-1.0.30 | Estable, reemplaza kapt |

## 2. Módulos completados
Ninguno todavía.

## 3. Módulos pendientes (orden propuesto)
1. core-common + core-network + core-database + core-ui
2. domain
3. data
4. feature-downloads
5. feature-browser
6. feature-history
7. feature-media
8. feature-settings
9. app
10. Tests de integración cruzados + UI tests finales

## 4. Archivos creados
- settings.gradle.kts
- build.gradle.kts (raíz)
- gradle.properties
- gradle/libs.versions.toml
- PROJECT_INDEX.md
- Estructura de carpetas de los 12 módulos (vacías, sin código Kotlin ni build.gradle.kts por módulo todavía)

## 5. Dependencias declaradas
Ver gradle/libs.versions.toml. Todavía no aplicadas a ningún build.gradle.kts de módulo (eso ocurre en Módulo 1).

## 6. Interfaces públicas existentes
Ninguna todavía.

## 7. Clases existentes
Ninguna todavía.

## 8. Decisiones arquitectónicas registradas
- Clean Architecture + MVVM + Repository Pattern, domain puro en Kotlin sin Android SDK.
- KSP en lugar de kapt para Hilt y Room.
- Sin MANAGE_EXTERNAL_STORAGE: MediaStore + Scoped Storage exclusivamente.
- Desarrollo y pruebas: recursos locales, archivos de ejemplo, servidores mock embebidos. Sin dependencia de sitios externos. La capa data es la única consciente del origen de los datos; domain y feature-* lo desconocen, permitiendo conectar fuentes reales después sin cambios estructurales.
- ffmpeg-kit-https (no GPL) para mantener licencia LGPL en todo el proyecto.
- Ningún feature-* depende de otro feature-*; toda comunicación cruzada pasa por domain o por app.

## 9. Pendientes críticos antes de Módulo 1
- Confirmación explícita del usuario para iniciar Módulo 1.

## 9. Correcciones de versión post-Paso 1 (descubiertas al generar el wrapper real)

Al ejecutar `gradle wrapper` (que evalúa `build.gradle.kts` y por lo tanto resuelve todos los plugins declarados), se detectaron tres errores de versión en el stack original del Paso 1, todos corregidos antes de seguir:

1. **KSP `2.2.0-1.0.30` no existe.** El formato correcto de versión de KSP para Kotlin 2.2.0 en ese momento era `2.2.0-2.0.2`. Corregido en un primer paso intermedio.
2. **Hilt `2.57.1` tiene un POM roto** (bug confirmado en `google/dagger#4937`: declara una dependencia inexistente `kotlin-stdlib:KOTLIN_2_0`), y además **Hilt 2.57.x/2.58.x no es compatible con AGP 9.2.0** según la guía oficial de Google para migración a AGP 9.
3. **Hilt 2.59 (sin el `.2`) tiene un bug distinto con AGP 9.0.0** (`google/dagger#5099`: genera código que referencia `ComponentTreeDeps`, clase que no existe en ningún artefacto runtime de esa versión).

**Resolución final, según la skill oficial "AGP 9 migration" de Android Developers:** con AGP 9+, Hilt debe ser **2.59.2 o superior** y KSP debe ser **2.3.6 o superior**. KSP, desde su versión 2.3.0, ya no está atado a una versión específica de Kotlin (cambio de esquema de versionado confirmado por el equipo de KSP), por lo que KSP 2.3.6 es compatible con Kotlin 2.2.0 sin necesidad de subir la versión de Kotlin.

**Valores finales corregidos en `gradle/libs.versions.toml`:**
- `ksp = "2.3.6"` (era `2.2.0-1.0.30`, inexistente)
- `hiltAndroid = "2.59.2"` (era `2.57.1`, con POM roto y sin soporte AGP 9)
- `hiltAndroidx = "1.3.0"` — verificado por separado y confirmado compatible con AGP 9.2.0 y Dagger-Hilt 2.59.2. No requirió cambio.

**Stack final verificado, sin pendientes:** `kotlin 2.2.0` / `agp 9.2.0` / `ksp 2.3.6` / `hiltAndroid 2.59.2` / `hiltAndroidx 1.3.0`.

**Nota adicional (sin impacto en este proyecto):** con AGP 9, el plugin Kotlin Multiplatform deja de ser compatible con `com.android.application`/`com.android.library` en el mismo subproyecto. Este proyecto no usa KMP, así que no aplica, pero queda registrado por si se planteara en el futuro.

## 10. Wrapper de Gradle generado

- `./gradlew` generado vía `gradle wrapper --gradle-version 8.13 --distribution-type bin` (usando Gradle 8.5 local solo como herramienta de arranque).
- `gradle/wrapper/gradle-wrapper.properties` apunta a Gradle 8.13, distribución `bin`.
- Wrapper generado ANTES de la corrección de Hilt/KSP, pero sigue siendo válido: la generación del wrapper solo depende de que los plugins declarados sean *resolvibles* en algún repositorio, no de que compilen correctamente con código real. Las correcciones de versión no invalidan el wrapper ya generado.

## 11. Pendientes críticos antes de Módulo 1

- Confirmación explícita del usuario para iniciar Módulo 1.
- Crear `app/build.gradle.kts` y el resto de `build.gradle.kts` por módulo (ninguno existe todavía; solo el raíz).
