pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "CentillionExample"
include(":centillion-sdk", ":app")

project(":centillion-sdk").projectDir = file("vendor/centillion-android-sdk/centillion-sdk")
