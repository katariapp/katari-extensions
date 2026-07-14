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
        versionCode = 1
        versionName = "2.0.0"
        applicationId = "eu.kanade.tachiyomi.extension.en.gutenberg"

        manifestPlaceholders += mapOf(
            "appName" to "Katari: Project Gutenberg",
            "extClass" to ".GutenbergFactory",
            "nsfw" to 0,
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
    val katariSourceApiVersion = providers.gradleProperty("katariSourceApiVersion")
        .orElse("local-SNAPSHOT")
        .get()

    testImplementation("com.github.katariapp.katari:entry-source-api:$katariSourceApiVersion") {
        exclude(group = "com.github.katariapp.katari", module = "core-common")
    }
    testImplementation(libs.jsoup)
    testImplementation(libs.junit)
    testImplementation(kotlin("test-junit"))
}
