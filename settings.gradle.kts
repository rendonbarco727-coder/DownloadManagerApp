pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DownloadManagerApp"

include(":app")
include(":domain")
include(":data")
include(":core:core-common")
include(":core:core-network")
include(":core:core-database")
include(":core:core-ui")
include(":core:core-downloader")
include(":feature:feature-browser")
include(":feature:feature-downloads")
include(":feature:feature-history")
include(":feature:feature-settings")
include(":feature:feature-media")
