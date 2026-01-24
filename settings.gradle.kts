@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven { url=uri("https://maven.aliyun.com/repository/public") }
        maven { url=uri("https://maven.aliyun.com/repository/central") }
        maven { url=uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://jitpack.io") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()

        maven{ url =uri ("https://oss.sonatype.org/content/repositories/snapshots")}
    }

}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url=uri("https://maven.aliyun.com/repository/public") }
        maven { url=uri("https://maven.aliyun.com/repository/google") }
        maven { url=uri("https://maven.aliyun.com/repository/central") }
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()

        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }

    }
}

rootProject.name = "music"
include(":app")
