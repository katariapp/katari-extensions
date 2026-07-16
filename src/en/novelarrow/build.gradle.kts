plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

@Suppress("UNCHECKED_CAST")
val configureSharedExtensionModule = rootProject.extra["configureSharedExtensionModule"] as (Project) -> Unit

configureSharedExtensionModule(project)

android {
    defaultConfig {
        versionCode = 2
        versionName = "2.1.1"
        applicationId = "eu.kanade.tachiyomi.extension.en.novelarrow"

        manifestPlaceholders += mapOf(
            "appName" to "Katari: NovelArrow",
            "extClass" to ".NovelArrowFactory",
            "nsfw" to 1,
        )
    }

    sourceSets {
        getByName("test") {
            java.setSrcDirs(listOf("test"))
            resources.setSrcDirs(listOf("test-resources"))
        }
    }
}

dependencies {
    val katariSourceApiVersion = rootProject.extra["katariSourceApiVersion"] as String

    testImplementation("com.github.katariapp.katari:entry-source-api:$katariSourceApiVersion") {
        exclude(group = "com.github.katariapp.katari", module = "core-common")
    }
    testImplementation(libs.jsoup)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.core)
    testImplementation(kotlin("test-junit"))
}
